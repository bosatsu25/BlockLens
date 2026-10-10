package dev.blocklens.testing;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Test-only observation of a stalled external-Masa lifecycle; never changes test outcomes. */
public final class ExternalLifecycleDiagnostics implements AutoCloseable {
    public static final String FILE_NAME = "external-lifecycle-diagnostics.txt";
    private static final int MAX_THREADS = 16;
    private static final int MAX_FRAMES = 24;
    private static final int MAX_BYTES = 65536;
    private final Object lock = new Object();
    private final Path output;
    private final long stallNanos;
    private final long started = System.nanoTime();
    private final ArrayDeque<String> history = new ArrayDeque<>();
    private final ScheduledThreadPoolExecutor executor;
    private final ScheduledFuture<?> watchdog;
    private Phase phase = new Phase("started", started);
    private boolean closed;
    private boolean captured;

    public static ExternalLifecycleDiagnostics open(Path output, boolean enabled) {
        return new ExternalLifecycleDiagnostics(output, enabled, 60_000);
    }

    ExternalLifecycleDiagnostics(Path output, boolean enabled, long stallMillis) {
        this.output = Objects.requireNonNull(output);
        if (stallMillis <= 0) throw new IllegalArgumentException("Positive diagnostic delay required");
        stallNanos = TimeUnit.MILLISECONDS.toNanos(stallMillis);
        if (!enabled) {
            executor = null;
            watchdog = null;
            return;
        }
        executor = new ScheduledThreadPoolExecutor(1, action -> {
            Thread thread = new Thread(action, "BlockLens external lifecycle watchdog");
            thread.setDaemon(true);
            return thread;
        });
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        watchdog = executor.scheduleWithFixedDelay(this::observeStall, stallMillis,
                Math.min(stallMillis, 1000), TimeUnit.MILLISECONDS);
    }

    public void phase(String name) {
        if (executor == null) return;
        if (!name.matches("[a-z][a-z0-9_]{0,63}")) throw new IllegalArgumentException("Invalid diagnostic phase");
        synchronized (lock) {
            if (closed) return;
            long now = System.nanoTime();
            phase = new Phase(name, now);
            if (history.size() == 32) history.removeFirst();
            history.addLast(TimeUnit.NANOSECONDS.toMillis(now - started) + ":" + name);
        }
        System.out.println("BLOCKLENS_EXTERNAL_LIFECYCLE phase=" + name);
    }

    private void observeStall() {
        Phase stalled;
        List<String> phases;
        long elapsed;
        synchronized (lock) {
            elapsed = System.nanoTime() - phase.since;
            if (closed || captured || elapsed < stallNanos) return;
            captured = true;
            stalled = phase;
            phases = List.copyOf(history);
        }
        try {
            BoundedText text = new BoundedText();
            text.line("schema=1");
            text.line("status=diagnostic-only");
            text.line("stalledPhase=" + stalled.name);
            text.line("stallMillis=" + TimeUnit.NANOSECONDS.toMillis(elapsed));
            text.line("snapshotCount=1");
            text.line("threadLimit=" + MAX_THREADS);
            text.line("frameLimit=" + MAX_FRAMES);
            phases.forEach(value -> text.line("phase=" + value));
            var bean = ManagementFactory.getThreadMXBean();
            // First inspect names only; request bounded stacks only for selected lifecycle threads.
            long[] selected = Arrays.stream(bean.getThreadInfo(bean.getAllThreadIds(), 0))
                    .filter(Objects::nonNull).filter(info -> priority(info.getThreadName()) < 9)
                    .sorted(Comparator.comparingInt((ThreadInfo info) -> priority(info.getThreadName()))
                            .thenComparingLong(ThreadInfo::getThreadId))
                    .limit(MAX_THREADS).mapToLong(ThreadInfo::getThreadId).toArray();
            for (ThreadInfo info : bean.getThreadInfo(selected, MAX_FRAMES)) {
                if (info == null) continue;
                text.line("thread=" + info.getThreadName());
                text.line("id=" + info.getThreadId() + " state=" + info.getThreadState()
                        + " lock=" + info.getLockName());
                text.line("lockOwnerId=" + info.getLockOwnerId() + " lockOwner=" + info.getLockOwnerName());
                for (StackTraceElement frame : info.getStackTrace()) text.line("at=" + frame);
            }
            writeAtomically(text.finish());
            System.out.println("BLOCKLENS_EXTERNAL_LIFECYCLE snapshot=written phase=" + stalled.name);
        } catch (IOException | RuntimeException exception) {
            System.out.println("BLOCKLENS_EXTERNAL_LIFECYCLE snapshot=failed type="
                    + exception.getClass().getSimpleName());
        } finally {
            executor.shutdown();
        }
    }

    private static int priority(String name) {
        if (name.equals("Render thread")) return 0;
        if (name.equals("Test thread")) return 1;
        if (name.equals("Server thread")) return 2;
        if (name.startsWith("Netty Local Client IO")) return 3;
        if (name.startsWith("Netty Local Server IO")) return 4;
        if (name.startsWith("Litematica Placement Manager")) return 5;
        if (name.startsWith("Litematica ")) return 6;
        if (name.startsWith("Worker-Main-") || name.startsWith("Chunk Render")
                || name.startsWith("IO-Worker-")) return 7;
        return 9;
    }

    private void writeAtomically(String text) throws IOException {
        Files.createDirectories(output);
        Path temporary = Files.createTempFile(output, "external-lifecycle-", ".tmp");
        try {
            Files.writeString(temporary, text, StandardCharsets.US_ASCII);
            Files.move(temporary, output.resolve(FILE_NAME),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public void close() {
        synchronized (lock) { closed = true; }
        if (watchdog != null) watchdog.cancel(false);
        if (executor != null) executor.shutdown();
        // No joins, target-thread interrupts, forced exits, or changes to GameTest completion.
    }

    private record Phase(String name, long since) { }

    private static final class BoundedText {
        private final StringBuilder value = new StringBuilder();
        private boolean truncated;

        void line(String line) {
            String safe = line.substring(0, Math.min(line.length(), 160)).replaceAll("[^ -~]", "?");
            truncated |= safe.length() < line.length();
            if (value.length() + safe.length() + 1 > MAX_BYTES - 32) truncated = true;
            else value.append(safe).append('\n');
        }

        String finish() { return value + "truncated=" + truncated + "\n"; }
    }
}

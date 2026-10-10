package dev.blocklens.testing;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExternalLifecycleDiagnosticsTest {
    @TempDir Path directory;

    @Test
    void disabledDiagnosticsCreateNoThreadOrFile() {
        try (var diagnostics = new ExternalLifecycleDiagnostics(directory, false, 30)) {
            diagnostics.phase("world_close");
            assertEquals(0, watchdogCount());
            assertFalse(Files.exists(directory.resolve(ExternalLifecycleDiagnostics.FILE_NAME)));
        }
    }

    @Test
    void stalledPhaseProducesOneAtomicBoundedSnapshotIncludingNettyWaiter() throws Exception {
        CountDownLatch waiting = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        String threadName = "Netty Local Client IO #blocklens-regression" + "_".repeat(200);
        Thread netty = new Thread(() -> {
            waiting.countDown();
            try { release.await(); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
        }, threadName);
        netty.setDaemon(true);
        netty.start();
        assertTrue(waiting.await(1, TimeUnit.SECONDS));
        try (var diagnostics = new ExternalLifecycleDiagnostics(directory, true, 30)) {
            Path file = directory.resolve(ExternalLifecycleDiagnostics.FILE_NAME);
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!Files.exists(file) && System.nanoTime() < deadline) Thread.sleep(10);
            assertTrue(Files.isRegularFile(file), "watchdog must publish its atomic snapshot");
            byte[] first = Files.readAllBytes(file);
            String text = new String(first, StandardCharsets.US_ASCII);
            assertTrue(first.length <= 65536);
            assertTrue(text.contains("stalledPhase=started\n"));
            assertTrue(text.contains("snapshotCount=1\n"));
            assertTrue(text.contains(("thread=" + threadName).substring(0, 160) + "\n"));
            assertFalse(text.contains(threadName), "long thread names must be bounded");
            assertTrue(text.endsWith("truncated=true\n"));
            int threads = 0, frames = 0;
            for (String line : text.lines().toList()) {
                if (line.startsWith("thread=")) { threads++; frames = 0; }
                if (line.startsWith("at=")) assertTrue(++frames <= 24);
            }
            assertTrue(threads > 0 && threads <= 16);
            try (var files = Files.list(directory)) { assertEquals(1, files.count(), "no partial temporary file"); }
            diagnostics.phase("another_phase");
            Thread.sleep(100);
            assertArrayEquals(first, Files.readAllBytes(file), "only one snapshot may be written");
        } finally {
            release.countDown();
        }
    }

    @Test
    void closeCancelsDaemonWithoutWaitingForTheDeadline() throws Exception {
        var diagnostics = new ExternalLifecycleDiagnostics(directory, true, 60_000);
        diagnostics.phase("world_close");
        assertTrue(Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().equals("BlockLens external lifecycle watchdog"))
                .allMatch(Thread::isDaemon));
        long start = System.nanoTime();
        diagnostics.close();
        diagnostics.close();
        assertTrue(System.nanoTime() - start < TimeUnit.SECONDS.toNanos(1), "close must not join the watchdog");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (watchdogCount() != 0 && System.nanoTime() < deadline) Thread.sleep(10);
        assertEquals(0, watchdogCount());
        assertFalse(Files.exists(directory.resolve(ExternalLifecycleDiagnostics.FILE_NAME)));
    }

    private static long watchdogCount() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().equals("BlockLens external lifecycle watchdog")).count();
    }
}

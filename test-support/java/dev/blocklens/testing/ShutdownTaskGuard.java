package dev.blocklens.testing;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Bounded test-only server task pump. Never bundled in the product JAR. */
public final class ShutdownTaskGuard implements Runnable {
    private final LongSupplier clock;
    private final BooleanSupplier running;
    private final Consumer<BooleanSupplier> pump;
    private final long startedAt;
    private final long limitNanos;
    private volatile boolean started;
    private volatile boolean completed;
    private volatile boolean cancelled;
    private volatile boolean timedOut;
    private volatile Throwable failure;

    public ShutdownTaskGuard(LongSupplier clock, BooleanSupplier running,
            Consumer<BooleanSupplier> pump, long limitNanos) {
        if (limitNanos <= 0) throw new IllegalArgumentException("Shutdown bound must be positive");
        this.clock = Objects.requireNonNull(clock);
        this.running = Objects.requireNonNull(running);
        this.pump = Objects.requireNonNull(pump);
        this.limitNanos = limitNanos;
        this.startedAt = clock.getAsLong();
    }

    @Override public void run() {
        started = true;
        try {
            pump.accept(this::stopWaiting);
        } catch (RuntimeException | Error exception) {
            failure = exception;
            throw exception;
        } finally {
            completed = true;
        }
    }

    private boolean stopWaiting() {
        if (cancelled) return true;
        // Expiry is a failure even if halt happens after the allowed window.
        if (clock.getAsLong() - startedAt >= limitNanos) {
            timedOut = true;
            return true;
        }
        return !running.getAsBoolean();
    }

    public void cancel() { cancelled = true; }

    public void verifySucceeded() {
        if (failure != null) throw new AssertionError("Shutdown task failed", failure);
        if (timedOut) throw new AssertionError("Shutdown task timed out");
        if (cancelled) throw new AssertionError("Shutdown task cancelled");
        if (!started || !completed) throw new AssertionError("Shutdown task did not complete");
        if (running.getAsBoolean()) throw new AssertionError("Shutdown server still running");
    }
}

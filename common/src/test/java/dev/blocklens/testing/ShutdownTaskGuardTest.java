package dev.blocklens.testing;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ShutdownTaskGuardTest {
    @Test void queuedShutdownRunsWhileTheOrdinaryTickWouldBeParked() {
        var running = new AtomicBoolean(true);
        var queue = new ArrayDeque<Runnable>();
        var halt = new CompletableFuture<Void>();
        var guard = new ShutdownTaskGuard(() -> 0L, running::get, stop -> {
            while (!stop.getAsBoolean()) queue.remove().run();
        }, 30);
        queue.add(guard);
        queue.add(() -> { running.set(false); halt.complete(null); });
        assertFalse(halt.isDone());
        queue.remove().run();
        assertTrue(halt.isDone(), "The halt task must run before the server parks");
        guard.verifySucceeded();
        assertTrue(queue.isEmpty());
    }

    @Test void deadlineIsFailureEvenIfPumpReturnsNormally() {
        var time = new AtomicLong();
        var guard = new ShutdownTaskGuard(time::getAndIncrement, () -> true,
                stop -> { while (!stop.getAsBoolean()) { } }, 3);
        guard.run();
        assertTrue(assertThrows(AssertionError.class, guard::verifySucceeded).getMessage().contains("timed out"));
    }

    @Test void stoppedServerCompletesWithoutPollingMoreTasks() {
        var guard = new ShutdownTaskGuard(() -> 0L, () -> false,
                stop -> assertTrue(stop.getAsBoolean()), 30);
        guard.run();
        guard.verifySucceeded();
    }

    @Test void cancelledPumpTerminatesButCannotPass() {
        var guard = new ShutdownTaskGuard(() -> 0L, () -> true,
                stop -> assertTrue(stop.getAsBoolean()), 30);
        guard.cancel();
        guard.run();
        assertTrue(assertThrows(AssertionError.class, guard::verifySucceeded).getMessage().contains("cancelled"));
    }

    @Test void cancellationDuringPumpReleasesTheWait() {
        var task = new ShutdownTaskGuard[1];
        task[0] = new ShutdownTaskGuard(() -> 0L, () -> true, stop -> {
            assertFalse(stop.getAsBoolean());
            task[0].cancel();
            assertTrue(stop.getAsBoolean());
        }, 30);
        task[0].run();
        assertThrows(AssertionError.class, task[0]::verifySucceeded);
    }

    @Test void pumpExceptionCannotBecomeSuccessfulCompletion() {
        var original = new IllegalStateException("pump failed");
        var guard = new ShutdownTaskGuard(() -> 0L, () -> true, stop -> { throw original; }, 30);
        assertSame(original, assertThrows(IllegalStateException.class, guard::run));
        assertSame(original, assertThrows(AssertionError.class, guard::verifySucceeded).getCause());
    }

    @Test void unstartedAndPrematurelyReturnedTasksFail() {
        var guard = new ShutdownTaskGuard(() -> 0L, () -> true, stop -> { }, 30);
        assertThrows(AssertionError.class, guard::verifySucceeded);
        guard.run();
        assertTrue(assertThrows(AssertionError.class, guard::verifySucceeded).getMessage().contains("still running"));
    }

    @Test void invalidDurationIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ShutdownTaskGuard(() -> 0L, () -> true, stop -> { }, 0));
    }
}

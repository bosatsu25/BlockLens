package dev.blocklens.testing;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ShutdownHandshakeTest {
    @Test void earlyOwnerLogoutCannotSkipLateClientHalt() {
        var server = new Object();
        var ownerStopped = new AtomicBoolean();
        var queue = new ArrayDeque<Runnable>();
        var session = new ShutdownHandshake<>(server, () -> 0L, stop -> {
            while (!stop.getAsBoolean()) queue.remove().run();
        }, 30);
        queue.add(() -> { ownerStopped.set(true); session.haltReturned(server, false); });
        queue.add(() -> { assertTrue(ownerStopped.get()); session.haltReturned(server, true); });
        session.pumpOnServer(server);
        session.verifySucceeded();
        assertTrue(queue.isEmpty(), "Both halt origins must be processed");
    }

    @Test void matchingServerAndClientThreadAreBothRequired() {
        var server = new Object();
        var other = new Object();
        var calls = new AtomicInteger();
        var sessions = new ShutdownHandshake<?>[1];
        var session = new ShutdownHandshake<>(server, () -> 0L, stop -> {
            calls.incrementAndGet();
            assertFalse(stop.getAsBoolean());
            sessions[0].cancel();
        }, 30);
        sessions[0] = session;
        session.haltReturned(other, true);
        session.haltReturned(server, false);
        session.pumpOnServer(other);
        assertEquals(0, calls.get());
        session.pumpOnServer(server);
        assertEquals(1, calls.get());
        assertThrows(AssertionError.class, session::verifySucceeded);
    }

    @Test void directPumpRunsOnceEvenWithRepeatedWaitHooks() {
        var server = new Object();
        var calls = new AtomicInteger();
        var session = new ShutdownHandshake<>(server, () -> 0L, stop -> calls.incrementAndGet(), 30);
        session.haltReturned(server, true);
        session.pumpOnServer(server);
        session.pumpOnServer(server);
        session.verifySucceeded();
        assertEquals(1, calls.get());
    }

    @Test void haltReturnAloneCannotPassAnUnstartedPump() {
        var server = new Object();
        var session = new ShutdownHandshake<>(server, () -> 0L, stop -> { }, 30);
        session.haltReturned(server, true);
        assertThrows(AssertionError.class, session::verifySucceeded);
    }

    @Test void prematurelyReturnedPumpCannotPassWithoutClientHalt() {
        var server = new Object();
        var session = new ShutdownHandshake<>(server, () -> 0L, stop -> { }, 30);
        session.pumpOnServer(server);
        assertThrows(AssertionError.class, session::verifySucceeded);
    }
}

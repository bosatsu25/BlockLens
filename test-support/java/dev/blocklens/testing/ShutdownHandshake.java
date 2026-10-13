package dev.blocklens.testing;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Explicit client-halt handshake for one test world; server logout is not completion. */
public final class ShutdownHandshake<T> {
    private final T target;
    private final AtomicBoolean pumpClaimed = new AtomicBoolean();
    private volatile boolean clientHaltReturned;
    private final ShutdownTaskGuard guard;

    public ShutdownHandshake(T target, LongSupplier clock, Consumer<BooleanSupplier> pump, long limitNanos) {
        this.target = Objects.requireNonNull(target);
        guard = new ShutdownTaskGuard(clock, () -> !clientHaltReturned, pump, limitNanos);
    }

    public void pumpOnServer(T server) {
        if (server == target && pumpClaimed.compareAndSet(false, true)) guard.run();
    }

    public void haltReturned(T server, boolean clientThread) {
        if (server == target && clientThread) clientHaltReturned = true;
    }

    public void cancel() { guard.cancel(); }

    public void verifySucceeded() { guard.verifySucceeded(); }
}

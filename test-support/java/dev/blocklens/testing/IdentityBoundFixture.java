package dev.blocklens.testing;

import java.util.Objects;
import java.util.function.Consumer;

/** Test-only state for a controlled fixture within one client world. Use on the client thread. */
public final class IdentityBoundFixture<T> {
    private final Object expectedScope;
    private final Consumer<? super T> initializer;
    private T bound;

    public IdentityBoundFixture(Object expectedScope, Consumer<? super T> initializer) {
        this.expectedScope = Objects.requireNonNull(expectedScope, "expectedScope");
        this.initializer = Objects.requireNonNull(initializer, "initializer");
    }

    /** Returns true when the controlled state was initialized on this call. */
    public boolean refresh(Object currentScope, T entity) {
        if (currentScope != expectedScope || entity == null || entity == bound) return false;
        initializer.accept(entity);
        bound = entity;
        return true;
    }
}

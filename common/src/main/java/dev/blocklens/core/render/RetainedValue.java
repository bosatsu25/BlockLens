package dev.blocklens.core.render;

import java.util.Objects;
import java.util.function.Function;

/** One lazy result per baked owner, including missing results, published atomically. */
public final class RetainedValue<T> {
    private volatile Snapshot<T> snapshot;

    /** The owner supplies one stable lookup identity throughout its lifetime. */
    public <K> T get(K key, Function<? super K, ? extends T> loader) {
        Objects.requireNonNull(loader, "loader");
        Snapshot<T> observed = snapshot;
        if (observed != null) return observed.value();
        synchronized (this) {
            observed = snapshot;
            if (observed == null) {
                observed = new Snapshot<>(loader.apply(key));
                snapshot = observed;
            }
            return observed.value();
        }
    }

    private record Snapshot<T>(T value) { }
}

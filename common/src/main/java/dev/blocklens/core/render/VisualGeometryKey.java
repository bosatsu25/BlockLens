package dev.blocklens.core.render;

import java.util.Objects;

/** A retained base plus its independent visual generation; callers delegate the disabled key. */
public record VisualGeometryKey(Object base, long enabledMask) {
    public VisualGeometryKey { Objects.requireNonNull(base,"base"); }
}
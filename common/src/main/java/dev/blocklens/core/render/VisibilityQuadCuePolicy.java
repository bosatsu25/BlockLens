package dev.blocklens.core.render;

import java.util.Objects;

/** Shared M4 quad instruction policy for outline/high-visibility and fine-line cues. */
public final class VisibilityQuadCuePolicy {
    private VisibilityQuadCuePolicy() {
    }

    public static DecorationQuadInstruction instruction(VisibilityRenderDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        // Preserve the active baked model/texture and only add BlockLens-owned visibility data.
        // Every M4 cue owns one immutable full-bright instruction, so the render hot path reuses a
        // bounded object instead of constructing an equivalent record for each emitted quad.
        return descriptor.cue().instruction();
    }
}

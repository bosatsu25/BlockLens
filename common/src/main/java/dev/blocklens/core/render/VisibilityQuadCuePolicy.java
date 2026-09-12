package dev.blocklens.core.render;

import java.util.Objects;

/** Shared M4 quad instruction policy for outline/high-visibility and fine-line cues. */
public final class VisibilityQuadCuePolicy {
    private VisibilityQuadCuePolicy() {
    }

    public static DecorationQuadInstruction instruction(VisibilityRenderDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        // Preserve the active baked model/texture and only add BlockLens-owned visibility data.
        // Tripwire already supplies its thin geometry; outline-family source value is represented
        // as a stable full-bright accent without bundling the source pack's replacement textures.
        return DecorationQuadInstruction.emissiveTint(descriptor.cue().accentArgb());
    }
}

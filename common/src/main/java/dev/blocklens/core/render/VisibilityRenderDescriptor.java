package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.VisibilityStateKind;
import java.util.Objects;

/** Minecraft-independent M4 visibility instruction consumed by thin version render adapters. */
public record VisibilityRenderDescriptor(
        CapabilityId capability,
        VisibilityVisualCue cue,
        SemanticState state) {

    public VisibilityRenderDescriptor {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(cue, "cue");
        Objects.requireNonNull(state, "state");
        if (!VisibilityStateKind.isVisibilityCapability(capability)) {
            throw new IllegalArgumentException("Not an M4 visibility capability: " + capability.sourceKey());
        }
    }
}

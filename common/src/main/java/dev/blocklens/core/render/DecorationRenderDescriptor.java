package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import java.util.Objects;

/** Minecraft-independent instruction consumed by version render adapters. */
public record DecorationRenderDescriptor(
        CapabilityId capability,
        DecorationVisualCue cue,
        SemanticState state) {

    public DecorationRenderDescriptor {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(cue, "cue");
        Objects.requireNonNull(state, "state");
        DecorationVisualCue expected = DecorationVisualCue.forCapability(capability);
        if (cue != expected) {
            throw new IllegalArgumentException(
                    "Visual cue " + cue + " does not match " + capability.sourceKey() + "; expected " + expected);
        }
    }

    public static DecorationRenderDescriptor of(CapabilityId capability, SemanticState state) {
        return new DecorationRenderDescriptor(
                capability,
                DecorationVisualCue.forCapability(capability),
                state);
    }
}

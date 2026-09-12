package dev.blocklens.core.render;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.VisibilityStateKind;
import java.util.Objects;
import java.util.Optional;

/** Shared feature-toggle and source-derived cue policy for M4 visibility rendering. */
public final class VisibilityRenderPolicy {
    private VisibilityRenderPolicy() {
    }

    public static Optional<VisibilityRenderDescriptor> describe(
            CapabilityId capability,
            SemanticState state,
            BlockLensConfig config) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(config, "config");

        VisibilityStateKind kind = VisibilityStateKind.forCapability(capability);
        if (kind == VisibilityStateKind.NONE || !config.isEnabled(capability)) {
            return Optional.empty();
        }

        SemanticState requiredState = Objects.requireNonNull(state, "state");
        VisibilityVisualCue cue = switch (kind) {
            case NONE -> throw new IllegalStateException("NONE visibility kind reached descriptor creation");
            case STATIC_OUTLINE -> staticOutlineCue(capability);
            case SCULK_BLOOM -> requiredState.bloom()
                    ? VisibilityVisualCue.SCULK_CATALYST_BLOOM
                    : VisibilityVisualCue.SCULK_CATALYST_IDLE;
            case TRIPWIRE -> requiredState.powered()
                    ? VisibilityVisualCue.STRING_POWERED
                    : VisibilityVisualCue.STRING_UNPOWERED;
        };
        return Optional.of(new VisibilityRenderDescriptor(capability, cue, requiredState));
    }

    private static VisibilityVisualCue staticOutlineCue(CapabilityId capability) {
        return switch (capability) {
            case BLUE_ICE -> VisibilityVisualCue.BLUE_ICE_OUTLINE;
            case DEAD_CORAL -> VisibilityVisualCue.DEAD_CORAL_OUTLINE;
            case POWDER_SNOW -> VisibilityVisualCue.POWDER_SNOW_OUTLINE;
            default -> throw new IllegalArgumentException(
                    "Capability is not a static M4 outline: " + capability.sourceKey());
        };
    }
}

package dev.blocklens.core.render;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.DecorationStateKind;
import dev.blocklens.core.state.SemanticState;
import java.util.Objects;
import java.util.Optional;

/** Shared feature-toggle and semantic validation policy for M3 decoration rendering. */
public final class DecorationRenderPolicy {
    private DecorationRenderPolicy() {
    }

    public static Optional<DecorationRenderDescriptor> describe(
            CapabilityId capability,
            SemanticState state,
            BlockLensConfig config) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(config, "config");

        DecorationStateKind kind = DecorationStateKind.forCapability(capability);
        if (kind == DecorationStateKind.NONE || !config.isEnabled(capability)) {
            return Optional.empty();
        }

        SemanticState requiredState = Objects.requireNonNull(state, "state");
        validate(kind, requiredState, capability);
        return Optional.of(DecorationRenderDescriptor.of(capability, requiredState));
    }

    private static void validate(
            DecorationStateKind kind,
            SemanticState state,
            CapabilityId capability) {
        boolean valid = switch (kind) {
            case NONE -> false;
            case FACING, FACING_LIT -> state.facing() != SemanticState.Facing.NONE;
            case FACING_HONEY_LEVEL -> state.facing() != SemanticState.Facing.NONE
                    && state.level() >= 0
                    && state.level() <= 5;
            case MOUNT_FACE_FACING -> state.mountFace() != SemanticState.MountFace.NONE
                    && state.facing() != SemanticState.Facing.NONE;
            case FENCE_GATE -> state.facing() != SemanticState.Facing.NONE;
            case AXIS -> state.axis() != SemanticState.Axis.NONE;
            case SLAB -> state.slabType() != SemanticState.SlabType.NONE;
            case CONNECTIONS -> true;
            case STAIRS -> state.facing() != SemanticState.Facing.NONE
                    && state.half() != SemanticState.Half.NONE
                    && state.stairShape() != SemanticState.StairShape.NONE;
            case TRAPDOOR -> state.facing() != SemanticState.Facing.NONE
                    && state.half() != SemanticState.Half.NONE;
        };

        if (!valid) {
            throw new IllegalArgumentException(
                    "Semantic state does not satisfy " + capability.sourceKey() + " / " + kind
                            + ": " + state);
        }
    }
}

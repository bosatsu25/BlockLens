package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import java.util.Objects;

/** Shared visual intent for the 13 decoration/orientation capabilities. */
public enum DecorationVisualCue {
    FACING_MARKER,
    FACING_LEVEL_MARKER,
    FACING_LIT_MARKER,
    MOUNT_FACE_MARKER,
    FENCE_GATE_STATE_MARKER,
    AXIS_MARKER,
    SLAB_STATE_MARKER,
    OPAQUE_STAINED_GLASS,
    STAIRS_STATE_MARKER,
    TRAPDOOR_STATE_MARKER;

    public static DecorationVisualCue forCapability(CapabilityId capability) {
        return switch (Objects.requireNonNull(capability, "capability")) {
            case ANVIL, GLAZED_TERRACOTTA -> FACING_MARKER;
            case BEEHIVE -> FACING_LEVEL_MARKER;
            case CAMPFIRE -> FACING_LIT_MARKER;
            case GRINDSTONE -> MOUNT_FACE_MARKER;
            case FENCE_GATE -> FENCE_GATE_STATE_MARKER;
            case FROGLIGHT, WOOD, LOG -> AXIS_MARKER;
            case SLABS -> SLAB_STATE_MARKER;
            case STAINED_GLASS -> OPAQUE_STAINED_GLASS;
            case STAIRS -> STAIRS_STATE_MARKER;
            case TRAPDOOR -> TRAPDOOR_STATE_MARKER;
            default -> throw new IllegalArgumentException(
                    "Capability is not a decoration/orientation target: " + capability.sourceKey());
        };
    }
}

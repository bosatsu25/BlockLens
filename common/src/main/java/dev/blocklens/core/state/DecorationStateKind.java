package dev.blocklens.core.state;

import dev.blocklens.core.CapabilityId;
import java.util.Objects;

/**
 * Common product policy describing which semantic BlockState shape each M2/M3
 * decoration capability requires. Version modules only translate Minecraft values.
 */
public enum DecorationStateKind {
    NONE,
    FACING,
    FACING_HONEY_LEVEL,
    FACING_LIT,
    MOUNT_FACE_FACING,
    FENCE_GATE,
    AXIS,
    SLAB,
    CONNECTIONS,
    STAIRS,
    TRAPDOOR;

    public static DecorationStateKind forCapability(CapabilityId capability) {
        return switch (Objects.requireNonNull(capability, "capability")) {
            case ANVIL, GLAZED_TERRACOTTA -> FACING;
            case BEEHIVE -> FACING_HONEY_LEVEL;
            case CAMPFIRE -> FACING_LIT;
            case GRINDSTONE -> MOUNT_FACE_FACING;
            case FENCE_GATE -> FENCE_GATE;
            case FROGLIGHT, WOOD, LOG -> AXIS;
            case SLABS -> SLAB;
            case STAINED_GLASS -> CONNECTIONS;
            case STAIRS -> STAIRS;
            case TRAPDOOR -> TRAPDOOR;
            default -> NONE;
        };
    }

    public static boolean isDecorationCapability(CapabilityId capability) {
        return forCapability(capability) != NONE;
    }
}

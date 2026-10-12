package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import java.util.Objects;

/**
 * Vanilla targets present in the supplied Glowing Ores border-pack CTM inventory.
 * The border is a clean-room exposed-face overlay, not a copied OptiFine CTM tile.
 * Existing individual ore highlight settings own its enable/disable lifecycle.
 */
public final class ResourceBorderPolicy {
    private ResourceBorderPolicy() { }

    /** Cardinal bits0..3 and following diagonal bits4..7, clockwise in face UV space. */
    public static int connectedBorderMask(int neighbors) {
        if (neighbors < 0 || neighbors > 255) throw new IllegalArgumentException("Expected eight neighbor bits");
        int visible = ~neighbors & 15;
        for (int side = 0; side < 4; side++) {
            int adjacent = (1 << side) | (1 << ((side + 1) % 4)) | (1 << (side + 4));
            if ((neighbors & adjacent) != adjacent) visible |= 1 << (side + 4);
        }
        return visible;
    }

    public static boolean supports(CapabilityId capability) {
        return switch (Objects.requireNonNull(capability, "capability")) {
            case COAL_ORE, DEEPSLATE_COAL_ORE,
                    COPPER_ORE, DEEPSLATE_COPPER_ORE,
                    DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                    EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                    GOLD_ORE, DEEPSLATE_GOLD_ORE,
                    IRON_ORE, DEEPSLATE_IRON_ORE,
                    LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                    REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                    NETHER_GOLD_ORE, NETHER_QUARTZ_ORE -> true;
            default -> false;
        };
    }
}

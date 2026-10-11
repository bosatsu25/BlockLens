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

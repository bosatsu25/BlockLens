package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import java.util.Objects;

/**
 * BlockLens-owned procedural accents for M5 resource visibility.
 *
 * <p>The pinned source proves that each resource capability redirects to dedicated animated
 * highlight assets. BlockLens preserves that user value without redistributing those binaries by
 * retaining the active baked base model and applying a stable full-bright accent.</p>
 *
 * <p>Each enum value owns one immutable quad instruction. Resource-highlight rendering can therefore
 * reuse the same instruction for every emitted quad instead of constructing an equivalent record
 * at the render boundary.</p>
 */
public enum ResourceHighlightCue {
    OBSIDIAN(0xFFB96CFF),
    ANCIENT_DEBRIS(0xFFFF8A5B),
    DIAMOND(0xFF55FFFF),
    GOLD(0xFFFFD34D),
    EMERALD(0xFF55FF80),
    COAL(0xFFE0E0E0),
    IRON(0xFFFFC49A),
    COPPER(0xFFFFA05A),
    LAPIS(0xFF6A7CFF),
    REDSTONE(0xFFFF5555);

    private final int accentArgb;
    private final DecorationQuadInstruction instruction;

    ResourceHighlightCue(int accentArgb) {
        this.accentArgb = accentArgb;
        this.instruction = DecorationQuadInstruction.emissiveTint(accentArgb);
    }

    public int accentArgb() {
        return accentArgb;
    }

    public boolean emissive() {
        return true;
    }

    public DecorationQuadInstruction instruction() {
        return instruction;
    }

    public static ResourceHighlightCue forCapability(CapabilityId capability) {
        return switch (Objects.requireNonNull(capability, "capability")) {
            case OBSIDIAN -> OBSIDIAN;
            case ANCIENT_DEBRIS -> ANCIENT_DEBRIS;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> DIAMOND;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> GOLD;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> EMERALD;
            case COAL_ORE, DEEPSLATE_COAL_ORE -> COAL;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> IRON;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> COPPER;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> LAPIS;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> REDSTONE;
            default -> throw new IllegalArgumentException(
                    "Capability is not an M5 resource highlight: " + capability.sourceKey());
        };
    }
}

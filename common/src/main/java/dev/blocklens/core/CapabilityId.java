package dev.blocklens.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stable BlockLens product catalog.
 *
 * <p>The first 37 entries preserve the pinned M0 source contract and their historical ordinal
 * positions. New product capabilities are appended only, so existing runtime bit positions stay
 * stable while BlockLens absorbs selected ChiseTweaks behavior.</p>
 *
 * <p>This enum is intentionally compiled code: normal startup must not scan the classpath,
 * resource packs, or legacy configuration files to discover capabilities.</p>
 */
public enum CapabilityId {
    ANVIL("deco.anvil", Category.DECORATION, false),
    BEEHIVE("deco.beehive", Category.DECORATION, false),
    CAMPFIRE("deco.campfire", Category.DECORATION, false),
    GLAZED_TERRACOTTA("deco.glazedterracotta", Category.DECORATION, false),
    GRINDSTONE("deco.grindstone", Category.DECORATION, false),
    FENCE_GATE("deco.fence_gate", Category.DECORATION, false),
    FROGLIGHT("deco.froglight", Category.DECORATION, false),
    SLABS("deco.slabs", Category.DECORATION, false),
    STAINED_GLASS("deco.stainedglass", Category.DECORATION, false),
    STAIRS("deco.stairs", Category.DECORATION, false),
    TRAPDOOR("deco.trapdoor", Category.DECORATION, false),
    WOOD("deco.wood", Category.DECORATION, false),
    LOG("deco.log", Category.DECORATION, false),

    OBSIDIAN("gaming.obsidian", Category.RESOURCE, false),
    ANCIENT_DEBRIS("gaming.ancient_debris", Category.RESOURCE, false),
    DIAMOND_ORE("gaming.diamond_ore", Category.RESOURCE, false),
    DEEPSLATE_DIAMOND_ORE("gaming.deepslate_diamond_ore", Category.RESOURCE, false),
    GOLD_ORE("gaming.gold_ore", Category.RESOURCE, false),
    DEEPSLATE_GOLD_ORE("gaming.deepslate_gold_ore", Category.RESOURCE, false),
    EMERALD_ORE("gaming.emerald_ore", Category.RESOURCE, false),
    DEEPSLATE_EMERALD_ORE("gaming.deepslate_emerald_ore", Category.RESOURCE, false),
    COAL_ORE("gaming.coal_ore", Category.RESOURCE, false),
    DEEPSLATE_COAL_ORE("gaming.deepslate_coal_ore", Category.RESOURCE, false),
    IRON_ORE("gaming.iron_ore", Category.RESOURCE, false),
    DEEPSLATE_IRON_ORE("gaming.deepslate_iron_ore", Category.RESOURCE, false),
    COPPER_ORE("gaming.copper_ore", Category.RESOURCE, false),
    DEEPSLATE_COPPER_ORE("gaming.deepslate_copper_ore", Category.RESOURCE, false),
    LAPIS_ORE("gaming.lapis_ore", Category.RESOURCE, false),
    DEEPSLATE_LAPIS_ORE("gaming.deepslate_lapis_ore", Category.RESOURCE, false),
    REDSTONE_ORE("gaming.redstone_ore", Category.RESOURCE, false),
    DEEPSLATE_REDSTONE_ORE("gaming.deepslate_redstone_ore", Category.RESOURCE, false),

    BLUE_ICE("outline.blueice", Category.OUTLINE, true),
    DEAD_CORAL("outline.deadcoral", Category.OUTLINE, true),
    POWDER_SNOW("outline.powdersnow", Category.OUTLINE, true),
    SCULK_CATALYST("outline.sculk_catalyst", Category.OUTLINE, true),

    NETHER_TWEAKS("others.nethertweaks", Category.OTHER, false),
    STRING_TWEAKS("others.stringtweaks", Category.OTHER, true),

    // P0 ChiseTweaks migration additions. Appended to preserve all original bit positions.
    CRYING_OBSIDIAN("gaming.crying_obsidian", Category.RESOURCE, false),
    NETHER_GOLD_ORE("gaming.nether_gold_ore", Category.RESOURCE, false),
    NETHER_QUARTZ_ORE("gaming.nether_quartz_ore", Category.RESOURCE, false),

    // Lightweight visual migration: independent additions, preserving the original 40 bits.
    GLASS_HIGHLIGHT("outline.glass_highlight", Category.OUTLINE, false),
    KELP_HIGHLIGHT("outline.kelp_highlight", Category.OUTLINE, false),
    BRIGHT_CONCRETE("outline.bright_concrete", Category.OUTLINE, true),
    LOW_FIRE("others.low_fire", Category.OTHER, false),
    HANDHELD_SIZE("others.handheld_size", Category.OTHER, false),
    BRIGHT_CHEST("outline.bright_chest", Category.OUTLINE, true);

    public enum Category {
        DECORATION,
        RESOURCE,
        OUTLINE,
        OTHER
    }

    private static final Map<String, CapabilityId> BY_SOURCE_KEY;

    static {
        Map<String, CapabilityId> byKey = new LinkedHashMap<>();
        Arrays.stream(values()).forEach(capability -> {
            CapabilityId previous = byKey.put(capability.sourceKey, capability);
            if (previous != null) {
                throw new IllegalStateException("Duplicate source key: " + capability.sourceKey);
            }
        });
        BY_SOURCE_KEY = Collections.unmodifiableMap(byKey);
    }

    private final String sourceKey;
    private final Category category;
    private final boolean defaultEnabled;

    CapabilityId(String sourceKey, Category category, boolean defaultEnabled) {
        this.sourceKey = sourceKey;
        this.category = category;
        this.defaultEnabled = defaultEnabled;
    }

    public String sourceKey() {
        return sourceKey;
    }

    public Category category() {
        return category;
    }

    public boolean defaultEnabled() {
        return defaultEnabled;
    }

    public static Map<String, CapabilityId> bySourceKey() {
        return BY_SOURCE_KEY;
    }
}

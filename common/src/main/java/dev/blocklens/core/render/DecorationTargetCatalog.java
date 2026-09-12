package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Exact M0 target scope for the 13 decoration/orientation capabilities. */
public final class DecorationTargetCatalog {
    private static final Map<CapabilityId, List<String>> TARGETS;

    static {
        EnumMap<CapabilityId, List<String>> targets = new EnumMap<>(CapabilityId.class);
        targets.put(CapabilityId.ANVIL, List.of("anvil"));
        targets.put(CapabilityId.BEEHIVE, List.of("beehive"));
        targets.put(CapabilityId.CAMPFIRE, List.of("campfire", "soul_campfire"));
        targets.put(CapabilityId.FENCE_GATE, List.of(
                "acacia_fence_gate", "bamboo_fence_gate", "birch_fence_gate", "cherry_fence_gate",
                "crimson_fence_gate", "dark_oak_fence_gate", "jungle_fence_gate", "mangrove_fence_gate",
                "oak_fence_gate", "pale_oak_fence_gate", "spruce_fence_gate", "warped_fence_gate"));
        targets.put(CapabilityId.FROGLIGHT, List.of(
                "ochre_froglight", "pearlescent_froglight", "verdant_froglight"));
        targets.put(CapabilityId.GLAZED_TERRACOTTA, List.of(
                "black_glazed_terracotta", "blue_glazed_terracotta", "brown_glazed_terracotta",
                "cyan_glazed_terracotta", "gray_glazed_terracotta", "green_glazed_terracotta",
                "light_blue_glazed_terracotta", "light_gray_glazed_terracotta", "lime_glazed_terracotta",
                "magenta_glazed_terracotta", "orange_glazed_terracotta", "pink_glazed_terracotta",
                "purple_glazed_terracotta", "red_glazed_terracotta", "white_glazed_terracotta",
                "yellow_glazed_terracotta"));
        targets.put(CapabilityId.GRINDSTONE, List.of("grindstone"));
        targets.put(CapabilityId.LOG, List.of(
                "acacia_log", "bamboo_block", "birch_log", "cherry_log", "crimson_stem", "dark_oak_log",
                "jungle_log", "mangrove_log", "oak_log", "pale_oak_log", "spruce_log",
                "stripped_acacia_log", "stripped_bamboo_block", "stripped_birch_log", "stripped_cherry_log",
                "stripped_crimson_stem", "stripped_dark_oak_log", "stripped_jungle_log", "stripped_mangrove_log",
                "stripped_oak_log", "stripped_pale_oak_log", "stripped_spruce_log", "stripped_warped_stem",
                "warped_stem"));
        targets.put(CapabilityId.SLABS, List.of(
                "acacia_slab", "andesite_slab", "bamboo_mosaic_slab", "bamboo_slab", "birch_slab",
                "blackstone_slab", "brick_slab", "cherry_slab", "cobbled_deepslate_slab", "cobblestone_slab",
                "crimson_slab", "cut_copper_slab", "cut_red_sandstone_slab", "cut_sandstone_slab",
                "dark_oak_slab", "dark_prismarine_slab", "deepslate_brick_slab", "deepslate_tile_slab",
                "diorite_slab", "end_stone_brick_slab", "exposed_cut_copper_slab", "granite_slab", "jungle_slab",
                "mangrove_slab", "mossy_cobblestone_slab", "mossy_stone_brick_slab", "mud_brick_slab",
                "nether_brick_slab", "oak_slab", "oxidized_cut_copper_slab", "pale_oak_slab",
                "polished_andesite_slab", "polished_blackstone_brick_slab", "polished_blackstone_slab",
                "polished_deepslate_slab", "polished_diorite_slab", "polished_granite_slab", "polished_tuff_slab",
                "prismarine_brick_slab", "prismarine_slab", "purpur_slab", "quartz_slab", "red_nether_brick_slab",
                "red_sandstone_slab", "resin_brick_slab", "sandstone_slab", "smooth_quartz_slab",
                "smooth_red_sandstone_slab", "smooth_sandstone_slab", "smooth_stone_slab", "spruce_slab",
                "stone_brick_slab", "stone_slab", "tuff_brick_slab", "tuff_slab", "warped_slab",
                "waxed_cut_copper_slab", "waxed_exposed_cut_copper_slab", "waxed_oxidized_cut_copper_slab",
                "waxed_weathered_cut_copper_slab", "weathered_cut_copper_slab"));
        targets.put(CapabilityId.STAINED_GLASS, List.of(
                "black_stained_glass", "black_stained_glass_pane", "blue_stained_glass", "blue_stained_glass_pane",
                "brown_stained_glass", "brown_stained_glass_pane", "cyan_stained_glass", "cyan_stained_glass_pane",
                "gray_stained_glass", "gray_stained_glass_pane", "green_stained_glass", "green_stained_glass_pane",
                "light_blue_stained_glass", "light_blue_stained_glass_pane", "light_gray_stained_glass",
                "light_gray_stained_glass_pane", "lime_stained_glass", "lime_stained_glass_pane",
                "magenta_stained_glass", "magenta_stained_glass_pane", "orange_stained_glass",
                "orange_stained_glass_pane", "pink_stained_glass", "pink_stained_glass_pane",
                "purple_stained_glass", "purple_stained_glass_pane", "red_stained_glass", "red_stained_glass_pane",
                "white_stained_glass", "white_stained_glass_pane", "yellow_stained_glass",
                "yellow_stained_glass_pane"));
        targets.put(CapabilityId.STAIRS, List.of(
                "acacia_stairs", "andesite_stairs", "bamboo_mosaic_stairs", "bamboo_stairs", "birch_stairs",
                "blackstone_stairs", "brick_stairs", "cherry_stairs", "cobbled_deepslate_stairs",
                "cobblestone_stairs", "crimson_stairs", "cut_copper_stairs", "dark_oak_stairs",
                "dark_prismarine_stairs", "deepslate_brick_stairs", "deepslate_tile_stairs", "diorite_stairs",
                "end_stone_brick_stairs", "exposed_cut_copper_stairs", "granite_stairs", "jungle_stairs",
                "mangrove_stairs", "mossy_cobblestone_stairs", "mossy_stone_brick_stairs", "mud_brick_stairs",
                "nether_brick_stairs", "oak_stairs", "oxidized_cut_copper_stairs", "pale_oak_stairs",
                "polished_andesite_stairs", "polished_blackstone_brick_stairs", "polished_blackstone_stairs",
                "polished_deepslate_stairs", "polished_diorite_stairs", "polished_granite_stairs",
                "polished_tuff_stairs", "prismarine_brick_stairs", "prismarine_stairs", "purpur_stairs",
                "quartz_stairs", "red_nether_brick_stairs", "red_sandstone_stairs", "resin_brick_stairs",
                "sandstone_stairs", "smooth_quartz_stairs", "smooth_red_sandstone_stairs",
                "smooth_sandstone_stairs", "spruce_stairs", "stone_brick_stairs", "stone_stairs",
                "tuff_brick_stairs", "tuff_stairs", "warped_stairs", "waxed_cut_copper_stairs",
                "waxed_exposed_cut_copper_stairs", "waxed_oxidized_cut_copper_stairs",
                "waxed_weathered_cut_copper_stairs", "weathered_cut_copper_stairs"));
        targets.put(CapabilityId.TRAPDOOR, List.of(
                "acacia_trapdoor", "bamboo_trapdoor", "birch_trapdoor", "cherry_trapdoor", "copper_trapdoor",
                "crimson_trapdoor", "dark_oak_trapdoor", "exposed_copper_trapdoor", "iron_trapdoor",
                "jungle_trapdoor", "mangrove_trapdoor", "oak_trapdoor", "oxidized_copper_trapdoor",
                "pale_oak_trapdoor", "spruce_trapdoor", "warped_trapdoor", "waxed_copper_trapdoor",
                "waxed_exposed_copper_trapdoor", "waxed_oxidized_copper_trapdoor",
                "waxed_weathered_copper_trapdoor", "weathered_copper_trapdoor"));
        targets.put(CapabilityId.WOOD, List.of(
                "acacia_wood", "birch_wood", "cherry_wood", "crimson_hyphae", "dark_oak_wood", "jungle_wood",
                "mangrove_wood", "oak_wood", "pale_oak_wood", "spruce_wood", "stripped_acacia_wood",
                "stripped_birch_wood", "stripped_cherry_wood", "stripped_crimson_hyphae", "stripped_dark_oak_wood",
                "stripped_jungle_wood", "stripped_mangrove_wood", "stripped_oak_wood", "stripped_pale_oak_wood",
                "stripped_spruce_wood", "stripped_warped_hyphae", "warped_hyphae"));
        TARGETS = Collections.unmodifiableMap(targets);
    }

    private DecorationTargetCatalog() {
    }

    public static List<String> targets(CapabilityId capability) {
        Objects.requireNonNull(capability, "capability");
        return TARGETS.getOrDefault(capability, List.of());
    }

    public static boolean contains(CapabilityId capability, String minecraftPath) {
        Objects.requireNonNull(minecraftPath, "minecraftPath");
        return targets(capability).contains(minecraftPath);
    }

    public static int totalBindingCount() {
        return TARGETS.values().stream().mapToInt(List::size).sum();
    }
}

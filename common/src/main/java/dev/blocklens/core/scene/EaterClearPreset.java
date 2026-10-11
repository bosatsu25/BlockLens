package dev.blocklens.core.scene;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Eater Clear source inventory: 196 blockstate files mapped to the transparent
 * clear model in eater-texture v1.2. Eleven specialized plant/foliage models are
 * intentionally excluded; they are not equivalent to invisible block meshes.
 *
 * <p>This compact, resource-free inventory is only resolved when the user selects
 * the opt-in preset. Unknown/non-block source filenames are ignored by the
 * Minecraft registry guard. Source artwork is never bundled.</p>
 */
public final class EaterClearPreset {
    private static final String PATHS = """
        acacia_leaves acacia_log acacia_planks acacia_slab andesite azalea azalea_leaves bamboo_slab birch_leaves
        birch_log birch_planks birch_slab black_terracotta blackstone blockofamethyst blue_ice blue_terracotta bone_block
        bookshelf brown_mushroom_block brown_terracotta calcite cave_vines cave_vines_plant cherry_leaves cherry_log cherry_planks
        cherry_slab chiseled_sandstone chiseled_stone_bricks chiseled_tuff chiseled_tuff_bricks chiseled_tuff_bricks_top chiseled_tuff_top clay coarse_dirt
        cobbled_deepslate cobblestone cobblestone_slab copper_block cracked_stone_bricks crafting_table crimson_planks crimson_slab crimson_stem
        cut_sandstone cyan_terracotta dark_oak_leaves dark_oak_log dark_oak_planks dark_oak_slab deepslate diorite dirt
        dripstone_block dripstoneblock flowering_azalea flowering_azalea_leaves granite grass_block gravel gray_terracotta green_terracotta
        ice jungle_leaves jungle_log jungle_planks jungle_slab light_blue_terracotta light_gray_terracotta lime_terracotta magenta_terracotta
        magma_block mangrove_leaves mangrove_log mangrove_planks mangrove_slab melon moss_block mossy_cobblestone mossy_stone_bricks
        mushroom_stem mycelium oak_leaves oak_log oak_planks oak_slab orange_terracotta oxidized_cut_copper packed_ice
        pale_oak_leaves pale_oak_log pale_oak_slab pink_terracotta podzol polished_andesite polished_diorite polished_granite polished_tuff
        polished_tuff_slab pumpkin purple_terracotta red_glazed_terracotta red_mushroom_block red_terracotta redsand redsandstone rooted_dirt
        sand sandstone sandstone_slab sculk smol smooth_basalt smooth_sandstone_slab smooth_stone_slab snow_block
        snowblock spore_blossom spruce_leaves spruce_log spruce_planks spruce_slab stone stone_bricks stripped_acacia_log
        stripped_birch_log stripped_cherry_log stripped_crimson_stem stripped_dark_oak_log stripped_jungle_log stripped_mangrove_log stripped_oak_log stripped_pale_oak_log stripped_spruce_log
        stripped_warped_stem terracotta tuff tuff_bricks warped_planks warped_slab warped_stem waxed_chiseled_copper waxed_copper_block
        waxed_copper_bulb waxed_copper_bulb_lit waxed_copper_bulb_lit_powered waxed_copper_bulb_powered waxed_copper_cut_stairs waxed_copper_door_top waxed_copper_grate waxed_copper_slab waxed_copper_trapdoor
        waxed_cut_copper waxed_cut_copper_slab waxed_exposed_chiseled_copper waxed_exposed_copper waxed_exposed_copper_bulb waxed_exposed_copper_bulb_lit waxed_exposed_copper_bulb_lit_powered waxed_exposed_copper_bulb_powered waxed_exposed_copper_door_top
        waxed_exposed_copper_grate waxed_exposed_copper_slab waxed_exposed_copper_trapdoor waxed_exposed_cut_copper waxed_exposed_cut_copper_slab waxed_exposed_cut_copper_stairs waxed_oxidized_chiseled_copper waxed_oxidized_copper waxed_oxidized_copper_bulb
        waxed_oxidized_copper_bulb_lit waxed_oxidized_copper_bulb_lit_powered waxed_oxidized_copper_bulb_powered waxed_oxidized_copper_door waxed_oxidized_copper_door_top waxed_oxidized_copper_grate waxed_oxidized_copper_slab waxed_oxidized_copper_trapdoor waxed_oxidized_cut_copper
        waxed_oxidized_cut_copper_ waxed_oxidized_cut_copper_stairs waxed_weathered_chiseled_copper waxed_weathered_copper waxed_weathered_copper_bulb waxed_weathered_copper_bulb_lit waxed_weathered_copper_bulb_lit_powered waxed_weathered_copper_bulb_powered waxed_weathered_copper_grate
        waxed_weathered_copper_slab waxed_weathered_copper_trapdoor waxed_weathered_cut_copper waxed_weathered_cut_copper_slab waxed_weathered_cut_copper_stairs white_terracotta yellow_terracotta
            """;
    private static final List<String> IDS = Arrays.stream(PATHS.trim().split("\\s+"))
            .map(path -> "minecraft:" + path).toList();

    private EaterClearPreset() { }

    public static List<String> sourceIds() { return IDS; }

    /** Compiles only registered block IDs; no world scans or runtime blacklist lookup. */
    public static List<String> available(Predicate<String> registered) {
        Objects.requireNonNull(registered, "registered");
        return IDS.stream().filter(registered).toList();
    }
}

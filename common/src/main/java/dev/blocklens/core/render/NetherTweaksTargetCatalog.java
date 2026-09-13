package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import java.util.List;
import java.util.Objects;

/**
 * Exact M0 target scope for Nether Tweaks.
 *
 * <p>This class deliberately contains no guessed render style. The pinned source contract only
 * allows runtime behavior to be enabled once the corresponding visual rule is evidence-backed.</p>
 */
public final class NetherTweaksTargetCatalog {
    public static final String DIRECT_PATH_SET_SHA256 =
            "e20ab1991f3b4da58f67b94585ec79f5bb73e6d872090480cac96c56f8b73e40";

    private static final List<String> TARGETS = List.of(
            "basalt",
            "blackstone",
            "chiseled_nether_bricks",
            "chiseled_polished_blackstone",
            "cracked_nether_bricks",
            "cracked_polished_blackstone_bricks",
            "crimson_nylium",
            "crimson_stem",
            "crying_obsidian",
            "gilded_blackstone",
            "glowstone",
            "gravel",
            "magma_block",
            "nether_bricks",
            "nether_gold_ore",
            "nether_quartz_ore",
            "nether_wart_block",
            "netherrack",
            "obsidian",
            "polished_blackstone",
            "polished_blackstone_bricks",
            "shroomlight",
            "soul_sand",
            "soul_soil",
            "warped_nylium",
            "warped_stem",
            "warped_wart_block");

    private NetherTweaksTargetCatalog() {
    }

    public static List<String> targets(CapabilityId capability) {
        Objects.requireNonNull(capability, "capability");
        return capability == CapabilityId.NETHER_TWEAKS ? TARGETS : List.of();
    }

    public static boolean contains(String minecraftPath) {
        return TARGETS.contains(Objects.requireNonNull(minecraftPath, "minecraftPath"));
    }

    public static int totalBindingCount() {
        return TARGETS.size();
    }
}

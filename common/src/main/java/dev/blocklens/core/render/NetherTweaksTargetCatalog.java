package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Exact compiled target scope for Nether Tweaks.
 *
 * <p>The original 27 source targets remain frozen as the M0 baseline. P0 adds ChiseTweaks'
 * `polished_basalt` target to the runtime union without dropping BlockLens' existing `obsidian`
 * target. This makes the migration additive and keeps the original baseline auditable.</p>
 */
public final class NetherTweaksTargetCatalog {
    /** Hash of the pinned source M0 direct-path set; intentionally remains baseline-only evidence. */
    public static final String DIRECT_PATH_SET_SHA256 =
            "e20ab1991f3b4da58f67b94585ec79f5bb73e6d872090480cac96c56f8b73e40";

    private static final List<String> M0_TARGETS = List.of(
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

    private static final List<String> TARGETS = withP0Extensions();

    private NetherTweaksTargetCatalog() {
    }

    public static List<String> targets(CapabilityId capability) {
        Objects.requireNonNull(capability, "capability");
        return capability == CapabilityId.NETHER_TWEAKS ? TARGETS : List.of();
    }

    /** Returns the exact 27-target source M0 baseline used by the frozen parity contract. */
    public static List<String> m0Targets() {
        return M0_TARGETS;
    }

    public static boolean contains(String minecraftPath) {
        return TARGETS.contains(Objects.requireNonNull(minecraftPath, "minecraftPath"));
    }

    public static int totalBindingCount() {
        return TARGETS.size();
    }

    private static List<String> withP0Extensions() {
        ArrayList<String> targets = new ArrayList<>(M0_TARGETS);
        targets.add("polished_basalt");
        return List.copyOf(targets);
    }
}

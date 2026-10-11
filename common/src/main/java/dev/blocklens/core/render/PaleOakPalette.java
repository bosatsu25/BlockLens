package dev.blocklens.core.render;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import java.util.Set;

/** Bounded, clean-room wood recoloring; never overrides an active external pack. */
public final class PaleOakPalette {
    private static final Set<String> TEXTURES = Set.of(
            "textures/block/pale_oak_door_bottom.png",
            "textures/block/pale_oak_door_top.png",
            "textures/block/pale_oak_log_top.png",
            "textures/block/pale_oak_planks.png",
            "textures/block/pale_oak_shelf.png",
            "textures/block/pale_oak_trapdoor.png",
            "textures/block/stripped_pale_oak_log.png",
            "textures/block/stripped_pale_oak_log_top.png",
            "textures/block/pale_oak_sign.png",
            "textures/block/pale_oak_hanging_sign.png",
            "textures/entity/boat/pale_oak.png",
            "textures/entity/chest_boat/pale_oak.png",
            "textures/entity/signs/pale_oak.png",
            "textures/entity/signs/hanging/pale_oak.png",
            "textures/gui/hanging_signs/pale_oak.png",
            "textures/gui/signs/pale_oak.png",
            "textures/item/pale_oak_boat.png",
            "textures/item/pale_oak_chest_boat.png",
            "textures/item/pale_oak_door.png",
            "textures/item/pale_oak_sign.png",
            "textures/item/pale_oak_hanging_sign.png");

    private PaleOakPalette() { }

    public static boolean applies(boolean enabled, String namespace, String path, String owner, int width, int height) {
        return enabled && "minecraft".equals(namespace) && "vanilla".equals(owner)
                && path != null && TEXTURES.contains(path)
                && width > 0 && height > 0 && width <= 256 && height <= 256;
    }

    /** Keep transparent RGB and saturated hardware; integer luminance preserves the wood grain. */
    public static int neutralWood(int argb) {
        if ((argb >>> 24) == 0) return argb;
        int r = argb >>> 16 & 255, g = argb >>> 8 & 255, b = argb & 255;
        if (r < g || g < b || r - b > 48) return argb;
        int gray = (299 * r + 587 * g + 114 * b + 500) / 1000;
        return (argb & 0xFF000000) | gray << 16 | gray << 8 | gray;
    }

    public static boolean requiresReload(BlockLensConfig before, BlockLensConfig after) {
        return before.isEnabled(CapabilityId.PALE_OAK_NEUTRAL) != after.isEnabled(CapabilityId.PALE_OAK_NEUTRAL);
    }
}

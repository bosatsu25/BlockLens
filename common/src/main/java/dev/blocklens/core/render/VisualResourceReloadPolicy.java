package dev.blocklens.core.render;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;

/** Texture/model decode policies are installed only after a successful saved edit. */
public final class VisualResourceReloadPolicy {
    private VisualResourceReloadPolicy() { }
    private static final long MASK = CapabilityId.PALE_OAK_NEUTRAL.renderBit()
            | CapabilityId.COMPACT_BLOCKS.renderBit()
            | CapabilityId.COMPACT_ITEMS.renderBit()
            | CapabilityId.COMPACT_TOOLS.renderBit()
            | CapabilityId.SIMPLIFIED_TERRAIN.renderBit()
            | CapabilityId.CONNECTED_ORE_EDGES.renderBit();
    public static boolean requiresReload(BlockLensConfig before, BlockLensConfig after) {
        return ((before.enabledMask() ^ after.enabledMask()) & MASK) != 0;
    }
}

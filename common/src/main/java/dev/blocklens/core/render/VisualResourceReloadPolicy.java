package dev.blocklens.core.render;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;

/** Texture/model decode policies are installed only after a successful saved edit. */
public final class VisualResourceReloadPolicy {
    private VisualResourceReloadPolicy() { }
    private static final long MASK = (1L << CapabilityId.PALE_OAK_NEUTRAL.bitPosition())
            | (1L << CapabilityId.COMPACT_BLOCKS.bitPosition())
            | (1L << CapabilityId.COMPACT_ITEMS.bitPosition())
            | (1L << CapabilityId.COMPACT_TOOLS.bitPosition())
            | (1L << CapabilityId.SIMPLIFIED_TERRAIN.bitPosition())
            | (1L << CapabilityId.CONNECTED_ORE_EDGES.bitPosition());
    public static boolean requiresReload(BlockLensConfig before, BlockLensConfig after) {
        return ((before.enabledMask() ^ after.enabledMask()) & MASK) != 0;
    }
}

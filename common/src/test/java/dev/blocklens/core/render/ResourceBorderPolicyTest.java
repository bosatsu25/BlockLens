package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.*;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigCodec;
import dev.blocklens.core.CapabilityId;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

final class ResourceBorderPolicyTest {
    private static final EnumSet<CapabilityId> ORES = EnumSet.of(
            CapabilityId.COAL_ORE, CapabilityId.DEEPSLATE_COAL_ORE,
            CapabilityId.COPPER_ORE, CapabilityId.DEEPSLATE_COPPER_ORE,
            CapabilityId.DIAMOND_ORE, CapabilityId.DEEPSLATE_DIAMOND_ORE,
            CapabilityId.EMERALD_ORE, CapabilityId.DEEPSLATE_EMERALD_ORE,
            CapabilityId.GOLD_ORE, CapabilityId.DEEPSLATE_GOLD_ORE,
            CapabilityId.IRON_ORE, CapabilityId.DEEPSLATE_IRON_ORE,
            CapabilityId.LAPIS_ORE, CapabilityId.DEEPSLATE_LAPIS_ORE,
            CapabilityId.REDSTONE_ORE, CapabilityId.DEEPSLATE_REDSTONE_ORE,
            CapabilityId.NETHER_GOLD_ORE, CapabilityId.NETHER_QUARTZ_ORE);

    @Test void onlyReviewedVanillaOresReceiveTheCleanRoomBorder() {
        assertEquals(18, ORES.size());
        for (CapabilityId capability : CapabilityId.values()) {
            assertEquals(ORES.contains(capability), ResourceBorderPolicy.supports(capability),
                    capability.sourceKey());
            if (ORES.contains(capability)) {
                assertEquals(1, ResourceTargetCatalog.targets(capability).size());
                assertEquals(0xFF, ResourceHighlightCue.forCapability(capability).accentArgb() >>> 24);
            }
        }
        assertFalse(ResourceBorderPolicy.supports(CapabilityId.ANCIENT_DEBRIS));
        assertFalse(ResourceBorderPolicy.supports(CapabilityId.OBSIDIAN));
        assertFalse(ResourceBorderPolicy.supports(CapabilityId.CRYING_OBSIDIAN));
        assertThrows(NullPointerException.class, () -> ResourceBorderPolicy.supports(null));
    }

    @Test void originalOreTogglesStayIndependentAfterConfigRoundTrip() {
        var defaults = BlockLensConfig.defaults();
        for (CapabilityId selected : ORES) {
            assertFalse(defaults.isEnabled(selected));
            var enabled = defaults.withEnabled(selected, true);
            assertEquals(defaults.enabledMask() | (1L << selected.bitPosition()),
                    enabled.enabledMask());
            var restored = BlockLensConfigCodec.decode(BlockLensConfigCodec.encode(enabled));
            assertEquals(enabled.enabledMask(), restored.enabledMask());
            for (CapabilityId other : ORES) {
                assertEquals(selected == other, restored.isEnabled(other), other.sourceKey());
            }
        }
        assertEquals(61, CapabilityId.values().length);
    }
    @Test void joinedEdgesAndMissingDiagonalsRetainOnlyRequiredCorners() {
        assertEquals(255, ResourceBorderPolicy.connectedBorderMask(0));
        assertEquals(0, ResourceBorderPolicy.connectedBorderMask(255));
        assertEquals(240, ResourceBorderPolicy.connectedBorderMask(15));
        assertEquals(252, ResourceBorderPolicy.connectedBorderMask(3));
        assertEquals(236, ResourceBorderPolicy.connectedBorderMask(3 | 16));
        assertEquals(255, ResourceBorderPolicy.connectedBorderMask(240));
    }

    @Test void allEightNeighborCasesProduceExactly47CanonicalBoundaries() {
        var shapes = new java.util.HashSet<Integer>();
        for (int neighbors = 0; neighbors < 256; neighbors++) {
            int border = ResourceBorderPolicy.connectedBorderMask(neighbors);
            shapes.add(border);
            for (int side = 0; side < 4; side++) {
                assertEquals((neighbors & (1 << side)) == 0, (border & (1 << side)) != 0);
                boolean corner = (neighbors & (1 << side)) == 0
                        || (neighbors & (1 << ((side + 1) % 4))) == 0
                        || (neighbors & (1 << (side + 4))) == 0;
                assertEquals(corner, (border & (1 << (side + 4))) != 0);
            }
        }
        assertEquals(47, shapes.size());
    }

    @Test void invalidNeighborMasksAreRejected() {
        for (int mask : new int[] {-1, 256, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> ResourceBorderPolicy.connectedBorderMask(mask));
        }
    }

    @Test void connectionsAreIndependentAndNeverRegisterAsAnOreMaterial() {
        var id=CapabilityId.CONNECTED_ORE_EDGES;
        assertEquals(62,id.bitPosition());assertEquals(CapabilityId.Category.RESOURCE,id.category());
        assertFalse(id.defaultEnabled());assertFalse(id.isResourceHighlight());assertFalse(ResourceBorderPolicy.supports(id));
        var defaults=BlockLensConfig.defaults();var enabled=defaults.withEnabled(id,true);
        assertEquals(defaults.enabledMask()|(1L<<62),enabled.enabledMask());
        assertEquals(enabled,BlockLensConfigCodec.decode(BlockLensConfigCodec.encode(enabled)));
        for(var ore:ORES)assertFalse(enabled.isEnabled(ore));
        assertFalse(new dev.blocklens.core.extension.OreExtensionCatalog().register(1,"example:ore",id.sourceKey()));
    }

}

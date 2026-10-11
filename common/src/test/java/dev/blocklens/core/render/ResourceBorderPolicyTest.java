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
        assertEquals(56, CapabilityId.values().length);
    }
}

package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ResourceHighlightPolicyTest {
    @Test
    void allTwentyOneResourceCapabilitiesAreIndependentlyControllable() {
        BlockLensConfig defaults = BlockLensConfig.defaults();
        int count = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.category() != CapabilityId.Category.RESOURCE) continue;
            count++;
            assertFalse(defaults.isEnabled(capability));

            BlockLensConfig enabled = defaults.withEnabled(capability, true);
            assertTrue(enabled.isEnabled(capability));
            assertEquals(ResourceHighlightCue.forCapability(capability),
                    ResourceHighlightCue.forCapability(capability));

            for (CapabilityId other : CapabilityId.values()) {
                if (other.category() == CapabilityId.Category.RESOURCE && other != capability) {
                    assertFalse(enabled.isEnabled(other),
                            () -> capability + " must not implicitly enable " + other);
                }
            }
        }
        assertEquals(21, count);
    }

    @Test
    void pairedNormalAndDeepslateOresShareSemanticAccentButNotToggle() {
        assertEquals(ResourceHighlightCue.DIAMOND, ResourceHighlightCue.forCapability(CapabilityId.DIAMOND_ORE));
        assertEquals(ResourceHighlightCue.DIAMOND,
                ResourceHighlightCue.forCapability(CapabilityId.DEEPSLATE_DIAMOND_ORE));

        BlockLensConfig config = BlockLensConfig.defaults().withEnabled(CapabilityId.DIAMOND_ORE, true);
        assertTrue(config.isEnabled(CapabilityId.DIAMOND_ORE));
        assertFalse(config.isEnabled(CapabilityId.DEEPSLATE_DIAMOND_ORE));
    }

    @Test
    void p0ChiseMaterialsHaveDedicatedCleanRoomCues() {
        assertEquals(ResourceHighlightCue.CRYING_OBSIDIAN,
                ResourceHighlightCue.forCapability(CapabilityId.CRYING_OBSIDIAN));
        assertEquals(ResourceHighlightCue.NETHER_GOLD,
                ResourceHighlightCue.forCapability(CapabilityId.NETHER_GOLD_ORE));
        assertEquals(ResourceHighlightCue.NETHER_QUARTZ,
                ResourceHighlightCue.forCapability(CapabilityId.NETHER_QUARTZ_ORE));
        assertEquals(0xFFC42DFF, ResourceHighlightCue.CRYING_OBSIDIAN.accentArgb());
        assertEquals(0xFFFFD220, ResourceHighlightCue.NETHER_GOLD.accentArgb());
        assertEquals(0xFFF6EEE2, ResourceHighlightCue.NETHER_QUARTZ.accentArgb());
    }

    @Test
    void allResourceCuesAreOpaqueVisibleAccents() {
        Set<ResourceHighlightCue> seen = EnumSet.noneOf(ResourceHighlightCue.class);
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.category() != CapabilityId.Category.RESOURCE) continue;
            ResourceHighlightCue cue = ResourceHighlightCue.forCapability(capability);
            seen.add(cue);
            assertEquals(0xFF, cue.accentArgb() >>> 24);
            assertTrue(cue.emissive());
        }
        assertEquals(EnumSet.allOf(ResourceHighlightCue.class), seen);
    }

    @Test
    void nonResourceAndNullInputsFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> ResourceHighlightCue.forCapability(CapabilityId.ANVIL));
        assertThrows(NullPointerException.class,
                () -> ResourceHighlightCue.forCapability(null));
    }
}

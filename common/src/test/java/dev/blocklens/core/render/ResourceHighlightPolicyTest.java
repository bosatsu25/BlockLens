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
    void allEighteenResourceCapabilitiesAreIndependentlyControllable() {
        BlockLensConfig defaults = BlockLensConfig.defaults();
        int count = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.category() != CapabilityId.Category.RESOURCE) {
                continue;
            }
            count++;
            assertTrue(ResourceHighlightPolicy.cue(capability, defaults).isEmpty());
            BlockLensConfig enabled = defaults.withEnabled(capability, true);
            ResourceHighlightCue cue = ResourceHighlightPolicy.cue(capability, enabled).orElseThrow();
            assertEquals(ResourceHighlightCue.forCapability(capability), cue);
            assertTrue(cue.emissive());
            assertFalse(ResourceHighlightPolicy.cue(capability, defaults).isPresent());
        }
        assertEquals(18, count);
    }

    @Test
    void pairedNormalAndDeepslateOresShareSemanticAccentButNotToggle() {
        assertEquals(ResourceHighlightCue.DIAMOND, ResourceHighlightCue.forCapability(CapabilityId.DIAMOND_ORE));
        assertEquals(ResourceHighlightCue.DIAMOND,
                ResourceHighlightCue.forCapability(CapabilityId.DEEPSLATE_DIAMOND_ORE));

        BlockLensConfig config = BlockLensConfig.defaults().withEnabled(CapabilityId.DIAMOND_ORE, true);
        assertTrue(ResourceHighlightPolicy.cue(CapabilityId.DIAMOND_ORE, config).isPresent());
        assertTrue(ResourceHighlightPolicy.cue(CapabilityId.DEEPSLATE_DIAMOND_ORE, config).isEmpty());
    }

    @Test
    void allResourceCuesAreOpaqueVisibleAccents() {
        Set<ResourceHighlightCue> seen = EnumSet.noneOf(ResourceHighlightCue.class);
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.category() != CapabilityId.Category.RESOURCE) {
                continue;
            }
            ResourceHighlightCue cue = ResourceHighlightCue.forCapability(capability);
            seen.add(cue);
            assertEquals(0xFF, cue.accentArgb() >>> 24);
            assertTrue(cue.emissive());
        }
        assertEquals(EnumSet.allOf(ResourceHighlightCue.class), seen);
    }

    @Test
    void nonResourceAndNullInputsFailClosed() {
        BlockLensConfig config = BlockLensConfig.defaults().withEnabled(CapabilityId.ANVIL, true);
        assertTrue(ResourceHighlightPolicy.cue(CapabilityId.ANVIL, config).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> ResourceHighlightCue.forCapability(CapabilityId.ANVIL));
        assertThrows(NullPointerException.class,
                () -> ResourceHighlightCue.forCapability(null));
        assertThrows(NullPointerException.class,
                () -> ResourceHighlightPolicy.cue(null, config));
        assertThrows(NullPointerException.class,
                () -> ResourceHighlightPolicy.cue(CapabilityId.OBSIDIAN, null));
    }
}

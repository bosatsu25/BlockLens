package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.*;

import dev.blocklens.core.render.BlockLensTargetCatalog;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class LightweightCapabilitiesTest {
    @Test
    void migratedControlsAreIndependentAdditionsWithSourceDefaults() {
        List<String> keys = List.of("outline.glass_highlight", "outline.kelp_highlight",
                "outline.bright_concrete", "others.low_fire", "others.handheld_size",
                "outline.bright_chest");
        Set<String> enabledByDefault = Set.of("outline.bright_concrete", "outline.bright_chest");
        for (int i = 0; i < keys.size(); i++) {
            CapabilityId capability = CapabilityId.bySourceKey().get(keys.get(i));
            assertNotNull(capability, "Missing independent migrated control: " + keys.get(i));
            assertEquals(40 + i, capability.ordinal(), "Existing bit positions must not move");
            assertEquals(enabledByDefault.contains(keys.get(i)), capability.defaultEnabled());
        }
        assertEquals(39, CapabilityId.NETHER_QUARTZ_ORE.ordinal());
        assertEquals(36, CapabilityId.STRING_TWEAKS.ordinal());
    }
    @Test
    void modelTargetsAreExactAndRendererOnlyControlsDoNotScanBlocks() {
        List<String> glass = BlockLensTargetCatalog.targets(CapabilityId.GLASS_HIGHLIGHT);
        assertEquals(35, glass.size());
        assertTrue(glass.containsAll(List.of("glass", "tinted_glass", "glass_pane",
                "white_stained_glass", "black_stained_glass_pane")));
        assertFalse(glass.contains("ice"));
        assertEquals(35, Set.copyOf(glass).size());
        assertEquals(List.of("kelp", "kelp_plant"), BlockLensTargetCatalog.targets(CapabilityId.KELP_HIGHLIGHT));
        assertEquals(List.of("white_concrete"), BlockLensTargetCatalog.targets(CapabilityId.BRIGHT_CONCRETE));
        assertTrue(BlockLensTargetCatalog.targets(CapabilityId.LOW_FIRE).isEmpty());
        assertTrue(BlockLensTargetCatalog.targets(CapabilityId.HANDHELD_SIZE).isEmpty());
        assertTrue(BlockLensTargetCatalog.targets(CapabilityId.BRIGHT_CHEST).isEmpty());
    }
}

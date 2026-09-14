package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.CapabilityId;
import java.util.List;
import org.junit.jupiter.api.Test;

final class VisibilityTargetCatalogTest {
    @Test
    void currentVisibilityScopeContainsTwentyFiveBindings() {
        assertEquals(List.of("blue_ice"), VisibilityTargetCatalog.targets(CapabilityId.BLUE_ICE));
        assertEquals(List.of("powder_snow"), VisibilityTargetCatalog.targets(CapabilityId.POWDER_SNOW));
        assertEquals(List.of("sculk_catalyst"), VisibilityTargetCatalog.targets(CapabilityId.SCULK_CATALYST));
        assertEquals(List.of("tripwire", "tripwire_hook"),
                VisibilityTargetCatalog.targets(CapabilityId.STRING_TWEAKS));

        List<String> coral = VisibilityTargetCatalog.targets(CapabilityId.DEAD_CORAL);
        assertEquals(20, coral.size());
        for (String family : List.of("brain", "bubble", "fire", "horn", "tube")) {
            assertTrue(coral.contains("dead_" + family + "_coral"));
            assertTrue(coral.contains("dead_" + family + "_coral_block"));
            assertTrue(coral.contains("dead_" + family + "_coral_fan"));
            assertTrue(coral.contains("dead_" + family + "_coral_wall_fan"));
        }
        assertEquals(25, VisibilityTargetCatalog.totalBindingCount());
    }

    @Test
    void catalogDoesNotBroadenBySuffixOrCategory() {
        assertFalse(VisibilityTargetCatalog.contains(CapabilityId.BLUE_ICE, "packed_ice"));
        assertFalse(VisibilityTargetCatalog.contains(CapabilityId.DEAD_CORAL, "brain_coral"));
        assertTrue(VisibilityTargetCatalog.contains(CapabilityId.STRING_TWEAKS, "tripwire_hook"));
        assertTrue(VisibilityTargetCatalog.contains(CapabilityId.DEAD_CORAL, "dead_tube_coral_wall_fan"));
        assertTrue(VisibilityTargetCatalog.targets(CapabilityId.ANVIL).isEmpty());
    }

    @Test
    void nullInputsAreRejected() {
        assertThrows(NullPointerException.class, () -> VisibilityTargetCatalog.targets(null));
        assertThrows(NullPointerException.class,
                () -> VisibilityTargetCatalog.contains(CapabilityId.BLUE_ICE, null));
    }
}

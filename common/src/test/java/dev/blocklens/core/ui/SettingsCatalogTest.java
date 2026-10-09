package dev.blocklens.core.ui;

import static org.junit.jupiter.api.Assertions.*;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.CapabilityId.Category;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

final class SettingsCatalogTest {
    @Test
    void everyCapabilityIsReachableExactlyOnceInItsOwnCategory() {
        var all = new ArrayList<CapabilityId>();
        int[] expected = {13, 21, 4, 2};
        for (Category category : Category.values()) {
            var entries = SettingsCatalog.capabilities(category);
            assertEquals(expected[category.ordinal()], entries.size());
            assertTrue(entries.stream().allMatch(c -> c.category() == category));
            all.addAll(entries);
            assertThrows(UnsupportedOperationException.class, () -> entries.clear());
        }
        assertEquals(40, all.size());
        assertEquals(new HashSet<>(Arrays.asList(CapabilityId.values())), new HashSet<>(all));
    }

    @Test
    void additionalResourcesFollowTheOriginalResourceOptions() {
        var entries = SettingsCatalog.capabilities(Category.RESOURCE);
        assertEquals(CapabilityId.OBSIDIAN, entries.getFirst());
        assertEquals(CapabilityId.DEEPSLATE_REDSTONE_ORE, entries.get(17));
        assertEquals(java.util.List.of(CapabilityId.CRYING_OBSIDIAN,
                CapabilityId.NETHER_GOLD_ORE, CapabilityId.NETHER_QUARTZ_ORE),
                entries.subList(18, 21));
        assertEquals("blocklens.capability.anvil", SettingsCatalog.nameKey(CapabilityId.ANVIL));
        assertEquals("blocklens.capability.anvil.description",
                SettingsCatalog.descriptionKey(CapabilityId.ANVIL));
    }
}

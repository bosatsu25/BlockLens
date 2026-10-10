package dev.blocklens.core.extension;

import static org.junit.jupiter.api.Assertions.*;

import dev.blocklens.core.CapabilityId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class OreExtensionCatalogTest {
    @Test
    void validRegistrationsAreSortedImmutableAndFrozen() {
        var catalog = new OreExtensionCatalog();
        assertTrue(catalog.register(1, "zinc:ore", "gaming.iron_ore"));
        assertTrue(catalog.register(1, "a:deep/ore", "gaming.deepslate_diamond_ore"));
        assertTrue(catalog.register(1, "zinc:ore", "gaming.iron_ore"));
        var bindings = catalog.freeze();
        assertEquals(List.of("a:deep/ore", "zinc:ore"), new ArrayList<>(bindings.keySet()));
        assertEquals(CapabilityId.DEEPSLATE_DIAMOND_ORE, bindings.get("a:deep/ore"));
        assertEquals(CapabilityId.IRON_ORE, bindings.get("zinc:ore"));
        assertSame(bindings, catalog.freeze());
        assertThrows(UnsupportedOperationException.class, () -> bindings.clear());
        assertFalse(catalog.add("later:ore", CapabilityId.DIAMOND_ORE));
        assertFalse(catalog.add("zinc:ore", CapabilityId.IRON_ORE));
        assertEquals(2, bindings.size());
        assertFalse(catalog.overflowed());
    }

    @Test
    void invalidInputsRejectOnlyThemselvesAndDoNotReserveIdentifiers() {
        var catalog = new OreExtensionCatalog();
        for (int version : new int[]{-1, 0, 2, Integer.MAX_VALUE}) {
            assertFalse(catalog.register(version, "example:ore", "gaming.diamond_ore"));
        }
        for (String id : new String[]{null, "", "ore", ":ore", "mod:", "a:b:c", "MOD:ore", "a: ore",
                "minecraft:diamond_ore", "a:" + "x".repeat(127)}) {
            assertFalse(catalog.register(1, id, "gaming.diamond_ore"));
        }
        for (String key : new String[]{null, "", "unknown", "deco.anvil", "scene.block_filter", "x".repeat(129)}) {
            assertFalse(catalog.register(1, "example:ore", key));
        }
        assertFalse(catalog.add("example:ore", null));
        assertTrue(catalog.register(1, "example:ore", "gaming.diamond_ore"));
        assertTrue(catalog.register(1, "a:" + "x".repeat(126), "gaming.obsidian"));
        assertEquals(2, catalog.freeze().size());
    }

    @Test
    void everyExistingResourceKeyIsAcceptedWithoutAddingCapabilities() {
        var catalog = new OreExtensionCatalog();
        int accepted = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            boolean resource = capability.category() == CapabilityId.Category.RESOURCE;
            assertEquals(resource, catalog.register(1, "example:block_" + capability.ordinal(), capability.sourceKey()));
            if (resource) accepted++;
        }
        assertEquals(21, accepted);
        assertEquals(accepted, catalog.freeze().size());
    }

    @Test
    void conflictingCategoriesDisableOnlyTheirTargetRegardlessOfOrder() {
        var forward = new OreExtensionCatalog();
        var reverse = new OreExtensionCatalog();
        assertTrue(forward.add("example:ambiguous", CapabilityId.DIAMOND_ORE));
        assertFalse(forward.add("example:ambiguous", CapabilityId.GOLD_ORE));
        assertTrue(reverse.add("example:ambiguous", CapabilityId.GOLD_ORE));
        assertFalse(reverse.add("example:ambiguous", CapabilityId.DIAMOND_ORE));
        assertFalse(forward.add("example:ambiguous", CapabilityId.DIAMOND_ORE));
        assertFalse(reverse.add("example:ambiguous", CapabilityId.GOLD_ORE));
        assertTrue(forward.add("example:valid", CapabilityId.IRON_ORE));
        assertTrue(reverse.add("example:valid", CapabilityId.IRON_ORE));
        assertEquals(forward.freeze(), reverse.freeze());
        assertTrue(forward.freeze().containsKey("example:ambiguous"));
        assertNull(forward.freeze().get("example:ambiguous"));
        assertEquals(CapabilityId.IRON_ORE, forward.freeze().get("example:valid"));
    }

    @Test
    void capacityRejectsNewTargetsWhileKeepingValidDuplicatesAndConflictsBounded() {
        var catalog = new OreExtensionCatalog();
        for (int i = 0; i < OreExtensionCatalog.MAX_TARGETS; i++) {
            assertTrue(catalog.add("example:ore_" + i, CapabilityId.DIAMOND_ORE));
        }
        assertFalse(catalog.overflowed());
        assertTrue(catalog.add("example:ore_0", CapabilityId.DIAMOND_ORE));
        assertFalse(catalog.add("example:ore_0", CapabilityId.GOLD_ORE));
        assertFalse(catalog.add("example:overflow", CapabilityId.DIAMOND_ORE));
        assertTrue(catalog.overflowed());
        assertEquals(256, catalog.freeze().size());
        assertNull(catalog.freeze().get("example:ore_0"));
        assertFalse(catalog.freeze().containsKey("example:overflow"));
    }

    @Test
    void fixedConventionalMaterialsUseTheMatchingExistingControls() {
        CapabilityId[] stone = {CapabilityId.COAL_ORE, CapabilityId.COPPER_ORE, CapabilityId.DIAMOND_ORE,
                CapabilityId.EMERALD_ORE, CapabilityId.GOLD_ORE, CapabilityId.IRON_ORE, CapabilityId.LAPIS_ORE,
                CapabilityId.ANCIENT_DEBRIS, CapabilityId.NETHER_QUARTZ_ORE, CapabilityId.REDSTONE_ORE};
        CapabilityId[] deep = {CapabilityId.DEEPSLATE_COAL_ORE, CapabilityId.DEEPSLATE_COPPER_ORE,
                CapabilityId.DEEPSLATE_DIAMOND_ORE, CapabilityId.DEEPSLATE_EMERALD_ORE,
                CapabilityId.DEEPSLATE_GOLD_ORE, CapabilityId.DEEPSLATE_IRON_ORE, CapabilityId.DEEPSLATE_LAPIS_ORE,
                CapabilityId.ANCIENT_DEBRIS, CapabilityId.NETHER_QUARTZ_ORE, CapabilityId.DEEPSLATE_REDSTONE_ORE};
        for (int i = 0; i < stone.length; i++) {
            assertEquals(stone[i], OreExtensionCatalog.taggedCapability(i, false, false));
            assertEquals(deep[i], OreExtensionCatalog.taggedCapability(i, true, false));
            assertEquals(i == 4 ? CapabilityId.NETHER_GOLD_ORE : stone[i],
                    OreExtensionCatalog.taggedCapability(i, false, true));
            assertNull(OreExtensionCatalog.taggedCapability(i, true, true));
        }
        assertNull(OreExtensionCatalog.taggedCapability(-1, false, false));
        assertNull(OreExtensionCatalog.taggedCapability(10, true, false));
    }
}

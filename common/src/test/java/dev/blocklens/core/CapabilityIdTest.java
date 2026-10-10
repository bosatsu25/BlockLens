package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class CapabilityIdTest {
    @Test
    void catalogContainsCurrentCategoryCounts() {
        EnumMap<CapabilityId.Category, Long> counts = new EnumMap<>(CapabilityId.Category.class);
        for (CapabilityId capability : CapabilityId.values()) {
            counts.merge(capability.category(), 1L, Long::sum);
        }
        assertEquals(13L, counts.get(CapabilityId.Category.DECORATION));
        assertEquals(21L, counts.get(CapabilityId.Category.RESOURCE));
        assertEquals(8L, counts.get(CapabilityId.Category.OUTLINE));
        assertEquals(9L, counts.get(CapabilityId.Category.OTHER));
        assertEquals(51, CapabilityId.values().length);
        assertTrue(CapabilityId.values().length <= Long.SIZE, "capability mask exhausted");
    }

    @Test
    void originalM0OrdinalsRemainStableAndP0CapabilitiesAreAppended() {
        assertEquals(0, CapabilityId.ANVIL.ordinal());
        assertEquals(13, CapabilityId.OBSIDIAN.ordinal());
        assertEquals(36, CapabilityId.STRING_TWEAKS.ordinal());
        assertEquals(37, CapabilityId.CRYING_OBSIDIAN.ordinal());
        assertEquals(38, CapabilityId.NETHER_GOLD_ORE.ordinal());
        assertEquals(39, CapabilityId.NETHER_QUARTZ_ORE.ordinal());
    }

    @Test
    void catalogKeepsExactReferencePreset() {
        Set<String> enabled = java.util.Arrays.stream(CapabilityId.values())
                .filter(capability -> capability.ordinal() < 40)
                .filter(CapabilityId::defaultEnabled)
                .map(CapabilityId::sourceKey)
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(Set.of(
                "outline.blueice",
                "outline.deadcoral",
                "outline.powdersnow",
                "outline.sculk_catalyst",
                "others.stringtweaks"), enabled);
        assertFalse(CapabilityId.ANCIENT_DEBRIS.defaultEnabled());
        assertFalse(CapabilityId.NETHER_TWEAKS.defaultEnabled());
        assertFalse(CapabilityId.CRYING_OBSIDIAN.defaultEnabled());
        assertFalse(CapabilityId.NETHER_GOLD_ORE.defaultEnabled());
        assertFalse(CapabilityId.NETHER_QUARTZ_ORE.defaultEnabled());
        assertTrue(CapabilityId.STRING_TWEAKS.defaultEnabled());
    }

    @Test
    void sourceKeyIndexRoundTripsEveryEnumConstant() {
        Map<String, CapabilityId> index = CapabilityId.bySourceKey();
        assertEquals(CapabilityId.values().length, index.size());
        for (CapabilityId capability : CapabilityId.values()) {
            assertSame(capability, index.get(capability.sourceKey()));
        }
    }

    @Test
    void representativeMetadataRemainsStable() {
        assertEquals("deco.stairs", CapabilityId.STAIRS.sourceKey());
        assertEquals(CapabilityId.Category.DECORATION, CapabilityId.STAIRS.category());
        assertEquals("gaming.diamond_ore", CapabilityId.DIAMOND_ORE.sourceKey());
        assertEquals(CapabilityId.Category.RESOURCE, CapabilityId.DIAMOND_ORE.category());
        assertEquals("outline.powdersnow", CapabilityId.POWDER_SNOW.sourceKey());
        assertEquals(CapabilityId.Category.OUTLINE, CapabilityId.POWDER_SNOW.category());
        assertEquals("others.nethertweaks", CapabilityId.NETHER_TWEAKS.sourceKey());
        assertEquals(CapabilityId.Category.OTHER, CapabilityId.NETHER_TWEAKS.category());
        assertEquals("gaming.crying_obsidian", CapabilityId.CRYING_OBSIDIAN.sourceKey());
        assertEquals(CapabilityId.Category.RESOURCE, CapabilityId.CRYING_OBSIDIAN.category());
    }
}

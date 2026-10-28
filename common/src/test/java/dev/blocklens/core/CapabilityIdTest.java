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
    void removedOptionsAreIgnoredAndHistoricalBitsAreNotReassigned() {
        assertEquals(45, CapabilityId.BRIGHT_CHEST.bitPosition());
        assertEquals(47, CapabilityId.LAVA_ANALYZER.bitPosition());
        assertEquals(55, CapabilityId.PATTERN_CONSISTENCY.bitPosition());
        assertFalse(CapabilityId.bySourceKey().containsKey("analyzer.debris_analyzer"));
        assertFalse(CapabilityId.bySourceKey().containsKey("builder.schematic_comparison"));
        var config = BlockLensConfigCodec.decode(
                "analyzer.debris_analyzer=true\nbuilder.schematic_comparison=true\n"
                + "analyzer.lava_analyzer=true\nbuilder.pattern_consistency=true\n");
        assertTrue(config.isEnabled(CapabilityId.LAVA_ANALYZER));
        assertTrue(config.isEnabled(CapabilityId.PATTERN_CONSISTENCY));
        assertTrue((config.enabledMask() & (1L << 46)) == 0L, "removed debris analyzer bit was reused");
        assertTrue((config.enabledMask() & (1L << 56)) == 0L, "removed schematic bit was reused");
        assertEquals(47, Long.numberOfTrailingZeros(
                BlockLensConfig.defaults().withEnabled(CapabilityId.LAVA_ANALYZER, true).enabledMask()
                & (1L << 47)));
    }

    @Test
    void catalogContainsCurrentCategoryCounts() {
        EnumMap<CapabilityId.Category, Long> counts = new EnumMap<>(CapabilityId.Category.class);
        for (CapabilityId capability : CapabilityId.values()) {
            counts.merge(capability.category(), 1L, Long::sum);
        }
        assertEquals(13L, counts.get(CapabilityId.Category.DECORATION));
        assertEquals(22L, counts.get(CapabilityId.Category.RESOURCE));
        assertEquals(9L, counts.get(CapabilityId.Category.OUTLINE));
        assertEquals(17L, counts.get(CapabilityId.Category.OTHER));
        assertEquals(61, CapabilityId.values().length);
    }

    @Test
    void everyCapabilityOwnsADistinctBitOfTheSixtyFourBitMask() {
        // Two reserved bits sit below the newest capabilities, so the count alone cannot catch an
        // overflow: Java masks shift distances, and bit 64 would silently alias bit 0.
        long seen = 0L;
        for (CapabilityId capability : CapabilityId.values()) {
            int bit = capability.bitPosition();
            assertTrue(bit >= 0 && bit < Long.SIZE, capability + " has no bit left in the 64-bit mask");
            assertEquals(0L, seen & (1L << bit), capability + " shares bit " + bit);
            seen |= 1L << bit;
        }
        assertEquals(0L, seen & ((1L << 46) | (1L << 56)), "reserved retired bits must stay unused");
        assertEquals(1, Long.SIZE - Long.bitCount(seen) - 2, "exactly one free bit remains (63)");
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
    void catalogKeepsKeysWithReviewedVisibilityOffDefaults() {
        Set<String> enabled = java.util.Arrays.stream(CapabilityId.values())
                .filter(capability -> capability.ordinal() < 40)
                .filter(CapabilityId::defaultEnabled)
                .map(CapabilityId::sourceKey)
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(Set.of("others.stringtweaks"), enabled);
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

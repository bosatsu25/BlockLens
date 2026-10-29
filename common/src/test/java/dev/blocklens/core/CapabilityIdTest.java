package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void everyCapabilityOwnsADistinctBitOfTheTwoWordMask() {
        assertEquals(128, CapabilityId.MASK_BITS);
        long[] seen = new long[2];
        for (CapabilityId capability : CapabilityId.values()) {
            int bit = capability.bitPosition();
            assertTrue(bit >= 0 && bit < CapabilityId.MASK_BITS, capability + " is outside the mask");
            assertFalse(BlockLensConfig.isSet(seen[0], seen[1], bit), capability + " shares bit " + bit);
            BlockLensConfig.set(seen, bit);
        }
        assertEquals(0L, seen[0] & ((1L << 46) | (1L << 56)), "reserved retired bits must stay unused");
        assertEquals(CapabilityId.values().length, Long.bitCount(seen[0]) + Long.bitCount(seen[1]));
    }

    @Test
    void renderBitsStayInWordZeroAndNeverAlias() {
        // Java masks shift distances, so 1L << 64 would silently be bit 0: renderBit() must refuse.
        for (CapabilityId capability : CapabilityId.values()) {
            assertEquals(capability.bitPosition() < Long.SIZE, capability.hasRenderBit(), capability.name());
            if (capability.hasRenderBit()) {
                assertEquals(1L << capability.bitPosition(), capability.renderBit(), capability.name());
            }
        }
        assertEquals(1L, CapabilityId.renderBit(0));
        assertEquals(Long.MIN_VALUE, CapabilityId.renderBit(63));
        assertThrows(IllegalStateException.class, () -> CapabilityId.renderBit(64));
        assertThrows(IllegalStateException.class, () -> CapabilityId.renderBit(-1));
        assertTrue(CapabilityId.ANVIL.hasRenderBit());
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
    void sourcePackCapabilitiesAreTheFirstThirtySevenInThePacksOptionGroups() {
        assertEquals(37, CapabilityId.SOURCE_PACK_CAPABILITIES);
        Map<String, Long> groups = java.util.Arrays.stream(CapabilityId.values())
                .filter(CapabilityId::isSourcePackCapability)
                .collect(Collectors.groupingBy(capability -> capability.sourceKey().split("\\.")[0],
                        Collectors.counting()));
        assertEquals(Map.of("deco", 13L, "gaming", 18L, "outline", 4L, "others", 2L), groups);
        for (CapabilityId capability : CapabilityId.values()) {
            assertEquals(capability.ordinal() < 37, capability.isSourcePackCapability(), capability.name());
        }
        assertTrue(CapabilityId.STRING_TWEAKS.isSourcePackCapability());
        assertFalse(CapabilityId.CRYING_OBSIDIAN.isSourcePackCapability());
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

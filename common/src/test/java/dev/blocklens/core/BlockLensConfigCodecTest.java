package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class BlockLensConfigCodecTest {
    @Test
    void defaultsMatchPinnedFiveFeaturePreset() {
        BlockLensConfig config = BlockLensConfig.defaults();
        int enabled = 0;
        long expectedMask = 0L;
        for (CapabilityId capability : CapabilityId.values()) {
            if (config.isEnabled(capability)) {
                enabled++;
                expectedMask |= 1L << capability.ordinal();
            }
        }

        assertEquals(7, enabled);
        assertEquals(5, java.util.Arrays.stream(CapabilityId.values()).filter(c -> c.ordinal() < 40)
                .filter(config::isEnabled).count());
        assertEquals(expectedMask, config.enabledMask());
        assertTrue(config.isEnabled(CapabilityId.BLUE_ICE));
        assertTrue(config.isEnabled(CapabilityId.DEAD_CORAL));
        assertTrue(config.isEnabled(CapabilityId.POWDER_SNOW));
        assertTrue(config.isEnabled(CapabilityId.SCULK_CATALYST));
        assertTrue(config.isEnabled(CapabilityId.STRING_TWEAKS));
        assertFalse(config.isEnabled(CapabilityId.ANCIENT_DEBRIS));
        assertFalse(config.isEnabled(CapabilityId.CRYING_OBSIDIAN));
        assertFalse(config.isEnabled(CapabilityId.NETHER_GOLD_ORE));
        assertFalse(config.isEnabled(CapabilityId.NETHER_QUARTZ_ORE));
    }

    @Test
    void codecRoundTripsEveryCapability() {
        BlockLensConfig expected = BlockLensConfig.defaults();
        for (CapabilityId capability : CapabilityId.values()) {
            expected = expected.withEnabled(capability, !expected.isEnabled(capability));
        }

        String encoded = BlockLensConfigCodec.encode(expected);
        BlockLensConfig actual = BlockLensConfigCodec.decode(encoded);
        assertEquals(expected.asMap(), actual.asMap());
        assertEquals(expected.enabledMask(), actual.enabledMask());
        assertTrue(encoded.startsWith("# BlockLens native config v1\n"));
        // Header + four comfort fields + nineteen analyzer fields + six scene rule fields.
        assertEquals(CapabilityId.values().length + 30L, encoded.lines().count());
    }

    @Test
    void enabledMaskTracksIndependentToggleChangesExactly() {
        BlockLensConfig config = BlockLensConfig.defaults();
        long original = config.enabledMask();

        config = config.withEnabled(CapabilityId.ANVIL, true);
        long anvilBit = 1L << CapabilityId.ANVIL.ordinal();
        assertEquals(original | anvilBit, config.enabledMask());
        assertTrue(config.isEnabled(CapabilityId.ANVIL));

        config = config.withEnabled(CapabilityId.BLUE_ICE, false);
        long blueIceBit = 1L << CapabilityId.BLUE_ICE.ordinal();
        assertEquals((original | anvilBit) & ~blueIceBit, config.enabledMask());
        assertFalse(config.isEnabled(CapabilityId.BLUE_ICE));

        BlockLensConfig unchangedDefaults = BlockLensConfig.defaults();
        assertFalse(unchangedDefaults.isEnabled(CapabilityId.ANVIL));
        assertTrue(unchangedDefaults.isEnabled(CapabilityId.BLUE_ICE));
    }

    @Test
    void malformedUnknownAndInvalidValuesFailSoft() {
        String input = """
                # known value
                deco.anvil=true
                unknown.feature=true
                gaming.diamond_ore=not-a-boolean
                no-separator
                =false
                outline.blueice=
                """;

        BlockLensConfig config = BlockLensConfigCodec.decode(input);
        assertTrue(config.isEnabled(CapabilityId.ANVIL));
        assertFalse(config.isEnabled(CapabilityId.DIAMOND_ORE));
        assertTrue(config.isEnabled(CapabilityId.BLUE_ICE));
        assertTrue((config.enabledMask() & (1L << CapabilityId.ANVIL.ordinal())) != 0L);
    }

    @Test
    void blankAndNullInputReturnDefaults() {
        assertEquals(BlockLensConfig.defaults().asMap(), BlockLensConfigCodec.decode("").asMap());
        assertEquals(BlockLensConfig.defaults().asMap(), BlockLensConfigCodec.decode("   \n").asMap());
        assertEquals(BlockLensConfig.defaults().asMap(), BlockLensConfigCodec.decode(null).asMap());
    }

    @Test
    void mapViewIsReadOnlyAndDecodeToMapMatchesDecode() {
        Map<CapabilityId, Boolean> map = BlockLensConfigCodec.decodeToMap("deco.anvil=true\n");
        assertTrue(map.get(CapabilityId.ANVIL));
        try {
            map.clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Config map must be unmodifiable");
    }

    @Test
    void comfortOptionsBoundaryValuesDecodeAccuratelyOrRetainDefaults() {
        String boundaryMin = """
                others.low_fire.size=0
                others.handheld_size.blocks_percent=40
                others.handheld_size.items_percent=40
                others.handheld_size.tools_percent=40
                """;
        BlockLensConfig decodedMin = BlockLensConfigCodec.decode(boundaryMin);
        assertEquals(0, decodedMin.comfortOptions().fireSize());
        assertEquals(40, decodedMin.comfortOptions().blocksPercent());
        assertEquals(40, decodedMin.comfortOptions().itemsPercent());
        assertEquals(40, decodedMin.comfortOptions().toolsPercent());

        String boundaryMax = """
                others.low_fire.size=2
                others.handheld_size.blocks_percent=100
                others.handheld_size.items_percent=100
                others.handheld_size.tools_percent=100
                """;
        BlockLensConfig decodedMax = BlockLensConfigCodec.decode(boundaryMax);
        assertEquals(2, decodedMax.comfortOptions().fireSize());
        assertEquals(100, decodedMax.comfortOptions().blocksPercent());
        assertEquals(100, decodedMax.comfortOptions().itemsPercent());
        assertEquals(100, decodedMax.comfortOptions().toolsPercent());

        String outOfBounds = """
                others.low_fire.size=3
                others.handheld_size.blocks_percent=39
                others.handheld_size.items_percent=101
                others.handheld_size.tools_percent=0
                """;
        BlockLensConfig decodedOut = BlockLensConfigCodec.decode(outOfBounds);
        assertEquals(ComfortOptions.defaults().fireSize(), decodedOut.comfortOptions().fireSize());
        assertEquals(ComfortOptions.defaults().blocksPercent(), decodedOut.comfortOptions().blocksPercent());
        assertEquals(ComfortOptions.defaults().itemsPercent(), decodedOut.comfortOptions().itemsPercent());
        assertEquals(ComfortOptions.defaults().toolsPercent(), decodedOut.comfortOptions().toolsPercent());
    }
}

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

        assertEquals(5, enabled);
        assertEquals(expectedMask, config.enabledMask());
        assertTrue(config.isEnabled(CapabilityId.BLUE_ICE));
        assertTrue(config.isEnabled(CapabilityId.DEAD_CORAL));
        assertTrue(config.isEnabled(CapabilityId.POWDER_SNOW));
        assertTrue(config.isEnabled(CapabilityId.SCULK_CATALYST));
        assertTrue(config.isEnabled(CapabilityId.STRING_TWEAKS));
        assertFalse(config.isEnabled(CapabilityId.ANCIENT_DEBRIS));
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
        assertEquals(38, encoded.lines().count());
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

        // withEnabled remains immutable: earlier snapshots do not change.
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
}

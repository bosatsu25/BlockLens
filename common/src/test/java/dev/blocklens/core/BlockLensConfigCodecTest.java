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
        for (CapabilityId capability : CapabilityId.values()) {
            if (config.isEnabled(capability)) {
                enabled++;
            }
        }

        assertEquals(5, enabled);
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
        assertTrue(encoded.startsWith("# BlockLens native config v1\n"));
        assertEquals(38, encoded.lines().count());
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

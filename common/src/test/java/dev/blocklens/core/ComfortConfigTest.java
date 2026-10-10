package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class ComfortConfigTest {
    @Test
    void sourceComfortDefaultsAreEncodedExplicitly() {
        String encoded = BlockLensConfigCodec.encode(BlockLensConfig.defaults());
        assertTrue(encoded.contains("others.low_fire.size=1\n"));
        assertTrue(encoded.contains("others.handheld_size.blocks_percent=70\n"));
        assertTrue(encoded.contains("others.handheld_size.items_percent=60\n"));
        assertTrue(encoded.contains("others.handheld_size.tools_percent=75\n"));
    }

    @Test
    void independentBoundedOptionsRoundTripWithoutChangingLegacyControls() {
        String text = "others.low_fire.size=2\nothers.handheld_size.blocks_percent=40\n"
                + "others.handheld_size.items_percent=100\nothers.handheld_size.tools_percent=50\n"
                + "gaming.diamond_ore=true\n";
        BlockLensConfig decoded = BlockLensConfigCodec.decode(text);
        String encoded = BlockLensConfigCodec.encode(decoded);
        assertTrue(encoded.contains("others.low_fire.size=2\n"));
        assertTrue(encoded.contains("others.handheld_size.blocks_percent=40\n"));
        assertTrue(encoded.contains("others.handheld_size.items_percent=100\n"));
        assertTrue(encoded.contains("others.handheld_size.tools_percent=50\n"));
        assertTrue(decoded.isEnabled(CapabilityId.DIAMOND_ORE));
        assertFalse(decoded.isEnabled(CapabilityId.LOW_FIRE));
        assertFalse(decoded.isEnabled(CapabilityId.HANDHELD_SIZE));
        assertEquals(encoded, BlockLensConfigCodec.encode(BlockLensConfigCodec.decode(encoded)));
        assertTrue(BlockLensConfigCodec.encode(decoded.withEnabled(CapabilityId.ANVIL, true))
                .contains("others.low_fire.size=2\n"));
    }

    @Test
    void directOptionsRejectInvalidBoundsAndNullReplacement() {
        assertThrows(IllegalArgumentException.class, () -> new ComfortOptions(-1, 70, 60, 75));
        assertThrows(IllegalArgumentException.class, () -> new ComfortOptions(3, 70, 60, 75));
        for (int value : new int[] {39, 101}) {
            assertThrows(IllegalArgumentException.class, () -> new ComfortOptions(1, value, 60, 75));
            assertThrows(IllegalArgumentException.class, () -> new ComfortOptions(1, 70, value, 75));
            assertThrows(IllegalArgumentException.class, () -> new ComfortOptions(1, 70, 60, value));
        }
        assertEquals(new ComfortOptions(0, 40, 100, 40), new ComfortOptions(0, 40, 100, 40));
        assertEquals(new ComfortOptions(2, 100, 40, 100), new ComfortOptions(2, 100, 40, 100));
        assertThrows(NullPointerException.class, () -> BlockLensConfig.defaults().withComfortOptions(null));
    }

    @Test
    void invalidFieldsFailSoftIndividuallyAndValidDuplicatesStayEffective() {
        String encoded = BlockLensConfigCodec.encode(BlockLensConfigCodec.decode(
                "others.low_fire.size=-1\nothers.handheld_size.blocks_percent=39\n"
                + "others.handheld_size.items_percent=101\nothers.handheld_size.tools_percent=oops\n"));
        assertTrue(encoded.contains("others.low_fire.size=1\n"));
        assertTrue(encoded.contains("others.handheld_size.blocks_percent=70\n"));
        assertTrue(encoded.contains("others.handheld_size.items_percent=60\n"));
        assertTrue(encoded.contains("others.handheld_size.tools_percent=75\n"));
        String duplicate = BlockLensConfigCodec.encode(BlockLensConfigCodec.decode(
                "others.low_fire.size=0\nothers.low_fire.size=99999999999999999999\n"));
        assertTrue(duplicate.contains("others.low_fire.size=0\n"));
    }
}

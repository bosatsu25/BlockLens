package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.*;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigCodec;
import dev.blocklens.core.CapabilityId;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

final class PaleOakPaletteTest {
    private static final String PLANKS = "textures/block/pale_oak_planks.png";

    @Test void appendedControlPreservesHistoricalBitsAndBothRemovedBits() {
        var added = CapabilityId.bySourceKey().get("outline.pale_oak_neutral");
        assertNotNull(added);
        assertEquals(57, added.bitPosition());
        assertFalse(added.defaultEnabled());
        for (var c : CapabilityId.values()) {
            if (c.ordinal() < 55) assertEquals(c.ordinal() < 46 ? c.ordinal() : c.ordinal() + 1, c.bitPosition());
            assertNotEquals(46, c.bitPosition());
            assertNotEquals(56, c.bitPosition());
            assertTrue(c.bitPosition() < Long.SIZE);
        }
        assertEquals(CapabilityId.values().length, Arrays.stream(CapabilityId.values()).map(CapabilityId::bitPosition).distinct().count());
        var saved = BlockLensConfigCodec.decode("outline.pale_oak_neutral=true\n");
        assertTrue(saved.isEnabled(added));
        assertEquals(1L << 57, saved.enabledMask() & (1L << 57));
        assertTrue(BlockLensConfigCodec.encode(saved).contains("outline.pale_oak_neutral=true"));
    }

    @Test void supportsExactVanillaAtlasAndStandaloneTexturesIncludingModernSigns() {
        for (var path : new String[] {PLANKS, "textures/entity/boat/pale_oak.png",
                "textures/entity/chest_boat/pale_oak.png", "textures/entity/signs/pale_oak.png",
                "textures/entity/signs/hanging/pale_oak.png", "textures/item/pale_oak_sign.png",
                "textures/gui/hanging_signs/pale_oak.png", "textures/block/pale_oak_sign.png",
                "textures/block/pale_oak_hanging_sign.png", "textures/gui/signs/pale_oak.png"}) {
            assertTrue(PaleOakPalette.applies(true, "minecraft", path, "vanilla", 16, 16), path);
        }
    }

    @Test void externalPriorityDisabledUnknownAndOversizedInputsRemainUntouched() {
        assertFalse(PaleOakPalette.applies(false, "minecraft", PLANKS, "vanilla", 16, 16));
        assertFalse(PaleOakPalette.applies(true, "other", PLANKS, "vanilla", 16, 16));
        assertFalse(PaleOakPalette.applies(true, "minecraft", PLANKS, "file/custom.zip", 16, 16));
        for (var path : new String[] {"textures/block/oak_planks.png", "textures/block/pale_oak_log.png",
                "textures/block/pale_oak_leaves.png", "textures/block/pale_oak_sapling.png",
                "textures/block/pale_oak_planks.png.mcmeta", "textures/block/pale_oak_planks_extra.png", "../" + PLANKS}) {
            assertFalse(PaleOakPalette.applies(true, "minecraft", path, "vanilla", 16, 16), path);
        }
        for (int size : new int[] {-1, 0, 257, Integer.MAX_VALUE}) {
            assertFalse(PaleOakPalette.applies(true, "minecraft", PLANKS, "vanilla", size, 16));
            assertFalse(PaleOakPalette.applies(true, "minecraft", PLANKS, "vanilla", 16, size));
        }
    }

    @Test void neutralizesWarmWoodPreservingGrainAlphaAndIdempotence() {
        assertEquals(0xFFCFCFCF, PaleOakPalette.neutralWood(0xFFD8CEBE));
        assertEquals(0x80CFCFCF, PaleOakPalette.neutralWood(0x80D8CEBE));
        assertEquals(0xFFE7E7E7, PaleOakPalette.neutralWood(0xFFF0E6D6));
        assertNotEquals(PaleOakPalette.neutralWood(0xFFD8CEBE), PaleOakPalette.neutralWood(0xFFF0E6D6));
        var once = PaleOakPalette.neutralWood(0xFFD8CEBE);
        assertEquals(once, PaleOakPalette.neutralWood(once));
        assertEquals(0xFF808080, PaleOakPalette.neutralWood(0xFF808080));
    }

    @Test void preservesTransparentPixelsAndColoredHardware() {
        for (int pixel : new int[] {0x00D8CEBE, 0xFFFF9E21, 0xFF439564, 0xFF2149C0, 0xFF735028}) {
            assertEquals(pixel, PaleOakPalette.neutralWood(pixel));
        }
    }

    @Test void reloadNeededOnlyForSuccessfulPaletteBitChanges() {
        var c = CapabilityId.bySourceKey().get("outline.pale_oak_neutral");
        assertNotNull(c);
        var original = BlockLensConfig.defaults();
        var changed = original.withEnabled(c, true);
        assertTrue(PaleOakPalette.requiresReload(original, changed));
        assertTrue(PaleOakPalette.requiresReload(changed, original));
        assertFalse(PaleOakPalette.requiresReload(original, original));
        assertFalse(PaleOakPalette.requiresReload(original, original.withEnabled(CapabilityId.LOW_FIRE, true)));
    }
    @Test void warmChromaAndSizeBoundariesAreInclusiveAndNullsFailClosed() {
        assertEquals(0xFF717171, PaleOakPalette.neutralWood(0xFF806F50));
        assertEquals(0xFF806F4F, PaleOakPalette.neutralWood(0xFF806F4F));
        assertEquals(0xFF6F8050, PaleOakPalette.neutralWood(0xFF6F8050));
        assertEquals(0xFF80506F, PaleOakPalette.neutralWood(0xFF80506F));
        assertTrue(PaleOakPalette.applies(true, "minecraft", PLANKS, "vanilla", 1, 1));
        assertTrue(PaleOakPalette.applies(true, "minecraft", PLANKS, "vanilla", 256, 256));
        assertFalse(PaleOakPalette.applies(true, null, PLANKS, "vanilla", 16, 16));
        assertFalse(PaleOakPalette.applies(true, "minecraft", null, "vanilla", 16, 16));
        assertFalse(PaleOakPalette.applies(true, "minecraft", PLANKS, null, 16, 16));
    }

}

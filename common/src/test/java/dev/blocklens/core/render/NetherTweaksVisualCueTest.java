package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.CapabilityId;
import org.junit.jupiter.api.Test;

final class NetherTweaksVisualCueTest {
    @Test
    void everyCurrentNetherTargetHasAProceduralCue() {
        assertEquals(28, NetherTweaksTargetCatalog.totalBindingCount());
        assertEquals(28, NetherTweaksVisualCue.targetCount());
        for (String path : NetherTweaksTargetCatalog.targets(CapabilityId.NETHER_TWEAKS)) {
            assertNotNull(NetherTweaksVisualCue.forTarget(path), path);
        }
    }

    @Test
    void representativeFlatFillAndFramePalettesRemainStable() {
        assertCue("netherrack", 0xFF723232, 0xFF723232, 0xFF723232, 0xFF501B1B);
        assertCue("blackstone", 0xFF312C36, 0xFF312C36, 0xFF312C36, 0xFF160F10);
        assertCue("nether_bricks", 0xFF44242A, 0xFF44242A, 0xFF44242A, 0xFF30181C);
        assertCue("obsidian", 0xFF100C1C, 0xFF100C1C, 0xFF100C1C, 0xFF3B2754);
        assertCue("glowstone", 0xFFCC8654, 0xFFCC8654, 0xFFCC8654, 0xFF6F4522);
        assertCue("nether_quartz_ore", 0xFF723232, 0xFF723232, 0xFF723232, 0xFFEAE5DE);
        assertCue("shroomlight", 0xFFFEAC6D, 0xFFFEAC6D, 0xFFFEAC6D, 0xFFE47205);
        assertCue("polished_basalt", 0xFF6A6A6A, 0xFF777777, 0xFF777777, 0xFF454653);
    }

    @Test
    void nyliumRetainsDistinctTopBandAndNetherrackLowerBody() {
        NetherTweaksVisualCue crimson = NetherTweaksVisualCue.forTarget("crimson_nylium");
        NetherTweaksVisualCue warped = NetherTweaksVisualCue.forTarget("warped_nylium");

        assertEquals(0xFF723232, crimson.sideFillArgb());
        assertEquals(0xFFAC2020, crimson.upFillArgb());
        assertEquals(0xFF723232, crimson.downFillArgb());
        assertEquals(0xFF501B1B, crimson.frameArgb());
        assertEquals(0xFFAC2020, crimson.upperBandArgb());
        assertTrue(crimson.hasUpperBand());

        assertEquals(0xFF723232, warped.sideFillArgb());
        assertEquals(0xFF186F69, warped.upFillArgb());
        assertEquals(0xFF723232, warped.downFillArgb());
        assertEquals(0xFF501B1B, warped.frameArgb());
        assertEquals(0xFF186F69, warped.upperBandArgb());
        assertTrue(warped.hasUpperBand());
    }

    @Test
    void onlyNyliumUsesTheFourPixelUpperBandFamily() {
        int withBand = 0;
        for (String path : NetherTweaksTargetCatalog.targets(CapabilityId.NETHER_TWEAKS)) {
            NetherTweaksVisualCue cue = NetherTweaksVisualCue.forTarget(path);
            if (cue.hasUpperBand()) {
                withBand++;
                assertTrue(path.equals("crimson_nylium") || path.equals("warped_nylium"));
            } else {
                assertEquals(NetherTweaksVisualCue.NO_BAND, cue.upperBandArgb());
            }
        }
        assertEquals(2, withBand);
    }

    @Test
    void unknownTargetsFailClosed() {
        assertNull(NetherTweaksVisualCue.forTarget("stone"));
        assertThrows(NullPointerException.class, () -> NetherTweaksVisualCue.forTarget(null));
    }

    @Test
    void cueRejectsNonOpaquePaletteEntriesExceptNoBandSentinel() {
        assertThrows(IllegalArgumentException.class,
                () -> new NetherTweaksVisualCue(0x00723232, 0xFF723232, 0xFF723232, 0xFF501B1B, 0));
        NetherTweaksVisualCue valid = new NetherTweaksVisualCue(
                0xFF723232, 0xFF723232, 0xFF723232, 0xFF501B1B, NetherTweaksVisualCue.NO_BAND);
        assertFalse(valid.hasUpperBand());
    }

    private static void assertCue(String path, int side, int up, int down, int frame) {
        NetherTweaksVisualCue cue = NetherTweaksVisualCue.forTarget(path);
        assertNotNull(cue, path);
        assertEquals(side, cue.sideFillArgb(), path);
        assertEquals(up, cue.upFillArgb(), path);
        assertEquals(down, cue.downFillArgb(), path);
        assertEquals(frame, cue.frameArgb(), path);
        assertFalse(cue.hasUpperBand(), path);
    }
}

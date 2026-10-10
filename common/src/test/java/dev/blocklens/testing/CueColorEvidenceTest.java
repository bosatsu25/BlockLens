package dev.blocklens.testing;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class CueColorEvidenceTest {
    @Test void NativeRodCoverageRetainsItsHueBelowFullBrightness() {
        assertTrue(CueColorEvidence.matches(0x4BC3DF,0x56DFFF));
        assertTrue(CueColorEvidence.matches(0x4DC9E5,0x56DFFF));
        assertTrue(CueColorEvidence.matches(0x56DFFF,0x56DFFF));
        assertFalse(CueColorEvidence.matches(0x000000,0x56DFFF));
        assertFalse(CueColorEvidence.matches(0x143746,0x56DFFF));
        assertFalse(CueColorEvidence.matches(0xE1E1E1,0x56DFFF));
        assertFalse(CueColorEvidence.matches(0x62FF87,0x56DFFF));
    }
    @Test void AllFiveCuesRemainDistinguishableAtPartialCoverage() {
        int[] colors={0xFF8A24,0xFFDB45,0x62FF87,0x56DFFF,0xFF6DEA};
        for(int i=0;i<colors.length;i++) {
            int color=colors[i];
            int covered=((color>>16&255)*7/8)<<16 | ((color>>8&255)*7/8)<<8 | (color&255)*7/8;
            for(int j=0;j<colors.length;j++) assertEquals(i==j,CueColorEvidence.matches(covered,colors[j]));
        }
    }
}
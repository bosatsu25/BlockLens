package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class BBEChestCompatibilityTest {
    @Test
    void onlyTheInspectedMinecraftBbeAndSodiumTripleEnablesTheAdapter() {
        assertTrue(BBEChestCompatibility.supported("26.3", "1.3.9+mc26.3", "0.9.2+mc26.3"));
        for (String minecraft : new String[] {null, "", "26.1.2", "26.2", "26.4", "26.3.1"}) {
            assertFalse(BBEChestCompatibility.supported(minecraft, "1.3.9+mc26.3", "0.9.2+mc26.3"));
        }
        for (String bbe : new String[] {null, "", "1.3.8+mc26.3", "1.3.9+mc26.2", "1.4.0+mc26.3"}) {
            assertFalse(BBEChestCompatibility.supported("26.3", bbe, "0.9.2+mc26.3"));
        }
        for (String sodium : new String[] {null, "", "0.9.1+mc26.3", "0.9.2+mc26.2", "0.10.0+mc26.3"}) {
            assertFalse(BBEChestCompatibility.supported("26.3", "1.3.9+mc26.3", sodium));
        }
    }

    @Test
    void offAndNonRegularAndChristmasMaterialsNeverReceiveTheReplacement() {
        for (int inputs = 0; inputs < 8; inputs++) {
            boolean enabled = (inputs & 1) != 0;
            boolean regular = (inputs & 2) != 0;
            boolean christmas = (inputs & 4) != 0;
            assertEquals(inputs == 3, BBEChestCompatibility.useBrightMaterial(enabled, regular, christmas));
        }
    }

    @Test
    void emissiveSelectionClearsOnlyBbesNonEmissiveLightingFlag() {
        assertEquals(0, BBEChestCompatibility.emissiveFlags(0));
        assertEquals(1, BBEChestCompatibility.emissiveFlags(1));
        assertEquals(0, BBEChestCompatibility.emissiveFlags(2));
        assertEquals(1, BBEChestCompatibility.emissiveFlags(3));
        for (int original : new int[] {4, 6, 0x12345678, Integer.MAX_VALUE, Integer.MIN_VALUE, -1}) {
            int updated = BBEChestCompatibility.emissiveFlags(original);
            assertEquals(0, updated & 2);
            assertEquals(original | 2, updated | 2, "unrelated flags were changed");
            assertEquals(updated, BBEChestCompatibility.emissiveFlags(updated));
        }
    }
}

package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class SodiumTerrainCompatibilityTest {
    private static final String[][] PINS = {
            {"26.1.2", "0.9.2+mc26.1.2"},
            {"26.2", "0.9.2+mc26.2"},
            {"26.3", "0.9.2+mc26.3"}
    };

    @Test
    void acceptsEachVerifiedMinecraftAndSodiumPair() {
        for (String[] pin : PINS) {
            assertTrue(SodiumTerrainCompatibility.supported(pin[0], pin[1]), pin[0]);
        }
    }

    @Test
    void rejectsSodiumBuiltForAnotherMinecraftVersion() {
        for (String[] minecraft : PINS) {
            for (String[] sodium : PINS) {
                if (minecraft != sodium) {
                    assertFalse(SodiumTerrainCompatibility.supported(minecraft[0], sodium[1]));
                }
            }
        }
    }

    @Test
    void missingOptionalModOrMinecraftMetadataDisablesTheAdapter() {
        assertFalse(SodiumTerrainCompatibility.supported(null, null));
        assertFalse(SodiumTerrainCompatibility.supported("", ""));
        for (String[] pin : PINS) {
            assertFalse(SodiumTerrainCompatibility.supported(pin[0], null));
            assertFalse(SodiumTerrainCompatibility.supported(pin[0], ""));
            assertFalse(SodiumTerrainCompatibility.supported(null, pin[1]));
            assertFalse(SodiumTerrainCompatibility.supported("", pin[1]));
        }
    }

    @Test
    void unverifiedVersionsDoNotInheritSupportFromPrefixesOrVersionRanges() {
        for (String[] pin : PINS) {
            assertFalse(SodiumTerrainCompatibility.supported(pin[0], "0.9.2"));
            assertFalse(SodiumTerrainCompatibility.supported(pin[0], "0.9.3+mc" + pin[0]));
            assertFalse(SodiumTerrainCompatibility.supported(pin[0], pin[1] + ".1"));
            assertFalse(SodiumTerrainCompatibility.supported(pin[0], pin[1] + "-dev"));
            assertFalse(SodiumTerrainCompatibility.supported(pin[0] + ".1", pin[1]));
            assertFalse(SodiumTerrainCompatibility.supported(pin[0] + "-pre1", pin[1]));
        }
        assertFalse(SodiumTerrainCompatibility.supported("26.4", "0.9.2+mc26.4"));
    }
}

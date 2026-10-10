package dev.blocklens.core.builder;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class SchematicCompatibilityTest {
    @Test void acceptsOnlyEachExactMinecraftAndOptionalModPair() {
        String[][] pins = {{"26.1.2", "0.27.14", "0.28.12"}, {"26.2", "0.28.8", "0.29.6"},
                {"26.3", "0.29.1", "0.30.2"}};
        for (var pin : pins) {
            assertTrue(SchematicCompatibility.supported(pin[0], pin[1], pin[2]));
            assertFalse(SchematicCompatibility.supported(pin[0], pin[1] + ".1", pin[2]));
            assertFalse(SchematicCompatibility.supported(pin[0], pin[1], pin[2] + ".1"));
            for (var other : pins) if (other != pin) {
                assertFalse(SchematicCompatibility.supported(pin[0], other[1], pin[2]));
                assertFalse(SchematicCompatibility.supported(pin[0], pin[1], other[2]));
            }
        }
    }

    @Test void missingAndUnknownVersionsRemainAbsentSafe() {
        assertFalse(SchematicCompatibility.supported(null, null, null));
        assertFalse(SchematicCompatibility.supported("26.4", "0.29.1", "0.30.2"));
        assertFalse(SchematicCompatibility.supported("26.3", null, "0.30.2"));
        assertFalse(SchematicCompatibility.supported("26.3", "0.29.1", null));
        assertFalse(SchematicCompatibility.supported("26.3", "", ""));
    }
}

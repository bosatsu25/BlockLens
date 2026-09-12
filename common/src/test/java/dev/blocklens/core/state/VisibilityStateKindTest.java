package dev.blocklens.core.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.CapabilityId;
import org.junit.jupiter.api.Test;

final class VisibilityStateKindTest {
    @Test
    void mapsExactlyFiveM4Capabilities() {
        assertEquals(VisibilityStateKind.STATIC_OUTLINE,
                VisibilityStateKind.forCapability(CapabilityId.BLUE_ICE));
        assertEquals(VisibilityStateKind.STATIC_OUTLINE,
                VisibilityStateKind.forCapability(CapabilityId.DEAD_CORAL));
        assertEquals(VisibilityStateKind.STATIC_OUTLINE,
                VisibilityStateKind.forCapability(CapabilityId.POWDER_SNOW));
        assertEquals(VisibilityStateKind.SCULK_BLOOM,
                VisibilityStateKind.forCapability(CapabilityId.SCULK_CATALYST));
        assertEquals(VisibilityStateKind.TRIPWIRE,
                VisibilityStateKind.forCapability(CapabilityId.STRING_TWEAKS));
    }

    @Test
    void everythingElseRemainsOutsideM4() {
        int visible = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            boolean expected = switch (capability) {
                case BLUE_ICE, DEAD_CORAL, POWDER_SNOW, SCULK_CATALYST, STRING_TWEAKS -> true;
                default -> false;
            };
            assertEquals(expected, VisibilityStateKind.isVisibilityCapability(capability));
            if (expected) {
                visible++;
                assertTrue(VisibilityStateKind.forCapability(capability) != VisibilityStateKind.NONE);
            } else {
                assertFalse(VisibilityStateKind.isVisibilityCapability(capability));
                assertEquals(VisibilityStateKind.NONE, VisibilityStateKind.forCapability(capability));
            }
        }
        assertEquals(5, visible);
    }

    @Test
    void nullCapabilityIsRejected() {
        assertThrows(NullPointerException.class, () -> VisibilityStateKind.forCapability(null));
        assertThrows(NullPointerException.class, () -> VisibilityStateKind.isVisibilityCapability(null));
    }
}

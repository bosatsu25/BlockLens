package dev.blocklens.core.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.CapabilityId;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class DecorationStateKindTest {
    @Test
    void allThirteenDecorationCapabilitiesMapToExpectedSemanticFamilies() {
        assertEquals(DecorationStateKind.FACING, DecorationStateKind.forCapability(CapabilityId.ANVIL));
        assertEquals(DecorationStateKind.FACING_HONEY_LEVEL, DecorationStateKind.forCapability(CapabilityId.BEEHIVE));
        assertEquals(DecorationStateKind.FACING_LIT, DecorationStateKind.forCapability(CapabilityId.CAMPFIRE));
        assertEquals(DecorationStateKind.FACING, DecorationStateKind.forCapability(CapabilityId.GLAZED_TERRACOTTA));
        assertEquals(DecorationStateKind.MOUNT_FACE_FACING, DecorationStateKind.forCapability(CapabilityId.GRINDSTONE));
        assertEquals(DecorationStateKind.FENCE_GATE, DecorationStateKind.forCapability(CapabilityId.FENCE_GATE));
        assertEquals(DecorationStateKind.AXIS, DecorationStateKind.forCapability(CapabilityId.FROGLIGHT));
        assertEquals(DecorationStateKind.SLAB, DecorationStateKind.forCapability(CapabilityId.SLABS));
        assertEquals(DecorationStateKind.CONNECTIONS, DecorationStateKind.forCapability(CapabilityId.STAINED_GLASS));
        assertEquals(DecorationStateKind.STAIRS, DecorationStateKind.forCapability(CapabilityId.STAIRS));
        assertEquals(DecorationStateKind.TRAPDOOR, DecorationStateKind.forCapability(CapabilityId.TRAPDOOR));
        assertEquals(DecorationStateKind.AXIS, DecorationStateKind.forCapability(CapabilityId.WOOD));
        assertEquals(DecorationStateKind.AXIS, DecorationStateKind.forCapability(CapabilityId.LOG));
    }

    @Test
    void exactlyThirteenCapabilitiesAreDecorationStateTargets() {
        Set<CapabilityId> mapped = EnumSet.noneOf(CapabilityId.class);
        for (CapabilityId capability : CapabilityId.values()) {
            if (DecorationStateKind.isDecorationCapability(capability)) {
                mapped.add(capability);
            }
        }
        assertEquals(13, mapped.size());
        assertEquals(EnumSet.of(
                CapabilityId.ANVIL,
                CapabilityId.BEEHIVE,
                CapabilityId.CAMPFIRE,
                CapabilityId.GLAZED_TERRACOTTA,
                CapabilityId.GRINDSTONE,
                CapabilityId.FENCE_GATE,
                CapabilityId.FROGLIGHT,
                CapabilityId.SLABS,
                CapabilityId.STAINED_GLASS,
                CapabilityId.STAIRS,
                CapabilityId.TRAPDOOR,
                CapabilityId.WOOD,
                CapabilityId.LOG), mapped);
    }

    @Test
    void nonDecorationCapabilitiesRemainUnmapped() {
        assertEquals(DecorationStateKind.NONE, DecorationStateKind.forCapability(CapabilityId.OBSIDIAN));
        assertEquals(DecorationStateKind.NONE, DecorationStateKind.forCapability(CapabilityId.BLUE_ICE));
        assertEquals(DecorationStateKind.NONE, DecorationStateKind.forCapability(CapabilityId.NETHER_TWEAKS));
        assertEquals(DecorationStateKind.NONE, DecorationStateKind.forCapability(CapabilityId.STRING_TWEAKS));
        assertFalse(DecorationStateKind.isDecorationCapability(CapabilityId.DIAMOND_ORE));
        assertTrue(DecorationStateKind.isDecorationCapability(CapabilityId.STAIRS));
        assertThrows(NullPointerException.class, () -> DecorationStateKind.forCapability(null));
    }
}

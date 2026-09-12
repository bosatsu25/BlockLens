package dev.blocklens.core.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.CapabilityId;
import org.junit.jupiter.api.Test;

final class TargetCapabilityIndexTest {
    @Test
    void lookupIsFixedSizeConstantAddressedAndAllocationFree() {
        TargetCapabilityIndex index = TargetCapabilityIndex.builder(8)
                .bind(1, CapabilityId.ANVIL)
                .bind(5, CapabilityId.STAIRS)
                .build();
        assertEquals(8, index.capacity());
        assertEquals(2, index.mappedCount());
        assertEquals(2, index.bindingCount());
        assertTrue(index.contains(1, CapabilityId.ANVIL));
        assertTrue(index.contains(5, CapabilityId.STAIRS));
        assertFalse(index.contains(0, CapabilityId.ANVIL));
        assertFalse(index.contains(-1, CapabilityId.ANVIL));
        assertFalse(index.contains(8, CapabilityId.ANVIL));
        assertEquals(0L, index.mask(-1));
        assertEquals(0L, index.mask(8));
    }

    @Test
    void overlappingCapabilitiesComposeOnOneTarget() {
        TargetCapabilityIndex index = TargetCapabilityIndex.builder(3)
                .bind(1, CapabilityId.OBSIDIAN)
                .bind(1, CapabilityId.NETHER_TWEAKS)
                .bind(2, CapabilityId.LOG)
                .bind(2, CapabilityId.NETHER_TWEAKS)
                .build();

        assertEquals(2, index.mappedCount());
        assertEquals(4, index.bindingCount());
        assertTrue(index.contains(1, CapabilityId.OBSIDIAN));
        assertTrue(index.contains(1, CapabilityId.NETHER_TWEAKS));
        assertTrue(index.contains(2, CapabilityId.LOG));
        assertTrue(index.contains(2, CapabilityId.NETHER_TWEAKS));
        assertFalse(index.contains(1, CapabilityId.LOG));
    }

    @Test
    void rebindingSameCapabilityIsIdempotentAndDifferentCapabilitiesCompose() {
        TargetCapabilityIndex.Builder builder = TargetCapabilityIndex.builder(2);
        assertSame(builder, builder.bind(1, CapabilityId.LOG));
        assertSame(builder, builder.bind(1, CapabilityId.LOG));
        assertSame(builder, builder.bind(1, CapabilityId.NETHER_TWEAKS));
        TargetCapabilityIndex index = builder.build();
        assertEquals(1, index.mappedCount());
        assertEquals(2, index.bindingCount());
    }

    @Test
    void builderRejectsInvalidInputAndCannotBeReusedAfterBuild() {
        assertThrows(IllegalArgumentException.class, () -> TargetCapabilityIndex.builder(-1));
        TargetCapabilityIndex.Builder builder = TargetCapabilityIndex.builder(1);
        assertThrows(IndexOutOfBoundsException.class, () -> builder.bind(-1, CapabilityId.ANVIL));
        assertThrows(IndexOutOfBoundsException.class, () -> builder.bind(1, CapabilityId.ANVIL));
        assertThrows(NullPointerException.class, () -> builder.bind(0, null));
        TargetCapabilityIndex empty = builder.build();
        assertEquals(1, empty.capacity());
        assertEquals(0, empty.mappedCount());
        assertEquals(0, empty.bindingCount());
        assertThrows(NullPointerException.class, () -> empty.contains(0, null));
        assertThrows(IllegalStateException.class, builder::build);
        assertThrows(IllegalStateException.class, () -> builder.bind(0, CapabilityId.ANVIL));
    }
}

package dev.blocklens.core.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.blocklens.core.CapabilityId;
import org.junit.jupiter.api.Test;

final class TargetCapabilityIndexTest {
    @Test
    void lookupIsFixedSizeAndConstantAddressed() {
        TargetCapabilityIndex index = TargetCapabilityIndex.builder(8)
                .bind(1, CapabilityId.ANVIL)
                .bind(5, CapabilityId.STAIRS)
                .build();
        assertEquals(8, index.capacity());
        assertEquals(2, index.mappedCount());
        assertSame(CapabilityId.ANVIL, index.lookup(1));
        assertSame(CapabilityId.STAIRS, index.lookup(5));
        assertNull(index.lookup(0));
        assertNull(index.lookup(-1));
        assertNull(index.lookup(8));
    }

    @Test
    void rebindingSameCapabilityIsIdempotentButConflictsFail() {
        TargetCapabilityIndex.Builder builder = TargetCapabilityIndex.builder(2);
        assertSame(builder, builder.bind(1, CapabilityId.LOG));
        assertSame(builder, builder.bind(1, CapabilityId.LOG));
        assertThrows(IllegalStateException.class, () -> builder.bind(1, CapabilityId.WOOD));
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
        assertThrows(IllegalStateException.class, builder::build);
        assertThrows(IllegalStateException.class, () -> builder.bind(0, CapabilityId.ANVIL));
    }
}

package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BlockLensRuntimeTest {
    @Test
    void initializePublishesVersionConfigAndMeasuredTime() {
        BlockLensConfig config = BlockLensConfig.defaults().withEnabled(CapabilityId.ANVIL, true);
        BlockLensRuntime.initialize("26.1.2", config, 42_000L);
        assertEquals("26.1.2", BlockLensRuntime.minecraftVersion());
        assertSame(config, BlockLensRuntime.config());
        assertEquals(42_000L, BlockLensRuntime.initializationNanos());
    }

    @Test
    void twoArgumentInitializeKeepsCompatibilityWithZeroTiming() {
        BlockLensConfig config = BlockLensConfig.defaults();
        BlockLensRuntime.initialize("26.2", config);
        assertEquals("26.2", BlockLensRuntime.minecraftVersion());
        assertSame(config, BlockLensRuntime.config());
        assertEquals(0L, BlockLensRuntime.initializationNanos());
    }

    @Test
    void initializeRejectsInvalidArguments() {
        assertThrows(NullPointerException.class,
                () -> BlockLensRuntime.initialize(null, BlockLensConfig.defaults(), 0L));
        assertThrows(NullPointerException.class,
                () -> BlockLensRuntime.initialize("26.2", null, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> BlockLensRuntime.initialize("26.2", BlockLensConfig.defaults(), -1L));
    }
}

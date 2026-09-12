package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BlockLensRuntimeTest {
    @Test
    void initializePublishesVersionAndConfig() {
        BlockLensConfig config = BlockLensConfig.defaults().withEnabled(CapabilityId.ANVIL, true);
        BlockLensRuntime.initialize("26.1.2", config);
        assertEquals("26.1.2", BlockLensRuntime.minecraftVersion());
        assertSame(config, BlockLensRuntime.config());
    }

    @Test
    void initializeRejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> BlockLensRuntime.initialize(null, BlockLensConfig.defaults()));
        assertThrows(NullPointerException.class, () -> BlockLensRuntime.initialize("26.2", null));
    }
}

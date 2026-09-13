package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BlockLensRuntimeTest {
    @TempDir
    Path tempDir;

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
    void reloadConfigPublishesPersistedFileWithoutResettingRuntimeIdentity() throws IOException {
        BlockLensConfig original = BlockLensConfig.defaults();
        BlockLensRuntime.initialize("26.2", original, 77_000L);

        BlockLensConfig persisted = original
                .withEnabled(CapabilityId.ANVIL, true)
                .withEnabled(CapabilityId.STRING_TWEAKS, false)
                .withEnabled(CapabilityId.ANCIENT_DEBRIS, true);
        BlockLensConfigFiles.save(tempDir, persisted);

        BlockLensConfig reloaded = BlockLensRuntime.reloadConfig(tempDir);

        assertEquals(persisted.asMap(), reloaded.asMap());
        assertEquals(persisted.enabledMask(), BlockLensRuntime.config().enabledMask());
        assertEquals("26.2", BlockLensRuntime.minecraftVersion());
        assertEquals(77_000L, BlockLensRuntime.initializationNanos());
    }

    @Test
    void initializeRejectsInvalidArguments() {
        assertThrows(NullPointerException.class,
                () -> BlockLensRuntime.initialize(null, BlockLensConfig.defaults(), 0L));
        assertThrows(NullPointerException.class,
                () -> BlockLensRuntime.initialize("26.2", null, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> BlockLensRuntime.initialize("26.2", BlockLensConfig.defaults(), -1L));
        assertThrows(NullPointerException.class,
                () -> BlockLensRuntime.reloadConfig(null));
    }
}

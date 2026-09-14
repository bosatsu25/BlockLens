package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BlockLensConfigFilesTest {
    @TempDir
    Path tempDir;

    @Test
    void firstLoadCreatesDefaultsAndSecondLoadRoundTrips() throws IOException {
        BlockLensConfig first = BlockLensConfigFiles.loadOrCreate(tempDir);
        Path file = tempDir.resolve(BlockLensConfigFiles.FILE_NAME);
        assertTrue(Files.isRegularFile(file));
        assertEquals(BlockLensConfig.defaults().asMap(), first.asMap());

        BlockLensConfig changed = first.withEnabled(CapabilityId.ANVIL, true)
                .withEnabled(CapabilityId.BLUE_ICE, false);
        BlockLensConfigFiles.save(tempDir, changed);
        BlockLensConfig second = BlockLensConfigFiles.loadOrCreate(tempDir);
        assertTrue(second.isEnabled(CapabilityId.ANVIL));
        assertFalse(second.isEnabled(CapabilityId.BLUE_ICE));
        assertEquals(changed.asMap(), second.asMap());
    }

    @Test
    void malformedFileFallsBackPerKeyWithoutDestroyingKnownValues() throws IOException {
        Files.writeString(tempDir.resolve(BlockLensConfigFiles.FILE_NAME), """
                deco.anvil=true
                gaming.diamond_ore=invalid
                unknown.setting=true
                """);

        BlockLensConfig config = BlockLensConfigFiles.loadOrCreate(tempDir);
        assertTrue(config.isEnabled(CapabilityId.ANVIL));
        assertFalse(config.isEnabled(CapabilityId.DIAMOND_ORE));
        assertTrue(config.isEnabled(CapabilityId.STRING_TWEAKS));
    }

    @Test
    void oversizedFileIsRejectedBeforeItCanCreateUnboundedDecodeWork() throws IOException {
        Path file = tempDir.resolve(BlockLensConfigFiles.FILE_NAME);
        Files.write(file, new byte[BlockLensConfigFiles.MAX_CONFIG_BYTES + 1]);

        IOException failure = assertThrows(
                IOException.class,
                () -> BlockLensConfigFiles.loadOrCreate(tempDir));
        assertTrue(failure.getMessage().contains("exceeds"));
    }

    @Test
    void saveUsesAUniqueTemporaryFileAndLeavesNoResidue() throws IOException {
        BlockLensConfig expected = BlockLensConfig.defaults().withEnabled(CapabilityId.ANVIL, true);
        BlockLensConfigFiles.save(tempDir, expected);

        assertEquals(expected.asMap(), BlockLensConfigFiles.loadOrCreate(tempDir).asMap());
        try (var entries = Files.list(tempDir)) {
            assertEquals(1L, entries.count());
        }
    }
}

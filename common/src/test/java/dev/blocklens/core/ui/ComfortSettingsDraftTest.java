package dev.blocklens.core.ui;

import static org.junit.jupiter.api.Assertions.*;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigCodec;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.ComfortOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ComfortSettingsDraftTest {
    @TempDir Path directory;

    @Test
    void optionOnlyEditsAreChangedAndPersistWithoutChangingCapabilityBits() throws IOException {
        SettingsDraft draft = new SettingsDraft(BlockLensConfig.defaults());
        long mask = draft.config().enabledMask();
        ComfortOptions edited = new ComfortOptions(2, 40, 100, 50);
        draft.setComfortOptions(edited);
        assertTrue(draft.changed());
        assertEquals(mask, draft.config().enabledMask());
        assertTrue(draft.save(directory));
        assertFalse(draft.changed());
        assertEquals(edited, BlockLensConfigCodec.decode(Files.readString(
                directory.resolve(BlockLensConfigFiles.FILE_NAME))).comfortOptions());
    }

    @Test
    void discardAndRevertRestoreComfortBaselineWithoutWriting() {
        SettingsDraft draft = new SettingsDraft(BlockLensConfig.defaults());
        draft.setComfortOptions(new ComfortOptions(0, 100, 40, 40));
        draft.discard();
        assertEquals(ComfortOptions.defaults(), draft.config().comfortOptions());
        assertFalse(draft.changed());
        draft.setComfortOptions(new ComfortOptions(2, 40, 40, 40));
        draft.setComfortOptions(ComfortOptions.defaults());
        assertFalse(draft.changed());
        assertFalse(Files.exists(directory.resolve(BlockLensConfigFiles.FILE_NAME)));
    }

    @Test
    void failedWriteKeepsOptionEditsAndAllowsRetry() throws IOException {
        SettingsDraft draft = new SettingsDraft(BlockLensConfig.defaults());
        ComfortOptions edited = new ComfortOptions(0, 90, 80, 50);
        draft.setComfortOptions(edited);
        Path regularFile = directory.resolve("not-a-directory");
        Files.writeString(regularFile, "fixture");
        assertThrows(IOException.class, () -> draft.save(regularFile));
        assertTrue(draft.changed());
        assertEquals(edited, draft.config().comfortOptions());
        assertTrue(draft.save(directory));
        assertFalse(draft.changed());
    }
}

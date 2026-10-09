package dev.blocklens.core.ui;

import static org.junit.jupiter.api.Assertions.*;
import dev.blocklens.core.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SettingsDraftTest {
    @TempDir Path directory;

    @Test
    void editsStayLocalAndCanBeDiscarded() {
        var original = BlockLensConfig.defaults();
        var draft = new SettingsDraft(original);
        draft.toggle(CapabilityId.ANVIL);
        draft.toggle(CapabilityId.NETHER_QUARTZ_ORE);
        assertTrue(draft.changed());
        assertFalse(original.isEnabled(CapabilityId.ANVIL));
        assertTrue(draft.config().isEnabled(CapabilityId.ANVIL));
        assertTrue(draft.config().isEnabled(CapabilityId.NETHER_QUARTZ_ORE));
        assertTrue(draft.config().isEnabled(CapabilityId.POWDER_SNOW));
        draft.discard();
        assertEquals(original.enabledMask(), draft.config().enabledMask());
        assertFalse(draft.changed());
    }

    @Test
    void revertingAnEditDoesNotCreateAConfigFile() throws IOException {
        var draft = new SettingsDraft(BlockLensConfig.defaults());
        assertFalse(draft.save(directory));
        draft.toggle(CapabilityId.ANVIL);
        draft.toggle(CapabilityId.ANVIL);
        assertFalse(draft.changed());
        assertFalse(draft.save(directory));
        assertFalse(Files.exists(directory.resolve(BlockLensConfigFiles.FILE_NAME)));
    }

    @Test
    void saveFailurePreservesEditsAndCanBeRetried() throws IOException {
        var draft = new SettingsDraft(BlockLensConfig.defaults());
        draft.toggle(CapabilityId.ANVIL);
        Path notADirectory = directory.resolve("occupied");
        Files.writeString(notADirectory, "keep");
        assertThrows(IOException.class, () -> draft.save(notADirectory));
        assertEquals("keep", Files.readString(notADirectory));
        assertTrue(draft.changed());
        assertTrue(draft.config().isEnabled(CapabilityId.ANVIL));
        assertTrue(draft.save(directory));
        assertEquals(draft.config().enabledMask(),
                BlockLensConfigFiles.loadOrCreate(directory).enabledMask());
        assertFalse(draft.changed());
        assertFalse(draft.save(directory));
    }
}

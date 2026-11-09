package dev.blocklens.core.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.ComfortOptions;
import dev.blocklens.core.analyzer.AnalyzerOptions;
import dev.blocklens.core.scene.SceneFilterOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * CTFL 4.2.4 state transition testing of {@link SettingsDraft}: two states, CLEAN (the draft equals
 * the saved baseline) and DIRTY, and the events toggle, discard, save into a writable directory and
 * save into an unwritable path. Every cell of the state table is exercised, including the invalid
 * ones (save while CLEAN), so all-transitions coverage is reached; see
 * {@code knowledge/current/test-design-ctfl.md} §5 (TD-S*). A 4.2.3 decision table covers the four
 * independent conditions behind {@link SettingsDraft#changed}.
 */
final class SettingsDraftStateTransitionTest {
    @TempDir Path directory;

    private static BlockLensConfig baseline() {
        return BlockLensConfig.defaults().withEnabled(CapabilityId.DIAMOND_ORE, true);
    }

    private static void assertClean(SettingsDraft draft, BlockLensConfig baseline) {
        assertFalse(draft.changed());
        assertEquals(baseline, draft.config());
    }

    @Test
    void cleanToggleDirtyToggleBackClean() {
        var draft = new SettingsDraft(baseline());
        assertClean(draft, baseline());
        draft.toggle(CapabilityId.ANVIL);                     // CLEAN --toggle--> DIRTY
        assertTrue(draft.changed());
        draft.toggle(CapabilityId.STAIRS);                    // DIRTY --toggle(other)--> DIRTY
        assertTrue(draft.changed());
        draft.toggle(CapabilityId.STAIRS);                    // DIRTY --toggle(back)--> DIRTY
        assertTrue(draft.changed());
        draft.toggle(CapabilityId.ANVIL);                     // DIRTY --toggle(last edit back)--> CLEAN
        assertClean(draft, baseline());
    }

    @Test
    void discardFromEitherStateIsClean() throws IOException {
        var draft = new SettingsDraft(baseline());
        draft.discard();                                      // CLEAN --discard--> CLEAN
        assertClean(draft, baseline());
        draft.toggle(CapabilityId.ANVIL);
        draft.setComfortOptions(new ComfortOptions(2, 40, 40, 40));
        draft.discard();                                      // DIRTY --discard--> CLEAN
        assertClean(draft, baseline());
        assertFalse(Files.exists(directory.resolve(BlockLensConfigFiles.FILE_NAME)), "discard never writes");
    }

    @Test
    void saveWhileCleanIsANoOpEvenIntoAnUnwritablePath() throws IOException {
        var draft = new SettingsDraft(baseline());
        assertFalse(draft.save(directory));                   // CLEAN --save--> CLEAN, nothing written
        assertFalse(Files.exists(directory.resolve(BlockLensConfigFiles.FILE_NAME)));
        Path file = directory.resolve("occupied");
        Files.writeString(file, "keep");
        assertFalse(draft.save(file), "the invalid transition is refused before any I/O");
        assertEquals("keep", Files.readString(file));
        assertClean(draft, baseline());
    }

    @Test
    void successfulSaveAdvancesTheBaselineSoDiscardKeepsTheSavedEdits() throws IOException {
        var draft = new SettingsDraft(baseline());
        draft.toggle(CapabilityId.ANVIL);
        var edited = draft.config();
        assertTrue(draft.save(directory));                    // DIRTY --save ok--> CLEAN
        assertClean(draft, edited);
        assertEquals(edited, BlockLensConfigFiles.loadOrCreate(directory));
        draft.toggle(CapabilityId.STAIRS);
        draft.discard();                                      // the new baseline, not the original one
        assertClean(draft, edited);
        assertFalse(draft.save(directory));                   // CLEAN again: no rewrite
    }

    @Test
    void failedSaveKeepsTheDraftDirtyAndTheBaselineUnchanged() throws IOException {
        var draft = new SettingsDraft(baseline());
        draft.toggle(CapabilityId.ANVIL);
        var edited = draft.config();
        Path file = directory.resolve("occupied");
        Files.writeString(file, "keep");
        assertThrows(IOException.class, () -> draft.save(file));   // DIRTY --save fail--> DIRTY
        assertTrue(draft.changed());
        assertEquals(edited, draft.config());
        draft.discard();                                      // the baseline did not move
        assertClean(draft, baseline());
    }

    @Test
    void shortcutsThatChangeNothingKeepTheDraftClean() throws IOException {
        var allOn = BlockLensConfig.defaults();
        for (CapabilityId capability : CapabilityId.values()) {
            allOn = allOn.withEnabled(capability, true);
        }
        var draft = new SettingsDraft(allOn);
        draft.enableSourcePack();                             // CLEAN --no-op shortcut--> CLEAN
        draft.enableOreGlowProfile();
        draft.enableObsidianPair();
        assertClean(draft, allOn);
        assertFalse(draft.save(directory));
        draft.disableResources();                             // CLEAN --shortcut with effect--> DIRTY
        assertTrue(draft.changed());
    }

    /**
     * Decision table for {@code changed()}: C1 capabilities differ, C2 comfort differs, C3 analyzer
     * options differ, C4 scene filters differ. Any true -> changed; all false -> unchanged. The four
     * single-condition rules and the all-false rule are the minimal cover.
     */
    @Test
    void changedIsTrueWhenAnySingleDimensionDiffers() {
        var base = baseline();
        var draft = new SettingsDraft(base);
        assertFalse(draft.changed(), "rule 0: nothing differs");

        draft.toggle(CapabilityId.ANVIL);
        assertTrue(draft.changed(), "rule 1: capabilities only");
        draft.discard();

        draft.setComfortOptions(new ComfortOptions(2, 40, 40, 40));
        assertTrue(draft.changed(), "rule 2: comfort only");
        draft.discard();

        var analyzers = AnalyzerOptions.defaults();
        draft.setAnalyzerOptions(analyzers.with(0, 0, AnalyzerOptions.maximum(0, 0)));
        assertTrue(draft.changed(), "rule 3: analyzer only");
        draft.discard();

        draft.setSceneFilterOptions(SceneFilterOptions.defaults().withMode(0, SceneFilterOptions.Mode.BLACKLIST));
        assertTrue(draft.changed(), "rule 4: scene filter only");
        draft.discard();

        draft.setComfortOptions(base.comfortOptions());
        draft.setAnalyzerOptions(base.analyzerOptions());
        draft.setSceneFilterOptions(base.sceneFilterOptions());
        assertFalse(draft.changed(), "setting equal values is not a change");
    }
}

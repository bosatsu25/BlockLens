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

    @Test void glowProfileEnablesExactlyNineteenResourceControlsWithoutMutatingOtherGroups() {
        var baseline=BlockLensConfig.defaults();
        var draft=new SettingsDraft(baseline);
        draft.enableOreGlowProfile();
        assertTrue(draft.changed());
        int changed=0;
        for(var id:dev.blocklens.core.CapabilityId.values()) {
            if(id.isResourceHighlight()
                    && id!=dev.blocklens.core.CapabilityId.OBSIDIAN
                    && id!=dev.blocklens.core.CapabilityId.CRYING_OBSIDIAN) {
                changed++;
                org.junit.jupiter.api.Assertions.assertTrue(draft.config().isEnabled(id),id.sourceKey());
            } else {
                org.junit.jupiter.api.Assertions.assertEquals(baseline.isEnabled(id),
                        draft.config().isEnabled(id),id.sourceKey());
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(19,changed);
        draft.discard();
        org.junit.jupiter.api.Assertions.assertFalse(draft.changed());
    }

    @Test
    void resourceOffDisablesAllTwentyTwoControlsAndPreservesOtherSettings() {
        var original = BlockLensConfig.defaults().withComfortOptions(new ComfortOptions(2, 43, 58, 91));
        for (var id : CapabilityId.values()) original = original.withEnabled(id, true);
        var draft = new SettingsDraft(original);
        draft.disableResources();
        int disabled = 0;
        for (var id : CapabilityId.values()) {
            if (id.category() == CapabilityId.Category.RESOURCE) {
                assertFalse(draft.config().isEnabled(id), id.sourceKey());
                disabled++;
            } else assertTrue(draft.config().isEnabled(id), id.sourceKey());
        }
        assertEquals(22, disabled);
        assertEquals(original.comfortOptions(), draft.config().comfortOptions());
        assertEquals(original.analyzerOptions(), draft.config().analyzerOptions());
        assertEquals(original.sceneFilterOptions(), draft.config().sceneFilterOptions());
        draft.discard();
        assertEquals(original, draft.config());
    }

    @Test
    void sourcePackEnablesExactlyThePacksThirtySevenOptionsAcrossCategoriesAndKeepsTheRest() {
        var original = BlockLensConfig.defaults()
                .withComfortOptions(new ComfortOptions(2, 43, 58, 91))
                .withEnabled(CapabilityId.CONNECTED_ORE_EDGES, true)
                .withEnabled(CapabilityId.STRING_TWEAKS, false);
        var draft = new SettingsDraft(original);
        draft.enableSourcePack();
        int enabled = 0;
        var categories = new java.util.EnumMap<CapabilityId.Category, Integer>(CapabilityId.Category.class);
        for (var id : CapabilityId.values()) {
            if (id.isSourcePackCapability()) {
                assertTrue(draft.config().isEnabled(id), id.sourceKey());
                categories.merge(id.category(), 1, Integer::sum);
                enabled++;
            } else assertEquals(original.isEnabled(id), draft.config().isEnabled(id), id.sourceKey());
        }
        assertEquals(37, enabled);
        assertEquals(java.util.Map.of(CapabilityId.Category.DECORATION, 13, CapabilityId.Category.RESOURCE, 18,
                CapabilityId.Category.OUTLINE, 4, CapabilityId.Category.OTHER, 2), categories);
        assertEquals(original.comfortOptions(), draft.config().comfortOptions());
        assertEquals(original.analyzerOptions(), draft.config().analyzerOptions());
        assertEquals(original.sceneFilterOptions(), draft.config().sceneFilterOptions());
        assertTrue(draft.changed());
        draft.discard();
        assertEquals(original, draft.config());
    }

    @Test
    void repeatedSourcePackDoesNotRewrite() throws IOException {
        var draft = new SettingsDraft(BlockLensConfig.defaults());
        draft.enableSourcePack();
        assertTrue(draft.save(directory));
        draft.enableSourcePack();
        assertFalse(draft.changed());
        assertFalse(draft.save(directory));
    }

    @Test
    void obsidianPairPreservesEnabledAndDisabledFlagsOutsideThePair() {
        var original = BlockLensConfig.defaults()
                .withComfortOptions(new ComfortOptions(0, 45, 62, 88))
                .withEnabled(CapabilityId.DIAMOND_ORE, true)
                .withEnabled(CapabilityId.CONNECTED_ORE_EDGES, true)
                .withEnabled(CapabilityId.ANVIL, true);
        var draft = new SettingsDraft(original);
        draft.enableObsidianPair();
        for (var id : CapabilityId.values()) {
            if (id == CapabilityId.OBSIDIAN || id == CapabilityId.CRYING_OBSIDIAN) {
                assertTrue(draft.config().isEnabled(id), id.sourceKey());
            } else assertEquals(original.isEnabled(id), draft.config().isEnabled(id), id.sourceKey());
        }
        assertEquals(original.comfortOptions(), draft.config().comfortOptions());
        draft.discard();
        assertEquals(original, draft.config());
    }

    @Test
    void unchangedAllOffDoesNotCreateAFile() throws IOException {
        var draft = new SettingsDraft(BlockLensConfig.defaults());
        draft.disableResources();
        assertFalse(draft.changed());
        assertFalse(draft.save(directory));
        assertFalse(Files.exists(directory.resolve(BlockLensConfigFiles.FILE_NAME)));
    }

    @Test
    void repeatedPairDoesNotRewriteAndDiscardKeepsTheAppliedPair() throws IOException {
        var draft = new SettingsDraft(BlockLensConfig.defaults());
        draft.enableObsidianPair();
        assertTrue(draft.save(directory));
        var applied = draft.config();
        var file = directory.resolve(BlockLensConfigFiles.FILE_NAME);
        var time = java.nio.file.attribute.FileTime.fromMillis(1234000);
        Files.setLastModifiedTime(file, time);
        draft.enableObsidianPair();
        assertFalse(draft.save(directory));
        assertEquals(time, Files.getLastModifiedTime(file));
        draft.disableResources();
        draft.discard();
        assertEquals(applied, draft.config());
        assertEquals(applied, BlockLensConfigFiles.loadOrCreate(directory));
    }

    @Test
    void failedShortcutSavePreservesTheDraftForRetry() throws IOException {
        var original = BlockLensConfig.defaults().withEnabled(CapabilityId.DIAMOND_ORE, true);
        var draft = new SettingsDraft(original);
        draft.disableResources();
        draft.enableObsidianPair();
        var expected = original.withEnabled(CapabilityId.DIAMOND_ORE, false)
                .withEnabled(CapabilityId.OBSIDIAN, true).withEnabled(CapabilityId.CRYING_OBSIDIAN, true);
        var occupied = directory.resolve("occupied");
        Files.writeString(occupied, "keep");
        assertThrows(IOException.class, () -> draft.save(occupied));
        assertEquals("keep", Files.readString(occupied));
        assertEquals(expected, draft.config());
        assertTrue(draft.changed());
        assertTrue(draft.save(directory));
        assertEquals(expected, BlockLensConfigFiles.loadOrCreate(directory));
    }

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
        assertFalse(draft.config().isEnabled(CapabilityId.POWDER_SNOW));
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

package dev.blocklens.core.scene;

import static org.junit.jupiter.api.Assertions.*;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigCodec;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.ComfortOptions;
import dev.blocklens.core.ui.SettingsDraft;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SceneFilterConfigurationTest {
    @TempDir Path directory;

    @Test void bothRulesRoundTripWithoutChangingEarlierCapabilitiesOrOptions() {
        var initial=BlockLensConfig.defaults().withComfortOptions(new ComfortOptions(1,50,60,70));
        var scene=SceneFilterOptions.defaults().withList(0,false,"minecraft:stone")
                .withList(0,true,"minecraft:dirt").withMode(0,SceneFilterOptions.Mode.WHITELIST)
                .withList(1,false,"minecraft:item").withList(1,true,"minecraft:villager")
                .withMode(1,SceneFilterOptions.Mode.BLACKLIST);
        var config=initial.withSceneFilterOptions(scene).withEnabled(CapabilityId.BLOCK_FILTER,true);
        var decoded=BlockLensConfigCodec.decode(BlockLensConfigCodec.encode(config));
        assertEquals(scene,decoded.sceneFilterOptions());
        assertEquals(config.enabledMask(),decoded.enabledMask());
        assertEquals(initial.comfortOptions(),decoded.comfortOptions());
        assertEquals(initial.analyzerOptions(),decoded.analyzerOptions());
        assertFalse(decoded.isEnabled(CapabilityId.ENTITY_FILTER));
        assertEquals(scene,config.withComfortOptions(ComfortOptions.defaults()).sceneFilterOptions());
        assertEquals(scene,config.withAnalyzerOptions(initial.analyzerOptions()).sceneFilterOptions());
    }
    @Test void invalidReplacementRetainsPriorValidListsAndMode() {
        var decoded=BlockLensConfigCodec.decode("scene.block_filter.blacklist=minecraft:stone\n"
                +"scene.block_filter.blacklist=#c:ores\nscene.block_filter.mode=WHITELIST\n"
                +"scene.block_filter.mode=invalid\nscene.entity_filter.whitelist=custom:ore\n");
        assertEquals(java.util.List.of("minecraft:stone"),decoded.sceneFilterOptions().blocks().blacklist());
        assertEquals(SceneFilterOptions.Mode.WHITELIST,decoded.sceneFilterOptions().blocks().mode());
        assertEquals(java.util.List.of("custom:ore"),decoded.sceneFilterOptions().entities().whitelist());
        assertEquals(SceneFilterOptions.defaults(),BlockLensConfigCodec.decode("").sceneFilterOptions());
    }
    @Test void draftEditsRequireSaveAndDiscardRestoresBaseline() throws IOException {
        var initial=BlockLensConfig.defaults();
        var scene=SceneFilterOptions.defaults().withEntityPreset(2);
        BlockLensConfigFiles.save(directory,initial);
        var draft=new SettingsDraft(initial);
        draft.setSceneFilterOptions(scene);
        assertTrue(draft.changed());
        assertEquals(SceneFilterOptions.defaults(),BlockLensConfigFiles.loadOrCreate(directory).sceneFilterOptions());
        draft.discard();
        assertFalse(draft.changed());
        assertEquals(initial.sceneFilterOptions(),draft.config().sceneFilterOptions());
        draft.setSceneFilterOptions(scene);
        assertTrue(draft.save(directory));
        assertFalse(draft.changed());
        assertEquals(scene,BlockLensConfigFiles.loadOrCreate(directory).sceneFilterOptions());
    }
    @Test void failedSaveKeepsFilterDraftForRetry() throws IOException {
        var draft=new SettingsDraft(BlockLensConfig.defaults());
        var scene=SceneFilterOptions.defaults().withEntityPreset(1);
        draft.setSceneFilterOptions(scene);
        var file=directory.resolve("blocked");
        Files.writeString(file,"controlled test fixture");
        assertThrows(IOException.class,()->draft.save(file));
        assertTrue(draft.changed());
        assertEquals(scene,draft.config().sceneFilterOptions());
        assertTrue(draft.save(directory));
    }
}

package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M7FullParityContractTest {
    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void sharedClientGameTestRunsCurrentFullParityOracle() throws IOException {
        String smoke = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/BlockLensSmokeClientGameTest.java"));
        String oracle = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/M7FullParityOracle.java"));

        assertTrue(smoke.contains("M7FullParityOracle.verify(context, singleplayer)"));
        assertTrue(oracle.contains("enabledCount(allOn) == 40"));
        assertTrue(oracle.contains("BlockLensConfigCodec.decode(BlockLensConfigCodec.encode(allOn))"));
        assertTrue(oracle.contains("onlyEnabled(original, CapabilityId.NETHER_TWEAKS)"));
        assertTrue(oracle.contains("requireReferencePreset(preset)"));
        assertTrue(oracle.contains("m7-all40-on"));
        assertTrue(oracle.contains("m7-all40-reloaded"));
        assertTrue(oracle.contains("m7-nether-only"));
        assertTrue(oracle.contains("m7-reference-preset"));
        assertTrue(oracle.contains("m7-all40-off-active-pack"));
        assertTrue(oracle.contains("netherOnlyOffDifferentPixels"));
        assertTrue(oracle.contains("execute in minecraft:the_nether"));
        assertTrue(oracle.contains("Level.NETHER.equals(client.level.dimension())"));
        assertTrue(oracle.contains("Level.OVERWORLD.equals(client.level.dimension())"));
        assertTrue(oracle.contains("reloadResourcePacks()"));
        assertTrue(oracle.contains("MinecraftTerrainInvalidator::invalidateAll"));
        assertTrue(oracle.contains("wrappedModelCount() >= 322"));
        assertTrue(oracle.contains("targetBindings=328"));
        assertTrue(oracle.contains("uniqueTargets=322"));
        assertTrue(oracle.contains("crying_obsidian"));
        assertTrue(oracle.contains("nether_gold_ore"));
        assertTrue(oracle.contains("nether_quartz_ore"));
        assertTrue(oracle.contains("tripwire_hook"));
        assertTrue(oracle.contains("polished_basalt"));
    }

    @Test
    void netherTweaksUsesTinyReusableBlockLensOwnedGeometry() throws IOException {
        String cue = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/NetherTweaksVisualCue.java"));
        String interior = Files.readString(root().resolve(
                "common/src/main/resources/assets/blocklens/models/block/nether/interior_fill.json"));
        String band = Files.readString(root().resolve(
                "common/src/main/resources/assets/blocklens/models/block/nether/upper_band.json"));

        assertTrue(cue.contains("targetCount()"));
        assertTrue(cue.contains("crimson_nylium"));
        assertTrue(cue.contains("warped_nylium"));
        assertTrue(cue.contains("polished_basalt"));
        assertTrue(interior.contains("minecraft:block/white_concrete"));
        assertTrue(interior.contains("\"particle\": \"minecraft:block/white_concrete\""));
        assertTrue(interior.contains("[1,1,-0.02]"));
        assertTrue(interior.contains("[15,15,16.02]"));
        assertTrue(band.contains("\"particle\": \"minecraft:block/white_concrete\""));
        assertTrue(band.contains("[1,12,-0.03]"));
        assertTrue(band.contains("[16.01,12,1]"));
    }

    @Test
    void bothVersionPipelinesRegisterAndComposeNetherOverlays() throws IOException {
        for (String version : new String[]{"mc26_1_2", "mc26_2"}) {
            Path sourceRoot = root().resolve("versions").resolve(version)
                    .resolve("src/main/java/dev/blocklens/fabric");
            String plugin = Files.readString(sourceRoot.resolve("MinecraftDecorationModelPlugin.java"));
            String model = Files.readString(sourceRoot.resolve("MinecraftDecorationModel.java"));
            String overlays = Files.readString(sourceRoot.resolve("NetherTweaksOverlayModels.java"));

            assertTrue(plugin.contains("NetherTweaksOverlayModels.register(pluginContext)"));
            assertTrue(plugin.contains("NetherTweaksVisualCue.forTarget(id.getPath())"));
            assertTrue(model.contains("netherCue.frameArgb()"));
            assertTrue(model.contains("lookupInteriorOverlay()"));
            assertTrue(model.contains("lookupBandOverlay()"));
            assertTrue(model.contains("applyNonNetherInstructions(quad, enabledCapabilities)"));
            assertTrue(overlays.contains("SimpleUnbakedExtraModel.blockStateModel"));
        }
    }

    @Test
    void runtimeAndCiRequireResolvedNetherModels() throws IOException {
        String convention = Files.readString(root().resolve("gradle/version-module.gradle"));
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));

        assertTrue(convention.contains("resources.srcDir project(':common').file('src/main/resources')"));
        assertTrue(convention.contains("assets/blocklens/models/block/nether/interior_fill.json"));
        assertTrue(convention.contains("assets/blocklens/models/block/nether/upper_band.json"));
        assertTrue(convention.contains("retired Nether Tweaks side_fill model"));
        assertTrue(ci.contains("Verify BlockLens model resources resolved"));
        assertTrue(ci.contains("Missing block model: blocklens:block/nether/"));
        assertTrue(ci.contains("Missing texture references in model blocklens:block/nether/"));
        assertTrue(ci.contains("Expected exactly five M7 visual screenshots"));
    }
}

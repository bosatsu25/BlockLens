package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M3VisualEvidenceContractTest {
    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void renderedVisualOracleRemainsPartOfSharedClientGameTest() throws IOException {
        String smoke = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/BlockLensSmokeClientGameTest.java"));
        String visual = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/M3VisualParityOracle.java"));

        assertTrue(smoke.contains("M3VisualParityOracle.verify(context, singleplayer)"));
        assertTrue(visual.contains("EXPECTED_DECORATION_CAPABILITIES = 13"));
        assertTrue(visual.contains("m3-all13-on"));
        assertTrue(visual.contains("m3-all13-reloaded"));
        assertTrue(visual.contains("m3-all13-off-active-pack"));
        assertTrue(visual.contains("reloadResourcePacks()"));
        assertTrue(visual.contains("MinecraftTerrainInvalidator::invalidateAll"));
        assertTrue(visual.contains("TERRAIN_REBUILD_SETTLE_TICKS"));
        assertTrue(visual.contains("MinecraftDecorationModelPlugin.wrappedModelCount() >= 254"));
        assertTrue(visual.contains("differentPixelCount"));
        assertTrue(visual.contains("MIN_RESOURCE_PACK_MARKER_PIXELS"));
        assertTrue(visual.contains("installRuntimeConfig(original)"));
    }

    @Test
    void terrainInvalidationStaysVersionSpecific() throws IOException {
        String mc2612 = Files.readString(root().resolve(
                "versions/mc26_1_2/src/main/java/dev/blocklens/fabric/MinecraftTerrainInvalidator.java"));
        String mc262 = Files.readString(root().resolve(
                "versions/mc26_2/src/main/java/dev/blocklens/fabric/MinecraftTerrainInvalidator.java"));

        assertTrue(mc2612.contains("client.levelRenderer.allChanged()"));
        assertTrue(mc262.contains("client.levelExtractor.allChanged()"));
        assertFalse(mc2612.contains("levelExtractor"));
        assertFalse(mc262.contains("levelRenderer.allChanged()"));
    }

    @Test
    void activeResourcePackMarkerIsTestOnlyAndNotRuntimePayload() throws IOException {
        Path marker = root().resolve(
                "gametest/resources/assets/minecraft/models/block/white_glazed_terracotta.json");
        String markerJson = Files.readString(marker);
        assertTrue(markerJson.contains("minecraft:block/magenta_concrete"));

        assertFalse(Files.exists(root().resolve(
                "versions/mc26_1_2/src/main/resources/assets/minecraft/models/block/white_glazed_terracotta.json")));
        assertFalse(Files.exists(root().resolve(
                "versions/mc26_2/src/main/resources/assets/minecraft/models/block/white_glazed_terracotta.json")));
    }

    @Test
    void ciArchivesThreeRenderedScreenshotsForBothMinecraftLines() throws IOException {
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));

        assertTrue(ci.contains("Collect M3 rendered visual evidence"));
        assertTrue(ci.contains("Expected exactly three M3 visual screenshots"));
        assertTrue(ci.contains("m3-visual-manifest.txt"));
        assertTrue(ci.contains("blocklens-m3-visual-${{ matrix.minecraft }}"));
        assertTrue(ci.contains("SHA256SUMS.txt"));
        assertTrue(ci.contains(":versions:${{ matrix.module }}:runClientGameTest"));
    }
}

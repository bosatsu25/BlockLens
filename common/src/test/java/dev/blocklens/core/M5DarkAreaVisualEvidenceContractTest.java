package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M5DarkAreaVisualEvidenceContractTest {
    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void darkAreaOracleRemainsIndependentAndFramebufferBacked() throws IOException {
        String smoke = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/BlockLensSmokeClientGameTest.java"));
        String oracle = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/M5DarkAreaVisualOracle.java"));

        assertTrue(smoke.contains("M5DarkAreaVisualOracle.verify(context, singleplayer)"));
        assertTrue(oracle.contains("EXPECTED_RESOURCE_CAPABILITIES = 21"));
        assertTrue(oracle.contains("capability.category() == CapabilityId.Category.RESOURCE"));
        assertTrue(oracle.contains("crying_obsidian"));
        assertTrue(oracle.contains("nether_gold_ore"));
        assertTrue(oracle.contains("nether_quartz_ore"));
        assertTrue(oracle.contains("time set midnight"));
        assertTrue(oracle.contains("minecraft:black_concrete hollow"));
        assertTrue(oracle.contains("m5-resource-dark-on"));
        assertTrue(oracle.contains("m5-resource-dark-off"));
        assertTrue(oracle.contains("MIN_DIFFERENT_PIXELS"));
        assertTrue(oracle.contains("MIN_BRIGHTER_PIXELS"));
        assertTrue(oracle.contains("MAX_OFF_AVERAGE_LUMINANCE"));
        assertTrue(oracle.contains("brighterPixels"));
        assertTrue(oracle.contains("luminanceGain"));
        assertTrue(oracle.contains("MinecraftTerrainInvalidator::invalidateAll"));
        assertTrue(oracle.contains("MinecraftDecorationModelPlugin.wrappedModelCount() >= 322"));
        assertTrue(oracle.contains("install(original)"));
    }

    @Test
    void ciArchivesDedicatedDarkAreaEvidenceForBothMinecraftLines() throws IOException {
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));

        assertTrue(ci.contains("Collect M5 dark-area visual evidence"));
        assertTrue(ci.contains("Expected exactly two M5 dark-area screenshots"));
        assertTrue(ci.contains("m5-dark-visual-manifest.txt"));
        assertTrue(ci.contains("blocklens-m5-dark-visual-${{ matrix.minecraft }}"));
        assertTrue(ci.contains("SHA256SUMS.txt"));
    }
}

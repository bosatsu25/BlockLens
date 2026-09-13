package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M8PerformanceBaselineContractTest {
    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void baselineRemainsRealClientMeasuredAndThresholdFree() throws IOException {
        String smoke = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/BlockLensSmokeClientGameTest.java"));
        String oracle = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/M8PerformanceBaselineOracle.java"));
        String strategy = Files.readString(root().resolve(
                "knowledge/current/performance-strategy.md"));

        assertTrue(smoke.contains("M8PerformanceBaselineOracle.verify(context, singleplayer)"));
        assertTrue(oracle.contains("RELOAD_WARMUPS = 1"));
        assertTrue(oracle.contains("MEASURED_SAMPLES = 3"));
        assertTrue(oracle.contains("client.reloadResourcePacks()"));
        assertTrue(oracle.contains("com.sun.management.ThreadMXBean"));
        assertTrue(oracle.contains("MinecraftTerrainInvalidator::invalidateAll"));
        assertTrue(oracle.contains("CapabilityId.Category.RESOURCE"));
        assertTrue(oracle.contains("BLOCKLENS_M8_BASELINE"));
        assertTrue(oracle.contains("m8-performance-baseline.txt"));
        assertTrue(oracle.contains("m8-observation-no-absolute-threshold"));
        assertTrue(oracle.contains("onMinusOffAllocatedMedianBytes"));
        assertTrue(oracle.contains("onMinusOffRebuildMedianNanos"));

        // The first M8 evidence runs establish observations. A tolerance can only be frozen after
        // repeated runs show how noisy the CI runner and client harness actually are.
        assertFalse(oracle.contains("MAX_RELOAD_NANOS"));
        assertFalse(oracle.contains("MAX_ALLOCATED_BYTES"));
        assertTrue(strategy.contains(
                "Absolute performance thresholds should be frozen only after a reproducible baseline exists."));
    }

    @Test
    void resourceHighlightRenderPathReusesPrebuiltInstructionsOnBothVersions() throws IOException {
        String cue = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/ResourceHighlightCue.java"));
        String model2612 = Files.readString(root().resolve(
                "versions/mc26_1_2/src/main/java/dev/blocklens/fabric/MinecraftDecorationModel.java"));
        String model262 = Files.readString(root().resolve(
                "versions/mc26_2/src/main/java/dev/blocklens/fabric/MinecraftDecorationModel.java"));

        assertTrue(cue.contains("private final DecorationQuadInstruction instruction;"));
        assertTrue(cue.contains("this.instruction = DecorationQuadInstruction.emissiveTint(accentArgb);"));
        assertTrue(cue.contains("public DecorationQuadInstruction instruction()"));

        assertTrue(model2612.contains("instruction = resource.instruction();"));
        assertTrue(model262.contains("instruction = resource.instruction();"));
        assertFalse(model2612.contains("DecorationQuadInstruction.emissiveTint(resource.accentArgb())"));
        assertFalse(model262.contains("DecorationQuadInstruction.emissiveTint(resource.accentArgb())"));
    }
}

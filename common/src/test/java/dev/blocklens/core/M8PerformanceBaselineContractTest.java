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
        assertTrue(oracle.contains("pre-optimization-baseline-no-absolute-threshold"));
        assertTrue(oracle.contains("onMinusOffAllocatedMedianBytes"));
        assertTrue(oracle.contains("onMinusOffRebuildMedianNanos"));

        // The first M8 evidence run establishes a baseline. A tolerance can only be frozen after
        // repeated observations show how noisy the CI runner and client harness actually are.
        assertFalse(oracle.contains("MAX_RELOAD_NANOS"));
        assertFalse(oracle.contains("MAX_ALLOCATED_BYTES"));
        assertTrue(strategy.contains("Do not invent thresholds first"));
    }
}

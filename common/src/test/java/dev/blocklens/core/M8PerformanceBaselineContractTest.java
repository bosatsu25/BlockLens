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
    void baselineUsesRepeatedEvidenceForCoarseRegressionGuardsAndFramePercentiles() throws IOException {
        String smoke = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/BlockLensSmokeClientGameTest.java"));
        String oracle = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/M8PerformanceBaselineOracle.java"));
        String frameProbe = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/M8FrameTimeProbe.java"));
        String strategy = Files.readString(root().resolve(
                "knowledge/current/performance-strategy.md"));

        assertTrue(smoke.contains("M8PerformanceBaselineOracle.verify(context, singleplayer)"));
        assertTrue(oracle.contains("EXPECTED_CAPABILITIES = 40"));
        assertTrue(oracle.contains("EXPECTED_RESOURCE_CAPABILITIES = 21"));
        assertTrue(oracle.contains("RELOAD_WARMUPS = 1"));
        assertTrue(oracle.contains("MEASURED_SAMPLES = 3"));
        assertTrue(oracle.contains("MAX_RELOAD_MEDIAN_NANOS = 6_000_000_000L"));
        assertTrue(oracle.contains("MAX_REBUILD_MEDIAN_NANOS = 2_500_000_000L"));
        assertTrue(oracle.contains("MAX_ALLOCATED_MEDIAN_BYTES = 32L * 1024L * 1024L"));
        assertTrue(oracle.contains("client.reloadResourcePacks()"));
        assertTrue(oracle.contains("com.sun.management.ThreadMXBean"));
        assertTrue(oracle.contains("MinecraftTerrainInvalidator::invalidateAll"));
        assertTrue(oracle.contains("CapabilityId.Category.RESOURCE"));
        assertTrue(oracle.contains("BlockLensConfig defaultConfig = BlockLensConfig.defaults()"));
        assertTrue(oracle.contains("BlockLensConfig allOn = allOn(original)"));
        assertTrue(oracle.contains("defaultAllocatedMedianBytes"));
        assertTrue(oracle.contains("defaultFrameMainPassP99Nanos"));
        assertTrue(oracle.contains("BLOCKLENS_M8_BASELINE"));
        assertTrue(oracle.contains("m8-performance-baseline.txt"));
        assertTrue(oracle.contains("m8-coarse-regression-guard"));
        assertTrue(oracle.contains("offFrameMainPassP95Nanos"));
        assertTrue(oracle.contains("offFrameMainPassP99Nanos"));
        assertTrue(oracle.contains("reloadRetentionStable=true"));
        assertTrue(oracle.contains("retainedCapabilitySlotsAfterReload"));
        assertTrue(oracle.contains("onMinusOffAllocatedMedianBytes"));
        assertTrue(oracle.contains("onMinusOffRebuildMedianNanos"));

        assertTrue(frameProbe.contains("LevelRenderEvents.START_MAIN.register"));
        assertTrue(frameProbe.contains("LevelRenderEvents.END_MAIN.register"));
        assertTrue(frameProbe.contains("System.nanoTime()"));
        assertTrue(strategy.contains(
                "Absolute performance thresholds should be frozen only after a reproducible baseline exists."));
    }

    @Test
    void modelRetentionIsMeasuredAtBakeTimeAndLazyOverlayLookupStaysOffTheHotPath() throws IOException {
        for (String module : new String[] {"mc26_1_2", "mc26_2"}) {
            Path sourceRoot = root().resolve("versions").resolve(module).resolve("src/main/java/dev/blocklens/fabric");
            String plugin = Files.readString(sourceRoot.resolve("MinecraftDecorationModelPlugin.java"));
            String model = Files.readString(sourceRoot.resolve("MinecraftDecorationModel.java"));

            assertTrue(plugin.contains("RETAINED_CAPABILITY_SLOTS.addAndGet(count)"));
            assertTrue(plugin.contains("MAX_CAPABILITIES_PER_MODEL.accumulateAndGet(count, Math::max)"));
            assertTrue(plugin.contains("public static int retainedCapabilitySlotCount()"));
            assertTrue(plugin.contains("public static int maxCapabilitiesPerWrappedModel()"));
            assertTrue(plugin.contains("public static int netherWrappedModelCount()"));

            assertTrue(model.contains("private volatile @Nullable BlockStateModel netherInteriorOverlay;"));
            assertTrue(model.contains("private volatile @Nullable BlockStateModel netherBandOverlay;"));
            assertTrue(model.contains("if (cached != null || interiorLookupAttempted)"));
            assertTrue(model.contains("if (cached != null || bandLookupAttempted)"));
            assertTrue(model.contains("cached = lookup(NetherTweaksOverlayModels.INTERIOR);"));
            assertTrue(model.contains("cached = lookup(NetherTweaksOverlayModels.UPPER_BAND);"));
            assertTrue(model.indexOf("netherInteriorOverlay = cached;")
                    < model.indexOf("interiorLookupAttempted = true;"));
            assertTrue(model.indexOf("netherBandOverlay = cached;")
                    < model.indexOf("bandLookupAttempted = true;"));
            assertTrue(model.contains("private final Object[] descriptors;"));
            assertFalse(model.contains("private final DecorationRenderDescriptor[]"));
            assertFalse(model.contains("private final VisibilityRenderDescriptor[]"));
            assertFalse(model.contains("private final ResourceHighlightCue[]"));
            assertFalse(plugin.contains("netherInteriorLookupCount"));
            assertFalse(plugin.contains("netherBandLookupCount"));
        }
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
        assertFalse(cue.contains("private final int accentArgb;"));

        assertTrue(model2612.contains("instruction = ((ResourceHighlightCue) descriptor).instruction();"));
        assertTrue(model262.contains("instruction = ((ResourceHighlightCue) descriptor).instruction();"));
        assertFalse(model2612.contains("DecorationQuadInstruction.emissiveTint(resource.accentArgb())"));
        assertFalse(model262.contains("DecorationQuadInstruction.emissiveTint(resource.accentArgb())"));
    }

    @Test
    void decorationStateAndInstructionsAvoidRepeatHotPathAllocation() throws IOException {
        String state = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/state/SemanticState.java"));
        String policy = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/DecorationQuadCuePolicy.java"));

        assertTrue(state.contains("private static final Facing[] FACINGS = Facing.values();"));
        assertTrue(state.contains("return FACINGS[(packed & FACING_MASK) >>> FACING_SHIFT];"));
        assertFalse(state.contains("return Facing.values()["));
        assertFalse(state.contains("return Axis.values()["));
        assertFalse(state.contains("return Half.values()["));
        assertTrue(policy.contains("private static final DecorationQuadInstruction AXIS_X_TINT"));
        assertTrue(policy.contains("private static DecorationQuadInstruction tintIf("));
        assertFalse(policy.contains("return condition ? DecorationQuadInstruction.tint(color)"));
    }

    @Test
    void visibilityRenderPathReusesOneBoundedInstructionPerCueWithoutPolicyIndirection() throws IOException {
        String cue = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/VisibilityVisualCue.java"));
        String model2612 = Files.readString(root().resolve(
                "versions/mc26_1_2/src/main/java/dev/blocklens/fabric/MinecraftDecorationModel.java"));
        String model262 = Files.readString(root().resolve(
                "versions/mc26_2/src/main/java/dev/blocklens/fabric/MinecraftDecorationModel.java"));

        assertTrue(cue.contains("private final DecorationQuadInstruction instruction;"));
        assertTrue(cue.contains("this.instruction = DecorationQuadInstruction.emissiveTint(accentArgb);"));
        assertTrue(cue.contains("public DecorationQuadInstruction instruction()"));
        assertTrue(model2612.contains("instruction = visibility.cue().instruction();"));
        assertTrue(model262.contains("instruction = visibility.cue().instruction();"));
        assertFalse(model2612.contains("VisibilityQuadCuePolicy.instruction(visibility)"));
        assertFalse(model262.contains("VisibilityQuadCuePolicy.instruction(visibility)"));
        assertFalse(Files.exists(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/VisibilityQuadCuePolicy.java")));
    }

    @Test
    void unreachableRuntimePolicyWrappersStayRemoved() {
        assertFalse(Files.exists(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/ResourceHighlightPolicy.java")));
        assertFalse(Files.exists(root().resolve(
                "common/src/main/java/dev/blocklens/core/render/DecorationRenderPolicy.java")));
    }

    @Test
    void runtimeJarBudgetsFreezeTheVerifiedBaselineAndEmitSizeEvidence() throws IOException {
        String properties = Files.readString(root().resolve("gradle.properties"));
        String versionModule = Files.readString(root().resolve("gradle/version-module.gradle"));
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));

        assertTrue(properties.contains("runtime_jar_source_pack_bytes=2366865"));
        assertTrue(properties.contains("runtime_jar_hard_max_bytes=1183432"));
        assertTrue(properties.contains("runtime_jar_baseline_bytes=112640"));
        assertTrue(properties.contains("runtime_jar_release_budget_bytes=153600"));
        assertTrue(properties.contains("user-facing product ceiling is now 150 KiB"));

        assertTrue(versionModule.contains("writeRuntimeJarSizeReport"));
        assertTrue(versionModule.contains("runtime-jar-size.txt"));
        assertTrue(versionModule.contains("[compressed-category-bytes]"));
        assertTrue(versionModule.contains("[top-compressed-entries]"));
        assertTrue(versionModule.contains("runtimeBaselineBytes"));
        assertTrue(versionModule.contains("runtimeReleaseBudgetBytes"));
        assertTrue(versionModule.contains("deliberately rebaseline with reviewed justification"));
        assertTrue(versionModule.contains("dependsOn tasks.named('writeRuntimeJarSizeReport')"));
        assertTrue(occurrences(versionModule,
                "dev/blocklens/core/render/DecorationRenderPolicy.class") == 1,
                "retired DecorationRenderPolicy bytecode must appear only in the explicit rejection list");
        assertTrue(versionModule.contains("Runtime JAR contains retired policy bytecode"));

        assertTrue(ci.contains(":versions:${{ matrix.module }}:build"));
        assertTrue(ci.contains("Build and verify Minecraft ${{ matrix.minecraft }}"));
    }

    private static int occurrences(String text, String needle) {
        int count = 0;
        int from = 0;
        while (true) {
            int index = text.indexOf(needle, from);
            if (index < 0) return count;
            count++;
            from = index + needle.length();
        }
    }
}

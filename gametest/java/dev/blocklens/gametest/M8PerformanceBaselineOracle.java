package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.MinecraftDecorationModelPlugin;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
import dev.blocklens.testing.GuardedEvidence;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;

/**
 * M8 repeatable real-client performance and bounded-retention oracle.
 *
 * <p>Runs #190 and #192 established a repeatable coarse envelope on both supported Minecraft
 * versions: reload medians around 3.86-4.45 s and terrain rebuild medians around 1.49-1.53 s.
 * Allocation raw samples remain noisy, so only robust medians receive a deliberately broad gross
 * regression guard. These limits detect large regressions; they are not evidence of a speedup.</p>
 */
final class M8PerformanceBaselineOracle {
    private static final int EXPECTED_CAPABILITIES = 40;
    private static final int EXPECTED_RESOURCE_CAPABILITIES = 21;
    private static final int RELOAD_WARMUPS = 1;
    private static final int MEASURED_SAMPLES = 3;
    private static final int RELOAD_TIMEOUT_TICKS = 1200;
    private static final int REBUILD_SETTLE_TICKS = 30;
    private static final int FRAME_CAPTURE_TICKS = 80;
    private static final int MIN_FRAME_SAMPLES = 30;
    private static final int MEASUREMENT_FPS_LIMIT = 60;

    // Coarse regression guards frozen only after repeated dual-version observations (#190/#192).
    private static final long MAX_RELOAD_MEDIAN_NANOS = 6_000_000_000L;
    private static final long MAX_REBUILD_MEDIAN_NANOS = 2_500_000_000L;
    private static final long MAX_ALLOCATED_MEDIAN_BYTES = 32L * 1024L * 1024L;

    private M8PerformanceBaselineOracle() {
    }

    static void verify(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        BlockLensConfig original = BlockLensRuntime.config();
        Path outputDir = FabricLoader.getInstance().getGameDir().resolve("m8-performance");
        int measurementFpsLimit = Integer.getInteger("blocklens.test.m8.fpsLimit", MEASUREMENT_FPS_LIMIT);
        require(measurementFpsLimit == 30 || measurementFpsLimit == 60 || measurementFpsLimit == 120,
                "M8 test FPS limit must be 30, 60 or 120");

        int originalFpsLimit = context.computeOnClient(client -> client.options.framerateLimit().get());
        boolean vsyncEnabled = context.computeOnClient(client -> client.options.enableVsync().get());
        try {
            Files.createDirectories(outputDir);
            verifyFailureEvidence(outputDir);
            context.runOnClient(client -> client.options.framerateLimit().set(measurementFpsLimit));
            context.runOnClient(client -> M8FrameTimeProbe.install());
            buildScene(singleplayer);
            context.waitTicks(20);

            BlockLensConfig defaultConfig = BlockLensConfig.defaults();
            BlockLensConfig allOn = allOn(original);
            BlockLensConfig allOff = allOff(original);
            require(CapabilityId.values().length == EXPECTED_CAPABILITIES,
                    "M8 capability count changed without updating its all-on scenario");
            require(enabledResourceCount(allOn) == EXPECTED_RESOURCE_CAPABILITIES,
                    "M8 all-on config must include exactly 21 resource capabilities");
            require(enabledCount(allOn) == EXPECTED_CAPABILITIES,
                    "M8 all-on config must enable every capability");

            install(allOn);
            for (int i = 0; i < RELOAD_WARMUPS; i++) {
                measureReload(context);
            }

            long[] reloadNanos = new long[MEASURED_SAMPLES];
            int[] wrappedModelsAfterReload = new int[MEASURED_SAMPLES];
            int[] retainedSlotsAfterReload = new int[MEASURED_SAMPLES];
            int[] maxCapabilitiesPerModelAfterReload = new int[MEASURED_SAMPLES];
            int[] netherWrappedModelsAfterReload = new int[MEASURED_SAMPLES];
            ModelRetention firstRetention = null;
            for (int i = 0; i < MEASURED_SAMPLES; i++) {
                reloadNanos[i] = measureReload(context);
                ModelRetention retention = captureRetention();
                require(retention.wrappedModels() >= 322,
                        "M8 expected at least 322 wrapped models after reload, got " + retention.wrappedModels());
                require(retention.retainedCapabilitySlots() >= retention.wrappedModels(),
                        "M8 retained capability slots must cover every wrapped model");
                require(retention.maxCapabilitiesPerModel() > 0,
                        "M8 max capabilities per wrapped model must be positive");
                require(retention.netherWrappedModels() > 0,
                        "M8 expected retained Nether Tweaks models");
                if (firstRetention == null) {
                    firstRetention = retention;
                } else {
                    require(firstRetention.equals(retention),
                            "M8 model retention changed across resource reloads: first="
                                    + firstRetention + " current=" + retention);
                }
                wrappedModelsAfterReload[i] = retention.wrappedModels();
                retainedSlotsAfterReload[i] = retention.retainedCapabilitySlots();
                maxCapabilitiesPerModelAfterReload[i] = retention.maxCapabilitiesPerModel();
                netherWrappedModelsAfterReload[i] = retention.netherWrappedModels();
            }
            requirePipeline("M8 post-reload observation");

            ensureAllocationProbeAvailable();
            install(allOff);
            rebuild(context);

            long[] offAllocatedBytes = new long[MEASURED_SAMPLES];
            long[] offRelevantAllocatedBytes = new long[MEASURED_SAMPLES];
            long[] offRebuildNanos = new long[MEASURED_SAMPLES];
            long[] offRenderedFrames = new long[MEASURED_SAMPLES];
            long[] defaultAllocatedBytes = new long[MEASURED_SAMPLES];
            long[] defaultRelevantAllocatedBytes = new long[MEASURED_SAMPLES];
            long[] defaultRebuildNanos = new long[MEASURED_SAMPLES];
            long[] defaultRenderedFrames = new long[MEASURED_SAMPLES];
            long[] onAllocatedBytes = new long[MEASURED_SAMPLES];
            long[] onRelevantAllocatedBytes = new long[MEASURED_SAMPLES];
            long[] onRebuildNanos = new long[MEASURED_SAMPLES];
            long[] onRenderedFrames = new long[MEASURED_SAMPLES];

            // Alternate OFF/default/all-on groups to reduce monotonic warmup bias.
            for (int i = 0; i < MEASURED_SAMPLES; i++) {
                install(allOff);
                RebuildMeasurement off = measureRebuild(context);
                offAllocatedBytes[i] = off.totalAllocatedBytes();
                offRelevantAllocatedBytes[i] = off.renderRelevantAllocatedBytes();
                offRebuildNanos[i] = off.elapsedNanos();
                offRenderedFrames[i] = off.renderedFrames();

                install(defaultConfig);
                RebuildMeasurement defaults = measureRebuild(context);
                defaultAllocatedBytes[i] = defaults.totalAllocatedBytes();
                defaultRelevantAllocatedBytes[i] = defaults.renderRelevantAllocatedBytes();
                defaultRebuildNanos[i] = defaults.elapsedNanos();
                defaultRenderedFrames[i] = defaults.renderedFrames();

                install(allOn);
                RebuildMeasurement on = measureRebuild(context);
                onAllocatedBytes[i] = on.totalAllocatedBytes();
                onRelevantAllocatedBytes[i] = on.renderRelevantAllocatedBytes();
                onRebuildNanos[i] = on.elapsedNanos();
                onRenderedFrames[i] = on.renderedFrames();
            }

            install(allOff);
            rebuild(context);
            long[] offFrameMainPassNanos = measureFrameMainPass(context);
            install(defaultConfig);
            rebuild(context);
            long[] defaultFrameMainPassNanos = measureFrameMainPass(context);
            install(allOn);
            rebuild(context);
            long[] onFrameMainPassNanos = measureFrameMainPass(context);

            Stats reload = Stats.of(reloadNanos);
            Stats offAllocation = Stats.of(offAllocatedBytes);
            Stats defaultAllocation = Stats.of(defaultAllocatedBytes);
            Stats onAllocation = Stats.of(onAllocatedBytes);
            Stats offRelevantAllocation = Stats.of(offRelevantAllocatedBytes);
            Stats defaultRelevantAllocation = Stats.of(defaultRelevantAllocatedBytes);
            Stats onRelevantAllocation = Stats.of(onRelevantAllocatedBytes);
            Stats offRebuild = Stats.of(offRebuildNanos);
            Stats defaultRebuild = Stats.of(defaultRebuildNanos);
            Stats onRebuild = Stats.of(onRebuildNanos);
            Stats offFrameMainPass = Stats.of(offFrameMainPassNanos);
            Stats defaultFrameMainPass = Stats.of(defaultFrameMainPassNanos);
            Stats onFrameMainPass = Stats.of(onFrameMainPassNanos);

            long allocationMedianDelta = onAllocation.median() - offAllocation.median();
            long relevantAllocationMedianDelta =
                    onRelevantAllocation.median() - offRelevantAllocation.median();
            long rebuildMedianDelta = onRebuild.median() - offRebuild.median();
            long frameMainPassMedianDelta = onFrameMainPass.median() - offFrameMainPass.median();

            boolean reloadWithinGuard = reload.median() <= MAX_RELOAD_MEDIAN_NANOS;
            boolean rebuildWithinGuard = offRebuild.median() <= MAX_REBUILD_MEDIAN_NANOS
                    && defaultRebuild.median() <= MAX_REBUILD_MEDIAN_NANOS
                    && onRebuild.median() <= MAX_REBUILD_MEDIAN_NANOS;
            boolean allocationWithinGuard = offAllocation.median() <= MAX_ALLOCATED_MEDIAN_BYTES
                    && defaultAllocation.median() <= MAX_ALLOCATED_MEDIAN_BYTES
                    && onAllocation.median() <= MAX_ALLOCATED_MEDIAN_BYTES
                    && offRelevantAllocation.median() <= MAX_ALLOCATED_MEDIAN_BYTES
                    && defaultRelevantAllocation.median() <= MAX_ALLOCATED_MEDIAN_BYTES
                    && onRelevantAllocation.median() <= MAX_ALLOCATED_MEDIAN_BYTES;
            ModelRetention observedRetention = firstRetention;
            GuardedEvidence.publishBeforeChecks(() -> writeManifest(
                    outputDir,
                    reloadNanos,
                    reload,
                    wrappedModelsAfterReload,
                    retainedSlotsAfterReload,
                    maxCapabilitiesPerModelAfterReload,
                    netherWrappedModelsAfterReload,
                    offAllocatedBytes,
                    defaultAllocatedBytes,
                    onAllocatedBytes,
                    offAllocation,
                    defaultAllocation,
                    onAllocation,
                    offRelevantAllocatedBytes,
                    defaultRelevantAllocatedBytes,
                    onRelevantAllocatedBytes,
                    offRelevantAllocation,
                    defaultRelevantAllocation,
                    onRelevantAllocation,
                    offRebuildNanos,
                    defaultRebuildNanos,
                    onRebuildNanos,
                    offRebuild,
                    defaultRebuild,
                    onRebuild,
                    offFrameMainPassNanos,
                    defaultFrameMainPassNanos,
                    onFrameMainPassNanos,
                    offFrameMainPass,
                    defaultFrameMainPass,
                    onFrameMainPass,
                    allocationMedianDelta,
                    relevantAllocationMedianDelta,
                    rebuildMedianDelta,
                    frameMainPassMedianDelta,
                    observedRetention,
                    offRenderedFrames, defaultRenderedFrames, onRenderedFrames,
                    reloadWithinGuard, rebuildWithinGuard, allocationWithinGuard,
                    measurementFpsLimit, vsyncEnabled), () -> {
                require(reloadWithinGuard,
                        "M8 reload median exceeded coarse regression guard: " + reload.median());
                require(rebuildWithinGuard,
                        "M8 rebuild median exceeded coarse regression guard: off="
                                + offRebuild.median() + " default=" + defaultRebuild.median()
                                + " allOn=" + onRebuild.median());
                require(allocationWithinGuard,
                        "M8 allocation median exceeded coarse regression guard: off=" + offAllocation.median()
                                + " default=" + defaultAllocation.median() + " allOn=" + onAllocation.median()
                                + " offRelevant=" + offRelevantAllocation.median()
                                + " defaultRelevant=" + defaultRelevantAllocation.median()
                                + " allOnRelevant=" + onRelevantAllocation.median());
            });

            System.out.println("BLOCKLENS_M8_BASELINE minecraft=" + BlockLensRuntime.minecraftVersion()
                    + " measuredSamples=" + MEASURED_SAMPLES
                    + " measurementFpsLimit=" + measurementFpsLimit
                    + " testAfkTimerReset=true"
                    + " offRenderedFrames=" + csv(offRenderedFrames)
                    + " defaultRenderedFrames=" + csv(defaultRenderedFrames)
                    + " onRenderedFrames=" + csv(onRenderedFrames)
                    + " reloadMedianNanos=" + reload.median()
                    + " reloadP95NearestRankNanos=" + reload.p95NearestRank()
                    + " offAllocatedMedianBytes=" + offAllocation.median()
                    + " defaultAllocatedMedianBytes=" + defaultAllocation.median()
                    + " onAllocatedMedianBytes=" + onAllocation.median()
                    + " onMinusOffAllocatedMedianBytes=" + allocationMedianDelta
                    + " offRelevantAllocatedMedianBytes=" + offRelevantAllocation.median()
                    + " defaultRelevantAllocatedMedianBytes=" + defaultRelevantAllocation.median()
                    + " onRelevantAllocatedMedianBytes=" + onRelevantAllocation.median()
                    + " onMinusOffRelevantAllocatedMedianBytes=" + relevantAllocationMedianDelta
                    + " offRebuildMedianNanos=" + offRebuild.median()
                    + " defaultRebuildMedianNanos=" + defaultRebuild.median()
                    + " onRebuildMedianNanos=" + onRebuild.median()
                    + " onMinusOffRebuildMedianNanos=" + rebuildMedianDelta
                    + " offFrameMainPassP99Nanos=" + offFrameMainPass.p99NearestRank()
                    + " defaultFrameMainPassP99Nanos=" + defaultFrameMainPass.p99NearestRank()
                    + " onFrameMainPassP99Nanos=" + onFrameMainPass.p99NearestRank()
                    + " wrappedModels=" + firstRetention.wrappedModels()
                    + " retainedCapabilitySlots=" + firstRetention.retainedCapabilitySlots()
                    + " maxCapabilitiesPerModel=" + firstRetention.maxCapabilitiesPerModel());

            require(Files.isRegularFile(outputDir.resolve("m8-performance-baseline.txt")),
                    "M8 performance manifest was not written");
            require(reload.min() > 0L, "M8 resource reload timing must be positive");
            require(offAllocation.min() >= 0L && defaultAllocation.min() >= 0L && onAllocation.min() >= 0L,
                    "M8 allocation deltas must be non-negative");
            require(offRebuild.min() > 0L && defaultRebuild.min() > 0L && onRebuild.min() > 0L,
                    "M8 rebuild timing must be positive");
            require(offFrameMainPassNanos.length >= MIN_FRAME_SAMPLES
                            && defaultFrameMainPassNanos.length >= MIN_FRAME_SAMPLES
                            && onFrameMainPassNanos.length >= MIN_FRAME_SAMPLES,
                    "M8 frame main-pass sample count too small: off="
                            + offFrameMainPassNanos.length + " default="
                            + defaultFrameMainPassNanos.length + " allOn=" + onFrameMainPassNanos.length);
        } catch (IOException exception) {
            throw new AssertionError("M8 performance observation failed", exception);
        } finally {
            M8FrameTimeProbe.endCapture();
            context.runOnClient(client -> client.options.framerateLimit().set(originalFpsLimit));
            install(original);
            rebuild(context);
        }
    }

    private static void verifyFailureEvidence(Path outputDir) throws IOException {
        Path evidence = outputDir.resolve("m8-failure-order.txt");
        AssertionError expected = new AssertionError("synthetic M8 guard exceeded");
        try {
            GuardedEvidence.publishBeforeChecks(
                    () -> Files.writeString(evidence,
                            "fixture=synthetic failure; allocationGuard=failed; allocatedBytes=33554433\n",
                            StandardCharsets.UTF_8),
                    () -> { throw expected; });
            throw new AssertionError("M8 synthetic guard did not fail");
        } catch (AssertionError failure) {
            if (failure != expected) throw failure;
            require(Files.readString(evidence, StandardCharsets.UTF_8).contains("33554433"),
                    "M8 failure must preserve raw evidence before propagating");
        }
    }

    private static void buildScene(TestSingleplayerContext singleplayer) {
        var server = singleplayer.getServer();
        server.runCommand("gamerule minecraft:advance_time false");
        server.runCommand("gamerule minecraft:advance_weather false");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("gamemode spectator @a");
        server.runCommand("fill -11 -61 -6 11 -50 16 minecraft:air");
        server.runCommand("fill -11 -60 -6 11 -60 16 minecraft:smooth_quartz");

        String[] resources = {
                "obsidian", "ancient_debris", "diamond_ore", "deepslate_diamond_ore",
                "gold_ore", "deepslate_gold_ore", "emerald_ore", "deepslate_emerald_ore",
                "coal_ore", "deepslate_coal_ore", "iron_ore", "deepslate_iron_ore",
                "copper_ore", "deepslate_copper_ore", "lapis_ore", "deepslate_lapis_ore",
                "redstone_ore", "deepslate_redstone_ore", "crying_obsidian",
                "nether_gold_ore", "nether_quartz_ore"
        };
        int[] xs = {-9, -6, -3, 0, 3, 6, 9};
        int[] zs = {-2, 1, 4};
        int index = 0;
        for (int z : zs) {
            for (int x : xs) {
                String block = resources[index++];
                for (int y = -59; y <= -56; y++) {
                    server.runCommand("setblock " + x + " " + y + " " + z + " minecraft:" + block);
                }
            }
        }

        String[] nonResources = {
                "anvil", "beehive", "campfire", "white_glazed_terracotta", "grindstone",
                "oak_fence_gate", "ochre_froglight", "stone_slab", "white_stained_glass",
                "stone_stairs", "oak_trapdoor", "oak_wood", "oak_log", "blue_ice",
                "dead_brain_coral_block", "powder_snow", "sculk_catalyst", "blackstone", "tripwire"
        };
        index = 0;
        for (int z : new int[] {7, 10, 13}) {
            for (int x : xs) {
                if (index >= nonResources.length) break;
                String block = nonResources[index++];
                for (int y = -59; y <= -56; y++) {
                    server.runCommand("setblock " + x + " " + y + " " + z + " minecraft:" + block);
                }
            }
        }
        server.runCommand("tp @a 0 -51 22 180 18");
    }

    private static long measureReload(ClientGameTestContext context) {
        long start = System.nanoTime();
        CompletableFuture<Void> reload = context.computeOnClient(client -> client.reloadResourcePacks());
        context.waitFor(client -> reload.isDone(), RELOAD_TIMEOUT_TICKS);
        reload.join();
        long elapsed = System.nanoTime() - start;
        context.waitTicks(2);
        return elapsed;
    }

    private static RebuildMeasurement measureRebuild(ClientGameTestContext context) {
        prepareMeasurementWindow(context);
        AllocationSnapshot before = captureAllocation();
        long framesBefore = M8FrameTimeProbe.renderedFrames();
        long start = System.nanoTime();
        context.runOnClient(MinecraftTerrainInvalidator::invalidateAll);
        context.waitTicks(REBUILD_SETTLE_TICKS);
        long elapsed = System.nanoTime() - start;
        AllocationSnapshot after = captureAllocation();
        return new RebuildMeasurement(
                elapsed,
                before.totalDeltaTo(after),
                before.renderRelevantDeltaTo(after),
                M8FrameTimeProbe.renderedFrames() - framesBefore);
    }

    private static long[] measureFrameMainPass(ClientGameTestContext context) {
        prepareMeasurementWindow(context);
        M8FrameTimeProbe.beginCapture();
        context.waitTicks(FRAME_CAPTURE_TICKS);
        long[] samples = M8FrameTimeProbe.endCapture();
        require(samples.length >= MIN_FRAME_SAMPLES,
                "M8 did not capture enough rendered main-pass frames: " + samples.length);
        return samples;
    }

    private static void prepareMeasurementWindow(ClientGameTestContext context) {
        context.runOnClient(client -> {
            // Test bookkeeping only: no key, mouse, interaction or world action is synthesized.
            client.getFramerateLimitTracker().onInputReceived();
            require(client.getFramerateLimitTracker().getThrottleReason()
                            == com.mojang.blaze3d.platform.FramerateLimitTracker.FramerateThrottleReason.NONE
                            && client.getFramerateLimitTracker().getFramerateLimit()
                            == client.options.framerateLimit().get(),
                    "M8 test window must not be AFK-throttled or minimized");
        });
    }

    private static void rebuild(ClientGameTestContext context) {
        context.runOnClient(MinecraftTerrainInvalidator::invalidateAll);
        context.waitTicks(REBUILD_SETTLE_TICKS);
    }

    private static void ensureAllocationProbeAvailable() {
        java.lang.management.ThreadMXBean standard = ManagementFactory.getThreadMXBean();
        require(standard instanceof com.sun.management.ThreadMXBean,
                "M8 allocation probe requires com.sun.management.ThreadMXBean");
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) standard;
        require(bean.isThreadAllocatedMemorySupported(),
                "M8 JVM does not support per-thread allocated-memory accounting");
        if (!bean.isThreadAllocatedMemoryEnabled()) {
            bean.setThreadAllocatedMemoryEnabled(true);
        }
        require(bean.isThreadAllocatedMemoryEnabled(),
                "M8 could not enable per-thread allocated-memory accounting");
    }

    private static AllocationSnapshot captureAllocation() {
        java.lang.management.ThreadMXBean standard = ManagementFactory.getThreadMXBean();
        com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) standard;
        long[] ids = bean.getAllThreadIds();
        long[] allocatedBytes = new long[ids.length];
        String[] names = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            allocatedBytes[i] = bean.getThreadAllocatedBytes(ids[i]);
            ThreadInfo info = bean.getThreadInfo(ids[i]);
            names[i] = info == null ? "" : info.getThreadName();
        }
        return new AllocationSnapshot(ids, allocatedBytes, names);
    }

    private static BlockLensConfig allOn(BlockLensConfig base) {
        BlockLensConfig result = base;
        for (CapabilityId capability : CapabilityId.values()) {
            result = result.withEnabled(capability, true);
        }
        return result;
    }

    private static int enabledResourceCount(BlockLensConfig config) {
        int count = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.category() == CapabilityId.Category.RESOURCE && config.isEnabled(capability)) count++;
        }
        return count;
    }

    private static BlockLensConfig allOff(BlockLensConfig base) {
        BlockLensConfig result = base;
        for (CapabilityId capability : CapabilityId.values()) {
            result = result.withEnabled(capability, false);
        }
        return result;
    }

    private static int enabledCount(BlockLensConfig config) {
        int count = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            if (config.isEnabled(capability)) count++;
        }
        return count;
    }

    private static void install(BlockLensConfig config) {
        BlockLensRuntime.initialize(
                BlockLensRuntime.minecraftVersion(),
                config,
                BlockLensRuntime.initializationNanos());
    }

    private static void requirePipeline(String phase) {
        require(MinecraftDecorationModelPlugin.isModelPipelineReady(), phase + ": model pipeline not ready");
        require(MinecraftDecorationModelPlugin.wrappedModelCount() >= 322,
                phase + ": expected at least 322 wrapped models, got "
                        + MinecraftDecorationModelPlugin.wrappedModelCount());
    }

    private static ModelRetention captureRetention() {
        return new ModelRetention(
                MinecraftDecorationModelPlugin.wrappedModelCount(),
                MinecraftDecorationModelPlugin.retainedCapabilitySlotCount(),
                MinecraftDecorationModelPlugin.maxCapabilitiesPerWrappedModel(),
                MinecraftDecorationModelPlugin.netherWrappedModelCount());
    }

    private static void writeManifest(
            Path outputDir,
            long[] reloadNanos,
            Stats reload,
            int[] wrappedModelsAfterReload,
            int[] retainedSlotsAfterReload,
            int[] maxCapabilitiesPerModelAfterReload,
            int[] netherWrappedModelsAfterReload,
            long[] offAllocatedBytes,
            long[] defaultAllocatedBytes,
            long[] onAllocatedBytes,
            Stats offAllocation,
            Stats defaultAllocation,
            Stats onAllocation,
            long[] offRelevantAllocatedBytes,
            long[] defaultRelevantAllocatedBytes,
            long[] onRelevantAllocatedBytes,
            Stats offRelevantAllocation,
            Stats defaultRelevantAllocation,
            Stats onRelevantAllocation,
            long[] offRebuildNanos,
            long[] defaultRebuildNanos,
            long[] onRebuildNanos,
            Stats offRebuild,
            Stats defaultRebuild,
            Stats onRebuild,
            long[] offFrameMainPassNanos,
            long[] defaultFrameMainPassNanos,
            long[] onFrameMainPassNanos,
            Stats offFrameMainPass,
            Stats defaultFrameMainPass,
            Stats onFrameMainPass,
            long allocationMedianDelta,
            long relevantAllocationMedianDelta,
            long rebuildMedianDelta,
            long frameMainPassMedianDelta,
            ModelRetention retention,
            long[] offRenderedFrames, long[] defaultRenderedFrames, long[] onRenderedFrames,
            boolean reloadWithinGuard, boolean rebuildWithinGuard, boolean allocationWithinGuard,
            int measurementFpsLimit, boolean vsyncEnabled) throws IOException {
        String manifest = "minecraft=" + BlockLensRuntime.minecraftVersion() + "\n"
                + "mode=m8-coarse-regression-guard\n"
                + "reloadGuard=" + (reloadWithinGuard ? "passed" : "failed") + "\n"
                + "rebuildGuard=" + (rebuildWithinGuard ? "passed" : "failed") + "\n"
                + "allocationGuard=" + (allocationWithinGuard ? "passed" : "failed") + "\n"
                + "resourceCapabilities=" + EXPECTED_RESOURCE_CAPABILITIES + "\n"
                + "resourceBlocksInScene=" + (EXPECTED_RESOURCE_CAPABILITIES * 4) + "\n"
                + "reloadWarmups=" + RELOAD_WARMUPS + "\n"
                + "measuredSamples=" + MEASURED_SAMPLES + "\n"
                + "maxReloadMedianNanos=" + MAX_RELOAD_MEDIAN_NANOS + "\n"
                + "maxRebuildMedianNanos=" + MAX_REBUILD_MEDIAN_NANOS + "\n"
                + "maxAllocatedMedianBytes=" + MAX_ALLOCATED_MEDIAN_BYTES + "\n"
                + "reloadNanos=" + csv(reloadNanos) + "\n"
                + "reloadMedianNanos=" + reload.median() + "\n"
                + "reloadP95NearestRankNanos=" + reload.p95NearestRank() + "\n"
                + "reloadP99NearestRankNanos=" + reload.p99NearestRank() + "\n"
                + "wrappedModelsAfterReload=" + csv(wrappedModelsAfterReload) + "\n"
                + "retainedCapabilitySlotsAfterReload=" + csv(retainedSlotsAfterReload) + "\n"
                + "maxCapabilitiesPerModelAfterReload=" + csv(maxCapabilitiesPerModelAfterReload) + "\n"
                + "netherWrappedModelsAfterReload=" + csv(netherWrappedModelsAfterReload) + "\n"
                + "reloadRetentionStable=true\n"
                + "measurementFpsLimit=" + measurementFpsLimit + "\n"
                + "fpsLimitMeaning=requested upper bound; actual frames are recorded separately\n"
                + "testAfkTimerResetBeforeEachWindow=true\n"
                + "vsyncEnabled=" + vsyncEnabled + "\n"
                + "osFamily=" + osFamily() + "\n"
                + "javaMajor=" + Runtime.version().feature() + "\n"
                + "allocationWindow=tick duration; rendered frame count varies\n"
                + "offRenderedFrames=" + csv(offRenderedFrames) + "\n"
                + "defaultRenderedFrames=" + csv(defaultRenderedFrames) + "\n"
                + "onRenderedFrames=" + csv(onRenderedFrames) + "\n"
                + "frameTimingSampleLimit=" + M8FrameTimeProbe.MAX_TIMING_SAMPLES + "\n"
                + "rebuildSettleTicks=" + REBUILD_SETTLE_TICKS + "\n"
                + "allocationProbe=com.sun.management.ThreadMXBean\n"
                + "allocationScope=live-thread cumulative delta; terminated threads may be missed; scenario-level JVM evidence\n"
                + "offAllocatedBytes=" + csv(offAllocatedBytes) + "\n"
                + "defaultAllocatedBytes=" + csv(defaultAllocatedBytes) + "\n"
                + "onAllocatedBytes=" + csv(onAllocatedBytes) + "\n"
                + "offAllocatedMedianBytes=" + offAllocation.median() + "\n"
                + "defaultAllocatedMedianBytes=" + defaultAllocation.median() + "\n"
                + "onAllocatedMedianBytes=" + onAllocation.median() + "\n"
                + "onMinusOffAllocatedMedianBytes=" + allocationMedianDelta + "\n"
                + "offRenderRelevantAllocatedBytes=" + csv(offRelevantAllocatedBytes) + "\n"
                + "defaultRenderRelevantAllocatedBytes=" + csv(defaultRelevantAllocatedBytes) + "\n"
                + "onRenderRelevantAllocatedBytes=" + csv(onRelevantAllocatedBytes) + "\n"
                + "offRenderRelevantAllocatedMedianBytes=" + offRelevantAllocation.median() + "\n"
                + "defaultRenderRelevantAllocatedMedianBytes=" + defaultRelevantAllocation.median() + "\n"
                + "onRenderRelevantAllocatedMedianBytes=" + onRelevantAllocation.median() + "\n"
                + "onMinusOffRenderRelevantAllocatedMedianBytes=" + relevantAllocationMedianDelta + "\n"
                + "offRebuildNanos=" + csv(offRebuildNanos) + "\n"
                + "defaultRebuildNanos=" + csv(defaultRebuildNanos) + "\n"
                + "onRebuildNanos=" + csv(onRebuildNanos) + "\n"
                + "offRebuildMedianNanos=" + offRebuild.median() + "\n"
                + "defaultRebuildMedianNanos=" + defaultRebuild.median() + "\n"
                + "onRebuildMedianNanos=" + onRebuild.median() + "\n"
                + "onMinusOffRebuildMedianNanos=" + rebuildMedianDelta + "\n"
                + "frameCaptureTicks=" + FRAME_CAPTURE_TICKS + "\n"
                + "offFrameMainPassSamples=" + offFrameMainPassNanos.length + "\n"
                + "defaultFrameMainPassSamples=" + defaultFrameMainPassNanos.length + "\n"
                + "onFrameMainPassSamples=" + onFrameMainPassNanos.length + "\n"
                + "offFrameMainPassNanos=" + csv(offFrameMainPassNanos) + "\n"
                + "defaultFrameMainPassNanos=" + csv(defaultFrameMainPassNanos) + "\n"
                + "onFrameMainPassNanos=" + csv(onFrameMainPassNanos) + "\n"
                + "offFrameMainPassMedianNanos=" + offFrameMainPass.median() + "\n"
                + "defaultFrameMainPassMedianNanos=" + defaultFrameMainPass.median() + "\n"
                + "onFrameMainPassMedianNanos=" + onFrameMainPass.median() + "\n"
                + "offFrameMainPassP95Nanos=" + offFrameMainPass.p95NearestRank() + "\n"
                + "defaultFrameMainPassP95Nanos=" + defaultFrameMainPass.p95NearestRank() + "\n"
                + "onFrameMainPassP95Nanos=" + onFrameMainPass.p95NearestRank() + "\n"
                + "offFrameMainPassP99Nanos=" + offFrameMainPass.p99NearestRank() + "\n"
                + "defaultFrameMainPassP99Nanos=" + defaultFrameMainPass.p99NearestRank() + "\n"
                + "onFrameMainPassP99Nanos=" + onFrameMainPass.p99NearestRank() + "\n"
                + "onMinusOffFrameMainPassMedianNanos=" + frameMainPassMedianDelta + "\n"
                + "wrappedModels=" + retention.wrappedModels() + "\n"
                + "retainedCapabilitySlots=" + retention.retainedCapabilitySlots() + "\n"
                + "maxCapabilitiesPerWrappedModel=" + retention.maxCapabilitiesPerModel() + "\n"
                + "netherWrappedModels=" + retention.netherWrappedModels() + "\n"
                + "initializationNanos=" + BlockLensRuntime.initializationNanos() + "\n";
        Files.writeString(
                outputDir.resolve("m8-performance-baseline.txt"),
                manifest,
                StandardCharsets.UTF_8);
    }

    private static String csv(long[] values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(values[i]);
        }
        return builder.toString();
    }

    private static String osFamily() {
        String name = System.getProperty("os.name", "");
        if (name.startsWith("Windows")) return "windows";
        if (name.startsWith("Linux")) return "linux";
        if (name.startsWith("Mac")) return "macos";
        return "other";
    }

    private static String csv(int[] values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) builder.append(',');
            builder.append(values[i]);
        }
        return builder.toString();
    }

    private static boolean renderRelevantThread(String name) {
        return name.contains("Render")
                || name.contains("Chunk")
                || name.startsWith("Worker-Main-")
                || name.startsWith("ForkJoinPool-");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record ModelRetention(
            int wrappedModels,
            int retainedCapabilitySlots,
            int maxCapabilitiesPerModel,
            int netherWrappedModels) {
    }

    private record RebuildMeasurement(
            long elapsedNanos,
            long totalAllocatedBytes,
            long renderRelevantAllocatedBytes,
            long renderedFrames) {
    }

    private record AllocationSnapshot(long[] ids, long[] allocatedBytes, String[] names) {
        long totalDeltaTo(AllocationSnapshot after) {
            return deltaTo(after, false);
        }

        long renderRelevantDeltaTo(AllocationSnapshot after) {
            return deltaTo(after, true);
        }

        private long deltaTo(AllocationSnapshot after, boolean relevantOnly) {
            long total = 0L;
            for (int i = 0; i < after.ids.length; i++) {
                long afterBytes = after.allocatedBytes[i];
                if (afterBytes < 0L || (relevantOnly && !renderRelevantThread(after.names[i]))) continue;
                int beforeIndex = indexOf(ids, after.ids[i]);
                long beforeBytes = beforeIndex < 0 ? 0L : allocatedBytes[beforeIndex];
                if (beforeBytes >= 0L && afterBytes >= beforeBytes) total += afterBytes - beforeBytes;
            }
            return total;
        }

        private static int indexOf(long[] values, long needle) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] == needle) return i;
            }
            return -1;
        }
    }

    private record Stats(long min, long median, long p95NearestRank, long p99NearestRank, long max) {
        static Stats of(long[] samples) {
            if (samples.length == 0) throw new IllegalArgumentException("performance samples must not be empty");
            long[] sorted = samples.clone();
            Arrays.sort(sorted);
            int medianIndex = sorted.length / 2;
            int p95Index = nearestRankIndex(sorted.length, 0.95d);
            int p99Index = nearestRankIndex(sorted.length, 0.99d);
            return new Stats(
                    sorted[0],
                    sorted[medianIndex],
                    sorted[p95Index],
                    sorted[p99Index],
                    sorted[sorted.length - 1]);
        }

        private static int nearestRankIndex(int sampleCount, double percentile) {
            return Math.max(0, (int) Math.ceil(sampleCount * percentile) - 1);
        }
    }
}

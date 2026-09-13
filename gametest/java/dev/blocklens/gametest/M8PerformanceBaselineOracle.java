package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.MinecraftDecorationModelPlugin;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
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
 * M8 pre-optimization performance baseline.
 *
 * <p>This oracle deliberately records measurements without defining an absolute regression
 * threshold. CI runner noise must be observed across repeated runs before a tolerance is frozen.
 * Resource reload is timed end-to-end around Minecraft's reload future. Allocation evidence uses
 * HotSpot/Temurin's per-thread cumulative allocation counters around a deterministic terrain
 * rebuild. The allocation delta is scenario-level JVM evidence, not a claim that every byte was
 * allocated by BlockLens.</p>
 */
final class M8PerformanceBaselineOracle {
    private static final int EXPECTED_RESOURCE_CAPABILITIES = 18;
    private static final int RELOAD_WARMUPS = 1;
    private static final int MEASURED_SAMPLES = 3;
    private static final int RELOAD_TIMEOUT_TICKS = 1200;
    private static final int REBUILD_SETTLE_TICKS = 30;

    private M8PerformanceBaselineOracle() {
    }

    static void verify(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        BlockLensConfig original = BlockLensRuntime.config();
        Path outputDir = FabricLoader.getInstance().getGameDir().resolve("m8-performance");

        try {
            Files.createDirectories(outputDir);
            buildScene(singleplayer);
            context.waitTicks(20);

            BlockLensConfig resourceOnly = resourceOnly(original);
            BlockLensConfig allOff = allOff(original);
            require(enabledCount(resourceOnly) == EXPECTED_RESOURCE_CAPABILITIES,
                    "M8 resource-only config must enable exactly 18 capabilities");

            install(resourceOnly);
            for (int i = 0; i < RELOAD_WARMUPS; i++) {
                measureReload(context);
            }

            long[] reloadNanos = new long[MEASURED_SAMPLES];
            for (int i = 0; i < MEASURED_SAMPLES; i++) {
                reloadNanos[i] = measureReload(context);
            }
            requirePipeline("M8 post-reload baseline");

            ensureAllocationProbeAvailable();
            // Warm the terrain rebuild path once before recording either side of the comparison.
            install(allOff);
            rebuild(context);

            long[] offAllocatedBytes = new long[MEASURED_SAMPLES];
            long[] offRelevantAllocatedBytes = new long[MEASURED_SAMPLES];
            long[] offRebuildNanos = new long[MEASURED_SAMPLES];
            long[] onAllocatedBytes = new long[MEASURED_SAMPLES];
            long[] onRelevantAllocatedBytes = new long[MEASURED_SAMPLES];
            long[] onRebuildNanos = new long[MEASURED_SAMPLES];

            // Alternate OFF/ON pairs to reduce monotonic warmup bias between the two scenarios.
            for (int i = 0; i < MEASURED_SAMPLES; i++) {
                install(allOff);
                RebuildMeasurement off = measureRebuild(context);
                offAllocatedBytes[i] = off.totalAllocatedBytes();
                offRelevantAllocatedBytes[i] = off.renderRelevantAllocatedBytes();
                offRebuildNanos[i] = off.elapsedNanos();

                install(resourceOnly);
                RebuildMeasurement on = measureRebuild(context);
                onAllocatedBytes[i] = on.totalAllocatedBytes();
                onRelevantAllocatedBytes[i] = on.renderRelevantAllocatedBytes();
                onRebuildNanos[i] = on.elapsedNanos();
            }

            Stats reload = Stats.of(reloadNanos);
            Stats offAllocation = Stats.of(offAllocatedBytes);
            Stats onAllocation = Stats.of(onAllocatedBytes);
            Stats offRelevantAllocation = Stats.of(offRelevantAllocatedBytes);
            Stats onRelevantAllocation = Stats.of(onRelevantAllocatedBytes);
            Stats offRebuild = Stats.of(offRebuildNanos);
            Stats onRebuild = Stats.of(onRebuildNanos);

            long allocationMedianDelta = onAllocation.median() - offAllocation.median();
            long relevantAllocationMedianDelta =
                    onRelevantAllocation.median() - offRelevantAllocation.median();
            long rebuildMedianDelta = onRebuild.median() - offRebuild.median();

            System.out.println("BLOCKLENS_M8_BASELINE minecraft=" + BlockLensRuntime.minecraftVersion()
                    + " measuredSamples=" + MEASURED_SAMPLES
                    + " reloadMedianNanos=" + reload.median()
                    + " reloadP95NearestRankNanos=" + reload.p95NearestRank()
                    + " offAllocatedMedianBytes=" + offAllocation.median()
                    + " onAllocatedMedianBytes=" + onAllocation.median()
                    + " onMinusOffAllocatedMedianBytes=" + allocationMedianDelta
                    + " offRelevantAllocatedMedianBytes=" + offRelevantAllocation.median()
                    + " onRelevantAllocatedMedianBytes=" + onRelevantAllocation.median()
                    + " onMinusOffRelevantAllocatedMedianBytes=" + relevantAllocationMedianDelta
                    + " offRebuildMedianNanos=" + offRebuild.median()
                    + " onRebuildMedianNanos=" + onRebuild.median()
                    + " onMinusOffRebuildMedianNanos=" + rebuildMedianDelta
                    + " wrappedModels=" + MinecraftDecorationModelPlugin.wrappedModelCount());

            writeManifest(
                    outputDir,
                    reloadNanos,
                    reload,
                    offAllocatedBytes,
                    onAllocatedBytes,
                    offAllocation,
                    onAllocation,
                    offRelevantAllocatedBytes,
                    onRelevantAllocatedBytes,
                    offRelevantAllocation,
                    onRelevantAllocation,
                    offRebuildNanos,
                    onRebuildNanos,
                    offRebuild,
                    onRebuild,
                    allocationMedianDelta,
                    relevantAllocationMedianDelta,
                    rebuildMedianDelta);

            require(Files.isRegularFile(outputDir.resolve("m8-performance-baseline.txt")),
                    "M8 performance manifest was not written");
            require(reload.min() > 0L, "M8 resource reload timing must be positive");
            require(offAllocation.min() >= 0L && onAllocation.min() >= 0L,
                    "M8 allocation deltas must be non-negative");
            require(offRebuild.min() > 0L && onRebuild.min() > 0L,
                    "M8 rebuild timing must be positive");
        } catch (IOException exception) {
            throw new AssertionError("M8 performance baseline failed", exception);
        } finally {
            install(original);
            rebuild(context);
        }
    }

    private static void buildScene(TestSingleplayerContext singleplayer) {
        var server = singleplayer.getServer();
        server.runCommand("gamerule minecraft:advance_time false");
        server.runCommand("gamerule minecraft:advance_weather false");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("gamemode spectator @a");
        server.runCommand("fill -10 -61 -6 10 -50 10 minecraft:air");
        server.runCommand("fill -10 -60 -6 10 -60 10 minecraft:smooth_quartz");

        String[] resources = {
                "obsidian", "ancient_debris", "diamond_ore", "deepslate_diamond_ore",
                "gold_ore", "deepslate_gold_ore", "emerald_ore", "deepslate_emerald_ore",
                "coal_ore", "deepslate_coal_ore", "iron_ore", "deepslate_iron_ore",
                "copper_ore", "deepslate_copper_ore", "lapis_ore", "deepslate_lapis_ore",
                "redstone_ore", "deepslate_redstone_ore"
        };
        int[] xs = {-8, -5, -2, 1, 4, 7};
        int[] zs = {-2, 1, 4};
        int index = 0;
        for (int z : zs) {
            for (int x : xs) {
                String block = resources[index++];
                // Four blocks per capability make BlockLens's incremental chunk-rebuild work
                // measurable without creating a world-scale stress test.
                for (int y = -59; y <= -56; y++) {
                    server.runCommand("setblock " + x + " " + y + " " + z + " minecraft:" + block);
                }
            }
        }
        server.runCommand("tp @a 0 -51 16 180 18");
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
        AllocationSnapshot before = captureAllocation();
        long start = System.nanoTime();
        context.runOnClient(MinecraftTerrainInvalidator::invalidateAll);
        context.waitTicks(REBUILD_SETTLE_TICKS);
        long elapsed = System.nanoTime() - start;
        AllocationSnapshot after = captureAllocation();
        return new RebuildMeasurement(
                elapsed,
                before.totalDeltaTo(after),
                before.renderRelevantDeltaTo(after));
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

    private static BlockLensConfig resourceOnly(BlockLensConfig base) {
        BlockLensConfig result = allOff(base);
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.category() == CapabilityId.Category.RESOURCE) {
                result = result.withEnabled(capability, true);
            }
        }
        return result;
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
            if (config.isEnabled(capability)) {
                count++;
            }
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
        require(MinecraftDecorationModelPlugin.wrappedModelCount() >= 320,
                phase + ": expected at least 320 wrapped models, got "
                        + MinecraftDecorationModelPlugin.wrappedModelCount());
    }

    private static void writeManifest(
            Path outputDir,
            long[] reloadNanos,
            Stats reload,
            long[] offAllocatedBytes,
            long[] onAllocatedBytes,
            Stats offAllocation,
            Stats onAllocation,
            long[] offRelevantAllocatedBytes,
            long[] onRelevantAllocatedBytes,
            Stats offRelevantAllocation,
            Stats onRelevantAllocation,
            long[] offRebuildNanos,
            long[] onRebuildNanos,
            Stats offRebuild,
            Stats onRebuild,
            long allocationMedianDelta,
            long relevantAllocationMedianDelta,
            long rebuildMedianDelta) throws IOException {
        String manifest = "minecraft=" + BlockLensRuntime.minecraftVersion() + "\n"
                + "mode=pre-optimization-baseline-no-absolute-threshold\n"
                + "resourceCapabilities=" + EXPECTED_RESOURCE_CAPABILITIES + "\n"
                + "resourceBlocksInScene=" + (EXPECTED_RESOURCE_CAPABILITIES * 4) + "\n"
                + "reloadWarmups=" + RELOAD_WARMUPS + "\n"
                + "measuredSamples=" + MEASURED_SAMPLES + "\n"
                + "reloadNanos=" + csv(reloadNanos) + "\n"
                + "reloadMedianNanos=" + reload.median() + "\n"
                + "reloadP95NearestRankNanos=" + reload.p95NearestRank() + "\n"
                + "rebuildSettleTicks=" + REBUILD_SETTLE_TICKS + "\n"
                + "allocationProbe=com.sun.management.ThreadMXBean\n"
                + "allocationScope=live-thread cumulative delta; terminated threads may be missed; scenario-level JVM evidence\n"
                + "offAllocatedBytes=" + csv(offAllocatedBytes) + "\n"
                + "onAllocatedBytes=" + csv(onAllocatedBytes) + "\n"
                + "offAllocatedMedianBytes=" + offAllocation.median() + "\n"
                + "onAllocatedMedianBytes=" + onAllocation.median() + "\n"
                + "onMinusOffAllocatedMedianBytes=" + allocationMedianDelta + "\n"
                + "offRenderRelevantAllocatedBytes=" + csv(offRelevantAllocatedBytes) + "\n"
                + "onRenderRelevantAllocatedBytes=" + csv(onRelevantAllocatedBytes) + "\n"
                + "offRenderRelevantAllocatedMedianBytes=" + offRelevantAllocation.median() + "\n"
                + "onRenderRelevantAllocatedMedianBytes=" + onRelevantAllocation.median() + "\n"
                + "onMinusOffRenderRelevantAllocatedMedianBytes=" + relevantAllocationMedianDelta + "\n"
                + "offRebuildNanos=" + csv(offRebuildNanos) + "\n"
                + "onRebuildNanos=" + csv(onRebuildNanos) + "\n"
                + "offRebuildMedianNanos=" + offRebuild.median() + "\n"
                + "onRebuildMedianNanos=" + onRebuild.median() + "\n"
                + "onMinusOffRebuildMedianNanos=" + rebuildMedianDelta + "\n"
                + "wrappedModels=" + MinecraftDecorationModelPlugin.wrappedModelCount() + "\n"
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

    private static boolean renderRelevantThread(String name) {
        return name.contains("Render")
                || name.contains("Chunk")
                || name.startsWith("Worker-Main-")
                || name.startsWith("ForkJoinPool-");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record RebuildMeasurement(
            long elapsedNanos,
            long totalAllocatedBytes,
            long renderRelevantAllocatedBytes) {
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
                if (afterBytes < 0L || (relevantOnly && !renderRelevantThread(after.names[i]))) {
                    continue;
                }
                int beforeIndex = indexOf(ids, after.ids[i]);
                long beforeBytes = beforeIndex < 0 ? 0L : allocatedBytes[beforeIndex];
                if (beforeBytes >= 0L && afterBytes >= beforeBytes) {
                    total += afterBytes - beforeBytes;
                }
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

    private record Stats(long min, long median, long p95NearestRank, long max) {
        static Stats of(long[] samples) {
            if (samples.length == 0) {
                throw new IllegalArgumentException("performance samples must not be empty");
            }
            long[] sorted = samples.clone();
            Arrays.sort(sorted);
            int medianIndex = sorted.length / 2;
            int p95Index = Math.max(0, (int) Math.ceil(sorted.length * 0.95d) - 1);
            return new Stats(
                    sorted[0],
                    sorted[medianIndex],
                    sorted[p95Index],
                    sorted[sorted.length - 1]);
        }
    }
}

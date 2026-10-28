package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.MinecraftDecorationModelPlugin;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/**
 * Pack-vs-mod cost comparison (knowledge/current/pack-cost-comparison.md).
 *
 * <p>Three conditions in one client: BlockLens installed with every capability off and no pack
 * (baseline), the same plus the source-pack structural twin or, in private local runs, the pinned
 * source pack itself, and BlockLens with the 37 pack-equivalent capabilities on and no pack. Six
 * rounds rotate the order so each condition takes every position twice; the summarizer compares
 * paired rounds. Only measurement validity fails this oracle, never which condition is cheaper.</p>
 */
final class PackCostComparisonOracle {
    static final String SOURCE_SHA256 = "070bc338936f89320408e7b78bb6f74856917f5944deecc1a1b52390f5363616";
    static final String TWIN_MARKER = "block/bltwin/t0";
    private static final String STAGED = "blocklens-pack-cost.zip";
    private static final String[] CONDITIONS = {"baseline", "pack", "mod"};
    private static final int[][] ORDERS = {{0, 1, 2}, {1, 2, 0}, {2, 0, 1}, {0, 2, 1}, {2, 1, 0}, {1, 0, 2}};
    private static final int PACK_EQUIVALENT_CAPABILITIES = 37;
    private static final int RELOAD_TIMEOUT_TICKS = 1200;
    // GameTest ticks are not paced in real time, so compile quiescence is judged on the wall clock.
    private static final long QUIET_NANOS = 1_000_000_000L;
    private static final long SCENE_QUIET_NANOS = 3_000_000_000L;
    private static final long COMPILE_TIMEOUT_NANOS = 180_000_000_000L;
    private static final int FRAME_TICKS = 60;
    private static final int MIN_FRAMES = 20;
    private static final int FPS_LIMIT = 60;
    // Scene: x/z -32..31, y -60..-53, i.e. 4 x 1 x 4 sections in a flat world.
    private static final int SCENE_SECTIONS = 16;

    private PackCostComparisonOracle() { }

    static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        String source = System.getProperty("blocklens.test.packCost.source", "twin");
        require(source.equals("twin") || source.equals("source"), "pack-cost source must be twin or source");
        Path input = Path.of(requireProperty("blocklens.test.packCost.pack"));
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path output = gameDir.resolve("pack-cost");
        BlockLensConfig originalConfig = BlockLensRuntime.config();
        List<String> originalPacks = context.computeOnClient(c -> List.copyOf(c.getResourcePackRepository().getSelectedIds()));
        int originalFps = context.computeOnClient(c -> c.options.framerateLimit().get());
        try {
            Files.createDirectories(output);
            require(Files.isRegularFile(input) && !Files.isSymbolicLink(input) && Files.size(input) <= 16L << 20,
                    "pack-cost input missing, linked or oversized");
            String sha256 = sha256(input);
            if (source.equals("source")) require(SOURCE_SHA256.equals(sha256), "private source pack bytes differ from the pin");
            Path resourcepacks = gameDir.resolve("resourcepacks");
            Files.createDirectories(resourcepacks);
            Files.copy(input, resourcepacks.resolve(STAGED), StandardCopyOption.REPLACE_EXISTING);
            String packId = "file/" + STAGED;
            context.runOnClient(c -> c.getResourcePackRepository().reload());
            boolean discovered = context.computeOnClient(c -> c.getResourcePackRepository().getAvailableIds().contains(packId));
            require(discovered, "staged pack was not discovered");

            BlockLensConfig off = originalConfig, mod = originalConfig;
            for (CapabilityId capability : CapabilityId.values()) {
                off = off.withEnabled(capability, false);
                mod = mod.withEnabled(capability, capability.ordinal() < PACK_EQUIVALENT_CAPABILITIES);
            }
            BlockLensConfig[] configs = {off, off, mod};
            context.runOnClient(c -> c.options.framerateLimit().set(FPS_LIMIT));
            context.runOnClient(c -> M8FrameTimeProbe.install());
            buildScene(world);
            // Block, light and chunk updates from the scene finish before anything is measured.
            awaitCompileQuiet(context, SCENE_QUIET_NANOS, "scene build", false);

            // One discarded pass per condition warms class loading and JIT for every path.
            for (int condition = 0; condition < CONDITIONS.length; condition++) {
                measure(context, condition, configs[condition], originalPacks, packId);
            }
            List<Sample> samples = new ArrayList<>();
            for (int round = 0; round < ORDERS.length; round++) {
                for (int position = 0; position < 3; position++) {
                    int condition = ORDERS[round][position];
                    samples.add(measure(context, condition, configs[condition], originalPacks, packId)
                            .at(round, position));
                }
            }
            long modSpritePixels = context.computeOnClient(PackCostComparisonOracle::modSpritePixels);
            int mipLevels = context.computeOnClient(c -> c.options.mipmapLevels().get());
            int renderDistance = context.computeOnClient(c -> c.options.renderDistance().get());
            writeEvidence(output, source, sha256, samples, modSpritePixels, mipLevels, renderDistance);
            verifyValidity(samples, source);
            System.out.println("BLOCKLENS_PACK_COST minecraft=" + BlockLensRuntime.minecraftVersion() + " source=" + source
                    + " rounds=" + ORDERS.length + " samples=" + samples.size());
        } catch (IOException exception) {
            throw new AssertionError("pack-cost comparison could not write evidence", exception);
        } finally {
            context.runOnClient(c -> {
                c.options.framerateLimit().set(originalFps);
                BlockLensRuntime.installConfig(originalConfig);
                c.getResourcePackRepository().setSelected(originalPacks);
            });
            reload(context);
            context.runOnClient(c -> c.getResourcePackRepository().reload());
        }
        require(originalPacks.equals(context.computeOnClient(c -> List.copyOf(c.getResourcePackRepository().getSelectedIds()))),
                "selected packs were not restored");
        require(BlockLensRuntime.config().enabledMask() == originalConfig.enabledMask(), "config was not restored");
    }

    private static Sample measure(ClientGameTestContext context, int condition, BlockLensConfig config,
            List<String> originalPacks, String packId) {
        context.runOnClient(c -> {
            List<String> selected = new ArrayList<>(originalPacks);
            if (condition == 1) selected.add(packId);
            c.getResourcePackRepository().setSelected(selected);
            BlockLensRuntime.installConfig(config);
        });
        long reloadNanos = reload(context);
        require(MinecraftDecorationModelPlugin.isModelPipelineReady(), "model pipeline not ready after reload");
        int[] atlas = context.computeOnClient(PackCostComparisonOracle::blockAtlas);
        boolean marker = context.computeOnClient(c -> hasSprite(c, TWIN_MARKER));

        long start = System.nanoTime();
        SectionCompileTimingProbe.Snapshot compile = awaitCompileQuiet(context, QUIET_NANOS,
                CONDITIONS[condition] + " rebuild", true);

        context.runOnClient(c -> {
            // Test bookkeeping only: keeps the measurement window out of AFK throttling.
            c.getFramerateLimitTracker().onInputReceived();
        });
        M8FrameTimeProbe.beginCapture();
        context.waitTicks(FRAME_TICKS);
        long[] frames = M8FrameTimeProbe.endCapture();
        Arrays.sort(frames);

        System.gc();
        context.waitTicks(2);
        System.gc();
        context.waitTicks(2);
        long heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        return new Sample(-1, -1, condition, reloadNanos, heap, atlas[0], atlas[1], marker,
                compile.sceneCompiles(), compile.sceneCpuNanos(), compile.sceneWallNanos(), compile.sceneAllocatedBytes(),
                compile.otherCompiles(), compile.otherCpuNanos(), compile.incomplete(),
                Math.max(0L, compile.lastReturnNanos() - start), frames.length,
                frames.length == 0 ? 0L : frames[frames.length / 2],
                frames.length == 0 ? 0L : frames[Math.max(0, (int) Math.ceil(frames.length * 0.95) - 1)]);
    }

    private static long reload(ClientGameTestContext context) {
        long start = System.nanoTime();
        var pending = context.computeOnClient(c -> c.reloadResourcePacks());
        context.waitFor(c -> pending.isDone(), RELOAD_TIMEOUT_TICKS);
        pending.join();
        long elapsed = System.nanoTime() - start;
        context.waitFor(SettingsClientAccess::ready, RELOAD_TIMEOUT_TICKS);
        // The reload's own terrain rebuild finishes before the measured rebuild starts.
        awaitCompileQuiet(context, QUIET_NANOS, "reload", false);
        return elapsed;
    }

    /**
     * Captures section compiles until none has returned for {@code quietNanos}, optionally after
     * invalidating all terrain. Fails with the observed counts if compilation never settles.
     */
    private static SectionCompileTimingProbe.Snapshot awaitCompileQuiet(ClientGameTestContext context,
            long quietNanos, String phase, boolean invalidate) {
        SectionCompileTimingProbe.begin();
        long start = System.nanoTime();
        if (invalidate) context.runOnClient(MinecraftTerrainInvalidator::invalidateAll);
        while (true) {
            context.waitTicks(1);
            long now = System.nanoTime();
            long last = Math.max(start, SectionCompileTimingProbe.lastReturnNanos());
            boolean compiled = !invalidate || SectionCompileTimingProbe.completed() > 0;
            if (compiled && now - last >= quietNanos) return SectionCompileTimingProbe.end();
            if (now - start > COMPILE_TIMEOUT_NANOS) {
                var snapshot = SectionCompileTimingProbe.end();
                throw new AssertionError("Pack-cost comparison: section compilation did not settle after " + phase
                        + ": scene=" + snapshot.sceneCompiles() + " other=" + snapshot.otherCompiles()
                        + " incomplete=" + snapshot.incomplete());
            }
        }
    }

    /** Block atlas size from one sprite's extent; texture APIs differ less than atlas accessors. */
    private static int[] blockAtlas(Minecraft client) {
        var sprite = client.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(Identifier.fromNamespaceAndPath("minecraft", "block/stone"));
        return new int[] {
                Math.round(sprite.contents().width() / (sprite.getU1() - sprite.getU0())),
                Math.round(sprite.contents().height() / (sprite.getV1() - sprite.getV0()))};
    }

    private static boolean hasSprite(Minecraft client, String path) {
        Identifier id = Identifier.fromNamespaceAndPath("minecraft", path);
        return id.equals(client.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id).contents().name());
    }

    /** Pixels BlockLens adds to the block atlas in every condition (its static atlas share). */
    private static long modSpritePixels(Minecraft client) {
        long pixels = 0L;
        var resources = client.getResourceManager().listResources("textures/block",
                id -> id.getNamespace().equals("blocklens") && id.getPath().endsWith(".png"));
        for (var resource : resources.values()) {
            try (InputStream stream = resource.open()) {
                var image = ImageIO.read(stream);
                pixels += (long) image.getWidth() * image.getHeight();
            } catch (IOException exception) {
                throw new AssertionError("BlockLens sprite could not be read", exception);
            }
        }
        return pixels;
    }

    private static void buildScene(TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("gamerule minecraft:advance_time false");
        server.runCommand("gamerule minecraft:advance_weather false");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("gamemode spectator @a");
        // Pack-equivalent targets in the states each source capability distinguishes.
        String[] blocks = {
                "white_glazed_terracotta[facing=north]", "blue_glazed_terracotta[facing=east]",
                "lime_glazed_terracotta[facing=south]", "red_glazed_terracotta[facing=west]",
                "oak_log[axis=x]", "oak_log[axis=y]", "spruce_log[axis=z]", "birch_wood[axis=y]",
                "dark_oak_wood[axis=x]", "ochre_froglight[axis=y]", "verdant_froglight[axis=z]",
                "stone_slab[type=bottom]", "stone_slab[type=top]", "oak_slab[type=double]", "brick_slab[type=bottom]",
                "stone_stairs[facing=north,half=bottom]", "oak_stairs[facing=east,half=top]",
                "brick_stairs[facing=south,half=bottom]", "oak_trapdoor[facing=north,half=bottom,open=false]",
                "spruce_trapdoor[facing=east,half=top,open=true]", "anvil[facing=north]",
                "beehive[facing=south,honey_level=5]", "campfire[facing=east,lit=true]",
                "grindstone[face=floor,facing=west]", "oak_fence_gate[facing=north,open=false]",
                "birch_fence_gate[facing=east,open=true]", "black_stained_glass", "red_stained_glass",
                "diamond_ore", "deepslate_diamond_ore", "gold_ore", "iron_ore", "coal_ore", "emerald_ore",
                "copper_ore", "lapis_ore", "redstone_ore", "deepslate_iron_ore", "obsidian", "ancient_debris",
                "blue_ice", "dead_brain_coral_block", "powder_snow", "sculk_catalyst", "netherrack",
                "basalt[axis=y]", "blackstone", "nether_bricks"};
        for (int z = -32; z < 32; z++) {
            for (int x = -32; x < 32; x += 4) {
                String block = blocks[Math.floorMod(x / 4 + z * 5, blocks.length)];
                server.runCommand("fill " + x + " -60 " + z + " " + (x + 3) + " -53 " + z + " minecraft:" + block);
            }
        }
        SectionCompileTimingProbe.scene(-2, -4, -2, 1, -4, 1);
        server.runCommand("tp @a 0.5 10 0.5 180 90");
    }

    private static void verifyValidity(List<Sample> samples, String source) {
        require(samples.size() == ORDERS.length * 3, "every round must measure every condition");
        for (Sample sample : samples) {
            String name = CONDITIONS[sample.condition()] + " round " + sample.round();
            require(sample.reloadNanos() > 0 && sample.heapBytes() > 0, name + ": reload and heap must be positive");
            require(sample.sceneCompiles() >= SCENE_SECTIONS && sample.sceneCpuNanos() > 0,
                    name + ": every scene section must compile, got " + sample.sceneCompiles());
            require(sample.frames() >= MIN_FRAMES, name + ": too few frames " + sample.frames());
        }
        Sample baseline = first(samples, 0), pack = first(samples, 1), mod = first(samples, 2);
        for (Sample sample : samples) {
            Sample reference = first(samples, sample.condition());
            require(sample.atlasWidth() == reference.atlasWidth() && sample.atlasHeight() == reference.atlasHeight(),
                    "block atlas size must be deterministic per condition");
        }
        long baseArea = (long) baseline.atlasWidth() * baseline.atlasHeight();
        require((long) pack.atlasWidth() * pack.atlasHeight() > baseArea, "the pack did not grow the block atlas");
        require((long) mod.atlasWidth() * mod.atlasHeight() == baseArea, "enabling BlockLens changed the block atlas");
        if (source.equals("twin")) {
            for (Sample sample : samples) {
                require(sample.marker() == (sample.condition() == 1), "twin sprite presence must follow pack selection");
            }
        }
    }

    private static Sample first(List<Sample> samples, int condition) {
        return samples.stream().filter(sample -> sample.condition() == condition).findFirst().orElseThrow();
    }

    private static void writeEvidence(Path output, String source, String sha256, List<Sample> samples,
            long modSpritePixels, int mipLevels, int renderDistance) throws IOException {
        StringBuilder csv = new StringBuilder(Sample.HEADER).append('\n');
        for (Sample sample : samples) csv.append(sample.csv()).append('\n');
        Files.writeString(output.resolve("pack-cost-samples.csv"), csv.toString(), StandardCharsets.UTF_8);
        String manifest = "minecraft=" + BlockLensRuntime.minecraftVersion() + "\n"
                + "mode=pack-cost-comparison\n"
                + "source=" + source + "\n"
                + "packSha256=" + sha256 + "\n"
                + "conditions=" + String.join(",", CONDITIONS) + "\n"
                + "packEquivalentCapabilities=" + PACK_EQUIVALENT_CAPABILITIES + "\n"
                + "rounds=" + ORDERS.length + "\n"
                + "sceneSections=" + SCENE_SECTIONS + "\n"
                + "sceneBlocks=" + (64 * 64 * 8) + "\n"
                + "fpsLimit=" + FPS_LIMIT + "\n"
                + "mipmapLevels=" + mipLevels + "\n"
                + "renderDistanceChunks=" + renderDistance + "\n"
                + "modSpritePixels=" + modSpritePixels + "\n"
                + "initializationNanos=" + BlockLensRuntime.initializationNanos() + "\n"
                + "availableProcessors=" + Runtime.getRuntime().availableProcessors() + "\n"
                + "maxHeapBytes=" + Runtime.getRuntime().maxMemory() + "\n"
                + "javaVersion=" + Runtime.version() + "\n";
        Files.writeString(output.resolve("pack-cost-comparison.txt"), manifest, StandardCharsets.UTF_8);
    }

    private static String sha256(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static String requireProperty(String name) {
        String value = System.getProperty(name);
        require(value != null && !value.isBlank(), name + " is required");
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("Pack-cost comparison: " + message);
    }

    private record Sample(int round, int position, int condition, long reloadNanos, long heapBytes,
            int atlasWidth, int atlasHeight, boolean marker, long sceneCompiles, long sceneCpuNanos,
            long sceneWallNanos, long sceneAllocatedBytes, long otherCompiles, long otherCpuNanos,
            long incompleteCompiles, long rebuildNanos, int frames, long frameMedianNanos, long frameP95Nanos) {
        static final String HEADER = "round,position,condition,reloadNanos,heapBytes,atlasWidth,atlasHeight,"
                + "twinMarker,sceneCompiles,sceneCpuNanos,sceneWallNanos,sceneAllocatedBytes,otherCompiles,"
                + "otherCpuNanos,incompleteCompiles,rebuildNanos,frames,frameMedianNanos,frameP95Nanos";

        Sample at(int round, int position) {
            return new Sample(round, position, condition, reloadNanos, heapBytes, atlasWidth, atlasHeight, marker,
                    sceneCompiles, sceneCpuNanos, sceneWallNanos, sceneAllocatedBytes, otherCompiles, otherCpuNanos,
                    incompleteCompiles, rebuildNanos, frames, frameMedianNanos, frameP95Nanos);
        }

        String csv() {
            return round + "," + position + "," + CONDITIONS[condition] + "," + reloadNanos + "," + heapBytes + ","
                    + atlasWidth + "," + atlasHeight + "," + marker + "," + sceneCompiles + "," + sceneCpuNanos + ","
                    + sceneWallNanos + "," + sceneAllocatedBytes + "," + otherCompiles + "," + otherCpuNanos + ","
                    + incompleteCompiles + "," + rebuildNanos + "," + frames + "," + frameMedianNanos + ","
                    + frameP95Nanos;
        }
    }
}

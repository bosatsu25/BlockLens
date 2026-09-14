package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigCodec;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.MinecraftDecorationModelPlugin;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.level.Level;

/** Full-product integration oracle shared by both supported Minecraft lines. */
final class M7FullParityOracle {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int TARGET_MIN_X = 70;
    private static final int TARGET_MIN_Y = 170;
    private static final int TARGET_MAX_X = 570;
    private static final int TARGET_MAX_Y = 310;
    private static final int RELOAD_TIMEOUT_TICKS = 1200;
    private static final int SETTLE_TICKS = 30;
    private static final long OVERLAY_FADE_MILLIS = 1_500L;

    private M7FullParityOracle() {
    }

    static void verify(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        BlockLensConfig original = BlockLensRuntime.config();
        Path outputDir = FabricLoader.getInstance().getGameDir().resolve("m7-visual");
        try {
            Files.createDirectories(outputDir);
            buildScene(singleplayer);
            context.waitTicks(20);

            BlockLensConfig allOn = withAllEnabled(original, true);
            require(enabledCount(allOn) == 40, "full-parity all-on config must enable 40 capabilities");
            require(BlockLensConfigCodec.decode(BlockLensConfigCodec.encode(allOn)).enabledMask() == allOn.enabledMask(),
                    "all-on config round-trip changed enabled mask");

            install(allOn);
            reload(context);
            requirePipeline("all 40 enabled");
            rebuild(context);
            Path onPath = screenshot(context, outputDir, "m7-all40-on");

            reload(context);
            requirePipeline("all 40 enabled after reload");
            rebuild(context);
            Path reloadedPath = screenshot(context, outputDir, "m7-all40-reloaded");

            dimensionRoundTrip(context, singleplayer);

            BlockLensConfig netherOnly = onlyEnabled(original, CapabilityId.NETHER_TWEAKS);
            require(enabledCount(netherOnly) == 1 && netherOnly.isEnabled(CapabilityId.NETHER_TWEAKS),
                    "Nether-only config must enable only Nether Tweaks");
            install(netherOnly);
            reload(context);
            rebuild(context);
            Path netherOnlyPath = screenshot(context, outputDir, "m7-nether-only");

            BlockLensConfig preset = BlockLensConfig.defaults();
            requireReferencePreset(preset);
            install(preset);
            reload(context);
            rebuild(context);
            Path presetPath = screenshot(context, outputDir, "m7-reference-preset");

            BlockLensConfig allOff = withAllEnabled(original, false);
            require(enabledCount(allOff) == 0, "all-off config still has enabled capabilities");
            install(allOff);
            reload(context);
            rebuild(context);
            Path offPath = screenshot(context, outputDir, "m7-all40-off-active-pack");

            BufferedImage on = inspect(onPath);
            BufferedImage reloaded = inspect(reloadedPath);
            BufferedImage netherOnlyImage = inspect(netherOnlyPath);
            BufferedImage presetImage = inspect(presetPath);
            BufferedImage off = inspect(offPath);
            int onOff = difference(on, off);
            int reloadOff = difference(reloaded, off);
            int netherOff = difference(netherOnlyImage, off);
            int presetOff = difference(presetImage, off);
            require(onOff >= 700, "all-on visual delta too small: " + onOff);
            require(reloadOff >= 700, "reload visual delta too small: " + reloadOff);
            require(netherOff >= 250, "Nether-only visual delta too small: " + netherOff);
            require(presetOff >= 80, "reference-preset visual delta too small: " + presetOff);

            String manifest = "minecraft=" + BlockLensRuntime.minecraftVersion() + "\n"
                    + "capabilities=40\nreferencePresetCapabilities=5\n"
                    + "targetBindings=328\nuniqueTargets=322\n"
                    + "wrappedModels=" + MinecraftDecorationModelPlugin.wrappedModelCount() + "\n"
                    + "allOnOffDifferentPixels=" + onOff + "\n"
                    + "reloadedOffDifferentPixels=" + reloadOff + "\n"
                    + "netherOnlyOffDifferentPixels=" + netherOff + "\n"
                    + "presetOffDifferentPixels=" + presetOff + "\n"
                    + "dimensionRoundTrip=PASS\nconfigRoundTrip=PASS\n"
                    + "allOnFile=" + onPath.getFileName() + "\n"
                    + "reloadedFile=" + reloadedPath.getFileName() + "\n"
                    + "netherOnlyFile=" + netherOnlyPath.getFileName() + "\n"
                    + "referencePresetFile=" + presetPath.getFileName() + "\n"
                    + "allOffFile=" + offPath.getFileName() + "\n";
            Files.writeString(outputDir.resolve("m7-visual-manifest.txt"), manifest, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("full parity evidence failed", exception);
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
        server.runCommand("fill -13 -60 -10 13 -60 7 minecraft:smooth_quartz");

        set(singleplayer, -9, 4, "blue_ice");
        set(singleplayer, -6, 4, "dead_brain_coral_block");
        set(singleplayer, -3, 4, "powder_snow");
        set(singleplayer, 0, 4, "sculk_catalyst[bloom=true]");
        set(singleplayer, 3, 4, "tripwire[attached=true,powered=true,north=true,east=true,south=false,west=false]");
        set(singleplayer, 6, 4, "netherrack");
        set(singleplayer, 9, 4, "crimson_nylium");
        set(singleplayer, -9, 7, "tripwire_hook[attached=true,powered=true,facing=north]");
        set(singleplayer, -6, 7, "polished_basalt[axis=z]");
        set(singleplayer, -9, 1, "warped_nylium");
        set(singleplayer, -6, 1, "crimson_stem[axis=x]");

        String[] resources = {
                "obsidian", "ancient_debris", "diamond_ore", "deepslate_diamond_ore", "gold_ore",
                "deepslate_gold_ore", "emerald_ore", "deepslate_emerald_ore", "coal_ore",
                "deepslate_coal_ore", "iron_ore", "deepslate_iron_ore", "copper_ore",
                "deepslate_copper_ore", "lapis_ore", "deepslate_lapis_ore", "redstone_ore",
                "deepslate_redstone_ore", "crying_obsidian", "nether_gold_ore", "nether_quartz_ore"
        };
        int[][] positions = {
                {-3,1},{0,1},{3,1},{6,1},{9,1},
                {-9,-2},{-6,-2},{-3,-2},{0,-2},{3,-2},{6,-2},{9,-2},
                {-9,-5},{-6,-5},{-3,-5},{0,-5},{3,-5},{6,-5},{9,-5},{-3,-8},{0,-8}
        };
        for (int i = 0; i < resources.length; i++) {
            set(singleplayer, positions[i][0], positions[i][1], resources[i]);
        }
        server.runCommand("tp @a 0 -53 20 180 17");
    }

    private static void set(TestSingleplayerContext singleplayer, int x, int z, String blockState) {
        singleplayer.getServer().runCommand("setblock " + x + " -59 " + z + " minecraft:" + blockState);
    }

    private static void dimensionRoundTrip(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        singleplayer.getServer().runCommand("execute in minecraft:the_nether run tp @a 0 90 0");
        context.waitTicks(30);
        context.runOnClient(client -> require(
                client.level != null && Level.NETHER.equals(client.level.dimension()),
                "client did not reach the Nether"));
        singleplayer.getServer().runCommand("execute in minecraft:overworld run tp @a 0 -53 20 180 17");
        context.waitTicks(30);
        context.runOnClient(client -> require(
                client.level != null && Level.OVERWORLD.equals(client.level.dimension()),
                "client did not return to the Overworld"));
        rebuild(context);
    }

    private static BlockLensConfig withAllEnabled(BlockLensConfig base, boolean enabled) {
        BlockLensConfig result = base;
        for (CapabilityId capability : CapabilityId.values()) result = result.withEnabled(capability, enabled);
        return result;
    }

    private static BlockLensConfig onlyEnabled(BlockLensConfig base, CapabilityId enabledCapability) {
        BlockLensConfig result = withAllEnabled(base, false);
        return result.withEnabled(enabledCapability, true);
    }

    private static int enabledCount(BlockLensConfig config) {
        int count = 0;
        for (CapabilityId capability : CapabilityId.values()) if (config.isEnabled(capability)) count++;
        return count;
    }

    private static void requireReferencePreset(BlockLensConfig config) {
        require(enabledCount(config) == 5, "reference preset must enable exactly five capabilities");
        for (CapabilityId capability : CapabilityId.values()) {
            boolean expected = capability == CapabilityId.BLUE_ICE
                    || capability == CapabilityId.DEAD_CORAL
                    || capability == CapabilityId.POWDER_SNOW
                    || capability == CapabilityId.SCULK_CATALYST
                    || capability == CapabilityId.STRING_TWEAKS;
            require(config.isEnabled(capability) == expected, "reference preset mismatch: " + capability.sourceKey());
        }
    }

    private static void install(BlockLensConfig config) {
        BlockLensRuntime.initialize(BlockLensRuntime.minecraftVersion(), config, BlockLensRuntime.initializationNanos());
    }

    private static void reload(ClientGameTestContext context) {
        CompletableFuture<Void> future = context.computeOnClient(client -> client.reloadResourcePacks());
        context.waitFor(client -> future.isDone(), RELOAD_TIMEOUT_TICKS);
        future.join();
        try {
            Thread.sleep(OVERLAY_FADE_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for reload overlay", exception);
        }
        context.waitTicks(2);
    }

    private static void rebuild(ClientGameTestContext context) {
        context.runOnClient(MinecraftTerrainInvalidator::invalidateAll);
        context.waitTicks(SETTLE_TICKS);
    }

    private static void requirePipeline(String phase) {
        require(MinecraftDecorationModelPlugin.isModelPipelineReady(), phase + ": model pipeline not ready");
        require(MinecraftDecorationModelPlugin.wrappedModelCount() >= 322,
                phase + ": expected at least 322 wrapped models, got " + MinecraftDecorationModelPlugin.wrappedModelCount());
    }

    private static Path screenshot(ClientGameTestContext context, Path outputDir, String name) throws IOException {
        Path path = context.takeScreenshot(TestScreenshotOptions.of(name)
                .withDestinationDir(outputDir).withSize(WIDTH, HEIGHT).disableCounterPrefix());
        require(Files.isRegularFile(path) && Files.size(path) > 1_000L, "invalid screenshot: " + path);
        return path;
    }

    private static BufferedImage inspect(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        require(image != null && image.getWidth() == WIDTH && image.getHeight() == HEIGHT,
                "invalid framebuffer capture: " + path);
        return image;
    }

    private static int difference(BufferedImage left, BufferedImage right) {
        int count = 0;
        for (int y = TARGET_MIN_Y; y < TARGET_MAX_Y; y++) {
            for (int x = TARGET_MIN_X; x < TARGET_MAX_X; x++) {
                int a = left.getRGB(x, y), b = right.getRGB(x, y);
                int delta = Math.abs(((a >>> 16) & 255) - ((b >>> 16) & 255))
                        + Math.abs(((a >>> 8) & 255) - ((b >>> 8) & 255))
                        + Math.abs((a & 255) - (b & 255));
                if (delta >= 24) count++;
            }
        }
        return count;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

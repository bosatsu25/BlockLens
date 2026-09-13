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

/** Full-product M7 integration oracle shared by both supported Minecraft lines. */
final class M7FullParityOracle {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int CAPABILITY_COUNT = 37;
    private static final int REFERENCE_PRESET_COUNT = 5;
    private static final int TARGET_MIN_X = 70;
    private static final int TARGET_MIN_Y = 175;
    private static final int TARGET_MAX_X = 570;
    private static final int TARGET_MAX_Y = 305;
    private static final int MIN_ALL_ON_DIFFERENCE = 700;
    private static final int MIN_PRESET_DIFFERENCE = 80;
    private static final int RESOURCE_RELOAD_TIMEOUT_TICKS = 1200;
    private static final int TERRAIN_SETTLE_TICKS = 30;
    private static final long RELOAD_OVERLAY_FADE_MILLIS = 1_500L;

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
            require(enabledCount(allOn) == CAPABILITY_COUNT, "M7 all-on config must enable 37 capabilities");
            BlockLensConfig roundTrip = BlockLensConfigCodec.decode(BlockLensConfigCodec.encode(allOn));
            require(roundTrip.enabledMask() == allOn.enabledMask(), "M7 all-on config round-trip changed enabled mask");

            installRuntimeConfig(allOn);
            reloadResources(context);
            requireModelPipeline("all 37 enabled");
            invalidateTerrain(context);
            context.waitTicks(TERRAIN_SETTLE_TICKS);
            Path allOnImage = takeScreenshot(context, outputDir, "m7-all37-on");

            reloadResources(context);
            requireModelPipeline("all 37 enabled after reload");
            invalidateTerrain(context);
            context.waitTicks(TERRAIN_SETTLE_TICKS);
            Path reloadedImage = takeScreenshot(context, outputDir, "m7-all37-reloaded");

            verifyDimensionRoundTrip(context, singleplayer);

            BlockLensConfig referencePreset = BlockLensConfig.defaults();
            require(enabledCount(referencePreset) == REFERENCE_PRESET_COUNT,
                    "frozen source reference preset must enable exactly five capabilities");
            requireReferencePreset(referencePreset);
            installRuntimeConfig(referencePreset);
            reloadResources(context);
            invalidateTerrain(context);
            context.waitTicks(TERRAIN_SETTLE_TICKS);
            Path presetImage = takeScreenshot(context, outputDir, "m7-reference-preset");

            BlockLensConfig allOff = withAllEnabled(original, false);
            require(enabledCount(allOff) == 0, "M7 all-off config still has enabled capabilities");
            installRuntimeConfig(allOff);
            reloadResources(context);
            invalidateTerrain(context);
            context.waitTicks(TERRAIN_SETTLE_TICKS);
            Path allOffImage = takeScreenshot(context, outputDir, "m7-all37-off-active-pack");

            BufferedImage on = inspect(allOnImage);
            BufferedImage reloaded = inspect(reloadedImage);
            BufferedImage preset = inspect(presetImage);
            BufferedImage off = inspect(allOffImage);
            int onOff = targetDifferentPixelCount(on, off);
            int reloadOff = targetDifferentPixelCount(reloaded, off);
            int presetOff = targetDifferentPixelCount(preset, off);

            require(onOff >= MIN_ALL_ON_DIFFERENCE,
                    "M7 all-37 ON target region is not visually distinct from OFF; differentPixels=" + onOff);
            require(reloadOff >= MIN_ALL_ON_DIFFERENCE,
                    "M7 all-37 rendering did not survive resource reload; differentPixels=" + reloadOff);
            require(presetOff >= MIN_PRESET_DIFFERENCE,
                    "M7 frozen five-feature preset is not visually distinct from OFF; differentPixels=" + presetOff);

            writeManifest(outputDir, allOnImage, reloadedImage, presetImage, allOffImage,
                    onOff, reloadOff, presetOff);
        } catch (IOException exception) {
            throw new AssertionError("M7 full parity evidence failed", exception);
        } finally {
            installRuntimeConfig(original);
            invalidateTerrain(context);
        }
    }

    private static void buildScene(TestSingleplayerContext singleplayer) {
        var server = singleplayer.getServer();
        server.runCommand("gamerule minecraft:advance_time false");
        server.runCommand("gamerule minecraft:advance_weather false");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("gamemode spectator @a");
        server.runCommand("fill -13 -60 -7 13 -60 6 minecraft:smooth_quartz");

        // M4: all five outline/fine-visibility capabilities.
        set(server, -9, 4, "blue_ice");
        set(server, -6, 4, "dead_brain_coral_block");
        set(server, -3, 4, "powder_snow");
        setState(server, 0, 4, "sculk_catalyst[bloom=true]");
        setState(server, 3, 4,
                "tripwire[attached=true,powered=true,north=true,east=true,south=false,west=false]");

        // M6 representatives, including both nylium band families and overlap targets.
        set(server, 6, 4, "netherrack");
        set(server, 9, 4, "crimson_nylium");
        set(server, -9, 1, "warped_nylium");
        setState(server, -6, 1, "crimson_stem[axis=x]");

        // M5: all 18 independently controlled resource targets.
        String[] resources = {
                "obsidian", "ancient_debris", "diamond_ore", "deepslate_diamond_ore",
                "gold_ore", "deepslate_gold_ore", "emerald_ore", "deepslate_emerald_ore",
                "coal_ore", "deepslate_coal_ore", "iron_ore", "deepslate_iron_ore",
                "copper_ore", "deepslate_copper_ore", "lapis_ore", "deepslate_lapis_ore",
                "redstone_ore", "deepslate_redstone_ore"
        };
        int index = 0;
        for (int z : new int[]{1, -2, -5}) {
            for (int x : new int[]{-3, 0, 3, 6, 9, -9}) {
                if (index >= resources.length) break;
                set(server, x, z, resources[index++]);
            }
        }
        require(index == resources.length, "M7 resource scene did not place all 18 targets");

        // M3 overlap markers prove older render families remain active in the same all-on session.
        setState(server, 6, -5, "oak_log[axis=z]");
        setState(server, 9, -5, "quartz_stairs[facing=east,half=top,shape=outer_left]");
        server.runCommand("tp @a 0 -53 20 180 17");
    }

    private static void set(TestSingleplayerContext.TestServerContext server, int x, int z, String block) {
        server.runCommand("setblock " + x + " -59 " + z + " minecraft:" + block);
    }

    private static void setState(TestSingleplayerContext.TestServerContext server, int x, int z, String blockState) {
        server.runCommand("setblock " + x + " -59 " + z + " minecraft:" + blockState);
    }

    private static void verifyDimensionRoundTrip(
            ClientGameTestContext context,
            TestSingleplayerContext singleplayer) {
        var server = singleplayer.getServer();
        server.runCommand("execute in minecraft:the_nether run tp @a 0 90 0");
        context.waitTicks(30);
        context.runOnClient(client -> require(
                client.level != null && Level.NETHER.equals(client.level.dimension()),
                "M7 client did not reach the Nether with all capabilities enabled"));

        server.runCommand("execute in minecraft:overworld run tp @a 0 -53 20 180 17");
        context.waitTicks(30);
        context.runOnClient(client -> require(
                client.level != null && Level.OVERWORLD.equals(client.level.dimension()),
                "M7 client did not return to the Overworld with all capabilities enabled"));
        invalidateTerrain(context);
        context.waitTicks(TERRAIN_SETTLE_TICKS);
    }

    private static BlockLensConfig withAllEnabled(BlockLensConfig base, boolean enabled) {
        BlockLensConfig result = base;
        for (CapabilityId capability : CapabilityId.values()) {
            result = result.withEnabled(capability, enabled);
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

    private static void requireReferencePreset(BlockLensConfig config) {
        for (CapabilityId capability : CapabilityId.values()) {
            boolean expected = capability == CapabilityId.BLUE_ICE
                    || capability == CapabilityId.DEAD_CORAL
                    || capability == CapabilityId.POWDER_SNOW
                    || capability == CapabilityId.SCULK_CATALYST
                    || capability == CapabilityId.STRING_TWEAKS;
            require(config.isEnabled(capability) == expected,
                    "reference preset mismatch for " + capability.sourceKey());
        }
    }

    private static void installRuntimeConfig(BlockLensConfig config) {
        BlockLensRuntime.initialize(
                BlockLensRuntime.minecraftVersion(), config, BlockLensRuntime.initializationNanos());
    }

    private static void reloadResources(ClientGameTestContext context) {
        CompletableFuture<Void> reload = context.computeOnClient(client -> client.reloadResourcePacks());
        context.waitFor(client -> reload.isDone(), RESOURCE_RELOAD_TIMEOUT_TICKS);
        reload.join();
        try {
            Thread.sleep(RELOAD_OVERLAY_FADE_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for M7 reload overlay fade", exception);
        }
        context.waitTicks(2);
    }

    private static void invalidateTerrain(ClientGameTestContext context) {
        context.runOnClient(MinecraftTerrainInvalidator::invalidateAll);
    }

    private static void requireModelPipeline(String phase) {
        require(MinecraftDecorationModelPlugin.isModelPipelineReady(), phase + ": model pipeline not ready");
        require(MinecraftDecorationModelPlugin.wrappedModelCount() >= 320,
                phase + ": expected at least 320 wrapped target models but got "
                        + MinecraftDecorationModelPlugin.wrappedModelCount());
    }

    private static Path takeScreenshot(ClientGameTestContext context, Path outputDir, String name) throws IOException {
        Path image = context.takeScreenshot(
                TestScreenshotOptions.of(name)
                        .withDestinationDir(outputDir)
                        .withSize(WIDTH, HEIGHT)
                        .disableCounterPrefix());
        require(Files.isRegularFile(image), "M7 screenshot was not created: " + image);
        require(Files.size(image) > 1_000L, "M7 screenshot is unexpectedly small: " + image);
        return image;
    }

    private static BufferedImage inspect(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        require(image != null, "failed to decode M7 screenshot: " + path);
        require(image.getWidth() == WIDTH && image.getHeight() == HEIGHT,
                "unexpected M7 screenshot dimensions: " + image.getWidth() + "x" + image.getHeight());
        int nonBlack = 0;
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >>> 16) & 0xFF;
                int g = (rgb >>> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (r + g + b > 45) nonBlack++;
            }
        }
        require(nonBlack > (WIDTH * HEIGHT) / 5, "M7 screenshot appears blank: " + path);
        return image;
    }

    private static int targetDifferentPixelCount(BufferedImage left, BufferedImage right) {
        int different = 0;
        for (int y = TARGET_MIN_Y; y < TARGET_MAX_Y; y++) {
            for (int x = TARGET_MIN_X; x < TARGET_MAX_X; x++) {
                int a = left.getRGB(x, y);
                int b = right.getRGB(x, y);
                int delta = Math.abs(((a >>> 16) & 0xFF) - ((b >>> 16) & 0xFF))
                        + Math.abs(((a >>> 8) & 0xFF) - ((b >>> 8) & 0xFF))
                        + Math.abs((a & 0xFF) - (b & 0xFF));
                if (delta >= 24) different++;
            }
        }
        return different;
    }

    private static void writeManifest(
            Path outputDir,
            Path on,
            Path reloaded,
            Path preset,
            Path off,
            int onOff,
            int reloadOff,
            int presetOff) throws IOException {
        String manifest = "minecraft=" + BlockLensRuntime.minecraftVersion() + "\n"
                + "capabilities=37\n"
                + "referencePresetCapabilities=5\n"
                + "targetBindings=323\n"
                + "uniqueTargets=320\n"
                + "wrappedModels=" + MinecraftDecorationModelPlugin.wrappedModelCount() + "\n"
                + "targetRegion=" + TARGET_MIN_X + "," + TARGET_MIN_Y + "-" + TARGET_MAX_X + "," + TARGET_MAX_Y + "\n"
                + "allOnFile=" + on.getFileName() + "\n"
                + "reloadedFile=" + reloaded.getFileName() + "\n"
                + "referencePresetFile=" + preset.getFileName() + "\n"
                + "allOffFile=" + off.getFileName() + "\n"
                + "allOnOffDifferentPixels=" + onOff + "\n"
                + "reloadedOffDifferentPixels=" + reloadOff + "\n"
                + "presetOffDifferentPixels=" + presetOff + "\n"
                + "dimensionRoundTrip=PASS\n"
                + "configRoundTrip=PASS\n";
        Files.writeString(outputDir.resolve("m7-visual-manifest.txt"), manifest, StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

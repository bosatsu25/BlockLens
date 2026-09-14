package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.MinecraftDecorationModelPlugin;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Dedicated M5/P0 dark-area visual oracle for the current Resource capability set.
 *
 * <p>This test enables only the current 21 resource-highlight capabilities and renders them in a
 * sealed, unlit room. The original M5 18-capability set remains historical baseline evidence; P0
 * adds Crying Obsidian, Nether Gold Ore, and Nether Quartz Ore. The framebuffer assertion therefore
 * cannot pass because of M3/M4/M6/M7 cues. It checks both visible pixel change and a material
 * increase in target-region luminance.</p>
 */
final class M5DarkAreaVisualOracle {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int EXPECTED_RESOURCE_CAPABILITIES = 21;
    private static final int TARGET_MIN_X = 70;
    private static final int TARGET_MIN_Y = 150;
    private static final int TARGET_MAX_X = 570;
    private static final int TARGET_MAX_Y = 315;
    private static final int MIN_DIFFERENT_PIXELS = 250;
    private static final int MIN_BRIGHTER_PIXELS = 120;
    private static final int BRIGHTER_PIXEL_DELTA = 12;
    private static final int MAX_OFF_AVERAGE_LUMINANCE = 90;
    private static final int TERRAIN_REBUILD_SETTLE_TICKS = 40;

    private M5DarkAreaVisualOracle() {
    }

    static void verify(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        BlockLensConfig original = BlockLensRuntime.config();
        Path outputDir = FabricLoader.getInstance().getGameDir().resolve("m5-dark-visual");

        try {
            Files.createDirectories(outputDir);
            buildDarkScene(singleplayer);
            context.waitTicks(TERRAIN_REBUILD_SETTLE_TICKS);

            BlockLensConfig resourceOnly = resourceOnly(original);
            require(enabledCount(resourceOnly) == EXPECTED_RESOURCE_CAPABILITIES,
                    "M5/P0 resource-only config must enable exactly 21 capabilities");
            install(resourceOnly);
            requirePipeline("resource-only ON");
            rebuild(context);
            Path onPath = screenshot(context, outputDir, "m5-resource-dark-on");

            BlockLensConfig allOff = allOff(original);
            require(enabledCount(allOff) == 0, "M5 OFF config still has enabled capabilities");
            install(allOff);
            requirePipeline("resource-only OFF");
            rebuild(context);
            Path offPath = screenshot(context, outputDir, "m5-resource-dark-off");

            ImageMetrics on = inspect(onPath);
            ImageMetrics off = inspect(offPath);
            int differentPixels = differentPixels(on.image(), off.image());
            int brighterPixels = brighterPixels(on.image(), off.image());
            long luminanceGain = on.luminanceSum() - off.luminanceSum();
            int offAverageLuminance = averageLuminance(off.luminanceSum());
            int onAverageLuminance = averageLuminance(on.luminanceSum());

            System.out.println("BLOCKLENS_M5_DARK minecraft=" + BlockLensRuntime.minecraftVersion()
                    + " resourceCapabilities=" + EXPECTED_RESOURCE_CAPABILITIES
                    + " differentPixels=" + differentPixels
                    + " brighterPixels=" + brighterPixels
                    + " offAverageLuminance=" + offAverageLuminance
                    + " onAverageLuminance=" + onAverageLuminance
                    + " luminanceGain=" + luminanceGain);

            writeManifest(
                    outputDir,
                    onPath,
                    offPath,
                    on,
                    off,
                    differentPixels,
                    brighterPixels,
                    offAverageLuminance,
                    onAverageLuminance,
                    luminanceGain);

            require(offAverageLuminance <= MAX_OFF_AVERAGE_LUMINANCE,
                    "M5 OFF control is not dark enough; averageLuminance=" + offAverageLuminance);
            require(differentPixels >= MIN_DIFFERENT_PIXELS,
                    "M5 resource-only dark scene is not visually distinct from OFF; differentPixels="
                            + differentPixels);
            require(brighterPixels >= MIN_BRIGHTER_PIXELS,
                    "M5 emissive cue did not brighten enough dark-area pixels; brighterPixels="
                            + brighterPixels);
            require(luminanceGain > 0L,
                    "M5 resource-only dark scene did not increase target-region luminance; gain="
                            + luminanceGain);
        } catch (IOException exception) {
            throw new AssertionError("M5 dark-area visual evidence failed", exception);
        } finally {
            install(original);
            rebuild(context);
        }
    }

    private static void buildDarkScene(TestSingleplayerContext singleplayer) {
        var server = singleplayer.getServer();
        server.runCommand("gamerule minecraft:advance_time false");
        server.runCommand("gamerule minecraft:advance_weather false");
        server.runCommand("time set midnight");
        server.runCommand("weather clear");
        server.runCommand("gamemode spectator @a");

        // Hollow replaces the interior with air and creates a sealed opaque shell. A second black
        // floor keeps the target background deterministic and prevents light-colored geometry from
        // inflating the OFF-control luminance measurement.
        server.runCommand("fill -12 -61 -8 12 -50 20 minecraft:black_concrete hollow");
        server.runCommand("fill -10 -60 -6 10 -60 6 minecraft:black_concrete");

        String[] resources = {
                "obsidian", "ancient_debris", "diamond_ore", "deepslate_diamond_ore",
                "gold_ore", "deepslate_gold_ore", "emerald_ore", "deepslate_emerald_ore",
                "coal_ore", "deepslate_coal_ore", "iron_ore", "deepslate_iron_ore",
                "copper_ore", "deepslate_copper_ore", "lapis_ore", "deepslate_lapis_ore",
                "redstone_ore", "deepslate_redstone_ore", "crying_obsidian",
                "nether_gold_ore", "nether_quartz_ore"
        };
        int[] xs = {-9, -6, -3, 0, 3, 6, 9};
        int[] zs = {4, 1, -2};
        int index = 0;
        for (int z : zs) {
            for (int x : xs) {
                server.runCommand("setblock " + x + " -59 " + z + " minecraft:" + resources[index++]);
            }
        }

        // Fixed spectator camera inside the sealed room. There are no torches, luminous blocks,
        // night-vision effects, or open sky paths in the target scene.
        server.runCommand("tp @a 0 -53 17 180 18");
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

    private static void rebuild(ClientGameTestContext context) {
        context.runOnClient(MinecraftTerrainInvalidator::invalidateAll);
        context.waitTicks(TERRAIN_REBUILD_SETTLE_TICKS);
    }

    private static void requirePipeline(String phase) {
        require(MinecraftDecorationModelPlugin.isModelPipelineReady(),
                phase + ": model pipeline not ready");
        require(MinecraftDecorationModelPlugin.wrappedModelCount() >= 322,
                phase + ": expected at least 322 wrapped models, got "
                        + MinecraftDecorationModelPlugin.wrappedModelCount());
    }

    private static Path screenshot(ClientGameTestContext context, Path outputDir, String name)
            throws IOException {
        Path path = context.takeScreenshot(
                TestScreenshotOptions.of(name)
                        .withDestinationDir(outputDir)
                        .withSize(WIDTH, HEIGHT)
                        .disableCounterPrefix());
        require(Files.isRegularFile(path), "M5 screenshot was not created: " + path);
        require(Files.size(path) > 1_000L, "M5 screenshot is unexpectedly small: " + path);
        return path;
    }

    private static ImageMetrics inspect(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        require(image != null, "failed to decode M5 screenshot: " + path);
        require(image.getWidth() == WIDTH && image.getHeight() == HEIGHT,
                "unexpected M5 screenshot dimensions: " + image.getWidth() + "x" + image.getHeight());
        return new ImageMetrics(image, luminanceSum(image), Files.size(path));
    }

    private static int differentPixels(BufferedImage on, BufferedImage off) {
        int different = 0;
        for (int y = TARGET_MIN_Y; y < TARGET_MAX_Y; y++) {
            for (int x = TARGET_MIN_X; x < TARGET_MAX_X; x++) {
                int a = on.getRGB(x, y);
                int b = off.getRGB(x, y);
                int delta = Math.abs(red(a) - red(b))
                        + Math.abs(green(a) - green(b))
                        + Math.abs(blue(a) - blue(b));
                if (delta >= 24) {
                    different++;
                }
            }
        }
        return different;
    }

    private static int brighterPixels(BufferedImage on, BufferedImage off) {
        int brighter = 0;
        for (int y = TARGET_MIN_Y; y < TARGET_MAX_Y; y++) {
            for (int x = TARGET_MIN_X; x < TARGET_MAX_X; x++) {
                int onLuminance = luminance(on.getRGB(x, y));
                int offLuminance = luminance(off.getRGB(x, y));
                if (onLuminance >= offLuminance + BRIGHTER_PIXEL_DELTA) {
                    brighter++;
                }
            }
        }
        return brighter;
    }

    private static long luminanceSum(BufferedImage image) {
        long sum = 0L;
        for (int y = TARGET_MIN_Y; y < TARGET_MAX_Y; y++) {
            for (int x = TARGET_MIN_X; x < TARGET_MAX_X; x++) {
                sum += luminance(image.getRGB(x, y));
            }
        }
        return sum;
    }

    private static int averageLuminance(long sum) {
        int pixels = (TARGET_MAX_X - TARGET_MIN_X) * (TARGET_MAX_Y - TARGET_MIN_Y);
        return Math.toIntExact(sum / pixels);
    }

    private static int luminance(int argb) {
        // Integer approximation of Rec. 709 luma: 0.2126R + 0.7152G + 0.0722B.
        return (54 * red(argb) + 183 * green(argb) + 19 * blue(argb)) >>> 8;
    }

    private static int red(int argb) {
        return (argb >>> 16) & 0xFF;
    }

    private static int green(int argb) {
        return (argb >>> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }

    private static void writeManifest(
            Path outputDir,
            Path onPath,
            Path offPath,
            ImageMetrics on,
            ImageMetrics off,
            int differentPixels,
            int brighterPixels,
            int offAverageLuminance,
            int onAverageLuminance,
            long luminanceGain) throws IOException {
        String manifest = "minecraft=" + BlockLensRuntime.minecraftVersion() + "\n"
                + "resourceCapabilities=" + EXPECTED_RESOURCE_CAPABILITIES + "\n"
                + "scene=sealed-unlit-black-concrete-room\n"
                + "targetRegion=" + TARGET_MIN_X + "," + TARGET_MIN_Y + "-"
                + TARGET_MAX_X + "," + TARGET_MAX_Y + "\n"
                + "wrappedModels=" + MinecraftDecorationModelPlugin.wrappedModelCount() + "\n"
                + "onFile=" + onPath.getFileName() + "\n"
                + "onBytes=" + on.fileBytes() + "\n"
                + "offFile=" + offPath.getFileName() + "\n"
                + "offBytes=" + off.fileBytes() + "\n"
                + "differentPixels=" + differentPixels + "\n"
                + "brighterPixels=" + brighterPixels + "\n"
                + "offAverageLuminance=" + offAverageLuminance + "\n"
                + "onAverageLuminance=" + onAverageLuminance + "\n"
                + "luminanceGain=" + luminanceGain + "\n";
        Files.writeString(
                outputDir.resolve("m5-dark-visual-manifest.txt"),
                manifest,
                StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record ImageMetrics(BufferedImage image, long luminanceSum, long fileBytes) {
    }
}

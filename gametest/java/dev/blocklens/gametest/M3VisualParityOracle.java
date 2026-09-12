package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.MinecraftDecorationModelPlugin;
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

/**
 * Rendered M3 parity oracle.
 *
 * <p>The scene is intentionally built from vanilla commands so the same source runs on both
 * supported Minecraft lines. Screenshots are real client framebuffer captures, not synthetic
 * render-policy snapshots.</p>
 */
final class M3VisualParityOracle {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int EXPECTED_DECORATION_CAPABILITIES = 13;
    private static final int MIN_DIFFERENT_PIXELS = 250;
    private static final int MIN_RESOURCE_PACK_MARKER_PIXELS = 25;

    private M3VisualParityOracle() {
    }

    static void verify(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        BlockLensConfig original = BlockLensRuntime.config();
        Path outputDir = FabricLoader.getInstance().getGameDir().resolve("m3-visual");

        try {
            Files.createDirectories(outputDir);
            buildScene(singleplayer);
            settle(context, 20);

            BlockLensConfig allOn = withDecorationEnabled(original, true);
            require(decorationEnabledCount(allOn) == EXPECTED_DECORATION_CAPABILITIES,
                    "M3 all-on config did not enable exactly 13 decoration capabilities");
            installRuntimeConfig(allOn);
            reloadResources(context);
            requireModelPipeline("all 13 enabled after resource reload");
            settle(context, 15);
            Path allOnImage = takeScreenshot(context, outputDir, "m3-all13-on");

            // A second reload while features stay ON proves the wrapper is rebuilt from the
            // currently active baked resource-pack model rather than retaining stale model state.
            reloadResources(context);
            requireModelPipeline("all 13 enabled after second resource reload");
            settle(context, 15);
            Path reloadedImage = takeScreenshot(context, outputDir, "m3-all13-reloaded");

            BlockLensConfig allOff = withDecorationEnabled(original, false);
            require(decorationEnabledCount(allOff) == 0,
                    "M3 all-off config still has decoration capabilities enabled");
            installRuntimeConfig(allOff);
            reloadResources(context);
            requireModelPipeline("all 13 disabled after resource reload");
            settle(context, 15);
            Path allOffImage = takeScreenshot(context, outputDir, "m3-all13-off-active-pack");

            ImageMetrics on = inspect(allOnImage);
            ImageMetrics reloaded = inspect(reloadedImage);
            ImageMetrics off = inspect(allOffImage);
            int onOffDifference = differentPixelCount(on.image(), off.image());
            int reloadedOffDifference = differentPixelCount(reloaded.image(), off.image());

            require(onOffDifference >= MIN_DIFFERENT_PIXELS,
                    "M3 all-on screenshot is not visually distinct from OFF; differentPixels="
                            + onOffDifference);
            require(reloadedOffDifference >= MIN_DIFFERENT_PIXELS,
                    "M3 rendering did not survive resource reload; differentPixels="
                            + reloadedOffDifference);

            // The GameTest mod overrides white_glazed_terracotta with vanilla magenta_concrete.
            // Seeing that marker with BlockLens ON and OFF proves the wrapper is preserving the
            // already-active resource-pack base model instead of replacing it with bundled assets.
            require(on.magentaPixels() >= MIN_RESOURCE_PACK_MARKER_PIXELS,
                    "active resource-pack marker missing with M3 enabled; magentaPixels="
                            + on.magentaPixels());
            require(off.magentaPixels() >= MIN_RESOURCE_PACK_MARKER_PIXELS,
                    "active resource-pack marker missing with M3 disabled; magentaPixels="
                            + off.magentaPixels());

            writeManifest(
                    outputDir,
                    allOnImage,
                    reloadedImage,
                    allOffImage,
                    on,
                    reloaded,
                    off,
                    onOffDifference,
                    reloadedOffDifference);
        } catch (IOException exception) {
            throw new AssertionError("M3 visual parity evidence failed", exception);
        } finally {
            installRuntimeConfig(original);
        }
    }

    private static void buildScene(TestSingleplayerContext singleplayer) {
        var server = singleplayer.getServer();
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("gamemode spectator @a");
        server.runCommand("fill -10 -60 -3 10 -60 5 minecraft:smooth_quartz");

        // Front row: seven representative capabilities.
        server.runCommand("setblock -6 -59 2 minecraft:anvil[facing=north]");
        server.runCommand("setblock -4 -59 2 minecraft:beehive[facing=south,honey_level=5]");
        server.runCommand("setblock -2 -59 2 minecraft:campfire[facing=east,lit=true]");
        server.runCommand("setblock 0 -59 2 minecraft:white_glazed_terracotta[facing=north]");
        server.runCommand("setblock 2 -59 2 minecraft:grindstone[face=wall,facing=east]");
        server.runCommand("setblock 4 -59 2 minecraft:oak_fence_gate[facing=west,open=true,in_wall=false]");
        server.runCommand("setblock 6 -59 2 minecraft:ochre_froglight[axis=x]");

        // Rear row: six remaining capabilities.
        server.runCommand("setblock -5 -59 -1 minecraft:oak_log[axis=z]");
        server.runCommand("setblock -3 -59 -1 minecraft:quartz_slab[type=top]");
        server.runCommand("setblock -1 -59 -1 minecraft:white_stained_glass");
        server.runCommand("setblock 1 -59 -1 minecraft:quartz_stairs[facing=east,half=top,shape=outer_left]");
        server.runCommand("setblock 3 -59 -1 minecraft:oak_trapdoor[facing=south,half=top,open=true]");
        server.runCommand("setblock 5 -59 -1 minecraft:oak_wood[axis=x]");

        // Fixed spectator camera: deterministic framing, no held item animation.
        server.runCommand("tp @a 0 -54 15 180 18");
    }

    private static void settle(ClientGameTestContext context, int ticks) {
        context.waitTicks(ticks);
    }

    private static BlockLensConfig withDecorationEnabled(BlockLensConfig base, boolean enabled) {
        BlockLensConfig result = base;
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.category() == CapabilityId.Category.DECORATION) {
                result = result.withEnabled(capability, enabled);
            }
        }
        return result;
    }

    private static int decorationEnabledCount(BlockLensConfig config) {
        int count = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.category() == CapabilityId.Category.DECORATION && config.isEnabled(capability)) {
                count++;
            }
        }
        return count;
    }

    private static void installRuntimeConfig(BlockLensConfig config) {
        BlockLensRuntime.initialize(
                BlockLensRuntime.minecraftVersion(),
                config,
                BlockLensRuntime.initializationNanos());
    }

    private static void reloadResources(ClientGameTestContext context) {
        CompletableFuture<Void> reload = context.computeOnClient(client -> client.reloadResourcePacks());
        reload.join();
    }

    private static void requireModelPipeline(String phase) {
        require(MinecraftDecorationModelPlugin.isModelPipelineReady(),
                phase + ": decoration model pipeline not ready");
        require(MinecraftDecorationModelPlugin.wrappedModelCount() >= 254,
                phase + ": expected at least 254 wrapped M0 state models but got "
                        + MinecraftDecorationModelPlugin.wrappedModelCount());
    }

    private static Path takeScreenshot(
            ClientGameTestContext context,
            Path outputDir,
            String name) throws IOException {
        Path image = context.takeScreenshot(
                TestScreenshotOptions.of(name)
                        .withDestinationDir(outputDir)
                        .withSize(WIDTH, HEIGHT)
                        .disableCounterPrefix());
        require(Files.isRegularFile(image), "screenshot was not created: " + image);
        require(Files.size(image) > 1_000L, "screenshot is unexpectedly small: " + image);
        return image;
    }

    private static ImageMetrics inspect(Path imagePath) throws IOException {
        BufferedImage image = ImageIO.read(imagePath.toFile());
        require(image != null, "failed to decode screenshot: " + imagePath);
        require(image.getWidth() == WIDTH && image.getHeight() == HEIGHT,
                "unexpected screenshot dimensions: " + image.getWidth() + "x" + image.getHeight());

        int magentaPixels = 0;
        int nonBlackPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                int red = (argb >>> 16) & 0xFF;
                int green = (argb >>> 8) & 0xFF;
                int blue = argb & 0xFF;
                if (red + green + blue > 45) {
                    nonBlackPixels++;
                }
                if (red >= 100 && blue >= 75 && green <= 125
                        && red >= green + 30 && blue >= green + 20) {
                    magentaPixels++;
                }
            }
        }
        require(nonBlackPixels > (WIDTH * HEIGHT) / 5,
                "screenshot appears blank or under-rendered: " + imagePath);
        return new ImageMetrics(image, magentaPixels, nonBlackPixels, Files.size(imagePath));
    }

    private static int differentPixelCount(BufferedImage left, BufferedImage right) {
        require(left.getWidth() == right.getWidth() && left.getHeight() == right.getHeight(),
                "visual parity images have different dimensions");
        int different = 0;
        for (int y = 0; y < left.getHeight(); y++) {
            for (int x = 0; x < left.getWidth(); x++) {
                int a = left.getRGB(x, y);
                int b = right.getRGB(x, y);
                int delta = Math.abs(((a >>> 16) & 0xFF) - ((b >>> 16) & 0xFF))
                        + Math.abs(((a >>> 8) & 0xFF) - ((b >>> 8) & 0xFF))
                        + Math.abs((a & 0xFF) - (b & 0xFF));
                if (delta >= 24) {
                    different++;
                }
            }
        }
        return different;
    }

    private static void writeManifest(
            Path outputDir,
            Path onPath,
            Path reloadPath,
            Path offPath,
            ImageMetrics on,
            ImageMetrics reloaded,
            ImageMetrics off,
            int onOffDifference,
            int reloadedOffDifference) throws IOException {
        String manifest = "minecraft=" + BlockLensRuntime.minecraftVersion() + "\n"
                + "decorationCapabilities=" + EXPECTED_DECORATION_CAPABILITIES + "\n"
                + "wrappedModels=" + MinecraftDecorationModelPlugin.wrappedModelCount() + "\n"
                + "onFile=" + onPath.getFileName() + "\n"
                + "onBytes=" + on.fileBytes() + "\n"
                + "onMagentaPixels=" + on.magentaPixels() + "\n"
                + "reloadedFile=" + reloadPath.getFileName() + "\n"
                + "reloadedBytes=" + reloaded.fileBytes() + "\n"
                + "reloadedMagentaPixels=" + reloaded.magentaPixels() + "\n"
                + "offFile=" + offPath.getFileName() + "\n"
                + "offBytes=" + off.fileBytes() + "\n"
                + "offMagentaPixels=" + off.magentaPixels() + "\n"
                + "onOffDifferentPixels=" + onOffDifference + "\n"
                + "reloadedOffDifferentPixels=" + reloadedOffDifference + "\n";
        Files.writeString(
                outputDir.resolve("m3-visual-manifest.txt"),
                manifest,
                StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record ImageMetrics(
            BufferedImage image,
            int magentaPixels,
            int nonBlackPixels,
            long fileBytes) {
    }
}

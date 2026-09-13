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
import java.util.concurrent.CompletableFuture;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Representative active-resource-pack preservation oracle for M5.
 *
 * <p>A dedicated always-enabled GameTest resource pack deliberately replaces three Minecraft
 * resource-target models with unmistakable non-vanilla geometry and vanilla marker textures. The
 * test proves that BlockLens wraps that already-active baked model instead of substituting its own
 * base geometry.</p>
 *
 * <p>BlockLens is allowed to add highlight pixels around the active model. Therefore preservation
 * is measured as containment of the OFF/base foreground by the ON foreground, rather than symmetric
 * IoU. IoU is retained only as a diagnostic because it intentionally drops when valid decoration
 * expands the rendered silhouette.</p>
 */
final class M5ActiveResourcePackOracle {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int TARGET_MIN_X = 105;
    private static final int TARGET_MIN_Y = 175;
    private static final int TARGET_MAX_X = 535;
    private static final int TARGET_MAX_Y = 315;
    private static final int EXPECTED_ENABLED_CAPABILITIES = 3;
    private static final int MIN_MARKER_PIXELS = 20;
    private static final int MIN_ON_OFF_DIFFERENT_PIXELS = 80;
    private static final int MIN_FOREGROUND_PIXELS = 120;
    private static final int MIN_BASE_RETENTION_PERMILLE = 950;
    private static final int FOREGROUND_DELTA = 24;
    private static final int RELOAD_TIMEOUT_TICKS = 1200;
    private static final int SETTLE_TICKS = 35;
    private static final long RELOAD_OVERLAY_FADE_MILLIS = 1_500L;

    private M5ActiveResourcePackOracle() {
    }

    static void verify(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        BlockLensConfig original = BlockLensRuntime.config();
        Path outputDir = FabricLoader.getInstance().getGameDir().resolve("m5-pack-visual");

        try {
            Files.createDirectories(outputDir);
            buildScene(singleplayer);
            context.waitTicks(20);

            BlockLensConfig offConfig = allOff(original);
            install(offConfig);
            reloadResources(context);
            ResolvedPackFixture resolvedPack = inspectActivePackResources(context);
            logResolvedPack(resolvedPack);
            requirePipeline("active-pack OFF");
            rebuild(context);
            Path offPath = screenshot(context, outputDir, "m5-pack-off");

            BlockLensConfig onConfig = representativeOnly(original);
            require(enabledCount(onConfig) == EXPECTED_ENABLED_CAPABILITIES,
                    "M5 active-pack config must enable exactly three representative capabilities");
            install(onConfig);
            reloadResources(context);
            requirePipeline("active-pack ON");
            rebuild(context);
            Path onPath = screenshot(context, outputDir, "m5-pack-on");

            // Remove only the three resource targets while preserving camera, floor, lighting and
            // active resource-pack state. This gives an image-space background oracle for masks.
            install(offConfig);
            removeTargets(singleplayer);
            rebuild(context);
            Path airPath = screenshot(context, outputDir, "m5-pack-air-control");

            BufferedImage off = inspect(offPath);
            BufferedImage on = inspect(onPath);
            BufferedImage air = inspect(airPath);

            MarkerCounts markers = markerCounts(off);
            int onOffDifferentPixels = differentPixels(on, off);
            MaskMetrics geometry = geometryMetrics(off, on, air);

            System.out.println("BLOCKLENS_M5_ACTIVE_PACK minecraft=" + BlockLensRuntime.minecraftVersion()
                    + " pack=" + M5ActiveResourcePackFixture.PACK_ID
                    + " enabledCapabilities=" + EXPECTED_ENABLED_CAPABILITIES
                    + " magentaMarkerPixels=" + markers.magenta()
                    + " limeMarkerPixels=" + markers.lime()
                    + " yellowMarkerPixels=" + markers.yellow()
                    + " onOffDifferentPixels=" + onOffDifferentPixels
                    + " offForegroundPixels=" + geometry.offForeground()
                    + " onForegroundPixels=" + geometry.onForeground()
                    + " geometryIntersectionPixels=" + geometry.intersection()
                    + " baseRetentionPermille=" + geometry.baseRetentionPermille()
                    + " geometryIouPermille=" + geometry.iouPermille());

            writeManifest(
                    outputDir,
                    onPath,
                    offPath,
                    airPath,
                    resolvedPack,
                    markers,
                    onOffDifferentPixels,
                    geometry);

            String expectedSourcePack = M5ActiveResourcePackFixture.PACK_ID.toString();
            require(resolvedPack.allFrom(expectedSourcePack),
                    "M5 model resources did not resolve from the active fixture pack; expected="
                            + expectedSourcePack + ", actual=" + resolvedPack.sources());
            require(resolvedPack.allMarkersPresent(),
                    "M5 active-pack model payloads were not resolved; " + resolvedPack.markerSummary());
            require(markers.magenta() >= MIN_MARKER_PIXELS,
                    "diamond-ore magenta active-pack marker missing; pixels=" + markers.magenta());
            require(markers.lime() >= MIN_MARKER_PIXELS,
                    "deepslate-redstone lime active-pack marker missing; pixels=" + markers.lime());
            require(markers.yellow() >= MIN_MARKER_PIXELS,
                    "obsidian yellow active-pack marker missing; pixels=" + markers.yellow());
            require(onOffDifferentPixels >= MIN_ON_OFF_DIFFERENT_PIXELS,
                    "M5 representative pack scene did not react to enabled resource highlights; differentPixels="
                            + onOffDifferentPixels);
            require(geometry.offForeground() >= MIN_FOREGROUND_PIXELS,
                    "active-pack OFF geometry mask is unexpectedly small: " + geometry.offForeground());
            require(geometry.onForeground() >= MIN_FOREGROUND_PIXELS,
                    "active-pack ON geometry mask is unexpectedly small: " + geometry.onForeground());
            require(geometry.baseRetentionPermille() >= MIN_BASE_RETENTION_PERMILLE,
                    "M5 did not preserve representative active-pack base geometry; retention permille="
                            + geometry.baseRetentionPermille()
                            + ", intersection=" + geometry.intersection()
                            + ", offForeground=" + geometry.offForeground());
        } catch (IOException exception) {
            throw new AssertionError("M5 active resource-pack evidence failed", exception);
        } finally {
            install(original);
            rebuild(context);
        }
    }

    private static ResolvedPackFixture inspectActivePackResources(ClientGameTestContext context)
            throws IOException {
        return context.computeOnClient(client -> {
            ResourceManager resources = client.getResourceManager();
            return new ResolvedPackFixture(
                    inspectResolvedModel(resources, "diamond_ore", "magenta_concrete"),
                    inspectResolvedModel(resources, "deepslate_redstone_ore", "lime_concrete"),
                    inspectResolvedModel(resources, "obsidian", "yellow_concrete"));
        });
    }

    private static ResolvedModel inspectResolvedModel(
            ResourceManager resources,
            String modelName,
            String markerToken) throws IOException {
        Identifier resourceId = Identifier.fromNamespaceAndPath(
                "minecraft", "models/block/" + modelName + ".json");
        var resource = resources.getResource(resourceId)
                .orElseThrow(() -> new AssertionError("M5 model resource is missing: " + resourceId));
        String content;
        try (var stream = resource.open()) {
            content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        return new ResolvedModel(
                resourceId.toString(),
                resource.sourcePackId(),
                content.contains(markerToken));
    }

    private static void logResolvedPack(ResolvedPackFixture resolvedPack) {
        System.out.println("BLOCKLENS_M5_ACTIVE_PACK_RESOLUTION minecraft=" + BlockLensRuntime.minecraftVersion()
                + " expectedSource=" + M5ActiveResourcePackFixture.PACK_ID
                + " diamondSource=" + resolvedPack.diamond().sourcePackId()
                + " diamondMarker=" + resolvedPack.diamond().markerPresent()
                + " deepslateRedstoneSource=" + resolvedPack.deepslateRedstone().sourcePackId()
                + " deepslateRedstoneMarker=" + resolvedPack.deepslateRedstone().markerPresent()
                + " obsidianSource=" + resolvedPack.obsidian().sourcePackId()
                + " obsidianMarker=" + resolvedPack.obsidian().markerPresent());
    }

    private static void buildScene(TestSingleplayerContext singleplayer) {
        var server = singleplayer.getServer();
        server.runCommand("gamerule minecraft:advance_time false");
        server.runCommand("gamerule minecraft:advance_weather false");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("gamemode spectator @a");
        server.runCommand("fill -8 -60 -3 8 -60 5 minecraft:smooth_quartz");
        placeTargets(singleplayer);
        server.runCommand("tp @a 0 -54 13 180 18");
    }

    private static void placeTargets(TestSingleplayerContext singleplayer) {
        var server = singleplayer.getServer();
        server.runCommand("setblock -4 -59 1 minecraft:diamond_ore");
        server.runCommand("setblock 0 -59 1 minecraft:deepslate_redstone_ore");
        server.runCommand("setblock 4 -59 1 minecraft:obsidian");
    }

    private static void removeTargets(TestSingleplayerContext singleplayer) {
        var server = singleplayer.getServer();
        server.runCommand("setblock -4 -59 1 minecraft:air");
        server.runCommand("setblock 0 -59 1 minecraft:air");
        server.runCommand("setblock 4 -59 1 minecraft:air");
    }

    private static BlockLensConfig representativeOnly(BlockLensConfig base) {
        BlockLensConfig result = allOff(base);
        result = result.withEnabled(CapabilityId.DIAMOND_ORE, true);
        result = result.withEnabled(CapabilityId.DEEPSLATE_REDSTONE_ORE, true);
        return result.withEnabled(CapabilityId.OBSIDIAN, true);
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

    private static void reloadResources(ClientGameTestContext context) {
        CompletableFuture<Void> reload = context.computeOnClient(client -> client.reloadResourcePacks());
        context.waitFor(client -> reload.isDone(), RELOAD_TIMEOUT_TICKS);
        reload.join();
        try {
            Thread.sleep(RELOAD_OVERLAY_FADE_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for active-pack reload overlay", exception);
        }
        context.waitTicks(2);
    }

    private static void rebuild(ClientGameTestContext context) {
        context.runOnClient(MinecraftTerrainInvalidator::invalidateAll);
        context.waitTicks(SETTLE_TICKS);
    }

    private static void requirePipeline(String phase) {
        require(MinecraftDecorationModelPlugin.isModelPipelineReady(), phase + ": model pipeline not ready");
        require(MinecraftDecorationModelPlugin.wrappedModelCount() >= 320,
                phase + ": expected at least 320 wrapped models, got "
                        + MinecraftDecorationModelPlugin.wrappedModelCount());
    }

    private static Path screenshot(ClientGameTestContext context, Path outputDir, String name)
            throws IOException {
        Path path = context.takeScreenshot(
                TestScreenshotOptions.of(name)
                        .withDestinationDir(outputDir)
                        .withSize(WIDTH, HEIGHT)
                        .disableCounterPrefix());
        require(Files.isRegularFile(path) && Files.size(path) > 1_000L,
                "invalid M5 active-pack screenshot: " + path);
        return path;
    }

    private static BufferedImage inspect(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        require(image != null && image.getWidth() == WIDTH && image.getHeight() == HEIGHT,
                "invalid M5 active-pack framebuffer capture: " + path);
        return image;
    }

    private static MarkerCounts markerCounts(BufferedImage image) {
        int magenta = 0;
        int lime = 0;
        int yellow = 0;
        for (int y = TARGET_MIN_Y; y < TARGET_MAX_Y; y++) {
            for (int x = TARGET_MIN_X; x < TARGET_MAX_X; x++) {
                int rgb = image.getRGB(x, y);
                int r = red(rgb), g = green(rgb), b = blue(rgb);
                if (r >= 70 && b >= 65 && g + 20 <= r && g + 20 <= b) {
                    magenta++;
                }
                if (g >= 65 && g >= r + 25 && g >= b + 25) {
                    lime++;
                }
                if (r >= 70 && g >= 55 && b + 30 <= r && b + 25 <= g) {
                    yellow++;
                }
            }
        }
        return new MarkerCounts(magenta, lime, yellow);
    }

    private static int differentPixels(BufferedImage left, BufferedImage right) {
        int different = 0;
        for (int y = TARGET_MIN_Y; y < TARGET_MAX_Y; y++) {
            for (int x = TARGET_MIN_X; x < TARGET_MAX_X; x++) {
                if (colorDelta(left.getRGB(x, y), right.getRGB(x, y)) >= FOREGROUND_DELTA) {
                    different++;
                }
            }
        }
        return different;
    }

    private static MaskMetrics geometryMetrics(
            BufferedImage off,
            BufferedImage on,
            BufferedImage air) {
        int offForeground = 0;
        int onForeground = 0;
        int intersection = 0;
        int union = 0;
        for (int y = TARGET_MIN_Y; y < TARGET_MAX_Y; y++) {
            for (int x = TARGET_MIN_X; x < TARGET_MAX_X; x++) {
                int airRgb = air.getRGB(x, y);
                boolean offMask = colorDelta(off.getRGB(x, y), airRgb) >= FOREGROUND_DELTA;
                boolean onMask = colorDelta(on.getRGB(x, y), airRgb) >= FOREGROUND_DELTA;
                if (offMask) offForeground++;
                if (onMask) onForeground++;
                if (offMask && onMask) intersection++;
                if (offMask || onMask) union++;
            }
        }
        int baseRetentionPermille = offForeground == 0 ? 0 : (intersection * 1000) / offForeground;
        int iouPermille = union == 0 ? 0 : (intersection * 1000) / union;
        return new MaskMetrics(
                offForeground,
                onForeground,
                intersection,
                union,
                baseRetentionPermille,
                iouPermille);
    }

    private static int colorDelta(int a, int b) {
        return Math.abs(red(a) - red(b))
                + Math.abs(green(a) - green(b))
                + Math.abs(blue(a) - blue(b));
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
            Path airPath,
            ResolvedPackFixture resolvedPack,
            MarkerCounts markers,
            int onOffDifferentPixels,
            MaskMetrics geometry) throws IOException {
        String manifest = "minecraft=" + BlockLensRuntime.minecraftVersion() + "\n"
                + "activePack=" + M5ActiveResourcePackFixture.PACK_ID + "\n"
                + "fixtureModels=3\n"
                + "enabledCapabilities=" + EXPECTED_ENABLED_CAPABILITIES + "\n"
                + "fixtures=diamond_ore:half-height-magenta,deepslate_redstone_ore:narrow-lime,obsidian:inset-yellow\n"
                + "diamondResource=" + resolvedPack.diamond().resourceId() + "\n"
                + "diamondSourcePack=" + resolvedPack.diamond().sourcePackId() + "\n"
                + "diamondMarkerResolved=" + resolvedPack.diamond().markerPresent() + "\n"
                + "deepslateRedstoneResource=" + resolvedPack.deepslateRedstone().resourceId() + "\n"
                + "deepslateRedstoneSourcePack=" + resolvedPack.deepslateRedstone().sourcePackId() + "\n"
                + "deepslateRedstoneMarkerResolved=" + resolvedPack.deepslateRedstone().markerPresent() + "\n"
                + "obsidianResource=" + resolvedPack.obsidian().resourceId() + "\n"
                + "obsidianSourcePack=" + resolvedPack.obsidian().sourcePackId() + "\n"
                + "obsidianMarkerResolved=" + resolvedPack.obsidian().markerPresent() + "\n"
                + "wrappedModels=" + MinecraftDecorationModelPlugin.wrappedModelCount() + "\n"
                + "onFile=" + onPath.getFileName() + "\n"
                + "offFile=" + offPath.getFileName() + "\n"
                + "airControlFile=" + airPath.getFileName() + "\n"
                + "magentaMarkerPixels=" + markers.magenta() + "\n"
                + "limeMarkerPixels=" + markers.lime() + "\n"
                + "yellowMarkerPixels=" + markers.yellow() + "\n"
                + "onOffDifferentPixels=" + onOffDifferentPixels + "\n"
                + "offForegroundPixels=" + geometry.offForeground() + "\n"
                + "onForegroundPixels=" + geometry.onForeground() + "\n"
                + "geometryIntersectionPixels=" + geometry.intersection() + "\n"
                + "geometryUnionPixels=" + geometry.union() + "\n"
                + "baseRetentionPermille=" + geometry.baseRetentionPermille() + "\n"
                + "geometryIouPermille=" + geometry.iouPermille() + "\n";
        Files.writeString(
                outputDir.resolve("m5-pack-visual-manifest.txt"),
                manifest,
                StandardCharsets.UTF_8);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record ResolvedModel(String resourceId, String sourcePackId, boolean markerPresent) {
    }

    private record ResolvedPackFixture(
            ResolvedModel diamond,
            ResolvedModel deepslateRedstone,
            ResolvedModel obsidian) {
        boolean allFrom(String expectedSourcePack) {
            return diamond.sourcePackId().equals(expectedSourcePack)
                    && deepslateRedstone.sourcePackId().equals(expectedSourcePack)
                    && obsidian.sourcePackId().equals(expectedSourcePack);
        }

        boolean allMarkersPresent() {
            return diamond.markerPresent()
                    && deepslateRedstone.markerPresent()
                    && obsidian.markerPresent();
        }

        String sources() {
            return "diamond=" + diamond.sourcePackId()
                    + ", deepslateRedstone=" + deepslateRedstone.sourcePackId()
                    + ", obsidian=" + obsidian.sourcePackId();
        }

        String markerSummary() {
            return "diamond=" + diamond.markerPresent()
                    + ", deepslateRedstone=" + deepslateRedstone.markerPresent()
                    + ", obsidian=" + obsidian.markerPresent();
        }
    }

    private record MarkerCounts(int magenta, int lime, int yellow) {
    }

    private record MaskMetrics(
            int offForeground,
            int onForeground,
            int intersection,
            int union,
            int baseRetentionPermille,
            int iouPermille) {
    }
}

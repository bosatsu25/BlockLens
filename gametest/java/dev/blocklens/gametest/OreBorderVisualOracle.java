package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
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
import net.minecraft.resources.Identifier;

/** Real-frame isolated ore-border toggles, resource reload, and off restoration. */
final class OreBorderVisualOracle {
    private OreBorderVisualOracle() { }

    static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        BlockLensConfig original = BlockLensRuntime.config();
        Path output = FabricLoader.getInstance().getGameDir().resolve("ore-border-visual");
        try {
            Files.createDirectories(output);
            var server = world.getServer();
            server.runCommand("gamerule minecraft:advance_time false");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("gamemode spectator @a");
            server.runCommand("fill -11 -59 -7 11 -50 19 minecraft:air");
            server.runCommand("fill -11 -60 -7 11 -60 19 minecraft:black_concrete");
            server.runCommand("fill -5 -59 4 -3 -57 4 minecraft:diamond_ore");
            server.runCommand("fill -1 -59 4 1 -57 4 minecraft:gold_ore");
            server.runCommand("fill 3 -59 4 5 -57 4 minecraft:nether_gold_ore");
            server.runCommand("tp @a 0 -53 17 180 18");

            BlockLensConfig off = original;
            for (CapabilityId capability : CapabilityId.values()) {
                off = off.withEnabled(capability, false);
            }
            install(context, off);
            require(context.computeOnClient(client -> client.getResourceManager().getResource(
                    Identifier.fromNamespaceAndPath("blocklens",
                            "textures/block/lightweight/ore_border.png")).isPresent()),
                    "clean-room ore border texture is missing in the running client");
            BufferedImage baseline = capture(context, output, "all-off");

            for (CapabilityId ore : new CapabilityId[] {CapabilityId.DIAMOND_ORE,
                    CapabilityId.GOLD_ORE, CapabilityId.NETHER_GOLD_ORE}) {
                install(context, off.withEnabled(ore, true));
                int changed = changedPixels(baseline,
                        capture(context, output, ore.name().toLowerCase() + "-on"));
                require(changed >= 80, "independent ore cue not visible: " + ore + "/" + changed);
            }

            var reload = context.computeOnClient(client -> client.reloadResourcePacks());
            context.waitFor(client -> reload.isDone(), 1200);
            reload.join();
            context.waitFor(SettingsClientAccess::ready, 1200);
            context.waitTicks(25);
            require(context.computeOnClient(client ->
                    BlockLensRuntime.config().isEnabled(CapabilityId.NETHER_GOLD_ORE)),
                    "resource reload reset enabled ore");
            require(changedPixels(baseline, capture(context, output, "nether-gold-reloaded")) >= 80,
                    "ore border cue lost after reload");
            install(context, off);
            require(changedPixels(baseline, capture(context, output, "restored-off")) < 80,
                    "ore border and base cue were not restored OFF");
            String evidence = "minecraft=" + BlockLensRuntime.minecraftVersion()
                    + "\noreVariants=3\nisolatedSwitches=true\nactiveTexture=true\n"
                    + "reloadRetained=true\noffRestored=true\ncleanRoom=true\n";
            Files.writeString(output.resolve("ore-border-visual-manifest.txt"),
                    evidence, StandardCharsets.UTF_8);
            System.out.println("BLOCKLENS_ORE_BORDER vanilla=3 independent=true reload=true off=true");
        } catch (IOException error) {
            throw new AssertionError("ore border evidence failed", error);
        } finally {
            install(context, original);
        }
    }

    private static void install(ClientGameTestContext context, BlockLensConfig config) {
        context.runOnClient(client -> {
            BlockLensRuntime.installConfig(config);
            MinecraftTerrainInvalidator.invalidateAll(client);
        });
        context.waitTicks(25);
    }

    private static BufferedImage capture(ClientGameTestContext context, Path directory, String name)
            throws IOException {
        Path image = context.takeScreenshot(TestScreenshotOptions.of(name)
                .withDestinationDir(directory).withSize(640, 360).disableCounterPrefix());
        require(Files.isRegularFile(image) && Files.size(image) > 1000, "frame missing: " + name);
        BufferedImage pixels = ImageIO.read(image.toFile());
        require(pixels != null && pixels.getWidth() == 640 && pixels.getHeight() == 360,
                "invalid ore frame: " + name);
        return pixels;
    }

    private static int changedPixels(BufferedImage a, BufferedImage b) {
        int changed = 0;
        for (int y = 70; y < 330; y++) for (int x = 50; x < 590; x++) {
            int old = a.getRGB(x, y), now = b.getRGB(x, y);
            int difference = Math.abs((old >> 16 & 255) - (now >> 16 & 255))
                    + Math.abs((old >> 8 & 255) - (now >> 8 & 255))
                    + Math.abs((old & 255) - (now & 255));
            if (difference >= 40) changed++;
        }
        return changed;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

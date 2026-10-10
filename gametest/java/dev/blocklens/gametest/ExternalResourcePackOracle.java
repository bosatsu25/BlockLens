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
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

/** Private reference bytes are read locally; published evidence contains only hashes and frames. */
final class ExternalResourcePackOracle {
    private static final List<String> PACKS = List.of("amateras", "chise", "lowfire", "glowing", "small");
    private static final List<String> PROBES = List.of(
            "blockstates/diamond_ore.json", "models/block/anvil/anvil.json",
            "textures/block/white_concrete.png", "models/block/fire_floor1.json",
            "textures/block/diamond_ore.png", "models/block/block.json",
            "textures/block/nether_gold_ore.png", "textures/block/nether_quartz_ore.png");

    private ExternalResourcePackOracle() { }

    static void verify(ClientGameTestContext context, TestSingleplayerContext world,
            Path directory, Properties manifest, Path output) throws IOException {
        require(String.join(",", PACKS).equals(manifest.getProperty("packs")), "Reference pack set differs");
        BlockLensConfig originalConfig = BlockLensRuntime.config();
        List<String> originalPacks = context.computeOnClient(client ->
                List.copyOf(client.getResourcePackRepository().getSelectedIds()));
        Path resourceDirectory = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
        Files.createDirectories(resourceDirectory);
        Map<String, Map<String, String>> hashes = new LinkedHashMap<>();
        for (String alias : PACKS) {
            Path source = directory.resolve("packs").resolve(alias + ".zip");
            require(Files.isRegularFile(source) && !Files.isSymbolicLink(source)
                    && Files.size(source) <= 8 * 1024 * 1024, "Reference pack input missing or oversized");
            require(ExternalCompatibilityOracle.sha256(Files.readAllBytes(source))
                    .equals(manifest.getProperty("pack." + alias + ".sha256")), "Reference pack bytes differ");
            Path target = resourceDirectory.resolve("blocklens-external-" + alias + ".zip");
            require(!Files.isSymbolicLink(target), "Reference pack destination is a symlink");
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            hashes.put(alias, probeHashes(source));
        }
        Map<String, String> forwardSources;
        Map<String, String> reverseSources;
        try {
            context.runOnClient(client -> client.getResourcePackRepository().reload());
            forwardSources = verifyOrder(context, world, originalPacks, PACKS, hashes,
                    originalConfig, output, "forward");
            List<String> reverse = new ArrayList<>(PACKS);
            Collections.reverse(reverse);
            reverseSources = verifyOrder(context, world, originalPacks, reverse, hashes,
                    originalConfig, output, "reverse");
            int changed = 0;
            for (String resource : PROBES) {
                if (!forwardSources.get(resource).equals(reverseSources.get(resource))) changed++;
            }
            require(changed == 2, "The two overlapping ore textures did not follow pack priority");
        } finally {
            context.runOnClient(client -> {
                BlockLensRuntime.installConfig(originalConfig);
                client.getResourcePackRepository().setSelected(originalPacks);
            });
            reload(context);
        }
        require(originalPacks.equals(context.computeOnClient(client ->
                List.copyOf(client.getResourcePackRepository().getSelectedIds()))), "Selected packs were not restored");
        require(BlockLensRuntime.config().enabledMask() == originalConfig.enabledMask(), "Config was not restored");
        Files.writeString(output.resolve("external-packs-restoration.txt"),
                "packCount=5\nprobesPerOrder=8\npriorityChangedSources=2\nselectedPacksRestored=true\nconfigRestored=true\n",
                StandardCharsets.UTF_8);
    }

    private static Map<String, String> probeHashes(Path archive) throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            for (String probe : PROBES) {
                var entry = zip.getEntry("assets/minecraft/" + probe);
                if (entry == null) continue;
                require(entry.getSize() >= 0 && entry.getSize() <= 4 * 1024 * 1024, "Reference probe exceeds bounds");
                try (var stream = zip.getInputStream(entry)) {
                    byte[] bytes = stream.readNBytes(4 * 1024 * 1024 + 1);
                    require(bytes.length == entry.getSize(), "Reference probe bytes exceed the recorded size");
                    result.put(probe, ExternalCompatibilityOracle.sha256(bytes));
                }
            }
        }
        return result;
    }

    private static Map<String, String> verifyOrder(ClientGameTestContext context, TestSingleplayerContext world,
            List<String> originals, List<String> order, Map<String, Map<String, String>> hashes,
            BlockLensConfig originalConfig, Path output, String name) throws IOException {
        context.runOnClient(client -> {
            var repository = client.getResourcePackRepository();
            List<String> selected = new ArrayList<>(originals);
            for (String alias : order) {
                String id = packId(alias);
                require(repository.getAvailableIds().contains(id), "A recorded pack was not discovered");
                selected.add(id);
            }
            repository.setSelected(selected);
        });
        reload(context);
        require(MinecraftDecorationModelPlugin.isModelPipelineReady(), "Pack reload did not restore model pipeline");
        Map<String, String> sources = verifyResources(context, order, hashes, output, name);
        buildScene(world);
        BlockLensConfig off = originalConfig;
        for (CapabilityId capability : CapabilityId.values()) off = off.withEnabled(capability, false);
        install(context, off);
        Path offFrame = frame(context, output, "external-packs-" + name + "-off");
        BlockLensConfig on = off.withEnabled(CapabilityId.NETHER_GOLD_ORE, true)
                .withEnabled(CapabilityId.ANVIL, true).withEnabled(CapabilityId.LOG, true);
        install(context, on);
        Path onFrame = frame(context, output, "external-packs-" + name + "-on");
        install(context, off);
        var server = world.getServer();
        server.runCommand("setblock -4 -59 1 minecraft:air");
        server.runCommand("setblock 0 -59 1 minecraft:air");
        server.runCommand("setblock 4 -59 1 minecraft:air");
        settle(context);
        Path airFrame = frame(context, output, "external-packs-" + name + "-air");
        verifyGeometry(offFrame, onFrame, airFrame, output, name);
        return sources;
    }

    private static Map<String, String> verifyResources(ClientGameTestContext context, List<String> order,
            Map<String, Map<String, String>> hashes, Path output, String orderName) throws IOException {
        Map<String, String> sources = new LinkedHashMap<>();
        List<String> records = new ArrayList<>();
        for (String resource : PROBES) {
            String highest = null;
            for (String alias : order) if (hashes.get(alias).containsKey(resource)) highest = alias;
            require(highest != null, "Reference resource does not belong to a recorded pack");
            String expectedSource = packId(highest);
            String expectedHash = hashes.get(highest).get(resource);
            context.runOnClient(client -> {
                var resolved = client.getResourceManager().getResource(
                        Identifier.fromNamespaceAndPath("minecraft", resource)).orElseThrow();
                require(resolved.sourcePackId().equals(expectedSource), "Active resource ignored pack priority");
                try (var stream = resolved.open()) {
                    byte[] bytes = stream.readNBytes(4 * 1024 * 1024 + 1);
                    require(bytes.length <= 4 * 1024 * 1024, "Active resource probe exceeds bounds");
                    require(ExternalCompatibilityOracle.sha256(bytes).equals(expectedHash),
                            "Active resource bytes differ from the selected third-party pack");
                } catch (IOException exception) {
                    throw new AssertionError("Active reference resource could not be read", exception);
                }
            });
            sources.put(resource, highest);
            records.add(resource + "\t" + highest + "\t" + expectedHash);
        }
        Files.write(output.resolve("external-packs-" + orderName + "-resources.tsv"), records, StandardCharsets.UTF_8);
        return sources;
    }

    private static void buildScene(TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("gamerule minecraft:advance_time false");
        server.runCommand("gamerule minecraft:advance_weather false");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("gamemode spectator @a");
        server.runCommand("fill -8 -60 -3 8 -60 5 minecraft:white_concrete");
        server.runCommand("setblock -4 -59 1 minecraft:nether_gold_ore");
        server.runCommand("setblock 0 -59 1 minecraft:anvil[facing=south]");
        server.runCommand("setblock 4 -59 1 minecraft:oak_log[axis=x]");
        server.runCommand("tp @a 0 -54 13 180 18");
    }

    private static void install(ClientGameTestContext context, BlockLensConfig config) {
        context.runOnClient(client -> BlockLensRuntime.installConfig(config));
        settle(context);
    }

    private static void settle(ClientGameTestContext context) {
        context.runOnClient(MinecraftTerrainInvalidator::invalidateAll);
        context.waitTicks(40);
    }

    private static void reload(ClientGameTestContext context) {
        var pending = context.computeOnClient(client -> client.reloadResourcePacks());
        context.waitFor(client -> pending.isDone(), 1200);
        pending.join();
        context.waitFor(SettingsClientAccess::ready, 1200);
        settle(context);
    }

    private static Path frame(ClientGameTestContext context, Path output, String name) throws IOException {
        Path path = context.takeScreenshot(TestScreenshotOptions.of(name).withDestinationDir(output)
                .withSize(640, 360).disableCounterPrefix());
        require(Files.isRegularFile(path) && Files.size(path) > 1000, "Reference frame missing or empty");
        return path;
    }

    private static void verifyGeometry(Path offPath, Path onPath, Path airPath, Path output, String name)
            throws IOException {
        BufferedImage off = read(offPath), on = read(onPath), air = read(airPath);
        int foreground = 0, retained = 0, changed = 0;
        for (int y = 175; y < 315; y++) {
            for (int x = 105; x < 535; x++) {
                if (delta(off.getRGB(x, y), on.getRGB(x, y)) >= 24) changed++;
                if (delta(off.getRGB(x, y), air.getRGB(x, y)) < 24) continue;
                foreground++;
                boolean found = false;
                for (int dy = -2; dy <= 2 && !found; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        if (delta(on.getRGB(x + dx, y + dy), air.getRGB(x + dx, y + dy)) >= 24) {
                            found = true;
                            break;
                        }
                    }
                }
                if (found) retained++;
            }
        }
        int retention = foreground == 0 ? 0 : retained * 1000 / foreground;
        Files.writeString(output.resolve("external-packs-" + name + "-visual.txt"),
                "changedPixels=" + changed + "\nbaseForeground=" + foreground + "\nretentionPermille=" + retention
                        + "\nmaskThreshold=24\nmaskToleranceRadius=2\nminimumRetentionPermille=950\n",
                StandardCharsets.UTF_8);
        require(foreground >= 120, "Reference pack scene has no substantial target geometry");
        require(changed >= 80, "Reference pack scene did not react to BlockLens cues");
        require(retention >= 950, "BlockLens did not preserve the active reference base geometry");
    }

    private static BufferedImage read(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        require(image != null && image.getWidth() == 640 && image.getHeight() == 360,
                "Reference image dimensions differ");
        return image;
    }

    private static int delta(int a, int b) {
        return Math.abs(((a >>> 16) & 255) - ((b >>> 16) & 255))
                + Math.abs(((a >>> 8) & 255) - ((b >>> 8) & 255)) + Math.abs((a & 255) - (b & 255));
    }

    private static String packId(String alias) {
        return "file/blocklens-external-" + alias + ".zip";
    }

    private static void require(boolean value, String message) {
        ExternalCompatibilityOracle.require(value, message);
    }
}

package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.ComfortOptions;
import dev.blocklens.core.render.ComfortRenderPolicy;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.resources.Identifier;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Individual rendered controls, animation, actual first-person transforms and all-46 reload. */
final class LightweightVisualOracle {
    private LightweightVisualOracle() { }
    static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        BlockLensConfig original = BlockLensRuntime.config();
        Path output = FabricLoader.getInstance().getGameDir().resolve("lightweight-visual");
        StringBuilder evidence = new StringBuilder("minecraft=" + BlockLensRuntime.minecraftVersion() + "\n");
        boolean bob = context.computeOnClient(c -> c.options.bobView().get());
        try {
            Files.createDirectories(output);
            context.runOnClient(c -> c.options.bobView().set(false));
            var server = world.getServer();
            server.runCommand("gamerule minecraft:advance_time false");
            server.runCommand("gamerule minecraft:advance_weather false");
            server.runCommand("gamerule minecraft:random_tick_speed 0");
            server.runCommand("time set midnight");
            server.runCommand("weather clear");
            server.runCommand("gamemode spectator @a");
            server.runCommand("fill -12 -61 -8 12 -49 20 minecraft:black_concrete hollow");
            server.runCommand("fill -10 -60 -6 10 -60 7 minecraft:black_concrete");
            server.runCommand("tp @a 0 -53 17 180 18");
            for (CapabilityId capability : new CapabilityId[] {CapabilityId.GLASS_HIGHLIGHT,
                    CapabilityId.KELP_HIGHLIGHT, CapabilityId.BRIGHT_CONCRETE, CapabilityId.BRIGHT_CHEST}) {
                scene(world, capability);
                server.runCommand(capability == CapabilityId.KELP_HIGHLIGHT
                        ? "tp @a 0 -55 13 180 18" : "tp @a 0 -53 17 180 18");
                BlockLensConfig off = all(original, false);
                install(context, off);
                if (capability == CapabilityId.KELP_HIGHLIGHT) {
                    context.runOnClient(client -> {
                        require(client.level != null && client.level.getBlockState(new BlockPos(-3, -59, 4)).is(Blocks.KELP),
                                "kelp head fixture was destroyed before capture");
                        require(client.level.getBlockState(new BlockPos(0, -59, 4)).is(Blocks.KELP_PLANT),
                                "kelp plant fixture was destroyed before capture");
                    });
                }
                Path offImage = capture(context, output, capability.name().toLowerCase() + "-off");
                install(context, off.withEnabled(capability, true));
                Path onImage = capture(context, output, capability.name().toLowerCase() + "-on");
                int pixels = capability == CapabilityId.KELP_HIGHLIGHT
                        ? kelpCueDifference(read(offImage), read(onImage)) : difference(read(offImage), read(onImage));
                evidence.append(capability.sourceKey()).append("DifferentPixels=").append(pixels).append('\n');
                require(pixels >= 80, capability + " has no visible isolated change: " + pixels);
                if (capability == CapabilityId.BRIGHT_CONCRETE || capability == CapabilityId.BRIGHT_CHEST) {
                    require(brightPixels(read(onImage)) >= 80, capability + " did not produce bright white pixels");
                }
                if (capability == CapabilityId.KELP_HIGHLIGHT) {
                    context.waitTicks(6);
                    Path animation = capture(context, output, "kelp-animation");
                    int animated = kelpCueShapeDifference(read(onImage), read(animation));
                    evidence.append("kelpAnimatedPixels=").append(animated).append('\n');
                    require(animated >= 80, "kelp overlay did not animate");
                    List<String> packs = context.computeOnClient(client ->
                            List.copyOf(client.getResourcePackRepository().getSelectedIds()));
                    try {
                        String control = context.computeOnClient(client -> {
                            var repository = client.getResourcePackRepository();
                            repository.reload();
                            String id = repository.getAvailableIds().stream()
                                    .filter(candidate -> candidate.contains("blocklens-gametest")
                                            && candidate.endsWith("kelp-static-control"))
                                    .findFirst().orElseThrow(() -> new AssertionError("static kelp control missing"));
                            var selected = new ArrayList<>(packs);
                            selected.add(id);
                            repository.setSelected(selected);
                            return id;
                        });
                        reload(context);
                        context.runOnClient(client -> require(client.getResourceManager().getResource(
                                Identifier.fromNamespaceAndPath("blocklens", "textures/block/lightweight/kelp.png"))
                                .orElseThrow().sourcePackId().equals(control), "static control did not win resource order"));
                        Path staticFirst = capture(context, output, "kelp-static-control-first");
                        context.waitTicks(6);
                        Path staticSecond = capture(context, output, "kelp-static-control-second");
                        int staticPixels = kelpCueShapeDifference(read(staticFirst), read(staticSecond));
                        int staticCuePixels = Math.min(kelpCuePixels(read(staticFirst)), kelpCuePixels(read(staticSecond)));
                        require(staticCuePixels >= 80, "static control lost its visible kelp cue");
                        System.out.println("BLOCKLENS_KELP_ANIMATION animatedCueShapePixels=" + animated
                                + " staticCueShapePixels=" + staticPixels + " staticVisibleCuePixels=" + staticCuePixels);
                        Files.writeString(output.resolve("kelp-animation-control.txt"),
                                "animatedCueShapePixels=" + animated + "\nstaticCueShapePixels=" + staticPixels + "\nstaticVisibleCuePixels=" + staticCuePixels + "\n",
                                StandardCharsets.UTF_8);
                        evidence.append("kelpStaticControlPixels=").append(staticPixels).append('\n');
                        require(staticPixels <= 12 && animated > 8 * staticPixels,
                                "water/base animation can falsely pass the kelp gate: " + animated + "/" + staticPixels);
                    } finally {
                        context.runOnClient(client -> client.getResourcePackRepository().setSelected(packs));
                        reload(context);
                    }
                }
                reload(context);
                Path reloaded = capture(context, output, capability.name().toLowerCase() + "-reloaded");
                int reloadPixels = capability == CapabilityId.KELP_HIGHLIGHT
                        ? kelpCueDifference(read(offImage), read(reloaded)) : difference(read(offImage), read(reloaded));
                require(reloadPixels >= 80, "isolated overlay lost after reload: " + capability);
                if (capability == CapabilityId.GLASS_HIGHLIGHT) verifyPanes(context, world, output, original, evidence);
                if (capability == CapabilityId.BRIGHT_CHEST) verifyChests(context, output, off, offImage, onImage, evidence);
            }
            scene(world, CapabilityId.BRIGHT_CHEST);
            server.runCommand("gamemode survival @a");
            server.runCommand("tp @a 0 -58 10 180 0");
            server.runCommand("effect give @a minecraft:fire_resistance infinite 0 true");
            server.runOnServer(s -> s.getPlayerList().getPlayers().forEach(player -> {
                player.setRemainingFireTicks(1200);
                player.setSharedFlagOnFire(true);
            }));
            context.waitTicks(12);
            require(context.computeOnClient(c -> c.player != null && c.player.isOnFire()),
                    "fire overlay fixture is not active");
            BlockLensConfig off = all(original, false);
            install(context, off);
            fireSample(context);
            float baselineY = ComfortTransformProbe.fireY;
            float baselineOffset = ComfortTransformProbe.fireOffsetY;
            require(Float.isFinite(baselineY) && Math.abs(baselineY) > .0001f, "fire draw was not observed");
            capture(context, output, "fire-off");
            for (int preset = 0; preset < 3; preset++) {
                install(context, off.withEnabled(CapabilityId.LOW_FIRE, true)
                        .withComfortOptions(new ComfortOptions(preset, 70, 60, 75)));
                fireSample(context);
                float ratio = ComfortTransformProbe.fireY / baselineY;
                float offset = (ComfortTransformProbe.fireOffsetY - baselineOffset) / baselineY;
                require(Math.abs(ratio - ComfortRenderPolicy.fireScaleY(preset)) < .001f,
                        "actual fire scale differs from preset: " + ratio);
                require(Math.abs(offset - ComfortRenderPolicy.fireOffsetY(preset)) < .001f,
                        "actual fire offset differs from preset: " + offset);
                evidence.append("firePreset").append(preset).append("Scale=").append(ratio)
                        .append("Offset=").append(offset).append('\n');
                capture(context, output, "fire-preset-" + preset);
            }
            server.runOnServer(s -> s.getPlayerList().getPlayers().forEach(player -> {
                player.setRemainingFireTicks(0);
                player.setSharedFlagOnFire(false);
            }));
            server.runCommand("effect clear @a minecraft:fire_resistance");
            context.waitTicks(10);
            String[] items = {"white_concrete", "apple", "diamond_sword", "shield"};
            float[] scales = {.70f, .60f, .75f, .95f};
            for (int index = 0; index < items.length; index++) {
                server.runCommand("item replace entity @a weapon.mainhand with minecraft:" + items[index]);
                server.runCommand("item replace entity @a weapon.offhand with minecraft:" + items[index]);
                context.waitTicks(16);
                install(context, off);
                heldSample(context);
                float left = ComfortTransformProbe.leftScale;
                float right = ComfortTransformProbe.rightScale;
                require(Float.isFinite(left) && Float.isFinite(right), "both held item draws were not observed");
                Path offImage = capture(context, output, "held-" + items[index] + "-off");
                install(context, off.withEnabled(CapabilityId.HANDHELD_SIZE, true)
                        .withComfortOptions(ComfortOptions.defaults()));
                heldSample(context);
                float leftRatio = ComfortTransformProbe.leftScale / left;
                float rightRatio = ComfortTransformProbe.rightScale / right;
                require(Math.abs(leftRatio - scales[index]) < .003f && Math.abs(rightRatio - scales[index]) < .003f,
                        "actual held item scale mismatch: " + items[index] + " left=" + leftRatio + " right=" + rightRatio);
                Path onImage = capture(context, output, "held-" + items[index] + "-on");
                int pixels = difference(read(offImage), read(onImage));
                require(pixels >= 30, "held size has no rendered change: " + items[index]);
                evidence.append("held").append(index).append("Left=").append(leftRatio)
                        .append("Right=").append(rightRatio).append("DifferentPixels=").append(pixels).append('\n');
            }
            scene(world, CapabilityId.GLASS_HIGHLIGHT);
            server.runCommand("setblock -3 -59 1 minecraft:white_concrete");
            server.runCommand("setblock 0 -59 1 minecraft:chest[facing=north]");
            server.runCommand("setblock 3 -59 1 minecraft:kelp[age=0]");
            server.runCommand("setblock -6 -59 1 minecraft:netherrack");
            server.runCommand("setblock 6 -59 1 minecraft:diamond_ore");
            BlockLensConfig allOn = all(original, true);
            require(Long.bitCount(allOn.enabledMask()) == 46, "all-46 fixture excludes a capability");
            install(context, allOn);
            server.runCommand("effect give @a minecraft:fire_resistance infinite 0 true");
            server.runOnServer(srv -> srv.getPlayerList().getPlayers().forEach(player -> {
                player.setRemainingFireTicks(1200);
                player.setSharedFlagOnFire(true);
            }));
            context.waitTicks(12);
            reload(context);
            fireSample(context);
            require(Math.abs(ComfortTransformProbe.fireY / baselineY
                    - ComfortRenderPolicy.fireScaleY(allOn.comfortOptions().fireSize())) < .001f,
                    "all-46 simultaneous fire transform missing");
            heldSample(context);
            require(Float.isFinite(ComfortTransformProbe.leftScale) && Float.isFinite(ComfortTransformProbe.rightScale),
                    "all-46 simultaneous held item submissions missing");
            capture(context, output, "all46-reloaded");
            server.runCommand("effect clear @a minecraft:fire_resistance");
            evidence.append("all46Enabled=true\nall46Reload=true\nall46FireAndBothHands=true\n");
            Files.writeString(output.resolve("lightweight-visual-manifest.txt"), evidence, StandardCharsets.UTF_8);
            System.out.println("BLOCKLENS_LIGHTWEIGHT_VISUAL isolated=4 animatedKelp=true firePresets=3 heldCategories=4 bothHands=true all46Reload=true");
        } catch (IOException e) {
            throw new AssertionError("lightweight visual evidence failed", e);
        } finally {
            ComfortTransformProbe.active = false;
            world.getServer().runOnServer(s -> s.getPlayerList().getPlayers().forEach(player -> {
                player.setRemainingFireTicks(0);
                player.setSharedFlagOnFire(false);
            }));
            context.runOnClient(c -> c.options.bobView().set(bob));
            install(context, original);
        }
    }
    private static void scene(TestSingleplayerContext world, CapabilityId capability) {
        var server = world.getServer();
        server.runCommand("fill -11 -59 -7 11 -50 19 minecraft:air");
        server.runCommand("fill -11 -60 -7 11 -60 19 minecraft:black_concrete");
        switch (capability) {
            case GLASS_HIGHLIGHT -> {
                String[] blocks = {"glass", "tinted_glass", "glass_pane", "white_stained_glass", "white_stained_glass_pane"};
                for (int i = 0; i < blocks.length; i++) server.runCommand("setblock " + (i * 3 - 6) + " -59 4 minecraft:" + blocks[i]);
            }
            case KELP_HIGHLIGHT -> {
                server.runCommand("fill -4 -59 3 4 -57 5 minecraft:water");
                server.runCommand("setblock -3 -59 4 minecraft:kelp[age=0]");
                server.runCommand("setblock 0 -59 4 minecraft:kelp[age=0]");
                server.runCommand("setblock 0 -58 4 minecraft:kelp[age=0]");
                server.runCommand("setblock 0 -59 4 minecraft:kelp_plant");
                server.runCommand("setblock 3 -59 4 minecraft:kelp[age=0]");
            }
            case BRIGHT_CONCRETE -> server.runCommand("fill -4 -59 4 4 -57 4 minecraft:white_concrete");
            case BRIGHT_CHEST -> {
                server.runCommand("setblock -6 -59 4 minecraft:chest[facing=north]");
                server.runCommand("setblock -1 -59 4 minecraft:chest[facing=north,type=left]");
                server.runCommand("setblock 0 -59 4 minecraft:chest[facing=north,type=right]");
                server.runCommand("setblock 5 -59 4 minecraft:trapped_chest[facing=north]");
                server.runCommand("setblock 7 -59 4 minecraft:ender_chest[facing=north]");
            }
            default -> throw new IllegalArgumentException("invalid scene capability");
        }
    }
    private static void verifyChests(ClientGameTestContext context, Path output, BlockLensConfig off,
            Path offPath, Path onPath, StringBuilder evidence) throws IOException {
        BufferedImage disabled = read(offPath), enabled = read(onPath);
        int[][] regular = {{210, 243}, {290, 312}, {312, 335}};
        for (int i = 0; i < regular.length; i++) {
            int white = brightPixels(enabled, regular[i][0], regular[i][1], 208, 240)
                    - brightPixels(disabled, regular[i][0], regular[i][1], 208, 240);
            require(white >= 20, "regular chest variant lacks white cue: " + i + "/" + white);
            evidence.append("regularChestVariant").append(i).append("WhiteGain=").append(white).append('\n');
        }
        for (int[] bounds : new int[][] {{384, 416}, {417, 449}}) {
            require(brightPixels(enabled, bounds[0], bounds[1], 208, 240)
                            == brightPixels(disabled, bounds[0], bounds[1], 208, 240),
                    "trapped/ender chest incorrectly received a white cue");
        }
        int[] positions = {-6, -1, 0};
        install(context, off);
        context.runOnClient(client -> {
            for (int x : positions) {
                var entity = client.level.getBlockEntity(new BlockPos(x, -59, 4));
                require(entity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity,
                        "regular chest animation fixture missing");
                entity.triggerEvent(1, 1);
            }
        });
        context.waitTicks(20);
        assertChestLids(context, positions, true);
        install(context, off.withEnabled(CapabilityId.BRIGHT_CHEST, true));
        assertChestLids(context, positions, true);
        BufferedImage open = read(capture(context, output, "bright-chest-open"));
        require(difference(enabled, open, 210, 335, 180, 260) >= 20,
                "open chest retained the closed rendered silhouette");
        context.runOnClient(client -> {
            for (int x : positions) client.level.getBlockEntity(new BlockPos(x, -59, 4)).triggerEvent(1, 0);
        });
        context.waitTicks(20);
        assertChestLids(context, positions, false);
        evidence.append("nonRegularChestsUnchanged=true\nchestLidAnimation=true\n");
    }
    private static void assertChestLids(ClientGameTestContext context, int[] positions, boolean open) {
        context.runOnClient(client -> {
            for (int x : positions) {
                var entity = (net.minecraft.world.level.block.entity.ChestBlockEntity)
                        client.level.getBlockEntity(new BlockPos(x, -59, 4));
                float progress = entity.getOpenNess(1.0f);
                require(open ? progress >= 0.9f : progress <= 0.1f, "chest lid transition stopped: " + progress);
            }
        });
    }
    private static void verifyPanes(ClientGameTestContext context, TestSingleplayerContext world,
            Path output, BlockLensConfig original, StringBuilder evidence) throws IOException {
        var server = world.getServer();
        server.runCommand("fill -11 -59 -7 11 -50 19 minecraft:air");
        server.runCommand("setblock 0 -59 4 minecraft:glass_pane");
        server.runCommand("setblock 3 -59 4 minecraft:white_stained_glass_pane");
        server.runCommand("tp @a 2 -58 10 180 12");
        BlockLensConfig off = all(original, false);
        install(context, off);
        BufferedImage baseline = read(capture(context, output, "panes-off"));
        require(amberPixels(baseline, 40, 320) == 0 && amberPixels(baseline, 320, 600) == 0,
                "pane fixture already contains amber cue colors");
        for (boolean stained : new boolean[] {false, true}) {
            install(context, off.withEnabled(CapabilityId.GLASS_HIGHLIGHT, true)
                    .withEnabled(CapabilityId.STAINED_GLASS, stained));
            BufferedImage image = read(capture(context, output, stained ? "panes-combined" : "panes-highlight"));
            int left = amberPixels(image, 40, 320), right = amberPixels(image, 320, 600);
            evidence.append("paneCombined").append(stained).append("LeftAmberPixels=").append(left)
                    .append("RightAmberPixels=").append(right).append('\n');
            require(left >= 12 && right >= 12,
                    "plain/stained pane cue missing with stained-glass=" + stained + ": " + left + "/" + right);
        }
    }
    private static int amberPixels(BufferedImage image, int startX, int endX) {
        int count = 0;
        for (int y = 70; y < 330; y++) for (int x = startX; x < endX; x++) {
            int rgb = image.getRGB(x, y);
            int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
            if (r >= 120 && g >= 80 && b < 130 && r > g + 20 && g > b + 30) count++;
        }
        return count;
    }
    private static void fireSample(ClientGameTestContext context) {
        context.runOnClient(c -> ComfortTransformProbe.reset());
        context.waitTicks(6);
        require(Float.isFinite(ComfortTransformProbe.fireY), "actual fire submission was not observed");
    }
    private static void heldSample(ClientGameTestContext context) {
        context.runOnClient(c -> ComfortTransformProbe.reset());
        context.waitTicks(6);
    }
    private static BlockLensConfig all(BlockLensConfig base, boolean enabled) {
        for (CapabilityId capability : CapabilityId.values()) base = base.withEnabled(capability, enabled);
        return base;
    }
    private static void install(ClientGameTestContext context, BlockLensConfig config) {
        BlockLensRuntime.installConfig(config);
        context.runOnClient(MinecraftTerrainInvalidator::invalidateAll);
        context.waitTicks(24);
    }
    private static void reload(ClientGameTestContext context) {
        CompletableFuture<Void> future = context.computeOnClient(c -> c.reloadResourcePacks());
        context.waitFor(c -> future.isDone(), 1200);
        future.join();
        context.waitFor(SettingsClientAccess::ready, 1200);
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
        context.waitTicks(24);
    }
    private static Path capture(ClientGameTestContext context, Path directory, String name) throws IOException {
        Path image = context.takeScreenshot(TestScreenshotOptions.of(name).withDestinationDir(directory)
                .withSize(640, 360).disableCounterPrefix());
        require(Files.isRegularFile(image) && Files.size(image) > 1000, "render capture missing");
        read(image);
        return image;
    }
    private static BufferedImage read(Path path) throws IOException {
        var image = ImageIO.read(path.toFile());
        require(image != null && image.getWidth() == 640 && image.getHeight() == 360, "invalid rendered evidence");
        return image;
    }
    private static boolean kelpCue(int rgb) {
        int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
        // Water attenuates the emissive cue; hue distinguishes it from blue water and green kelp.
        return r >= 40 && b >= 35 && r > g + 15 && b > g + 8
                || r >= 40 && g >= 20 && r > g + 16 && g > b + 8;
    }
    private static int kelpCuePixels(BufferedImage image) {
        int count = 0;
        for (int y = 70; y < 330; y++) for (int x = 40; x < 600; x++) {
            if (kelpCue(image.getRGB(x, y))) count++;
        }
        return count;
    }
    private static boolean nearbyKelpCue(BufferedImage image, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            if (kelpCue(image.getRGB(x + dx, y + dy))) return true;
        }
        return false;
    }
    private static int kelpCueShapeDifference(BufferedImage a, BufferedImage b) {
        int count = 0;
        for (int y = 70; y < 330; y++) for (int x = 40; x < 600; x++) {
            if (kelpCue(a.getRGB(x, y)) && !nearbyKelpCue(b, x, y)) count++;
            if (kelpCue(b.getRGB(x, y)) && !nearbyKelpCue(a, x, y)) count++;
        }
        return count;
    }
    private static int kelpCueDifference(BufferedImage a, BufferedImage b) {
        int count = 0;
        for (int y = 70; y < 330; y++) for (int x = 40; x < 600; x++) {
            int left = a.getRGB(x, y), right = b.getRGB(x, y);
            if (!kelpCue(left) && !kelpCue(right)) continue;
            int delta = Math.abs((left >> 16 & 255) - (right >> 16 & 255))
                    + Math.abs((left >> 8 & 255) - (right >> 8 & 255)) + Math.abs((left & 255) - (right & 255));
            if (delta >= 24) count++;
        }
        return count;
    }
    private static int brightPixels(BufferedImage image) {
        return brightPixels(image, 40, 600, 70, 330);
    }
    private static int brightPixels(BufferedImage image, int startX, int endX, int startY, int endY) {
        int count = 0;
        for (int y = startY; y < endY; y++) for (int x = startX; x < endX; x++) {
            int rgb = image.getRGB(x, y);
            int r = rgb >> 16 & 255, g = rgb >> 8 & 255, b = rgb & 255;
            if (Math.min(r, Math.min(g, b)) >= 160
                    && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) <= 40) count++;
        }
        return count;
    }
    private static int difference(BufferedImage a, BufferedImage b) {
        return difference(a, b, 40, 600, 70, 330);
    }
    private static int difference(BufferedImage a, BufferedImage b, int startX, int endX, int startY, int endY) {
        int pixels = 0;
        for (int y = startY; y < endY; y++) for (int x = startX; x < endX; x++) {
            int left = a.getRGB(x, y), right = b.getRGB(x, y);
            int delta = Math.abs((left >> 16 & 255) - (right >> 16 & 255))
                    + Math.abs((left >> 8 & 255) - (right >> 8 & 255)) + Math.abs((left & 255) - (right & 255));
            if (delta >= 24) pixels++;
        }
        return pixels;
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

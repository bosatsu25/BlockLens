package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.scene.SceneFilterOptions;
import dev.blocklens.fabric.MinecraftDecorationTargetIndex;
import dev.blocklens.fabric.MinecraftOreExtensions;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
import dev.blocklens.fabric.SceneFilterClient;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.storage.LevelResource;

/** Focused native evidence for the two optional Sodium terrain paths, not the full capability graph. */
final class SodiumTerrainOracle {
    private static final int MIN_DIFFERENCE = 80;
    private static final int MIN_RETENTION = 950;
    private static final int DELTA = 24;
    private static final int RADIUS = 2;
    private static final String PACK_NAME = "blocklens-sodium-terrain-test";
    private static final String AIR = "minecraft:air";
    private static final String STONE = "minecraft:stone";

    private SodiumTerrainOracle() { }

    static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        new Run(context, world).verify();
    }

    private static final class Run {
        private final ClientGameTestContext context;
        private final TestSingleplayerContext world;
        private final BlockLensConfig original = BlockLensRuntime.config();
        private final BlockLensConfig off = allOff(original);
        private final BlockLensConfig on = off.withEnabled(CapabilityId.DIAMOND_ORE, true);
        private final Path output = FabricLoader.getInstance().getGameDir().resolve("sodium-terrain");
        private final Properties evidence = new Properties();
        private final List<String> failures = new ArrayList<>();
        private final List<Path> images = new ArrayList<>();
        private Path dataPack;

        Run(ClientGameTestContext context, TestSingleplayerContext world) {
            this.context = context;
            this.world = world;
        }

        void verify() {
            try {
                Files.createDirectories(output);
                evidence.setProperty("schema", "1");
                evidence.setProperty("minecraft", BlockLensRuntime.minecraftVersion());
                evidence.setProperty("status", "started");
                evidence.setProperty("scope", "sodium-terrain");
                evidence.setProperty("captureMode", ExternalFrameCapture.required()
                        ? "completed-native-frame" : "fabric-test-capture");
                number("minimumDifferencePixels", MIN_DIFFERENCE);
                number("minimumRetentionPermille", MIN_RETENTION);
                number("colorDelta", DELTA);
                number("maskToleranceRadius", RADIUS);
                number("observationLimit", 4096);
                number("roiMinX", 90);
                number("roiMaxX", 550);
                number("roiMinY", 90);
                number("roiMaxY", 310);
                writeEvidence();
                prepare();

                place(AIR);
                install(off);
                Capture air = capture("air-control", AIR);
                place(OreExtensionFixture.EXPLICIT);
                Capture explicitOff = capture("explicit-off", OreExtensionFixture.EXPLICIT);
                install(on);
                Capture explicitOn = capture("explicit-on", OreExtensionFixture.EXPLICIT);
                install(off);
                Capture explicitRestored = capture("explicit-restored", OreExtensionFixture.EXPLICIT);
                check(explicitOff.emission().emitted() > 0, "explicitBaselineEmission");
                check(explicitOn.replacement() && explicitOn.emission().expectedModel() > 0,
                        "explicitRetainedModelEmission");
                changed(explicitOff, explicitOn, "explicitHighlight");
                same(explicitOff, explicitRestored, "explicitRestoration");
                retention(explicitOff, explicitOn, air, "explicitBase");

                place(OreExtensionFixture.UNSUPPORTED);
                Capture unsupportedOff = capture("unsupported-off", OreExtensionFixture.UNSUPPORTED);
                install(on);
                Capture unsupportedOn = capture("unsupported-on", OreExtensionFixture.UNSUPPORTED);
                check(!unsupportedOn.replacement() && unsupportedOn.emission().emitted() > 0
                                && unsupportedOn.emission().expectedModel() > 0,
                        "unsupportedBaseEmission");
                same(unsupportedOff, unsupportedOn, "unsupportedControl");

                place(OreExtensionFixture.TAGGED);
                install(off);
                Capture taggedOff = capture("tagged-off", OreExtensionFixture.TAGGED);
                check(!taggedOff.replacement() && taggedOff.emission().emitted() > 0,
                        "tagAbsentBaseline");
                reloadTags(true);
                install(on);
                Capture taggedOn = capture("tagged-on", OreExtensionFixture.TAGGED);
                check(taggedOn.replacement() && taggedOn.emission().expectedModel() > 0,
                        "tagRetainedModelEmission");
                changed(taggedOff, taggedOn, "tagHighlight");
                retention(taggedOff, taggedOn, air, "tagBase");
                reloadTags(false);
                Capture removed = capture("tag-removed", OreExtensionFixture.TAGGED);
                check(!removed.replacement() && removed.emission().expectedModel() > 0,
                        "tagRemovedBaseEmission");
                same(taggedOff, removed, "tagRemoval");

                reloadTags(true);
                Sample beforeHide = sample(OreExtensionFixture.TAGGED);
                recordObservation("tag-pre-hide", beforeHide.replacement(), beforeHide.emission());
                check(beforeHide.replacement() && beforeHide.emission().expectedModel() > 0,
                        "tagPreHideEmission");
                install(filtered(on, OreExtensionFixture.TAGGED));
                Capture taggedFiltered = capture("tagged-filtered", OreExtensionFixture.TAGGED);
                check(taggedFiltered.emission().emitted() == 0, "tagFilterEmissionSuppressed");
                same(air, taggedFiltered, "tagFilterAir");
                physicalState(OreExtensionFixture.TAGGED, "tagPhysicalStatePreserved");
                install(on);
                Capture taggedRestored = capture("tagged-restored", OreExtensionFixture.TAGGED);
                check(taggedRestored.emission().expectedModel() > 0, "tagRestoredEmission");
                same(taggedOn, taggedRestored, "tagFilterRestoration");

                var oldModels = context.computeOnClient(c -> c.getModelManager().getBlockStateModelSet());
                var reload = context.computeOnClient(c -> c.reloadResourcePacks());
                context.waitFor(c -> reload.isDone(), 1200);
                reload.join();
                context.waitFor(c -> MinecraftOreExtensions.ready()
                        && MinecraftOreExtensions.compiledModelCount() == 2, 600);
                Thread.sleep(1500);
                context.waitTicks(30);
                Capture reloaded = capture("tagged-reloaded", OreExtensionFixture.TAGGED);
                check(context.computeOnClient(c -> c.getModelManager().getBlockStateModelSet()) != oldModels
                                && reloaded.replacement() && reloaded.emission().expectedModel() > 0,
                        "resourceReloadEmission");
                changed(taggedOff, reloaded, "resourceReloadHighlight");

                place(STONE);
                install(off);
                Capture stoneOff = capture("stone-off", STONE);
                check(stoneOff.emission().emitted() > 0 && stoneOff.emission().expectedModel() > 0,
                        "terrainBaselineEmission");
                changed(air, stoneOff, "terrainVisible");
                install(filtered(off, STONE));
                Capture stoneHidden = capture("stone-hidden", STONE);
                check(stoneHidden.emission().emitted() == 0, "terrainEmissionSuppressed");
                same(air, stoneHidden, "terrainHiddenAir");
                physicalState(STONE, "terrainPhysicalStatePreserved");
                install(off);
                Capture stoneRestored = capture("stone-restored", STONE);
                check(stoneRestored.emission().emitted() > 0, "terrainRestoredEmission");
                same(stoneOff, stoneRestored, "terrainRestoration");

                require(images.size() == 15, "Sodium fixture did not produce exactly 15 frames");
                evidence.setProperty("status", failures.isEmpty() ? "passed" : "failed");
                evidence.setProperty("failedChecks", String.join(",", failures));
                writeEvidence();
                System.out.println("BLOCKLENS_SODIUM_TERRAIN minecraft=" + BlockLensRuntime.minecraftVersion()
                        + " explicit=" + evidence.getProperty("explicitRetainedModelEmission")
                        + " tagged=" + evidence.getProperty("tagRetainedModelEmission")
                        + " terrain=" + evidence.getProperty("terrainEmissionSuppressed")
                        + " composition=" + evidence.getProperty("tagFilterEmissionSuppressed")
                        + " images=" + images.size() + " passed=" + failures.isEmpty());
                require(failures.isEmpty(), "Sodium native terrain checks failed: " + String.join(",", failures));
            } catch (IOException exception) {
                throw new AssertionError("Sodium terrain evidence failed", exception);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Sodium resource reload interrupted", exception);
            } finally {
                SodiumTerrainProbe.clear();
                try {
                    if (Files.isDirectory(output)) writeEvidence();
                } catch (IOException exception) {
                    throw new AssertionError("Sodium partial evidence could not be preserved", exception);
                } finally {
                    context.runOnClient(c -> {
                        BlockLensRuntime.installConfig(original);
                        MinecraftTerrainInvalidator.invalidateAll(c);
                    });
                }
            }
        }

        private void prepare() throws IOException {
            String minecraft = BlockLensRuntime.minecraftVersion();
            String sodium = FabricLoader.getInstance().getModContainer("sodium").orElseThrow()
                    .getMetadata().getVersion().getFriendlyString();
            require(("0.9.2+mc" + minecraft).equals(sodium), "Sodium terrain fixture requires the exact tested release");
            evidence.setProperty("sodiumVersion", sodium);
            install(off);
            var server = world.getServer();
            server.runCommand("gamerule minecraft:advance_time false");
            server.runCommand("gamerule minecraft:advance_weather false");
            server.runCommand("gamerule minecraft:random_tick_speed 0");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("gamemode spectator @a");
            server.runCommand("kill @e[type=minecraft:villager]");
            server.runCommand("kill @e[type=minecraft:armor_stand]");
            server.runCommand("fill -12 -61 -8 12 -49 20 minecraft:black_concrete hollow");
            server.runCommand("fill -11 -60 -7 11 -50 19 minecraft:air");
            server.runCommand("fill -10 -50 -6 10 -50 7 minecraft:light[level=15]");
            server.runCommand("tp @a 0 -54 12 180 16");
            context.waitTicks(25);
            context.waitFor(c -> MinecraftOreExtensions.ready()
                    && MinecraftOreExtensions.compiledModelCount() == 1, 600);
            context.runOnClient(c -> {
                require(MinecraftOreExtensions.tagCandidateCount() == 0, "Sodium fixture starts with unexpected tag bindings");
                var index = MinecraftDecorationTargetIndex.build();
                require(index.bindingCount() == 366 && index.mappedCount() == 328, "Sodium extension changed original target index");
                for (String id : List.of(OreExtensionFixture.EXPLICIT, OreExtensionFixture.TAGGED,
                        OreExtensionFixture.UNSUPPORTED)) {
                    require(index.mask(BuiltInRegistries.BLOCK.getId(block(id))) == 0,
                            "Sodium fixture accidentally uses the existing decoration path");
                }
                var models = c.getModelManager().getBlockStateModelSet();
                var explicit = block(OreExtensionFixture.EXPLICIT).defaultBlockState();
                require(MinecraftOreExtensions.model(models.get(explicit), explicit) != models.get(explicit),
                        "Explicit retained model was not prepared");
                var resource = c.getResourceManager().getResource(Identifier.fromNamespaceAndPath(
                        "blocklens-gametest", "models/block/extension_ore.json")).orElseThrow();
                require(M5ActiveResourcePackFixture.PACK_ID.toString().equals(resource.sourcePackId()),
                        "Sodium fixture did not resolve the active non-cube model");
                try (var stream = resource.open()) {
                    String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    require(text.contains("gold_block") && text.contains("elements"),
                            "Sodium active model payload differs");
                }
            });
            check(true, "originalIndexUnchanged");
            check(true, "activeNonCubeResource");
            dataPack = world.getServer().computeOnServer(s -> s.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_NAME));
        }

        private void place(String id) {
            world.getServer().runCommand("setblock 0 -57 4 " + id);
            context.waitTicks(25);
        }

        private void install(BlockLensConfig config) {
            context.runOnClient(c -> {
                BlockLensRuntime.installConfig(config);
                MinecraftTerrainInvalidator.invalidateAll(c);
            });
            context.waitTicks(35);
            require(context.computeOnClient(c -> SceneFilterClient.readyForCurrentConfig()),
                    "Sodium fixture filter publication did not settle");
        }

        private Sample sample(String id) {
            boolean replacement = context.computeOnClient(c -> {
                var state = c.level.getBlockState(SodiumTerrainProbe.TARGET);
                require(state.getBlock() == block(id), "Sodium fixture target state did not arrive: " + id);
                var base = c.getModelManager().getBlockStateModelSet().get(state);
                var selected = MinecraftOreExtensions.model(base, state);
                SodiumTerrainProbe.arm(state, selected);
                MinecraftTerrainInvalidator.invalidateAll(c);
                return selected != base;
            });
            context.waitTicks(35);
            if (!AIR.equals(id)) {
                context.waitFor(c -> SodiumTerrainProbe.result().completed() > 0, 600);
            }
            return new Sample(replacement, SodiumTerrainProbe.result());
        }

        private Capture capture(String name, String id) throws IOException {
            evidence.setProperty("stage", name);
            Sample sample = sample(id);
            context.runOnClient(c -> c.particleEngine.clearParticles());
            String filename = "sodium-" + name;
            Path image = ExternalFrameCapture.required()
                    ? ExternalFrameCapture.capture(context, output, filename, 640, 360)
                    : context.takeScreenshot(TestScreenshotOptions.of(filename).withDestinationDir(output)
                            .withSize(640, 360).disableCounterPrefix());
            require(Files.isRegularFile(image) && Files.size(image) > 1000
                            && Files.size(image) <= 4 * 1024 * 1024,
                    "Sodium framebuffer is missing or oversized");
            BufferedImage pixels = ImageIO.read(image.toFile());
            require(pixels != null && pixels.getWidth() == 640 && pixels.getHeight() == 360,
                    "Sodium framebuffer dimensions differ");
            images.add(image);
            SodiumTerrainProbe.Result result = SodiumTerrainProbe.result();
            recordObservation(name, sample.replacement(), result);
            writeEvidence();
            return new Capture(pixels, sample.replacement(), result);
        }

        private void recordObservation(String name, boolean replacement, SodiumTerrainProbe.Result result) {
            number(name + ".visited", result.visited());
            number(name + ".completed", result.completed());
            number(name + ".emitted", result.emitted());
            number(name + ".expectedModel", result.expectedModel());
            evidence.setProperty(name + ".saturated", Boolean.toString(result.saturated()));
            evidence.setProperty(name + ".preparedReplacement", Boolean.toString(replacement));
            boolean air = name.equals("air-control");
            boolean hidden = name.equals("tagged-filtered") || name.equals("stone-hidden");
            check(air || hidden ? result.emitted() == 0
                    : result.emitted() > 0 && result.expectedModel() == result.emitted(),
                    name + ".expectedEmission");
            check(!result.saturated() && result.completed() == result.visited()
                    && result.expectedModel() <= result.emitted() && result.emitted() <= result.completed()
                    && (air ? result.visited() == 0 : result.completed() > 0), name + ".observationComplete");
        }

        private void reloadTags(boolean include) throws IOException {
            Path tag = dataPack.resolve("data/c/tags/block/ores/diamond.json");
            Files.createDirectories(tag.getParent());
            int format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).major();
            Files.writeString(dataPack.resolve("pack.mcmeta"),
                    "{\"pack\":{\"description\":\"BlockLens Sodium test\",\"min_format\":" + format
                            + ",\"max_format\":" + format + "}}", StandardCharsets.UTF_8);
            Files.writeString(tag, "{\"replace\":true,\"values\":["
                    + (include ? "\"" + OreExtensionFixture.TAGGED + "\"" : "") + "]}", StandardCharsets.UTF_8);
            int previous = context.computeOnClient(c -> MinecraftOreExtensions.tagUpdateCount());
            var reload = world.getServer().computeOnServer(server -> {
                server.getPackRepository().reload();
                var selected = new ArrayList<>(server.getPackRepository().getSelectedIds());
                String id = "file/" + PACK_NAME;
                require(server.getPackRepository().isAvailable(id), "Sodium test data pack was not discovered");
                if (!selected.contains(id)) selected.add(id);
                return server.reloadResources(selected);
            });
            context.waitFor(c -> reload.isDone(), 1200);
            reload.join();
            context.waitFor(c -> MinecraftOreExtensions.tagUpdateCount() > previous && MinecraftOreExtensions.ready()
                    && MinecraftOreExtensions.compiledModelCount() == (include ? 2 : 1), 600);
            context.waitTicks(35);
        }

        private void physicalState(String id, String key) {
            check(context.computeOnClient(c -> {
                var state = c.level.getBlockState(SodiumTerrainProbe.TARGET);
                return state.getBlock() == block(id) && state.getRenderShape() == RenderShape.MODEL
                        && state.isSolidRender() && SceneFilterClient.hiddenBlock(state.getBlock());
            }), key);
        }

        private void changed(Capture before, Capture after, String key) {
            int count = different(before.image(), after.image());
            number(key + "DifferentPixels", count);
            check(count >= MIN_DIFFERENCE, key);
        }

        private void same(Capture before, Capture after, String key) {
            int count = different(before.image(), after.image());
            number(key + "DifferentPixels", count);
            check(count < MIN_DIFFERENCE, key);
        }

        private void retention(Capture base, Capture highlighted, Capture air, String key) {
            int foreground = 0, retained = 0;
            for (int y = 90; y < 310; y++) {
                for (int x = 90; x < 550; x++) {
                    if (delta(base.image().getRGB(x, y), air.image().getRGB(x, y)) < DELTA) continue;
                    foreground++;
                    boolean found = false;
                    for (int yy = Math.max(90, y - RADIUS); yy <= Math.min(309, y + RADIUS) && !found; yy++) {
                        for (int xx = Math.max(90, x - RADIUS); xx <= Math.min(549, x + RADIUS); xx++) {
                            if (delta(highlighted.image().getRGB(xx, yy), air.image().getRGB(xx, yy)) >= DELTA) {
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) retained++;
                }
            }
            int ratio = foreground == 0 ? 0 : retained * 1000 / foreground;
            number(key + "Foreground", foreground);
            number(key + "Retained", retained);
            number(key + "RetentionPermille", ratio);
            check(foreground >= MIN_DIFFERENCE && ratio >= MIN_RETENTION, key + "Geometry");
        }

        private void check(boolean value, String name) {
            evidence.setProperty(name, Boolean.toString(value));
            if (!value && !failures.contains(name)) failures.add(name);
        }

        private void number(String key, int value) {
            evidence.setProperty(key, Integer.toString(value));
        }

        private void writeEvidence() throws IOException {
            number("imageCount", images.size());
            for (Path path : images) {
                try {
                    evidence.setProperty(path.getFileName() + ".sha256",
                            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
                } catch (NoSuchAlgorithmException exception) {
                    throw new AssertionError(exception);
                }
            }
            StringBuilder text = new StringBuilder();
            evidence.stringPropertyNames().stream().sorted().forEach(key ->
                    text.append(key).append('=').append(evidence.getProperty(key)).append('\n'));
            byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
            require(bytes.length <= 16384, "Sodium evidence manifest exceeded its bound");
            Files.write(output.resolve("sodium-terrain-manifest.properties"), bytes);
        }
    }

    private static BlockLensConfig allOff(BlockLensConfig config) {
        for (CapabilityId id : CapabilityId.values()) config = config.withEnabled(id, false);
        return config;
    }

    private static BlockLensConfig filtered(BlockLensConfig config, String id) {
        return config.withEnabled(CapabilityId.BLOCK_FILTER, true).withSceneFilterOptions(
                SceneFilterOptions.defaults().withMode(0, SceneFilterOptions.Mode.BLACKLIST).withList(0, false, id));
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(id));
    }

    private static int different(BufferedImage a, BufferedImage b) {
        int count = 0;
        for (int y = 90; y < 310; y++) for (int x = 90; x < 550; x++) {
            if (delta(a.getRGB(x, y), b.getRGB(x, y)) >= DELTA) count++;
        }
        return count;
    }

    private static int delta(int a, int b) {
        return Math.abs((a >> 16 & 255) - (b >> 16 & 255))
                + Math.abs((a >> 8 & 255) - (b >> 8 & 255)) + Math.abs((a & 255) - (b & 255));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record Sample(boolean replacement, SodiumTerrainProbe.Result emission) { }
    private record Capture(BufferedImage image, boolean replacement, SodiumTerrainProbe.Result emission) { }
}

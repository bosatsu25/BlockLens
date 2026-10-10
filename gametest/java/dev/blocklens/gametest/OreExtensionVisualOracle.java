package dev.blocklens.gametest;

import dev.blocklens.api.OreHighlightExtensions;
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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;

/** Packaged-client evidence for explicit registration and real synchronized data-pack tag changes. */
final class OreExtensionVisualOracle {
    private static final BlockPos TARGET = new BlockPos(0, -57, 4);
    private static final String PACK_NAME = "blocklens-ore-extension-test";
    private static final int MIN_DIFFERENCE = 80;
    private OreExtensionVisualOracle() { }

    static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        BlockLensConfig original = BlockLensRuntime.config();
        BlockLensConfig off = allOff(original);
        BlockLensConfig on = off.withEnabled(CapabilityId.DIAMOND_ORE, true);
        Path output = FabricLoader.getInstance().getGameDir().resolve("ore-extension-visual");
        List<Path> images = new ArrayList<>();
        StringBuilder evidence = new StringBuilder("minecraft=" + BlockLensRuntime.minecraftVersion() + "\n");
        try {
            Files.createDirectories(output);
            var server = world.getServer();
            server.runCommand("gamerule minecraft:advance_time false");
            server.runCommand("gamerule minecraft:advance_weather false");
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
            context.waitFor(client -> MinecraftOreExtensions.ready()
                    && MinecraftOreExtensions.compiledModelCount() == 1, 600);
            context.runOnClient(client -> {
                var index = MinecraftDecorationTargetIndex.build();
                require(index.bindingCount() == 366 && index.mappedCount() == 328, "extension changed original target index");
                var stateModels = client.getModelManager().getBlockStateModelSet();
                var explicitState = block(OreExtensionFixture.EXPLICIT).defaultBlockState();
                require(MinecraftOreExtensions.model(stateModels.get(explicitState), explicitState) != stateModels.get(explicitState),
                        "explicit registration did not compile without tags");
                for (String id : new String[]{OreExtensionFixture.EXPLICIT, OreExtensionFixture.UNSUPPORTED, OreExtensionFixture.CONFLICTED,
                        OreExtensionFixture.MANY_STATES}) {
                    require(index.mask(BuiltInRegistries.BLOCK.getId(block(id))) == 0, "extension was double indexed");
                }
                for (String id : new String[]{OreExtensionFixture.UNSUPPORTED, OreExtensionFixture.CONFLICTED,
                        OreExtensionFixture.MANY_STATES}) {
                    var state = block(id).defaultBlockState();
                    require(MinecraftOreExtensions.model(stateModels.get(state), state) == stateModels.get(state),
                            "unsupported fixture created an extension model");
                }
                require(!OreHighlightExtensions.register(1, OreExtensionFixture.TAGGED, "gaming.diamond_ore"), "late registration accepted");
                require(MinecraftOreExtensions.tagCandidateCount() == 0, "unavailable tags created candidates");
                Identifier modelId = Identifier.fromNamespaceAndPath("blocklens-gametest", "models/block/extension_ore.json");
                var resource = client.getResourceManager().getResource(modelId).orElseThrow();
                require(resource.sourcePackId().equals(M5ActiveResourcePackFixture.PACK_ID.toString()),
                        "extension model did not resolve from the active override pack");
                try (var stream = resource.open()) {
                    String payload = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    require(payload.contains("gold_block") && payload.contains("elements"), "active non-cube fixture not resolved");
                }
            });
            evidence.append("explicitRegistration=true\nunsupportedIsolation=true\nabsentTags=true\nactiveModelResource=true\n");

            place(world, OreExtensionFixture.EXPLICIT);
            install(context, off);
            BufferedImage explicitOff = capture(context, output, "explicit-off", images);
            install(context, on);
            BufferedImage explicitOn = capture(context, output, "explicit-on", images);
            require(difference(explicitOff, explicitOn) >= MIN_DIFFERENCE, "explicit ore did not highlight");
            install(context, off);
            require(difference(explicitOff, capture(context, output, "explicit-restored", images)) < MIN_DIFFERENCE,
                    "explicit OFF did not restore base");
            place(world, "minecraft:air");
            BufferedImage air = capture(context, output, "air-control", images);
            int retention = retention(explicitOff, explicitOn, air);
            require(retention >= 950, "extension highlight lost active non-cube base geometry");
            evidence.append("baseRetentionPermille=").append(retention).append('\n');

            place(world, OreExtensionFixture.UNSUPPORTED);
            BufferedImage unsupportedOff = capture(context, output, "unsupported-off", images);
            install(context, on);
            require(difference(unsupportedOff, capture(context, output, "unsupported-on", images)) < MIN_DIFFERENCE,
                    "unsupported API changed fixture rendering");

            Path dataPack = world.getServer().computeOnServer(s -> s.getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK_NAME));
            var initialModels = context.computeOnClient(c -> c.getModelManager().getBlockStateModelSet());
            writeTagPack(dataPack, normalTargets());
            reloadTags(context, world, 2);
            context.runOnClient(c -> {
                require(c.getModelManager().getBlockStateModelSet() == initialModels, "tag application reloaded client models");
                var explicitState = block(OreExtensionFixture.EXPLICIT).defaultBlockState();
                var base = initialModels.get(explicitState);
                var highlighted = MinecraftOreExtensions.model(base, explicitState);
                require(highlighted != base && MinecraftOreExtensions.model(highlighted, explicitState) == highlighted,
                        "explicit/tag collision double-wrapped the target");
            });
            place(world, OreExtensionFixture.EXPLICIT);
            require(difference(explicitOn, capture(context, output, "explicit-tag-precedence", images)) < MIN_DIFFERENCE,
                    "gold tag overrode the explicit diamond control");
            place(world, OreExtensionFixture.TAGGED);
            install(context, off);
            BufferedImage taggedOff = capture(context, output, "tagged-off", images);
            install(context, on);
            BufferedImage taggedOn = capture(context, output, "tagged-on", images);
            require(difference(taggedOff, taggedOn) >= MIN_DIFFERENCE, "synchronized ore tag did not highlight");

            int rebuilds = context.computeOnClient(c -> MinecraftOreExtensions.rebuildCount());
            reloadTags(context, world, 2);
            require(context.computeOnClient(c -> MinecraftOreExtensions.rebuildCount()) == rebuilds,
                    "equivalent tag update rebuilt models");
            writeTagPack(dataPack, List.of());
            reloadTags(context, world, 1);
            require(difference(taggedOff, capture(context, output, "tag-removed", images)) < MIN_DIFFERENCE,
                    "tag removal did not restore active base");
            evidence.append("tagAdd=true\ntagRemove=true\nnoModelReloadForTags=true\nequivalentTagsStable=true\nexplicitPrecedence=true\n");

            List<String> overflow = new ArrayList<>();
            for (int i = 0; i < OreExtensionFixture.OVERFLOW_TARGETS; i++) overflow.add(OreExtensionFixture.overflowId(i));
            writeTagPack(dataPack, overflow.subList(0, 256));
            reloadTags(context, world, 1);
            require(context.computeOnClient(c -> MinecraftOreExtensions.tagCandidateCount()) == 256,
                    "combined target overflow did not preserve bounded candidate input");
            writeTagPack(dataPack, overflow);
            reloadTags(context, world, 1);
            require(context.computeOnClient(c -> MinecraftOreExtensions.tagCandidateCount()) == 0,
                    "over-limit conventional target generation was partially applied");
            place(world, OreExtensionFixture.EXPLICIT);
            require(difference(explicitOn, capture(context, output, "overflow-explicit-survives", images)) < MIN_DIFFERENCE,
                    "tag overflow affected explicit integration");
            List<String> excessiveMembers = context.computeOnClient(c -> BuiltInRegistries.BLOCK.keySet().stream()
                    .filter(id -> id.getNamespace().equals("minecraft")).map(Identifier::toString).sorted().limit(513).toList());
            require(excessiveMembers.size() == 513, "raw tag member overflow fixture incomplete");
            writeTagPack(dataPack, excessiveMembers);
            reloadTags(context, world, 1);
            require(context.computeOnClient(c -> MinecraftOreExtensions.tagCandidateCount()) == 0, "oversized tag was ingested");
            writeTagPack(dataPack, normalTargets());
            reloadTags(context, world, 2);
            evidence.append("targetOverflowIsolated=true\ncombinedTargetOverflowIsolated=true\ntagMemberOverflowIsolated=true\noverflowRecovery=true\n");

            place(world, OreExtensionFixture.TAGGED);
            var rules = SceneFilterOptions.defaults().withMode(0, SceneFilterOptions.Mode.BLACKLIST)
                    .withList(0, false, OreExtensionFixture.TAGGED);
            install(context, on.withEnabled(CapabilityId.BLOCK_FILTER, true).withSceneFilterOptions(rules));
            require(context.computeOnClient(c -> SceneFilterClient.hiddenBlock(block(OreExtensionFixture.TAGGED))),
                    "scene filter did not suppress tagged target");
            require(difference(air, capture(context, output, "tagged-filtered", images)) < MIN_DIFFERENCE,
                    "tag wrapper bypassed scene filtering");
            context.runOnClient(c -> require(c.level.getBlockState(TARGET).getBlock() == block(OreExtensionFixture.TAGGED),
                    "scene filtering altered world data"));
            install(context, on);
            require(difference(taggedOn, capture(context, output, "tagged-filter-restored", images)) < MIN_DIFFERENCE,
                    "scene filter OFF did not restore tagged ore");

            var reload = context.computeOnClient(c -> c.reloadResourcePacks());
            context.waitFor(c -> reload.isDone(), 1200);
            reload.join();
            context.waitFor(c -> MinecraftOreExtensions.compiledModelCount() == 2, 600);
            Thread.sleep(1500);
            context.waitTicks(30);
            require(difference(taggedOff, capture(context, output, "tagged-resource-reloaded", images)) >= MIN_DIFFERENCE,
                    "resource reload lost tag highlight");
            require(context.computeOnClient(c -> c.getModelManager().getBlockStateModelSet()) != initialModels,
                    "resource reload fixture did not replace baked models");
            evidence.append("sceneFilterComposition=true\nresourceReload=true\n");

            for (Path image : images) evidence.append(image.getFileName()).append(".sha256=")
                    .append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(image)))).append('\n');
            Files.writeString(output.resolve("metrics.txt"), evidence, StandardCharsets.UTF_8);
            System.out.println("BLOCKLENS_ORE_EXTENSIONS minecraft=" + BlockLensRuntime.minecraftVersion()
                    + " explicit=true tags=true bounds=true restoration=true reload=true filter=true images=" + images.size());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("extension reload interrupted", exception);
        } catch (IOException | java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError("extension evidence failed", exception);
        } finally {
            install(context, original);
        }
    }

    private static List<String> normalTargets() {
        return List.of(OreExtensionFixture.TAGGED, OreExtensionFixture.EXPLICIT, OreExtensionFixture.CONFLICTED,
                OreExtensionFixture.MANY_STATES);
    }

    private static void writeTagPack(Path root, List<String> ids) throws IOException {
        Path tag = root.resolve("data/c/tags/block/ores/diamond.json");
        Files.createDirectories(tag.getParent());
        int format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).major();
        Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\":{\"description\":\"BlockLens tag fixture\",\"min_format\":"
                + format + ",\"max_format\":" + format + "}}", StandardCharsets.UTF_8);
        Files.writeString(tag, "{\"replace\":true,\"values\":[" + String.join(",", ids.stream().map(id -> "\"" + id + "\"").toList())
                + "]}", StandardCharsets.UTF_8);
        Files.writeString(tag.resolveSibling("gold.json"), "{\"replace\":true,\"values\":[\""
                + OreExtensionFixture.EXPLICIT + "\"]}", StandardCharsets.UTF_8);
    }

    private static void reloadTags(ClientGameTestContext context, TestSingleplayerContext world, int expectedModels) {
        int updates = context.computeOnClient(c -> MinecraftOreExtensions.tagUpdateCount());
        var reload = world.getServer().computeOnServer(server -> {
            server.getPackRepository().reload();
            var selected = new ArrayList<>(server.getPackRepository().getSelectedIds());
            String id = "file/" + PACK_NAME;
            require(server.getPackRepository().isAvailable(id), "test data pack was not discovered");
            if (!selected.contains(id)) selected.add(id);
            return server.reloadResources(selected);
        });
        context.waitFor(client -> reload.isDone(), 1200);
        reload.join();
        context.waitFor(client -> MinecraftOreExtensions.tagUpdateCount() > updates && MinecraftOreExtensions.ready()
                && MinecraftOreExtensions.compiledModelCount() == expectedModels, 600);
        context.waitTicks(25);
    }

    private static void place(TestSingleplayerContext world, String id) {
        world.getServer().runCommand("setblock 0 -57 4 " + id);
    }

    private static Block block(String id) { return BuiltInRegistries.BLOCK.getValue(Identifier.tryParse(id)); }

    private static BlockLensConfig allOff(BlockLensConfig config) {
        for (CapabilityId capability : CapabilityId.values()) config = config.withEnabled(capability, false);
        return config;
    }

    private static void install(ClientGameTestContext context, BlockLensConfig config) {
        context.runOnClient(client -> {
            BlockLensRuntime.installConfig(config);
            MinecraftTerrainInvalidator.invalidateAll(client);
        });
        context.waitTicks(30);
    }

    private static BufferedImage capture(ClientGameTestContext context, Path output, String name, List<Path> images) throws IOException {
        context.waitTicks(25);
        context.runOnClient(client -> client.particleEngine.clearParticles());
        Path path = context.takeScreenshot(TestScreenshotOptions.of(name).withSize(640, 360)
                .withDestinationDir(output).disableCounterPrefix());
        images.add(path);
        BufferedImage image = ImageIO.read(path.toFile());
        require(image != null && image.getWidth() == 640 && image.getHeight() == 360, "extension screenshot invalid");
        return image;
    }

    private static int difference(BufferedImage a, BufferedImage b) {
        int count = 0;
        for (int y = 90; y < 310; y++) for (int x = 90; x < 550; x++) {
            if (distance(a.getRGB(x, y), b.getRGB(x, y)) >= 24) count++;
        }
        return count;
    }

    private static int retention(BufferedImage off, BufferedImage on, BufferedImage air) {
        int base = 0;
        int retained = 0;
        for (int y = 90; y < 310; y++) for (int x = 90; x < 550; x++) {
            if (distance(off.getRGB(x, y), air.getRGB(x, y)) >= 24) {
                base++;
                if (distance(on.getRGB(x, y), air.getRGB(x, y)) >= 24) retained++;
            }
        }
        require(base >= MIN_DIFFERENCE, "extension base geometry was not visible");
        return retained * 1000 / base;
    }

    private static int distance(int a, int b) {
        return Math.abs((a >> 16 & 255) - (b >> 16 & 255))
                + Math.abs((a >> 8 & 255) - (b >> 8 & 255)) + Math.abs((a & 255) - (b & 255));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

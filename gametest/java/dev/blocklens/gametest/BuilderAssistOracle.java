package dev.blocklens.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.builder.BuilderState;
import dev.blocklens.fabric.BlockLensConfigScreen;
import dev.blocklens.fabric.BuilderAssistClient;
import dev.blocklens.fabric.BuilderAssistScreen;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.input.KeyEvent;

/** Real-client readonly predictions, bounded references, panel input and optional typed reads. */
final class BuilderAssistOracle {
    private static final BlockPos TARGET = new BlockPos(0, -58, 0);
    private static final BlockPos PLACED = TARGET.south();
    private BuilderAssistOracle() { }

    static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        var original = BlockLensRuntime.config();
        var previous = context.computeOnClient(BlockLensConfigScreen::current);
        var stack = context.computeOnClient(c -> c.player.getMainHandItem().copy());
        String language = context.computeOnClient(c -> c.getLanguageManager().getSelected());
        String optionLanguage = context.computeOnClient(c -> c.options.languageCode);
        int scale = context.computeOnClient(c -> c.options.guiScale().get());
        int width = context.computeOnClient(c -> c.getWindow().getWidth());
        int height = context.computeOnClient(c -> c.getWindow().getHeight());
        var output = FabricLoader.getInstance().getGameDir().resolve("builder-assist");
        StringBuilder evidence = new StringBuilder("minecraft=" + BlockLensRuntime.minecraftVersion() + "\n");
        try {
            Files.createDirectories(output);
            prepare(context, world);
            var enabled = tools(original, true);
            context.runOnClient(c -> {
                BlockLensRuntime.installConfig(enabled);
                BuilderAssistClient.clear();
                c.hitResult = hit(.75, Direction.SOUTH);
                BuilderAssistClient.tick(c);
                require(BuilderAssistClient.snapshot().kind() == 1, "block crosshair unavailable");
                require(BuilderAssistClient.snapshot().target().id().equals("minecraft:oak_stairs"), "wrong block read model");
                require(BuilderAssistClient.snapshot().target().properties().get("facing").equals("north"), "state property lost");
                c.hitResult = new EntityHitResult(c.player);
                BuilderAssistClient.tick(c);
                require(BuilderAssistClient.snapshot().kind() == 2
                        && BuilderAssistClient.snapshot().target().id().equals("minecraft:player")
                        && BuilderAssistClient.snapshot().target().properties().isEmpty(), "entity read model contains extra data");
                c.hitResult = BlockHitResult.miss(Vec3.ZERO, Direction.UP, TARGET);
                BuilderAssistClient.tick(c);
                require(BuilderAssistClient.snapshot().kind() == 0, "MISS retained crosshair state");
                predictions(c);
                independentTools(c, enabled);
            });
            evidence.append("blockAndEntityInspector=true\nmissCleared=true\nplacementFamilies=10\nplacementStateInputs=true\nreadonlyPrediction=true\nindependentTools=true\ncoordinateBounds=true\n");

            world.getServer().runCommand("setblock 0 -58 1 minecraft:water[level=0]");
            context.waitFor(c -> c.level.getBlockState(PLACED).is(Blocks.WATER), 600);
            context.runOnClient(c -> {
                c.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.OAK_TRAPDOOR));
                var prediction = BuilderAssistClient.predict(c, hit(.75, Direction.SOUTH));
                require(prediction != null && prediction.state().getValue(BlockStateProperties.WATERLOGGED), "waterlogged preview differs");
                require(c.level.getBlockState(PLACED).is(Blocks.WATER), "preview changed local fluid");
            });
            world.getServer().runCommand("setblock 0 -58 1 minecraft:stone");
            context.waitFor(c -> c.level.getBlockState(PLACED).is(Blocks.STONE), 600);
            context.runOnClient(c -> {
                var prediction = BuilderAssistClient.predict(c, hit(.75, Direction.SOUTH));
                require(prediction == null || prediction.state() == null, "occupied placement position predicted as free");
            });
            world.getServer().runCommand("setblock 0 -58 1 minecraft:air");
            context.waitFor(c -> c.level.getBlockState(PLACED).isAir(), 600);
            evidence.append("waterlogged=true\noccupiedUnavailable=true\n");

            var allEnabled = enabled;
            for (var capability : CapabilityId.values()) allEnabled = allEnabled.withEnabled(capability, true);
            var coexistence = allEnabled;
            context.runOnClient(c -> BlockLensRuntime.installConfig(coexistence));
            context.waitTicks(5);
            context.runOnClient(c -> {
                c.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.OAK_STAIRS));
                c.hitResult = hit(.75, Direction.SOUTH);
                require(BuilderAssistClient.selectReference(c), "explicit reference capture failed");
                for (int i = 0; i < 11; i++) {
                    BuilderAssistClient.tick(c);
                    require(BuilderAssistClient.patternOperations() <= 256, "native pattern work exceeded bound");
                }
                var pattern = BuilderAssistClient.pattern();
                require(pattern.complete() && pattern.compared() == 2 && pattern.matches() == 1
                        && pattern.mismatches() == 1, "controlled pattern counts differ");
                require(pattern.propertyMismatches().get("facing") == 1, "direction mismatch missing");
                require(!FabricLoader.getInstance().isModLoaded("litematica"), "base oracle must prove absent optional integration");
                require(BuilderAssistClient.schematicState(PLACED) == null, "absent Litematica produced state");
                require(BuilderAssistClient.snapshot().target() != null && BuilderAssistClient.snapshot().prediction() != null,
                        "combined capabilities suppressed native builder output");
                BlockLensRuntime.installConfig(enabled);
            });
            evidence.append("patternDomain=2601\npatternBudget=256\npatternCounts=true\noptionalAbsentSafe=true\nallEnabledCoexistence=true\n");

            context.runOnClient(c -> { c.options.guiScale().set(1); c.getWindow().setWindowed(320, 240); });
            context.waitFor(c -> c.getWindow().getWidth() == 320 && c.getWindow().getHeight() == 240);
            context.runOnClient(c -> c.resizeGui());
            for (String locale : new String[] {"en_us", "ja_jp"}) {
                context.runOnClient(c -> { c.getLanguageManager().setSelected(locale); c.options.languageCode = locale; });
                reload(context);
                context.runOnClient(c -> {
                    c.hitResult = hit(.75, Direction.SOUTH);
                    BuilderAssistClient.tick(c);
                    BlockLensConfigScreen.show(c, new BuilderAssistScreen(previous, s -> BlockLensConfigScreen.show(c, s)));
                    var screen = (BuilderAssistScreen) BlockLensConfigScreen.current(c);
                    for (var button : ResponsiveSettingsScreenOracle.buttons(screen)) require(button.getX() >= 0
                            && button.getRight() <= 320 && button.getY() >= 0 && button.getBottom() <= 240,
                            "builder control outside compact viewport");
                    press(screen, "blocklens.builder.select");
                    require(BuilderAssistClient.pattern().reference() != null, "reference button did not capture");
                });
                context.waitTicks(15);
                capture(context, output, "builder-" + locale + "-top");
                context.runOnClient(c -> BlockLensConfigScreen.current(c).keyPressed(new KeyEvent(InputConstants.KEY_END, 0, 0)));
                context.waitTicks(2);
                capture(context, output, "builder-" + locale + "-bottom");
                context.runOnClient(c -> {
                    var screen = (BuilderAssistScreen) BlockLensConfigScreen.current(c);
                    press(screen, "blocklens.builder.clear");
                    require(BuilderAssistClient.pattern().reference() == null, "clear button retained reference");
                    screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                    require(BlockLensConfigScreen.current(c) == previous, "builder Esc did not return to parent");
                    require(BlockLensRuntime.config().equals(enabled), "inspection changed saved config");
                });
            }
            evidence.append("panelLocales=en_us,ja_jp\ncompactControls=true\nreferenceButtons=true\npanelConfigReadonly=true\n");
            context.runOnClient(c -> {
                c.hitResult = hit(.75, Direction.SOUTH);
                require(BuilderAssistClient.selectReference(c), "reference preparation failed");
            });
            world.getServer().runCommand("setblock 0 -58 0 minecraft:oak_stairs[facing=east,half=bottom,shape=straight]");
            context.waitFor(c -> c.level.getBlockState(TARGET).getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST, 600);
            context.runOnClient(c -> { BuilderAssistClient.tick(c); require(BuilderAssistClient.pattern().reference() == null,
                    "changed reference retained results"); });
            context.runOnClient(c -> { c.hitResult = hit(.75, Direction.SOUTH); BuilderAssistClient.selectReference(c); });
            reload(context);
            require(context.computeOnClient(c -> BuilderAssistClient.pattern().reference() == null), "reload retained reference");
            context.runOnClient(c -> { c.hitResult = hit(.75, Direction.SOUTH); BuilderAssistClient.selectReference(c); });
            world.getServer().runCommand("execute in minecraft:the_nether run tp @a 0 80 0");
            context.waitFor(c -> c.level != null && c.level.dimension().equals(Level.NETHER), 1200);
            require(context.computeOnClient(c -> BuilderAssistClient.pattern().reference() == null), "dimension retained reference");
            world.getServer().runCommand("execute in minecraft:overworld run tp @a 0.5 -58 5.5 180 15");
            context.waitFor(c -> c.level != null && c.level.dimension().equals(Level.OVERWORLD), 1200);
            context.runOnClient(c -> {
                BlockLensRuntime.installConfig(tools(original, false)); BuilderAssistClient.tick(c);
                require(BuilderAssistClient.snapshot().kind() == 0 && BuilderAssistClient.pattern().reference() == null
                        && BuilderAssistClient.patternOperations() == 0, "OFF retained builder state or work");
            });
            evidence.append("referenceReplacement=true\nreloadCleanup=true\ndimensionCleanup=true\nfinalOffEmpty=true\n");
            Files.writeString(output.resolve("builder-assist-manifest.txt"), evidence, StandardCharsets.UTF_8);
            System.out.println("BLOCKLENS_BUILDER inspector=true placement=true pattern=true readonly=true optionalAbsent=true ui=true lifecycle=true");
        } catch (IOException exception) { throw new AssertionError("builder evidence failed", exception); }
        finally {
            context.runOnClient(c -> {
                BuilderAssistClient.clear(); BlockLensRuntime.installConfig(original);
                if (c.player != null) c.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                c.getLanguageManager().setSelected(language); c.options.languageCode = optionLanguage;
                c.options.guiScale().set(scale); c.getWindow().setWindowed(width, height);
                c.resizeGui(); BlockLensConfigScreen.show(c, previous);
            });
            context.waitFor(c -> c.getWindow().getWidth() == width && c.getWindow().getHeight() == height);
            reload(context);
        }
    }

    private static void predictions(net.minecraft.client.Minecraft client) {
        String[][] cases = {{"oak_trapdoor", "half", "top"}, {"stone_slab", "type", "top"},
                {"oak_stairs", "half", "top"}, {"white_glazed_terracotta", "facing", "south"},
                {"oak_fence_gate", "facing", "north"}, {"grindstone", "face", "wall"},
                {"beehive", "facing", "south"}, {"campfire", "lit", "true"},
                {"oak_log", "axis", "z"}, {"ochre_froglight", "axis", "z"}};
        var clicked = client.level.getBlockState(TARGET);
        var empty = client.level.getBlockState(PLACED);
        client.player.setYRot(180);
        for (var row : cases) {
            var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse("minecraft:" + row[0]));
            var held = new ItemStack(block, 7);
            client.player.setItemInHand(InteractionHand.MAIN_HAND, held);
            var preview = BuilderAssistClient.predict(client, hit(.75, Direction.SOUTH));
            require(preview != null && preview.state() != null, "supported placement family unavailable: " + row[0]);
            require(preview.position().equals(PLACED), "preview position differs: " + row[0]);
            require(BuilderAssistClient.describe(preview.state()).properties().get(row[1]).equals(row[2]), "preview state differs: " + row[0]);
            require(client.level.getBlockState(TARGET) == clicked && client.level.getBlockState(PLACED) == empty
                    && client.player.getMainHandItem() == held && held.getCount() == 7, "prediction mutated world or inventory");
        }
        var trapdoor = new ItemStack(Blocks.OAK_TRAPDOOR);
        client.player.setItemInHand(InteractionHand.MAIN_HAND, trapdoor);
        require(BuilderAssistClient.describe(BuilderAssistClient.predict(client, hit(.25, Direction.SOUTH)).state())
                .properties().get("half").equals("bottom"), "lower click was lost");
        trapdoor.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("facing", "west")));
        require(BuilderAssistClient.describe(BuilderAssistClient.predict(client, hit(.75, Direction.SOUTH)).state())
                .properties().get("facing").equals("west"), "held block-state component was lost");
        client.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.OAK_LOG));
        require(BuilderAssistClient.describe(BuilderAssistClient.predict(client, hit(.75, Direction.UP)).state())
                .properties().get("axis").equals("y"), "clicked face axis was lost");
        var far = new BlockPos(1_000_000, -58, 1_000_000);
        int chunks = client.level.getChunkSource().getLoadedChunksCount();
        require(BuilderAssistClient.predict(client, new BlockHitResult(Vec3.atCenterOf(far), Direction.UP, far, false)) == null,
                "unloaded placement target produced a prediction");
        require(client.level.getChunkSource().getLoadedChunksCount() == chunks, "preview loaded a missing chunk");
        for (var extreme : new BlockPos[] {new BlockPos(Integer.MAX_VALUE, -58, 0),
                new BlockPos(0, -58, Integer.MIN_VALUE)}) {
            require(BuilderAssistClient.predict(client,
                    new BlockHitResult(Vec3.atCenterOf(extreme), Direction.UP, extreme, false)) == null,
                    "placement neighborhood overflow bypassed the loaded-data guard");
        }
        require(client.level.getChunkSource().getLoadedChunksCount() == chunks, "extreme preview loaded a chunk");
        client.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.STONE));
        require(BuilderAssistClient.predict(client, hit(.75, Direction.SOUTH)) == null, "unsupported held block was predicted");
    }

    private static void independentTools(net.minecraft.client.Minecraft client, BlockLensConfig enabled) {
        client.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.OAK_STAIRS));
        for (var tool : new CapabilityId[] {CapabilityId.BLOCK_INSPECTOR, CapabilityId.PLACEMENT_PREVIEW,
                CapabilityId.PATTERN_CONSISTENCY, CapabilityId.SCHEMATIC_COMPARISON}) {
            BlockLensRuntime.installConfig(tools(enabled, false).withEnabled(tool, true));
            BuilderAssistClient.clear(); client.hitResult = hit(.75, Direction.SOUTH); BuilderAssistClient.tick(client);
            var snapshot = BuilderAssistClient.snapshot();
            require((snapshot.target() != null) == (tool == CapabilityId.BLOCK_INSPECTOR), "inspector toggle was coupled");
            require((snapshot.prediction() != null) == (tool == CapabilityId.PLACEMENT_PREVIEW
                    || tool == CapabilityId.SCHEMATIC_COMPARISON), "prediction inputs were coupled");
            require(BuilderAssistClient.selectReference(client) == (tool == CapabilityId.PATTERN_CONSISTENCY),
                    "reference selection was coupled");
        }
        BuilderAssistClient.clear(); BlockLensRuntime.installConfig(enabled);
    }

    private static void prepare(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runCommand("gamerule minecraft:random_tick_speed 0");
        world.getServer().runCommand("gamemode creative @a");
        world.getServer().runCommand("fill -10 -58 -10 10 -49 10 minecraft:air");
        world.getServer().runCommand("fill -10 -59 -10 10 -59 10 minecraft:stone");
        world.getServer().runCommand("setblock 0 -58 0 minecraft:oak_stairs[facing=north,half=bottom,shape=straight]");
        world.getServer().runCommand("setblock 2 -58 0 minecraft:oak_stairs[facing=north,half=bottom,shape=straight]");
        world.getServer().runCommand("setblock -2 -58 0 minecraft:oak_stairs[facing=south,half=bottom,shape=straight]");
        world.getServer().runCommand("tp @a 0.5 -58 5.5 180 15");
        context.waitFor(c -> c.level != null && c.level.getBlockState(TARGET).is(Blocks.OAK_STAIRS)
                && c.level.getBlockState(TARGET.offset(2, 0, 0)).is(Blocks.OAK_STAIRS)
                && c.level.getBlockState(TARGET.offset(-2, 0, 0)).is(Blocks.OAK_STAIRS), 600);
    }

    private static BlockHitResult hit(double height, Direction face) {
        return new BlockHitResult(new Vec3(.5, TARGET.getY() + height, 1), face, TARGET, false);
    }

    private static BlockLensConfig tools(BlockLensConfig config, boolean enabled) {
        for (var capability : new CapabilityId[] {CapabilityId.BLOCK_INSPECTOR, CapabilityId.PLACEMENT_PREVIEW,
                CapabilityId.PATTERN_CONSISTENCY, CapabilityId.SCHEMATIC_COMPARISON}) config = config.withEnabled(capability, enabled);
        return config;
    }

    private static void press(BuilderAssistScreen screen, String key) {
        String label = Component.translatable(key).getString();
        var button = ResponsiveSettingsScreenOracle.buttons(screen).stream().filter(b -> b.getMessage().getString().equals(label))
                .findFirst().orElseThrow(() -> new AssertionError("builder action missing"));
        require(button.active, "builder action disabled");
        button.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
    }

    private static void reload(ClientGameTestContext context) {
        var future = context.computeOnClient(c -> c.reloadResourcePacks());
        context.waitFor(c -> future.isDone(), 1200); future.join(); context.waitFor(SettingsClientAccess::ready, 1200);
    }

    private static void capture(ClientGameTestContext context, Path output, String name) throws IOException {
        var image = context.takeScreenshot(TestScreenshotOptions.of(name).withDestinationDir(output).disableCounterPrefix());
        var pixels = javax.imageio.ImageIO.read(image.toFile());
        require(Files.size(image) > 1000 && pixels != null && pixels.getWidth() == 320 && pixels.getHeight() == 240,
                "builder framebuffer viewport differs");
    }

    static void verifyLitematica(ClientGameTestContext context, TestSingleplayerContext world) {
        SchematicFixture.verify(context, world);
    }

    static void prepareDisconnect(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runCommand("setblock 0 -58 0 minecraft:stone");
        context.waitFor(c -> c.level != null && c.level.getBlockState(TARGET).is(Blocks.STONE), 600);
        context.runOnClient(c -> {
            BlockLensRuntime.installConfig(tools(BlockLensRuntime.config(), true));
            c.hitResult = hit(.75, Direction.SOUTH);
            require(BuilderAssistClient.selectReference(c), "disconnect requires a live builder reference");
            BuilderAssistClient.tick(c);
            require(BuilderAssistClient.snapshot().kind() == 1 && BuilderAssistClient.pattern().reference() != null,
                    "disconnect builder precondition missing");
        });
    }

    static void verifyDisconnected(ClientGameTestContext context) {
        context.runOnClient(c -> require(BuilderAssistClient.snapshot().kind() == 0
                && BuilderAssistClient.pattern().reference() == null && BuilderAssistClient.patternOperations() == 0,
                "disconnect retained builder state"));
        System.out.println("BLOCKLENS_BUILDER_DISCONNECT cleared=true");
    }

    private static final class SchematicFixture {
        static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
            var original = BlockLensRuntime.config();
            var stack = context.computeOnClient(c -> c.player.getMainHandItem().copy());
            try {
                prepare(context, world);
                context.waitFor(c -> fi.dy.masa.litematica.world.SchematicWorldHandler.getSchematicWorld() != null, 600);
                context.runOnClient(c -> {
                    BlockLensRuntime.installConfig(tools(original, true));
                    require(BuilderAssistClient.schematicSupported(), "pinned optional versions rejected");
                    c.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.OAK_STAIRS));
                    c.hitResult = hit(.75, Direction.SOUTH);
                    var schematic = fi.dy.masa.litematica.world.SchematicWorldHandler.getSchematicWorld();
                    var chunks = schematic.getChunkSource();
                    require(chunks.getChunkIfExists(0, 0) == null, "schematic fixture requires a fresh chunk");
                    int count = chunks.getLoadedChunksCount();
                    require(BuilderAssistClient.schematicState(PLACED) == null && chunks.getLoadedChunksCount() == count,
                            "missing schematic read loaded a chunk");
                    chunks.loadChunk(0, 0);
                    var predicted = BuilderAssistClient.predict(c, hit(.75, Direction.SOUTH)).state();
                    try {
                        schematic.setBlock(PLACED, predicted, 2);
                        BuilderAssistClient.tick(c);
                        require(BuilderState.compare(BuilderAssistClient.snapshot().expected(), BuilderAssistClient.snapshot().prediction())
                                == BuilderState.MATCH, "typed schematic match failed");
                        schematic.setBlock(PLACED, predicted.cycle(BlockStateProperties.HORIZONTAL_FACING), 2);
                        BuilderAssistClient.tick(c);
                        require(BuilderState.compare(BuilderAssistClient.snapshot().expected(), BuilderAssistClient.snapshot().prediction())
                                == BuilderState.ADJUSTED, "typed schematic state difference failed");
                        schematic.setBlock(PLACED, Blocks.STONE.defaultBlockState(), 2);
                        BuilderAssistClient.tick(c);
                        require(BuilderState.compare(BuilderAssistClient.snapshot().expected(), BuilderAssistClient.snapshot().prediction())
                                == BuilderState.DIFFERENT, "typed schematic block difference failed");
                        schematic.setBlock(PLACED, Blocks.AIR.defaultBlockState(), 2);
                        require(BuilderAssistClient.schematicState(PLACED) == null, "schematic air was treated as a requirement");
                        require(c.level.getBlockState(PLACED).isAir() && c.player.getMainHandItem().getCount() == 1,
                                "schematic comparison mutated client world or inventory");
                    } finally { chunks.unloadChunk(0, 0); }
                    require(chunks.getLoadedChunksCount() == count && BuilderAssistClient.schematicState(PLACED) == null,
                            "schematic unload retained state");
                });
                var output = FabricLoader.getInstance().getGameDir().resolve("builder-assist");
                Files.createDirectories(output);
                Files.writeString(output.resolve("builder-schematic-manifest.txt"), "minecraft=" + BlockLensRuntime.minecraftVersion()
                        + "\npinnedVersions=true\ntypedRead=true\nmatch=true\nstateDifference=true\nblockDifference=true\nairUnavailable=true\nmissingChunk=true\nunloadCleanup=true\nreadonly=true\n", StandardCharsets.UTF_8);
                System.out.println("BLOCKLENS_SCHEMATIC typedRead=true match=true adjusted=true different=true missing=true readonly=true");
            } catch (IOException exception) { throw new AssertionError("schematic evidence failed", exception); }
            finally {
                context.runOnClient(c -> { BuilderAssistClient.clear(); BlockLensRuntime.installConfig(original);
                    if (c.player != null) c.player.setItemInHand(InteractionHand.MAIN_HAND, stack); });
            }
        }
    }

    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}

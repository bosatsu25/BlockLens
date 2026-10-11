package dev.blocklens.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.PaleOakPalette;
import dev.blocklens.testing.PaleOakFrameMetrics;
import dev.blocklens.testing.ExternalLifecycleDiagnostics;
import dev.blocklens.fabric.BlockLensConfigScreen;
import dev.blocklens.gametest.mixin.PaletteSpriteAccess;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.data.AtlasIds;
import dev.blocklens.fabric.AbstractBlockLensConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Actual atlas + standalone decode, saved UI, resource priority and rendered-frame regression. */
final class PaleOakPaletteOracle {
    private static final String[] ROLES = {
        "block/pale_oak_door_bottom", "block/pale_oak_door_top", "block/pale_oak_log_top",
        "block/pale_oak_planks", "block/pale_oak_shelf", "block/pale_oak_trapdoor",
        "block/stripped_pale_oak_log", "block/stripped_pale_oak_log_top",
        "entity/boat/pale_oak", "entity/chest_boat/pale_oak", "entity/signs/pale_oak",
        "entity/signs/hanging/pale_oak", "gui/hanging_signs/pale_oak",
        "item/pale_oak_boat", "item/pale_oak_chest_boat", "item/pale_oak_door",
        "item/pale_oak_sign", "item/pale_oak_hanging_sign", "block/pale_oak_sign",
        "block/pale_oak_hanging_sign", "gui/signs/pale_oak"
    };
    private PaleOakPaletteOracle() { }

    static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        var original = BlockLensRuntime.config();
        var directory = FabricLoader.getInstance().getConfigDir();
        var output = FabricLoader.getInstance().getGameDir().resolve("pale-oak-visual");
        var packs = context.computeOnClient(c -> List.copyOf(c.getResourcePackRepository().getSelectedIds()));
        byte[] originalFile;
        try { originalFile = Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME)); }
        catch (IOException failure) { throw new AssertionError("Palette baseline storage unavailable", failure); }
        try {
            Files.createDirectories(output);
            var off = original;
            for (var capability : CapabilityId.values()) off = off.withEnabled(capability, false);
            var offConfig = off;
            context.runOnClient(c -> {
                BlockLensRuntime.installConfig(offConfig);
                try { BlockLensConfigFiles.save(directory, offConfig); }
                catch (IOException failure) { throw new AssertionError("Palette test configuration save failed", failure); }
            });
            reload(context);
            verifyPixels(context, false, false);
            var server = world.getServer();
            server.runCommand("gamerule minecraft:advance_time false");
            server.runCommand("gamerule minecraft:advance_weather false");
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            server.runCommand("gamemode spectator @a");
            server.runCommand("fill -12 -59 -8 12 -45 20 minecraft:air");
            server.runCommand("fill -11 -60 -7 11 -60 19 minecraft:white_concrete");
            server.runCommand("fill -4 -59 5 -2 -57 5 minecraft:pale_oak_planks");
            server.runCommand("fill 0 -59 5 2 -57 5 minecraft:stripped_pale_oak_log");
            server.runCommand("setblock 4 -59 5 minecraft:pale_oak_trapdoor[open=true]");
            server.runCommand("tp @a 0 -53 17 180 18");
            context.waitTicks(30);
            BufferedImage offFrame = capture(context, output, "palette-off");
            verifyUiDiscard(context, directory);
            verifyUiFailedSave(context, directory);
            saveUiToggle(context);
            verifyPixels(context, true, false);
            BufferedImage onFrame = capture(context, output, "palette-on");
            int changed = changed(offFrame, onFrame);
            require(changed > 80 && PaleOakFrameMetrics.visibleChange(offFrame, onFrame),
                    "Palette UI save produced no visible change in both wood roles");
            reload(context);
            verifyPixels(context, true, false);
            var reloaded = capture(context, output, "palette-on-reloaded");
            require(PaleOakFrameMetrics.stable(onFrame, reloaded), "Palette appearance changed across an unchanged reload");
            saveUiToggle(context);
            verifyPixels(context, false, false);
            var restored = capture(context, output, "palette-off-restored");
            require(PaleOakFrameMetrics.stable(offFrame, restored), "Palette disable did not restore ordinary wood");
            var allEnabled = offConfig;
            for (var capability : CapabilityId.values()) allEnabled = allEnabled.withEnabled(capability, true);
            var combined = allEnabled;
            context.runOnClient(c -> BlockLensRuntime.installConfig(combined));
            reload(context);
            verifyPixels(context, true, false);
            capture(context, output, "palette-all-features");
            String external = context.computeOnClient(c -> {
                c.getResourcePackRepository().reload();
                return c.getResourcePackRepository().getAvailableIds().stream()
                        .filter(id -> id.endsWith("palette-external")).findFirst().orElseThrow();
            });
            context.runOnClient(c -> {
                var selected = new ArrayList<>(packs); selected.add(external);
                c.getResourcePackRepository().setSelected(selected);
                BlockLensRuntime.installConfig(offConfig.withEnabled(CapabilityId.PALE_OAK_NEUTRAL, true));
            });
            reload(context);
            verifyPixels(context, true, true);
            capture(context, output, "palette-external-priority");
            // Keep the original controlled wood frames intact before auxiliary scenes mutate the world.
            context.runOnClient(c -> c.getResourcePackRepository().setSelected(packs));
            PaleOakRenderedScenes.verify(context, world, output, offConfig);
            Files.writeString(output.resolve("palette-manifest.txt"),
                    "minecraft=" + BlockLensRuntime.minecraftVersion()
                    + "\nchangedPixels=" + changed
                    + "\nactualAtlas=true\nstandaloneDecode=true\nuiSave=true\nuiDiscard=true\nfailedSave=true\nallFeatures=true"
                    + "\nreload=true\noffRestored=true\nexternalPriority=true\nstableWoodChroma=true\n");
            System.out.println("BLOCKLENS_PALE_OAK atlas=true standalone=true save=true discard=true failedSave=true allFeatures=true reload=true off=true external=true changed=" + changed);
        } catch (IOException failure) {
            throw new AssertionError("Palette evidence failed", failure);
        } finally {
            // Observe only the previously stalled Vulkan restoration, not healthy scene work.
            boolean observe = "vulkan".equals(System.getProperty("blocklens.test.external.profile"));
            try (var diagnostics = ExternalLifecycleDiagnostics.open(output.resolveSibling("external-compatibility"), observe)) {
                diagnostics.phase("palette_restore");
                context.runOnClient(c -> {
                    BlockLensConfigScreen.show(c, null);
                    c.getResourcePackRepository().setSelected(packs);
                    BlockLensRuntime.installConfig(original);
                });
                try { Files.write(directory.resolve(BlockLensConfigFiles.FILE_NAME), originalFile); }
                catch (IOException failure) { throw new AssertionError("Palette baseline restore failed", failure); }
                reload(context);
            }
        }
    }

    private static void verifyUiDiscard(ClientGameTestContext context, Path directory) throws IOException {
        byte[] before = Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME));
        var contents = context.computeOnClient(PaleOakPaletteOracle::planks);
        context.runOnClient(c -> {
            var screen = new BlockLensConfigScreen(null); BlockLensConfigScreen.show(c, screen);
            toggle(screen); screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
            require(!BlockLensRuntime.config().isEnabled(CapabilityId.PALE_OAK_NEUTRAL), "Discard applied palette config");
        });
        context.waitTicks(5);
        require(Arrays.equals(before, Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME))), "Discard persisted palette draft");
        require(context.computeOnClient(PaleOakPaletteOracle::planks) == contents, "Discard reloaded textures");
    }

    static void saveUiToggle(ClientGameTestContext context) {
        var before = context.computeOnClient(PaleOakPaletteOracle::planks);
        context.runOnClient(c -> {
            var screen = new BlockLensConfigScreen(null); BlockLensConfigScreen.show(c, screen);
            toggle(screen);
            String apply = Component.translatable("blocklens.settings.apply").getString();
            ResponsiveSettingsScreenOracle.buttons(screen).stream().filter(b -> b.getMessage().getString().equals(apply))
                    .findFirst().orElseThrow().onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
        });
        context.waitFor(c -> SettingsClientAccess.ready(c) && planks(c) != before, 1200);
        context.waitTicks(20);
    }

    private static void toggle(Screen screen) {
        press(screen, "blocklens.category.outline");
        screen.keyPressed(new KeyEvent(InputConstants.KEY_END, 0, 0));
        var button = ResponsiveSettingsScreenOracle.buttons(screen).get(12);
        require(button.visible, "Palette row not keyboard reachable");
        button.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
    }

    private static void press(Screen screen, String key) {
        String label = Component.translatable(key).getString();
        ResponsiveSettingsScreenOracle.buttons(screen).stream().filter(b -> b.getMessage().getString().equals(label))
                .findFirst().orElseThrow().onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
    }

    private static void verifyUiFailedSave(ClientGameTestContext context, Path directory) throws IOException {
        Path fixture = Files.createTempDirectory(directory, "palette-failure-");
        Path occupied = fixture.resolve("occupied");
        Files.writeString(occupied, "keep");
        var contents = context.computeOnClient(PaleOakPaletteOracle::planks);
        try {
            context.runOnClient(c -> {
                var screen = new FailingScreen(occupied); BlockLensConfigScreen.show(c, screen);
                toggle(screen); press(screen, "blocklens.settings.apply");
                require(BlockLensConfigScreen.current(c) == screen, "Failed palette save closed its draft");
                require(!BlockLensRuntime.config().isEnabled(CapabilityId.PALE_OAK_NEUTRAL), "Failed palette save applied config");
            });
            context.waitTicks(5);
            require(context.computeOnClient(PaleOakPaletteOracle::planks) == contents, "Failed palette save reloaded textures");
            require(Files.readString(occupied).equals("keep"), "Failed palette save overwrote a file");
        } finally {
            context.runOnClient(c -> {
                if (BlockLensConfigScreen.current(c) instanceof FailingScreen screen) screen.onClose();
            });
            Files.deleteIfExists(occupied); Files.deleteIfExists(fixture);
        }
    }

    private static final class FailingScreen extends AbstractBlockLensConfigScreen {
        FailingScreen(Path directory) { super(null, directory); }
        @Override protected void showParent(Screen parent) { BlockLensConfigScreen.show(minecraft, parent); }
    }

    private static SpriteContents planks(Minecraft client) {
        return client.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(Identifier.fromNamespaceAndPath("minecraft", "block/pale_oak_planks")).contents();
    }

    private static void verifyPixels(ClientGameTestContext context, boolean enabled, boolean external) {
        context.runOnClient(c -> {
            int found = 0, changed = 0;
            var atlasRoles = new java.util.HashSet<String>();
            for (String role : ROLES) {
                var id = Identifier.fromNamespaceAndPath("minecraft", "textures/" + role + ".png");
                var resolved = c.getResourceManager().getResource(id);
                if (resolved.isEmpty()) continue; // Explicit old/new sign aliases are version alternatives.
                found++;
                var resource = resolved.get();
                boolean transform = enabled && resource.sourcePackId().equals("vanilla");
                try (var stream = resource.open(); var raw = NativeImage.read(stream);
                        var decoded = TextureContents.load(c.getResourceManager(), id)) {
                    require(raw.getWidth() == decoded.image().getWidth() && raw.getHeight() == decoded.image().getHeight(), "Palette changed image dimensions");
                    for (int y = 0; y < raw.getHeight(); y++) for (int x = 0; x < raw.getWidth(); x++) {
                        int base = raw.getPixel(x, y);
                        int expected = transform ? PaleOakPalette.neutralWood(base) : base;
                        require(decoded.image().getPixel(x, y) == expected, "Standalone texture diverged: " + role);
                        if (expected != base) changed++;
                    }
                    var spriteId = Identifier.fromNamespaceAndPath("minecraft", role);
                    c.getAtlasManager().forEach((atlasId, atlas) -> {
                        var sprite = atlas.getSprite(spriteId).contents();
                        if (!sprite.name().equals(spriteId)) return;
                        atlasRoles.add(role);
                        var actual = ((PaletteSpriteAccess) sprite).blocklens$originalImage();
                        require(actual.getWidth() == raw.getWidth() && actual.getHeight() == raw.getHeight(), "Atlas palette image dimensions changed");
                        for (int y = 0; y < raw.getHeight(); y++) for (int x = 0; x < raw.getWidth(); x++) {
                            int expected = transform ? PaleOakPalette.neutralWood(raw.getPixel(x,y)) : raw.getPixel(x,y);
                            require(sameVisiblePixel(actual.getPixel(x,y), expected), "Retained atlas palette role diverged: " + role);
                        }
                    });
                    if (role.equals("block/pale_oak_planks")) {
                        var actual = ((PaletteSpriteAccess) planks(c)).blocklens$originalImage();
                        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                            int expected = transform ? PaleOakPalette.neutralWood(raw.getPixel(x, y)) : raw.getPixel(x, y);
                            require(sameVisiblePixel(actual.getPixel(x, y), expected), "Real atlas bypassed palette/source priority");
                        }
                        if (external) require(!resource.sourcePackId().equals("vanilla"), "External palette fixture did not win");
                    }
                } catch (IOException failure) { throw new AssertionError("Native palette decode failed", failure); }
            }
            int expected = BlockLensRuntime.minecraftVersion().equals("26.1.2") ? 18 : 19;
            require(atlasRoles.size() >= 13, "Actual block/item atlas coverage incomplete: " + atlasRoles.size());
            require(found == expected, "Pale-oak role coverage drift: " + found);
            if (enabled) require(changed > 100, "Native pale oak warm wood was not neutralized");
            else require(changed == 0, "Disabled palette changed texture pixels");
        });
    }

    static void reload(ClientGameTestContext context) {
        var future = context.computeOnClient(c -> c.reloadResourcePacks());
        context.waitFor(c -> future.isDone(), 1200); future.join();
        context.waitFor(SettingsClientAccess::ready, 1200); context.waitTicks(10);
    }

    static BufferedImage capture(ClientGameTestContext context, Path output, String name) throws IOException {
        Path image = ExternalFrameCapture.required()
                ? ExternalFrameCapture.capture(context, output, name, 640, 360)
                : context.takeScreenshot(TestScreenshotOptions.of(name).withDestinationDir(output)
                        .withSize(640, 360).disableCounterPrefix());
        var pixels = ImageIO.read(image.toFile());
        require(pixels != null && pixels.getWidth() == 640 && pixels.getHeight() == 360, "Palette framebuffer missing");
        return pixels;
    }

    // Vanilla mipmap preparation may fill RGB underneath zero alpha to avoid texture bleeding.
    private static boolean sameVisiblePixel(int actual, int expected) {
        return (actual >>> 24) == (expected >>> 24) && ((expected >>> 24) == 0 || actual == expected);
    }

    private static int changed(BufferedImage a, BufferedImage b) {
        int count = 0;
        for (int y = 180; y < 255; y++) for (int x = 220; x < 410; x++) {
            int p = a.getRGB(x,y), q = b.getRGB(x,y);
            int delta = Math.abs((p >>> 16 & 255)-(q >>> 16 & 255)) + Math.abs((p >>> 8 & 255)-(q >>> 8 & 255)) + Math.abs((p & 255)-(q & 255));
            if (delta >= 15) count++;
        }
        return count;
    }

    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}

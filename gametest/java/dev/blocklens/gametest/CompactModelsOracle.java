package dev.blocklens.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.CompactModelTransforms;
import dev.blocklens.core.render.CompactModelTransforms.Context;
import dev.blocklens.core.ui.SettingsCatalog;
import dev.blocklens.fabric.AbstractBlockLensConfigScreen;
import dev.blocklens.fabric.BlockLensConfigScreen;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;

/** Actual parsed models, both-hand submissions, saved settings, inherited priority and frames. */
final class CompactModelsOracle {
    private static final CapabilityId[] FLAGS = {CapabilityId.COMPACT_BLOCKS, CapabilityId.COMPACT_ITEMS, CapabilityId.COMPACT_TOOLS};
    private CompactModelsOracle() { }
    static void verify(ClientGameTestContext context, TestSingleplayerContext world) {
        var original = BlockLensRuntime.config();
        var directory = FabricLoader.getInstance().getConfigDir();
        var output = FabricLoader.getInstance().getGameDir().resolve("compact-model-visual");
        var packs = context.computeOnClient(c -> List.copyOf(c.getResourcePackRepository().getSelectedIds()));
        var camera = context.computeOnClient(c -> c.options.getCameraType());
        byte[] file;
        try { file = Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME)); }
        catch (IOException failure) { throw new AssertionError("Compact baseline storage unavailable", failure); }
        try {
            Files.createDirectories(output);
            var off = original;
            for (var id : CapabilityId.values()) off = off.withEnabled(id, false);
            var baseline = off;
            install(context, baseline);
            reload(context);
            var models = CompactModelProbe.snapshot();
            int expected = BlockLensRuntime.minecraftVersion().equals("26.1.2") ? 40 : 41;
            require(models.size() == expected, "Compact native model roles do not match the version");
            verifyDiscardAndFailure(context, directory);
            var server = world.getServer();
            server.runCommand("gamerule minecraft:advance_time false");
            server.runCommand("gamerule minecraft:advance_weather false");
            server.runCommand("time set noon"); server.runCommand("weather clear");
            server.runCommand("fill -12 -59 -8 12 -45 20 minecraft:air");
            server.runCommand("fill -11 -60 -7 11 -60 19 minecraft:black_concrete");
            server.runCommand("gamemode creative @a");
            server.runCommand("tp @a 0 -58 10 180 0");
            context.runOnClient(c -> c.options.setCameraType(CameraType.FIRST_PERSON));
            String[] items = {"white_concrete", "apple", "shield", "white_bed", "white_banner", "trident"};
            String[] roles = {"block/block", "item/generated", "item/shield", "item/template_bed", "item/template_banner", "item/trident_in_hand"};
            int[] groups = {0, 1, 2, 0, 1, 2};
            int changedTotal = 0;
            for (int i = 0; i < items.length; i++) {
                install(context, baseline); reload(context);
                server.runCommand("item replace entity @a weapon.mainhand with minecraft:" + items[i]);
                server.runCommand("item replace entity @a weapon.offhand with minecraft:" + items[i]);
                context.waitTicks(24);
                CompactDisplayProbe.captureHands();
                context.waitFor(c -> CompactDisplayProbe.left && CompactDisplayProbe.right, 120);
                var originalLeft = CompactDisplayProbe.observedLeft;
                var originalRight = CompactDisplayProbe.observedRight;
                CompactDisplayProbe.active = false;
                var before = capture(context, output, items[i] + "-off");
                saveToggle(context, FLAGS[groups[i]]);
                verifyParsed(context, baseline.withEnabled(FLAGS[groups[i]], true), models, false);
                String role = roles[i];
                if (role.equals("item/template_bed") && !models.containsKey(role)) role = "block/template_bed_head";
                observeHands(context, role);
                var after = capture(context, output, items[i] + "-on");
                int changed = difference(before, after);
                require(changed > 30, "Compact saved control produced no held-frame change");
                changedTotal += changed;
                CompactDisplayProbe.reset(originalLeft, originalRight);
                context.waitTicks(4);
                require(!CompactDisplayProbe.left && !CompactDisplayProbe.right, "Enabled compact poses falsely match OFF restoration");
                CompactDisplayProbe.active = false;
                if (i < 2) verifyDisplayContexts(context, output, items[i], role);
                if (i == 2 || i == 5) {
                    context.runOnClient(c -> { c.options.keyUse.setDown(true); c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND); });
                    context.waitTicks(24);
                    var state = CompactModelTransforms.find("minecraft", i == 2 ? "item/shield_blocking" : "item/trident_throwing", "vanilla", false, false, true);
                    CompactDisplayProbe.reset(null, state.get(Context.FIRST_RIGHT));
                    context.waitFor(c -> CompactDisplayProbe.right, 120);
                    capture(context, output, items[i] + "-using");
                    CompactDisplayProbe.active = false;
                    context.runOnClient(c -> { c.options.keyUse.setDown(false); c.gameMode.releaseUsingItem(c.player); });
                }
                saveToggle(context, FLAGS[groups[i]]);
                require(CompactModelProbe.snapshot().equals(models), "Compact disable did not restore parsed model poses");
                CompactDisplayProbe.reset(originalLeft, originalRight);
                context.waitFor(c -> CompactDisplayProbe.left && CompactDisplayProbe.right, 120);
                CompactDisplayProbe.active = false;
                capture(context, output, items[i] + "-restored");
            }
            var all = baseline;
            for (var id : CapabilityId.values()) all = all.withEnabled(id, true);
            var combined = all;
            install(context, combined); reload(context);
            verifyParsed(context, combined, models, false);
            capture(context, output, "all-features");
            reload(context); verifyParsed(context, combined, models, false);
            capture(context, output, "all-features-reloaded");
            String external = context.computeOnClient(c -> {
                c.getResourcePackRepository().reload();
                return c.getResourcePackRepository().getAvailableIds().stream().filter(id -> id.endsWith("compact-external")).findFirst().orElseThrow();
            });
            context.runOnClient(c -> { var chosen = new ArrayList<>(packs); chosen.add(external); c.getResourcePackRepository().setSelected(chosen); });
            reload(context); verifyParsed(context, combined, models, true);
            capture(context, output, "external-priority");
            server.runCommand("item replace entity @a weapon.mainhand with minecraft:air");
            server.runCommand("item replace entity @a weapon.offhand with minecraft:air");
            server.runCommand("summon minecraft:item 0 -58 8 {Item:{id:\"minecraft:blaze_rod\",count:1},PickupDelay:32767}");
            CompactDisplayProbe.resetFor(ItemDisplayContext.GROUND, null, new CompactModelTransforms.Transform(0, 0, 0, 0, 0, 0, .73f, .73f, .73f));
            context.waitFor(c -> CompactDisplayProbe.right, 120);
            CompactDisplayProbe.active = false;
            capture(context, output, "inherited-external-ground");
            Files.writeString(output.resolve("compact-manifest.txt"), "minecraft=" + BlockLensRuntime.minecraftVersion()
                    + "\nmodelRoles=" + expected + "\nchangedPixels=" + changedTotal
                    + "\nactualModelParser=true\nbothHands=true\nthirdPerson=true\ngui=true\ninheritedExternal=true\nindependentGroups=true\nstateExceptions=true"
                    + "\nuiSave=true\nuiDiscard=true\nfailedSave=true\nallFeatures=true\nreload=true\noffRestored=true\nexternalPriority=true\n");
            System.out.println("BLOCKLENS_COMPACT nativeModels=" + expected + " bothHands=true states=true groups=true save=true discard=true failure=true reload=true off=true external=true");
        } catch (IOException failure) { throw new AssertionError("Compact visual evidence failed", failure); }
        finally {
            CompactDisplayProbe.active = false; CompactModelProbe.active = false;
            context.runOnClient(c -> { BlockLensConfigScreen.show(c, null); c.options.setCameraType(camera); c.options.keyUse.setDown(false); c.player.stopUsingItem(); c.getResourcePackRepository().setSelected(packs); BlockLensRuntime.installConfig(original); });
            try { Files.write(directory.resolve(BlockLensConfigFiles.FILE_NAME), file); }
            catch (IOException failure) { throw new AssertionError("Compact baseline restoration failed", failure); }
            reload(context); CompactModelProbe.stop();
        }
    }
    private static void install(ClientGameTestContext context, BlockLensConfig config) {
        context.runOnClient(c -> { BlockLensRuntime.installConfig(config);
            try { BlockLensConfigFiles.save(FabricLoader.getInstance().getConfigDir(), config); }
            catch (IOException failure) { throw new AssertionError("Compact fixture save failed", failure); }
        });
    }
    private static void reload(ClientGameTestContext context) {
        context.runOnClient(c -> CompactModelProbe.reset());
        var future = context.computeOnClient(c -> c.reloadResourcePacks());
        context.waitFor(c -> future.isDone(), 1200); future.join(); context.waitFor(SettingsClientAccess::ready, 1200); context.waitTicks(12);
    }
    private static void observeHands(ClientGameTestContext context, String role) {
        var config = BlockLensRuntime.config();
        var profile = CompactModelTransforms.find("minecraft", role, "vanilla", config.isEnabled(FLAGS[0]), config.isEnabled(FLAGS[1]), config.isEnabled(FLAGS[2]));
        require(profile != null, "Requested compact hand role unavailable");
        CompactDisplayProbe.reset(profile.get(Context.FIRST_LEFT), profile.get(Context.FIRST_RIGHT));
        context.waitFor(c -> CompactDisplayProbe.left && CompactDisplayProbe.right, 120);
        CompactDisplayProbe.active = false;
    }
    private static void verifyDisplayContexts(ClientGameTestContext context, Path output, String item, String role) throws IOException {
        var config = BlockLensRuntime.config();
        var profile = CompactModelTransforms.find("minecraft", role, "vanilla", config.isEnabled(FLAGS[0]), config.isEnabled(FLAGS[1]), config.isEnabled(FLAGS[2]));
        context.runOnClient(c -> c.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        CompactDisplayProbe.resetFor(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, profile.get(Context.THIRD_LEFT), profile.get(Context.THIRD_RIGHT));
        context.waitFor(c -> CompactDisplayProbe.left && CompactDisplayProbe.right, 120);
        CompactDisplayProbe.active = false;
        capture(context, output, item + "-third-person");
        context.runOnClient(c -> BlockLensConfigScreen.show(c, new InventoryScreen(c.player)));
        var gui = profile.get(Context.GUI);
        if (gui == null) {
            var inherited = CompactModelProbe.snapshot().get(role).transforms().gui();
            gui = new CompactModelTransforms.Transform(inherited.rotation().x(), inherited.rotation().y(), inherited.rotation().z(),
                    inherited.translation().x(), inherited.translation().y(), inherited.translation().z(),
                    inherited.scale().x(), inherited.scale().y(), inherited.scale().z());
        }
        CompactDisplayProbe.resetFor(ItemDisplayContext.GUI, null, gui);
        context.waitFor(c -> CompactDisplayProbe.right, 120);
        CompactDisplayProbe.active = false;
        capture(context, output, item + "-gui");
        context.runOnClient(c -> { BlockLensConfigScreen.show(c, null); c.options.setCameraType(CameraType.FIRST_PERSON); });
        context.waitTicks(4);
    }
    private static void verifyParsed(ClientGameTestContext context, BlockLensConfig config,
            Map<String, CompactModelProbe.Sample> baseline, boolean external) {
        var parsed = CompactModelProbe.snapshot(); require(parsed.keySet().equals(baseline.keySet()), "Compact model set changed");
        for (var entry : parsed.entrySet()) {
            String role = entry.getKey(); var sample = entry.getValue();
            if (external && role.equals("item/generated")) {
                require(!sample.owner().equals("vanilla") && Math.abs(sample.transforms().firstPersonRightHand().scale().x() - .83f) < .00001f
                        && Math.abs(sample.transforms().ground().scale().x() - .73f) < .00001f, "External model transforms were overridden");
                continue;
            }
            var selected = CompactModelTransforms.find("minecraft", role, sample.owner(), config.isEnabled(FLAGS[0]), config.isEnabled(FLAGS[1]), config.isEnabled(FLAGS[2]));
            if (selected == null) { require(sample.equals(baseline.get(role)), "A disabled compact group changed a parsed model"); continue; }
            for (var expected : selected.entrySet()) {
                var actual = sample.transforms().getTransform(display(expected.getKey())); var value = expected.getValue();
                require(close(actual.rotation().x(), value.rx()) && close(actual.rotation().y(), value.ry()) && close(actual.rotation().z(), value.rz())
                        && close(actual.translation().x(), value.tx()) && close(actual.translation().y(), value.ty()) && close(actual.translation().z(), value.tz())
                        && close(actual.scale().x(), value.sx()) && close(actual.scale().y(), value.sy()) && close(actual.scale().z(), value.sz()), "Parsed compact pose differs from bounded policy");
            }
        }
    }
    private static ItemDisplayContext display(Context context) {
        return switch (context) {
            case THIRD_LEFT -> ItemDisplayContext.THIRD_PERSON_LEFT_HAND; case THIRD_RIGHT -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            case FIRST_LEFT -> ItemDisplayContext.FIRST_PERSON_LEFT_HAND; case FIRST_RIGHT -> ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
            case HEAD -> ItemDisplayContext.HEAD; case GUI -> ItemDisplayContext.GUI; case GROUND -> ItemDisplayContext.GROUND;
            case FIXED -> ItemDisplayContext.FIXED; case SHELF -> ItemDisplayContext.ON_SHELF;
        };
    }
    private static void saveToggle(ClientGameTestContext context, CapabilityId flag) {
        var before = CompactModelProbe.snapshot();
        context.runOnClient(c -> { var screen = new BlockLensConfigScreen(null); BlockLensConfigScreen.show(c, screen); toggle(screen, flag); CompactModelProbe.reset(); press(screen, "blocklens.settings.apply"); });
        context.waitFor(SettingsClientAccess::ready, 1200); context.waitTicks(20);
        var after = CompactModelProbe.snapshot();
        System.out.println("BLOCKLENS_COMPACT_SAVE enabled=" + BlockLensRuntime.config().isEnabled(flag)
                + " rolesBefore=" + before.size() + " rolesAfter=" + after.size() + " changed=" + !after.equals(before));
        require(after.size() == before.size() && !after.equals(before), "Compact save did not replace parsed poses");
    }
    private static void toggle(Screen screen, CapabilityId flag) {
        press(screen, "blocklens.category.other");
        int index = SettingsCatalog.capabilities(CapabilityId.Category.OTHER).indexOf(flag);
        var button = ResponsiveSettingsScreenOracle.buttons(screen).get(4 + index);
        int attempts = ResponsiveSettingsScreenOracle.buttons(screen).size() * 2;
        while (screen.getFocused() != button && attempts-- > 0) {
            screen.keyPressed(new KeyEvent(InputConstants.KEY_TAB, 0, 0));
        }
        require(screen.getFocused() == button && button.visible, "Compact row not keyboard reachable");
        screen.keyPressed(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
    }
    private static void press(Screen screen, String key) {
        String label = Component.translatable(key).getString();
        ResponsiveSettingsScreenOracle.buttons(screen).stream().filter(b -> b.getMessage().getString().equals(label)).findFirst().orElseThrow().onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
    }
    private static void verifyDiscardAndFailure(ClientGameTestContext context, Path directory) throws IOException {
        byte[] before = Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME)); var models = CompactModelProbe.snapshot();
        context.runOnClient(c -> { var screen = new BlockLensConfigScreen(null); BlockLensConfigScreen.show(c, screen); toggle(screen, FLAGS[0]); screen.onClose(); });
        require(Arrays.equals(before, Files.readAllBytes(directory.resolve(BlockLensConfigFiles.FILE_NAME))) && CompactModelProbe.snapshot().equals(models)
                && !BlockLensRuntime.config().isEnabled(FLAGS[0]), "Compact discard changed active state");
        Path fixture = Files.createTempDirectory(directory, "compact-failure-"); Path occupied = fixture.resolve("occupied"); Files.writeString(occupied, "keep");
        try {
            context.runOnClient(c -> { var screen = new FailingScreen(occupied); BlockLensConfigScreen.show(c, screen); toggle(screen, FLAGS[1]); press(screen, "blocklens.settings.apply");
                require(BlockLensConfigScreen.current(c) == screen && !BlockLensRuntime.config().isEnabled(FLAGS[1]), "Failed compact save applied or discarded state"); });
            context.waitTicks(5);
            require(Files.readString(occupied).equals("keep") && CompactModelProbe.snapshot().equals(models), "Failed compact save changed models or file");
        } finally { context.runOnClient(c -> BlockLensConfigScreen.show(c, null)); Files.deleteIfExists(occupied); Files.deleteIfExists(fixture); }
    }
    private static final class FailingScreen extends AbstractBlockLensConfigScreen {
        FailingScreen(Path directory) { super(null, directory); }
        @Override protected void showParent(Screen parent) { BlockLensConfigScreen.show(minecraft, parent); }
    }
    private static BufferedImage capture(ClientGameTestContext context, Path output, String name) throws IOException {
        var image = ExternalFrameCapture.required() ? ExternalFrameCapture.capture(context, output, name, 640, 360)
                : context.takeScreenshot(TestScreenshotOptions.of(name).withDestinationDir(output).withSize(640, 360).disableCounterPrefix());
        var decoded = ImageIO.read(image.toFile()); require(decoded != null && decoded.getWidth() == 640 && decoded.getHeight() == 360, "Compact frame missing"); return decoded;
    }
    private static int difference(BufferedImage before, BufferedImage after) {
        int changed = 0;
        for (int y = 80; y < 360; y++) for (int x = 0; x < 640; x++) {
            int a = before.getRGB(x, y), b = after.getRGB(x, y);
            if (Math.abs((a >>> 16 & 255) - (b >>> 16 & 255)) + Math.abs((a >>> 8 & 255) - (b >>> 8 & 255)) + Math.abs((a & 255) - (b & 255)) >= 30) changed++;
        }
        return changed;
    }
    private static boolean close(float a, float b) { return Math.abs(a - b) < .00001f; }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}

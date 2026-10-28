package dev.blocklens.gametest;

import com.mojang.blaze3d.platform.InputConstants;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.AbstractBlockLensConfigScreen;
import dev.blocklens.fabric.BlockLensConfigScreen;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Real UI actions, persistence and failure recovery; restores the original config afterwards. */
final class SettingsEditingOracle {
    private SettingsEditingOracle() {
    }

    static void verify(ClientGameTestContext context) {
        verifyResourceShortcuts(context);
        context.runOnClient(client -> {
            Screen previous = BlockLensConfigScreen.current(client);
            BlockLensConfig original = BlockLensRuntime.config();
            Path configDirectory = FabricLoader.getInstance().getConfigDir();
            Path fixture = null;
            try {
                BlockLensConfigScreen screen = new BlockLensConfigScreen(previous);
                BlockLensConfigScreen.show(client, screen);
                pressFirstToggle(screen);
                screen.resize(640, 360);
                require(firstToggle(screen).getMessage().getString().equals(state(!original.isEnabled(CapabilityId.ANVIL))),
                        "Resize lost a draft edit");
                ResponsiveSettingsScreenOracle.select(screen, CapabilityId.Category.RESOURCE);
                pressFirstToggle(screen);
                ResponsiveSettingsScreenOracle.select(screen, CapabilityId.Category.DECORATION);
                require(firstToggle(screen).getMessage().getString().equals(state(!original.isEnabled(CapabilityId.ANVIL))),
                        "Category change lost a draft edit");
                require(BlockLensRuntime.config().sameCapabilities(original), "Draft applied too early");
                press(screen, "blocklens.settings.discard");
                require(BlockLensConfigScreen.current(client) == previous, "Discard did not return to parent");
                require(BlockLensConfigFiles.loadOrCreate(configDirectory).sameCapabilities(original),
                        "Discard persisted edits");

                // Ore-source shortcut modifies the draft only and excludes non-ore obsidian toggles.
                screen = new BlockLensConfigScreen(previous);
                BlockLensConfigScreen.show(client, screen);
                ResponsiveSettingsScreenOracle.select(screen, CapabilityId.Category.RESOURCE);
                press(screen, "blocklens.resource.preset.glow");
                String enabledLabel=state(true);
                long enabledOreRows=ResponsiveSettingsScreenOracle.buttons(screen).subList(4,25).stream()
                        .filter(button->button.getMessage().getString().equals(enabledLabel)).count();
                require(enabledOreRows==19,"glow preset did not activate exactly nineteen ores");
                require(BlockLensRuntime.config().sameCapabilities(original),
                        "ore shortcut published its draft before Save");
                press(screen, "blocklens.settings.discard");
                require(BlockLensRuntime.config().sameCapabilities(original),
                        "ore shortcut discard changed the published config");

                screen = new BlockLensConfigScreen(previous);
                BlockLensConfigScreen.show(client, screen);
                pressFirstToggle(screen);
                screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                require(BlockLensConfigScreen.current(client) == previous, "Esc did not return to parent");
                require(BlockLensRuntime.config().sameCapabilities(original), "Esc applied edits");

                screen = new BlockLensConfigScreen(previous);
                BlockLensConfigScreen.show(client, screen);
                pressFirstToggle(screen);
                ResponsiveSettingsScreenOracle.select(screen, CapabilityId.Category.RESOURCE);
                pressFirstToggle(screen);
                BlockLensConfig expected = original.withEnabled(CapabilityId.ANVIL, !original.isEnabled(CapabilityId.ANVIL))
                        .withEnabled(CapabilityId.OBSIDIAN, !original.isEnabled(CapabilityId.OBSIDIAN));
                press(screen, "blocklens.settings.close");
                require(BlockLensConfigScreen.current(client) == previous, "Save did not return to parent");
                require(BlockLensRuntime.config().sameCapabilities(expected), "Save lost category edits");
                require(BlockLensConfigFiles.loadOrCreate(configDirectory).sameCapabilities(expected),
                        "Persisted edits differ from the runtime");

                screen = new BlockLensConfigScreen(previous);
                BlockLensConfigScreen.show(client, screen);
                pressFirstToggle(screen);
                press(screen, "blocklens.settings.apply");
                require(BlockLensConfigScreen.current(client) == screen, "Apply returned to the parent");
                BlockLensConfig applied = expected.withEnabled(CapabilityId.ANVIL, original.isEnabled(CapabilityId.ANVIL));
                require(BlockLensRuntime.config().equals(applied), "Apply did not install the snapshot");
                require(BlockLensConfigFiles.loadOrCreate(configDirectory).equals(applied), "Apply did not persist");
                Path savedPath = configDirectory.resolve(BlockLensConfigFiles.FILE_NAME);
                Files.setLastModifiedTime(savedPath, java.nio.file.attribute.FileTime.fromMillis(1234000));
                var savedTime = Files.getLastModifiedTime(savedPath);
                press(screen, "blocklens.settings.apply");
                require(BlockLensConfigScreen.current(client) == screen, "Unchanged Apply closed the screen");
                require(Files.getLastModifiedTime(savedPath).equals(savedTime), "Unchanged Apply rewrote settings");
                pressFirstToggle(screen);
                screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                require(BlockLensConfigScreen.current(client) == previous, "Esc after Apply did not return");
                require(BlockLensRuntime.config().equals(applied), "Esc undid applied edits");
                require(BlockLensConfigFiles.loadOrCreate(configDirectory).equals(applied), "Esc saved later edits");

                BlockLensConfigFiles.save(configDirectory, original);
                BlockLensRuntime.installConfig(original);
                fixture = Files.createTempDirectory(configDirectory, "blocklens-settings-test-");
                Path occupied = fixture.resolve("occupied");
                Files.writeString(occupied, "keep");
                Screen failing = new FixtureScreen(previous, occupied);
                BlockLensConfigScreen.show(client, failing);
                pressFirstToggle(failing);
                press(failing, "blocklens.settings.apply");
                require(BlockLensConfigScreen.current(client) == failing, "Save failure closed the screen");
                require(BlockLensRuntime.config().sameCapabilities(original), "Save failure applied edits");
                require(Files.readString(occupied).equals("keep"), "Save failure overwrote an existing file");
                failing.resize(320, 240);
                require(firstToggle(failing).getMessage().getString().equals(state(!original.isEnabled(CapabilityId.ANVIL))),
                        "Save failure or resize lost the draft");
                press(failing, "blocklens.settings.close");
                require(BlockLensConfigScreen.current(client) == failing, "Save-and-close failure closed the screen");
                require(BlockLensRuntime.config().equals(original), "Failed Save published edits");
                Files.delete(occupied);
                Files.createDirectory(occupied);
                press(failing, "blocklens.settings.apply");
                require(BlockLensConfigScreen.current(client) == failing, "Apply retry closed the screen");
                require(BlockLensConfigFiles.loadOrCreate(occupied).isEnabled(CapabilityId.ANVIL)
                        == !original.isEnabled(CapabilityId.ANVIL), "Retry lost the draft");
                require(BlockLensRuntime.config().isEnabled(CapabilityId.ANVIL)
                        == !original.isEnabled(CapabilityId.ANVIL), "Retry did not install the saved snapshot");
                pressFirstToggle(failing);
                press(failing, "blocklens.settings.discard");
                require(BlockLensRuntime.config().isEnabled(CapabilityId.ANVIL)
                        == !original.isEnabled(CapabilityId.ANVIL), "Discard undid applied changes");
                BlockLensConfigScreen.show(client, failing);
                press(failing, "blocklens.settings.close");
                require(BlockLensConfigScreen.current(client) == previous, "Save and close did not return");
            } catch (IOException exception) {
                throw new AssertionError("Settings editing oracle could not use its controlled storage");
            } finally {
                try {
                    BlockLensConfigFiles.save(configDirectory, original);
                    if (fixture != null) {
                        Path occupied = fixture.resolve("occupied");
                        if (Files.isDirectory(occupied)) {
                            Files.deleteIfExists(occupied.resolve(BlockLensConfigFiles.FILE_NAME));
                        }
                        Files.deleteIfExists(occupied);
                        Files.deleteIfExists(fixture);
                    }
                } catch (IOException exception) {
                    throw new AssertionError("Settings editing oracle could not restore its controlled storage");
                } finally {
                    BlockLensRuntime.installConfig(original);
                    BlockLensConfigScreen.show(client, previous);
                }
            }
            System.out.println("BLOCKLENS_SETTINGS_EDITING discard=true escape=true persistence=true retry=true apply_stays=true unchanged_no_write=true");
        });
        verifyApplyReload(context);
    }

    /** Resource shortcuts edit only the local draft and respect Apply's saved baseline. */
    private static void verifyResourceShortcuts(ClientGameTestContext context) {
        Screen parent = context.computeOnClient(BlockLensConfigScreen::current);
        BlockLensConfig original = BlockLensRuntime.config();
        Path directory = FabricLoader.getInstance().getConfigDir();
        BlockLensConfig seeded = original.withEnabled(CapabilityId.ANVIL, true)
                .withEnabled(CapabilityId.BRIGHT_CONCRETE, true)
                .withEnabled(CapabilityId.DIAMOND_ORE, true)
                .withEnabled(CapabilityId.CONNECTED_ORE_EDGES, true)
                .withEnabled(CapabilityId.OBSIDIAN, false)
                .withEnabled(CapabilityId.CRYING_OBSIDIAN, false);
        BlockLensConfig cleared = seeded;
        for (CapabilityId id : CapabilityId.values()) {
            if (id.category() == CapabilityId.Category.RESOURCE) cleared = cleared.withEnabled(id, false);
        }
        final BlockLensConfig allOff = cleared;
        BlockLensConfig applied = allOff.withEnabled(CapabilityId.OBSIDIAN, true)
                .withEnabled(CapabilityId.CRYING_OBSIDIAN, true);
        try {
            var screen = context.computeOnClient(client -> {
                try {
                    BlockLensConfigFiles.save(directory, seeded);
                    BlockLensRuntime.installConfig(seeded);
                    var editing = new BlockLensConfigScreen(parent);
                    BlockLensConfigScreen.show(client, editing);
                    ResponsiveSettingsScreenOracle.select(editing, CapabilityId.Category.RESOURCE);
                    editing.keyPressed(new KeyEvent(InputConstants.KEY_END, 0, 0));
                    press(editing, "blocklens.resource.preset.obsidian");
                    requireResourceRows(editing, seeded.withEnabled(CapabilityId.OBSIDIAN, true)
                            .withEnabled(CapabilityId.CRYING_OBSIDIAN, true));
                    require(ResponsiveSettingsScreenOracle.buttons(editing).get(25).visible,
                            "Resource shortcut lost scroll");
                    require(BlockLensRuntime.config().equals(seeded), "Obsidian shortcut applied early");
                    require(BlockLensConfigFiles.loadOrCreate(directory).equals(seeded),
                            "Obsidian shortcut saved early");
                    press(editing, "blocklens.resource.preset.off");
                    requireResourceRows(editing, allOff);
                    press(editing, "blocklens.resource.preset.obsidian");
                    requireResourceRows(editing, applied);
                    require(BlockLensRuntime.config().equals(seeded), "All-OFF shortcut applied early");
                    require(BlockLensConfigFiles.loadOrCreate(directory).equals(seeded),
                            "All-OFF shortcut saved early");
                    press(editing, "blocklens.settings.apply");
                    return editing;
                } catch (IOException exception) { throw new AssertionError("Resource shortcut fixture failed"); }
            });
            context.waitFor(SettingsClientAccess::ready, 1200);
            context.waitTicks(2);
            context.runOnClient(client -> {
                try {
                    require(BlockLensConfigScreen.current(client) == screen, "Resource Apply reload closed settings");
                    require(ResponsiveSettingsScreenOracle.buttons(screen).get(25).visible,
                            "Resource Apply reload lost category or scroll");
                    requireResourceRows(screen, applied);
                    require(BlockLensRuntime.config().equals(applied), "Resource Apply changed other settings");
                    require(BlockLensConfigFiles.loadOrCreate(directory).equals(applied),
                            "Resource Apply did not persist");
                    press(screen, "blocklens.resource.preset.off");
                    screen.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                    require(BlockLensRuntime.config().equals(applied), "Esc undid applied resource shortcuts");
                    require(BlockLensConfigFiles.loadOrCreate(directory).equals(applied),
                            "Esc persisted a later resource shortcut");
                    var reopened = new BlockLensConfigScreen(parent);
                    BlockLensConfigScreen.show(client, reopened);
                    ResponsiveSettingsScreenOracle.select(reopened, CapabilityId.Category.RESOURCE);
                    requireResourceRows(reopened, applied);
                    press(reopened, "blocklens.resource.preset.off");
                    press(reopened, "blocklens.settings.discard");
                    require(BlockLensRuntime.config().equals(applied), "Discard published resource edits");
                    require(BlockLensConfigFiles.loadOrCreate(directory).equals(applied),
                            "Discard saved resource edits");
                } catch (IOException exception) { throw new AssertionError("Resource Apply persistence failed"); }
            });
            System.out.println("BLOCKLENS_RESOURCE_SHORTCUTS all_off=22 obsidian_pair=2 other_flags=true draft_only=true apply=true discard=true scroll=true reload=true");
        } finally {
            context.runOnClient(client -> {
                try { BlockLensConfigFiles.save(directory, original); }
                catch (IOException exception) { throw new AssertionError("Resource fixture restoration failed"); }
                BlockLensRuntime.installConfig(original);
                BlockLensConfigScreen.show(client, parent);
            });
            var restored = context.computeOnClient(client -> client.reloadResourcePacks());
            context.waitFor(client -> restored.isDone(), 1200);
            restored.join();
            context.waitFor(SettingsClientAccess::ready, 1200);
        }
    }

    private static void requireResourceRows(Screen screen, BlockLensConfig expected) {
        var resources = dev.blocklens.core.ui.SettingsCatalog.capabilities(CapabilityId.Category.RESOURCE);
        require(resources.size() == 22, "Resource shortcut contract lost a control");
        var buttons = ResponsiveSettingsScreenOracle.buttons(screen);
        for (int index = 0; index < resources.size(); index++) {
            require(buttons.get(4 + index).getMessage().getString().equals(
                    state(expected.isEnabled(resources.get(index)))), "Resource shortcut changed the wrong row");
        }
    }

    /** Resource-changing Apply must keep the screen and its newly saved baseline after reload. */
    private static void verifyApplyReload(ClientGameTestContext context) {
        BlockLensConfig original = BlockLensRuntime.config();
        Screen parent = context.computeOnClient(BlockLensConfigScreen::current);
        var screen = context.computeOnClient(client -> new BlockLensConfigScreen(parent));
        BlockLensConfig applied = original.withEnabled(CapabilityId.PALE_OAK_NEUTRAL,
                !original.isEnabled(CapabilityId.PALE_OAK_NEUTRAL));
        Path directory = FabricLoader.getInstance().getConfigDir();
        try {
            context.runOnClient(client -> {
                BlockLensConfigScreen.show(client, screen);
                ResponsiveSettingsScreenOracle.select(screen, CapabilityId.Category.OUTLINE);
                int index = dev.blocklens.core.ui.SettingsCatalog.capabilities(CapabilityId.Category.OUTLINE)
                        .indexOf(CapabilityId.PALE_OAK_NEUTRAL);
                screen.keyPressed(new KeyEvent(InputConstants.KEY_END, 0, 0));
                ResponsiveSettingsScreenOracle.buttons(screen).get(4 + index)
                        .onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
                press(screen, "blocklens.settings.apply");
            });
            context.waitFor(SettingsClientAccess::ready, 1200);
            context.waitTicks(2);
            context.runOnClient(client -> {
                require(BlockLensConfigScreen.current(client) == screen, "Apply reload closed settings");
                require(BlockLensRuntime.config().equals(applied), "Apply reload lost runtime settings");
                require(ResponsiveSettingsScreenOracle.buttons(screen).get(12).visible,
                        "Apply reload lost category or scroll");
                try {
                    require(BlockLensConfigFiles.loadOrCreate(directory).equals(applied), "Apply reload lost saved settings");
                } catch (IOException exception) { throw new AssertionError("Apply reload persistence failed"); }
                press(screen, "blocklens.settings.discard");
                require(BlockLensConfigScreen.current(client) == parent, "Discard after reload did not return");
                require(BlockLensRuntime.config().equals(applied), "Discard after reload undid Apply");
            });
            System.out.println("BLOCKLENS_SETTINGS_APPLY reload=true stays=true scroll=true baseline=true");
        } finally {
            context.runOnClient(client -> {
                try { BlockLensConfigFiles.save(directory, original); }
                catch (IOException exception) { throw new AssertionError("Apply reload fixture restoration failed"); }
                BlockLensRuntime.installConfig(original);
                BlockLensConfigScreen.show(client, parent);
            });
            var reloaded = context.computeOnClient(client -> client.reloadResourcePacks());
            context.waitFor(client -> reloaded.isDone(), 1200);
            reloaded.join();
            context.waitFor(SettingsClientAccess::ready, 1200);
        }
    }

    private static String state(boolean enabled) {
        return Component.translatable(enabled ? "blocklens.state.enabled" : "blocklens.state.disabled").getString();
    }

    private static Button firstToggle(Screen screen) {
        return ResponsiveSettingsScreenOracle.buttons(screen).get(4);
    }

    private static void pressFirstToggle(Screen screen) {
        firstToggle(screen).onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
    }

    private static void press(Screen screen, String key) {
        String label = Component.translatable(key).getString();
        ResponsiveSettingsScreenOracle.buttons(screen).stream()
                .filter(button -> button.getMessage().getString().equals(label)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing settings action"))
                .onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
    }

    private static final class FixtureScreen extends AbstractBlockLensConfigScreen {
        FixtureScreen(Screen parent, Path directory) {
            super(parent, directory);
        }

        @Override
        protected void showParent(Screen parent) {
            BlockLensConfigScreen.show(minecraft, parent);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

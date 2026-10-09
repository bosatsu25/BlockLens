package dev.blocklens.gametest;

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
                require(BlockLensRuntime.config().enabledMask() == original.enabledMask(), "Draft applied too early");
                press(screen, "blocklens.settings.discard");
                require(BlockLensConfigScreen.current(client) == previous, "Discard did not return to parent");
                require(BlockLensConfigFiles.loadOrCreate(configDirectory).enabledMask() == original.enabledMask(),
                        "Discard persisted edits");

                screen = new BlockLensConfigScreen(previous);
                BlockLensConfigScreen.show(client, screen);
                pressFirstToggle(screen);
                screen.keyPressed(new KeyEvent(256, 0, 0));
                require(BlockLensConfigScreen.current(client) == previous, "Esc did not return to parent");
                require(BlockLensRuntime.config().enabledMask() == original.enabledMask(), "Esc applied edits");

                screen = new BlockLensConfigScreen(previous);
                BlockLensConfigScreen.show(client, screen);
                pressFirstToggle(screen);
                ResponsiveSettingsScreenOracle.select(screen, CapabilityId.Category.RESOURCE);
                pressFirstToggle(screen);
                BlockLensConfig expected = original.withEnabled(CapabilityId.ANVIL, !original.isEnabled(CapabilityId.ANVIL))
                        .withEnabled(CapabilityId.OBSIDIAN, !original.isEnabled(CapabilityId.OBSIDIAN));
                press(screen, "blocklens.settings.apply");
                require(BlockLensConfigScreen.current(client) == previous, "Save did not return to parent");
                require(BlockLensRuntime.config().enabledMask() == expected.enabledMask(), "Save lost category edits");
                require(BlockLensConfigFiles.loadOrCreate(configDirectory).enabledMask() == expected.enabledMask(),
                        "Persisted edits differ from the runtime");

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
                require(BlockLensRuntime.config().enabledMask() == original.enabledMask(), "Save failure applied edits");
                require(Files.readString(occupied).equals("keep"), "Save failure overwrote an existing file");
                failing.resize(320, 240);
                require(firstToggle(failing).getMessage().getString().equals(state(!original.isEnabled(CapabilityId.ANVIL))),
                        "Save failure or resize lost the draft");
                Files.delete(occupied);
                Files.createDirectory(occupied);
                press(failing, "blocklens.settings.apply");
                require(BlockLensConfigScreen.current(client) == previous, "Retry did not return to parent");
                require(BlockLensConfigFiles.loadOrCreate(occupied).isEnabled(CapabilityId.ANVIL)
                        == !original.isEnabled(CapabilityId.ANVIL), "Retry lost the draft");
                require(BlockLensRuntime.config().isEnabled(CapabilityId.ANVIL)
                        == !original.isEnabled(CapabilityId.ANVIL), "Retry did not install the saved snapshot");
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
            System.out.println("BLOCKLENS_SETTINGS_EDITING discard=true escape=true persistence=true retry=true");
        });
    }

    private static String state(boolean enabled) {
        return Component.translatable(enabled ? "blocklens.state.enabled" : "blocklens.state.disabled").getString();
    }

    private static Button firstToggle(Screen screen) {
        return ResponsiveSettingsScreenOracle.buttons(screen).get(4);
    }

    private static void pressFirstToggle(Screen screen) {
        firstToggle(screen).onPress(new KeyEvent(257, 0, 0));
    }

    private static void press(Screen screen, String key) {
        String label = Component.translatable(key).getString();
        ResponsiveSettingsScreenOracle.buttons(screen).stream()
                .filter(button -> button.getMessage().getString().equals(label)).findFirst()
                .orElseThrow(() -> new AssertionError("Missing settings action"))
                .onPress(new KeyEvent(257, 0, 0));
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

package dev.blocklens.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.ComfortOptions;
import dev.blocklens.fabric.BlockLensConfigScreen;
import dev.blocklens.fabric.ComfortOptionsScreen;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/** Numeric-only real UI edits, return/Esc, discard and explicit persistence. */
final class ComfortSettingsOracle {
    private ComfortSettingsOracle() { }
    static void verify(ClientGameTestContext context) {
        context.runOnClient(client -> {
            Screen previous = BlockLensConfigScreen.current(client);
            BlockLensConfig original = BlockLensRuntime.config();
            var directory = FabricLoader.getInstance().getConfigDir();
            var file = directory.resolve(BlockLensConfigFiles.FILE_NAME);
            try {
                byte[] baseline = Files.readAllBytes(file);
                for (boolean save : new boolean[] {false, true}) {
                    var parent = new BlockLensConfigScreen(previous);
                    BlockLensConfigScreen.show(client, parent);
                    parent.resize(320, 240);
                    ResponsiveSettingsScreenOracle.select(parent, CapabilityId.Category.OTHER);
                    details(parent, 0);
                    Screen child = BlockLensConfigScreen.current(client);
                    require(child instanceof ComfortOptionsScreen, "fire detail screen not opened");
                    child.resize(320, 240);
                    String small = Component.translatable("blocklens.comfort.fire.2").getString();
                    Button preset = ResponsiveSettingsScreenOracle.buttons(child).stream()
                            .filter(b -> b.getMessage().getString().equals(small)).findFirst().orElseThrow();
                    require(child.mouseClicked(new MouseButtonEvent(preset.getX() + 5.0, preset.getY() + 5.0,
                            new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false),
                            "fire preset mouse action was not handled");
                    child.afterMouseAction();
                    require(child.getFocused() == null || child.children().contains(child.getFocused()),
                            "fire preset click retained a removed widget");
                    child.keyPressed(new KeyEvent(InputConstants.KEY_TAB, 0, 0));
                    require(child.getFocused() == null || child.children().contains(child.getFocused()),
                            "fire preset keyboard navigation retained a removed widget");
                    child.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                    require(BlockLensConfigScreen.current(client) == parent, "detail Esc did not return");
                    details(parent, 1);
                    child = BlockLensConfigScreen.current(client);
                    require(child instanceof ComfortOptionsScreen, "held detail screen not opened");
                    child.resize(320, 240);
                    for (Button button : ResponsiveSettingsScreenOracle.buttons(child)) {
                        require(button.getX() >= 0 && button.getRight() <= 320 && button.getY() >= 0
                                && button.getBottom() <= 240, "numeric control exceeds compact bounds");
                    }
                    ResponsiveSettingsScreenOracle.buttons(child).get(1)
                            .onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
                    press(child, "blocklens.settings.back");
                    require(BlockLensRuntime.config().comfortOptions().equals(original.comfortOptions()),
                            "numeric draft applied before save");
                    require(Arrays.equals(baseline, Files.readAllBytes(file)), "numeric draft wrote before save");
                    if (save) {
                        press(parent, "blocklens.settings.apply");
                        var old = original.comfortOptions();
                        var expected = new ComfortOptions(2, Math.min(100, old.blocksPercent() + 5),
                                old.itemsPercent(), old.toolsPercent());
                        require(BlockLensRuntime.config().comfortOptions().equals(expected), "save lost numeric edits");
                        require(BlockLensConfigFiles.loadOrCreate(directory).comfortOptions().equals(expected),
                                "numeric disk configuration differs from UI");
                        require(BlockLensRuntime.config().enabledMask() == original.enabledMask(),
                                "numeric-only save altered toggles");
                    } else {
                        parent.keyPressed(new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0));
                        require(Arrays.equals(baseline, Files.readAllBytes(file)), "main Esc persisted numeric edits");
                    }
                }
            } catch (IOException e) {
                throw new AssertionError("numeric settings controlled storage failed", e);
            } finally {
                try {
                    BlockLensConfigFiles.save(directory, original);
                } catch (IOException e) {
                    throw new AssertionError("numeric settings restore failed", e);
                } finally {
                    BlockLensRuntime.installConfig(original);
                    BlockLensConfigScreen.show(client, previous);
                }
            }
        });
        System.out.println("BLOCKLENS_COMFORT_SETTINGS numericDraft=true detailEscape=true discard=true save=true compact=true");
    }
    private static void details(Screen parent, int index) {
        String label = Component.translatable("blocklens.settings.details").getString();
        ResponsiveSettingsScreenOracle.buttons(parent).stream()
                .filter(b -> b.getMessage().getString().equals(label)).toList().get(index)
                .onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
    }
    private static void press(Screen screen, String key) {
        String label = Component.translatable(key).getString();
        ResponsiveSettingsScreenOracle.buttons(screen).stream()
                .filter(b -> b.getMessage().getString().equals(label)).findFirst().orElseThrow()
                .onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

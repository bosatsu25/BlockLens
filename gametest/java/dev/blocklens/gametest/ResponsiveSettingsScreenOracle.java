package dev.blocklens.gametest;

import com.mojang.blaze3d.platform.InputConstants;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.ui.SettingsCatalog;
import dev.blocklens.fabric.BlockLensConfigScreen;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.Component;

/** Exercises all categories and keyboard reachability on the real screen in both adapters. */
final class ResponsiveSettingsScreenOracle {
    private ResponsiveSettingsScreenOracle() {
    }

    static void verify(ClientGameTestContext context) {
        context.runOnClient(client -> {
            Screen previous = BlockLensConfigScreen.current(client);
            long baseline = BlockLensRuntime.config().enabledMask();
            BlockLensConfigScreen screen = new BlockLensConfigScreen(previous);
            try {
                BlockLensConfigScreen.show(client, screen);
                for (int[] size : new int[][] {{320, 240}, {640, 360}, {854, 480}, {1920, 1080}}) {
                    screen.resize(size[0], size[1]);
                    for (CapabilityId.Category category : CapabilityId.Category.values()) {
                        select(screen, category);
                        verifySize(screen, size[0], size[1], category);
                    }
                }
                require(BlockLensRuntime.config().enabledMask() == baseline, "Navigation applied edits");
                System.out.println("BLOCKLENS_RESPONSIVE_SETTINGS sizes=320x240,640x360,854x480,1920x1080"
                        + " categories=4 capabilities=53 keyboard_reachable=true bounded=true");
            } finally {
                BlockLensConfigScreen.show(client, previous);
            }
        });
    }

    static void select(BlockLensConfigScreen screen, CapabilityId.Category category) {
        String label = Component.translatable("blocklens.category."
                + category.name().toLowerCase(Locale.ROOT)).getString();
        Button tab = buttons(screen).stream().filter(button -> button.getMessage().getString().equals(label))
                .findFirst().orElseThrow(() -> new AssertionError("Missing category tab"));
        if (tab.active) {
            require(screen.mouseClicked(new MouseButtonEvent(
                    tab.getX() + 5.0, tab.getY() + 5.0, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false),
                    "Category click was not handled");
            screen.afterMouseAction();
            require(screen.getFocused() == null || buttons(screen).contains(screen.getFocused()),
                    "Category click left focus on a removed control");
        }
    }

    static List<Button> buttons(Screen screen) {
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).toList();
    }

    private static void verifySize(
            BlockLensConfigScreen screen, int width, int height, CapabilityId.Category category) {
        List<Button> buttons = buttons(screen);
        int count = SettingsCatalog.capabilities(category).size();
        require(buttons.size() == count + 6 + (category == CapabilityId.Category.OTHER ? 9 : 0), "widget registry changed");
        List<Button> toggles = buttons.subList(4, 4 + count);
        var reached = new HashSet<Button>();
        for (int index = 0; index < buttons.size() * 2; index++) {
            screen.keyPressed(new KeyEvent(InputConstants.KEY_TAB, 0, 0));
            require(screen.getFocused() instanceof Button, "keyboard focus escaped the controls");
            Button focused = (Button) screen.getFocused();
            require(focused.visible && focused.active, "keyboard focus escaped the visible controls");
            if (toggles.contains(focused)) reached.add(focused);
            for (Button button : buttons) {
                if (!button.visible) continue;
                require(button.getX() >= 0 && button.getRight() <= width,
                        "Visible button exceeds horizontal bounds");
                require(button.getY() >= 0 && button.getBottom() <= height,
                        "Visible button exceeds vertical bounds");
            }
        }
        require(reached.size() == count, "Keyboard cannot reach every category option");
        for (int index = 0; index < buttons.size() * 2; index++) {
            screen.keyPressed(new KeyEvent(InputConstants.KEY_TAB, 0, InputConstants.MOD_SHIFT));
            require(screen.getFocused() instanceof Button focused && focused.visible && focused.active,
                    "Reverse keyboard focus escaped visible controls");
        }
        if (toggles.stream().anyMatch(button -> !button.visible)) {
            require(screen.mouseScrolled(width / 2.0, height / 2.0, 0.0, -2.0),
                    "Compact category did not accept scrolling");
            require(screen.getFocused() == null
                    || screen.getFocused() instanceof Button focused && focused.visible,
                    "Mouse scroll left focus on a hidden row");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

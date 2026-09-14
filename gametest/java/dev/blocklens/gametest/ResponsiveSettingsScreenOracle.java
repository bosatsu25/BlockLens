package dev.blocklens.gametest;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.BlockLensConfigScreen;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

/** Real-screen resize, scroll, and focus oracle shared by both Minecraft adapters. */
final class ResponsiveSettingsScreenOracle {
    private static final int FOOTER_BUTTONS = 1;

    private ResponsiveSettingsScreenOracle() {
    }

    static void verify(ClientGameTestContext context) {
        context.runOnClient(client -> {
            Screen previous = BlockLensConfigScreen.current(client);
            BlockLensConfigScreen screen = new BlockLensConfigScreen(previous);
            try {
                BlockLensConfigScreen.show(client, screen);
                int expectedChildren = CapabilityId.values().length + FOOTER_BUTTONS;
                verifySize(screen, 320, 240, expectedChildren, true);
                verifySize(screen, 640, 360, expectedChildren, true);
                verifySize(screen, 854, 480, expectedChildren, false);
                verifySize(screen, 1920, 1080, expectedChildren, false);
                System.out.println("BLOCKLENS_RESPONSIVE_SETTINGS sizes=320x240,640x360,854x480,1920x1080"
                        + " widgets=" + expectedChildren + " bounded=true");
            } finally {
                BlockLensConfigScreen.show(client, previous);
            }
        });
    }

    private static void verifySize(
            BlockLensConfigScreen screen,
            int width,
            int height,
            int expectedChildren,
            boolean expectsScroll) {
        screen.resize(width, height);
        List<Button> buttons = screen.children().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .toList();
        require(buttons.size() == expectedChildren,
                width + "x" + height + ": widget registry changed: " + buttons.size());
        long visible = buttons.stream().filter(button -> button.visible).count();
        require(visible >= FOOTER_BUTTONS && visible <= expectedChildren,
                width + "x" + height + ": visible widget count is invalid: " + visible);
        for (Button button : buttons) {
            if (!button.visible) continue;
            require(button.getX() >= 0 && button.getRight() <= width,
                    width + "x" + height + ": visible button exceeds horizontal bounds");
            require(button.getY() >= 0 && button.getBottom() <= height,
                    width + "x" + height + ": visible button exceeds vertical bounds");
        }
        if (expectsScroll) {
            require(visible < expectedChildren, width + "x" + height + ": compact layout should scroll");
            require(screen.mouseScrolled(width / 2.0, height / 2.0, 0.0, -2.0),
                    width + "x" + height + ": scroll input was not accepted");
        }
        screen.keyPressed(new KeyEvent(258, 0, 0));
        require(screen.getFocused() == null
                        || screen.getFocused() instanceof Button focused && focused.visible && focused.active,
                width + "x" + height + ": keyboard focus escaped the visible interactive widgets");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

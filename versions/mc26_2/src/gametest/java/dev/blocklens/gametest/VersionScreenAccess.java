package dev.blocklens.gametest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

final class VersionScreenAccess {
    private VersionScreenAccess() {
    }

    static Screen current(Minecraft client) {
        return client.gui.screen();
    }

    static void show(Minecraft client, Screen screen) {
        client.gui.setScreen(screen);
    }
}

package dev.blocklens.gametest;

import net.minecraft.client.Minecraft;

/** Test-only bridge for the supported version's loading-overlay API. */
final class SettingsClientAccess {
    private SettingsClientAccess() {
    }

    static boolean ready(Minecraft client) {
        return client.getOverlay() == null;
    }
}

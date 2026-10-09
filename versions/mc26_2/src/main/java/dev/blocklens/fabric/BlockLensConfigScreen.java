package dev.blocklens.fabric;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Version-specific screen entrypoint; the complete settings UI is shared. */
public final class BlockLensConfigScreen extends AbstractBlockLensConfigScreen {
    public BlockLensConfigScreen(Screen parent) {
        super(parent, FabricLoader.getInstance().getConfigDir());
    }

    public static Screen current(Minecraft client) {
        return client.gui.screen();
    }

    public static void show(Minecraft client, Screen screen) {
        client.gui.setScreen(screen);
    }

    @Override
    protected void showParent(Screen parent) {
        show(minecraft, parent);
    }
}

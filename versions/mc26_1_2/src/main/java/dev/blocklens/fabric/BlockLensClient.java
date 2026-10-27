package dev.blocklens.fabric;

import net.fabricmc.api.ClientModInitializer;

/** Minecraft 26.1.2 entrypoint; startup is shared by {@link BlockLensClientBootstrap}. */
public final class BlockLensClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockLensClientBootstrap.initialize("26.1.2");
    }
}

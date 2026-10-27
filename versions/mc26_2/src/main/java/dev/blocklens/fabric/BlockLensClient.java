package dev.blocklens.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.SharedConstants;

/** Modern entrypoint compiled for 26.2 and 26.3; startup is shared by {@link BlockLensClientBootstrap}. */
public final class BlockLensClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockLensClientBootstrap.initialize(SharedConstants.getCurrentVersion().id());
    }
}

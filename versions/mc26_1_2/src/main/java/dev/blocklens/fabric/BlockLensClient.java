package dev.blocklens.fabric;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import java.io.IOException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Minecraft 26.1.2 Fabric boundary. Product logic stays in common. */
public final class BlockLensClient implements ClientModInitializer {
    private static final System.Logger LOGGER = System.getLogger("BlockLens");

    @Override
    public void onInitializeClient() {
        BlockLensConfig config;
        try {
            config = BlockLensConfigFiles.loadOrCreate(FabricLoader.getInstance().getConfigDir());
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to load BlockLens config; using defaults.", exception);
            config = BlockLensConfig.defaults();
        }

        BlockLensRuntime.initialize("26.1.2", config);
        LOGGER.log(
                System.Logger.Level.INFO,
                "BlockLens initialized for Minecraft {0}; capabilities={1}",
                BlockLensRuntime.minecraftVersion(),
                CapabilityId.values().length);
    }
}

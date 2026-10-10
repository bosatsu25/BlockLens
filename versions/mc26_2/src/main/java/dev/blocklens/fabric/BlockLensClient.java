package dev.blocklens.fabric;

import com.mojang.blaze3d.platform.InputConstants;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import java.io.IOException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.SharedConstants;

/** Shared modern Fabric boundary compiled for 26.2 and 26.3. Product logic stays in common. */
public final class BlockLensClient implements ClientModInitializer {
    private static final System.Logger LOGGER = System.getLogger("BlockLens");
    private static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.blocklens.settings", InputConstants.KEY_B, KeyMapping.Category.MISC);

    @Override
    public void onInitializeClient() {
        long startedNanos = System.nanoTime();
        BlockLensConfig config;
        try {
            config = BlockLensConfigFiles.loadOrCreate(FabricLoader.getInstance().getConfigDir());
        } catch (IOException | SecurityException exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Failed to load BlockLens config; using defaults.", exception);
            config = BlockLensConfig.defaults();
        }

        long initializationNanos = System.nanoTime() - startedNanos;
        BlockLensRuntime.initialize(SharedConstants.getCurrentVersion().id(), config, initializationNanos);
        MinecraftDecorationModelPlugin.register();
        AnalyzerClient.register();
        KeyMappingHelper.registerKeyMapping(OPEN_SETTINGS);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_SETTINGS.consumeClick()) {
                if (BlockLensConfigScreen.current(client) == null) {
                    BlockLensConfigScreen.show(client, new BlockLensConfigScreen(null));
                }
            }
        });
        LOGGER.log(
                System.Logger.Level.INFO,
                "BlockLens initialized for Minecraft {0}; capabilities={1}; initMicros={2}",
                BlockLensRuntime.minecraftVersion(),
                CapabilityId.values().length,
                initializationNanos / 1_000L);
    }
}

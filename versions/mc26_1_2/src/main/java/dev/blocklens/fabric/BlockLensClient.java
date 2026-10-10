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

/** Minecraft 26.1.2 Fabric boundary. Product logic stays in common. */
public final class BlockLensClient implements ClientModInitializer {
    private static final System.Logger LOGGER = System.getLogger("BlockLens");
    private static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "key.blocklens.settings", 66, KeyMapping.Category.MISC);
    private static final KeyMapping OPEN_BUILDER = new KeyMapping(
            "key.blocklens.builder", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC);

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
        BlockLensRuntime.initialize("26.1.2", config, initializationNanos);
        MinecraftDecorationModelPlugin.register();
        SceneFilterClient.register();
        AnalyzerClient.register();
        BuilderAssistClient.register();
        MinecraftOreExtensions.register();
        KeyMappingHelper.registerKeyMapping(OPEN_SETTINGS);
        KeyMappingHelper.registerKeyMapping(OPEN_BUILDER);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_SETTINGS.consumeClick()) {
                if (BlockLensConfigScreen.current(client) == null) {
                    BlockLensConfigScreen.show(client, new BlockLensConfigScreen(null));
                }
            }
            while (OPEN_BUILDER.consumeClick()) {
                if (client.level != null && BlockLensConfigScreen.current(client) == null) {
                    BlockLensConfigScreen.show(client, new BuilderAssistScreen(null,
                            screen -> BlockLensConfigScreen.show(client, screen)));
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

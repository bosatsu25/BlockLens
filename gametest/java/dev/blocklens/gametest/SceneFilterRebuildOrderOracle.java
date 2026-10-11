package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.scene.SceneFilterOptions;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
import dev.blocklens.fabric.SceneFilterClient;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.level.block.Blocks;

/** Checks the publication boundary before any tick or async section compilation. */
final class SceneFilterRebuildOrderOracle {
    private SceneFilterRebuildOrderOracle() { }
    static void verify(ClientGameTestContext context) {
        context.runOnClient(client -> {
            var original = BlockLensRuntime.config();
            try {
                var rules = SceneFilterOptions.defaults().withMode(0, SceneFilterOptions.Mode.BLACKLIST)
                        .withList(0, false, "minecraft:stone");
                BlockLensRuntime.installConfig(original.withEnabled(CapabilityId.BLOCK_FILTER, true)
                        .withSceneFilterOptions(rules));
                MinecraftTerrainInvalidator.invalidateAll(client);
                require(SceneFilterClient.readyForCurrentConfig() && SceneFilterClient.blockFilterActive()
                        && SceneFilterClient.hiddenBlock(Blocks.STONE), "Terrain rebuild started before filter publication");
                BlockLensRuntime.installConfig(original.withEnabled(CapabilityId.BLOCK_FILTER, false));
                MinecraftTerrainInvalidator.invalidateAll(client);
                require(!SceneFilterClient.blockFilterActive() && !SceneFilterClient.hiddenBlock(Blocks.STONE),
                        "Filter disable retained a stale compiler generation");
                System.out.println("BLOCKLENS_FILTER_REBUILD synchronousEnable=true synchronousDisable=true");
            } finally {
                BlockLensRuntime.installConfig(original);
                MinecraftTerrainInvalidator.invalidateAll(client);
            }
        });
    }
    private static void require(boolean result, String message) { if (!result) throw new AssertionError(message); }
}

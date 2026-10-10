package dev.blocklens.gametest;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Test-only version adapter for the native block-entity extraction signature. */
final class SceneRenderAccess {
    private SceneRenderAccess() { }
    static boolean extracted(Minecraft client,BlockPos position) {
        var entity=client.level.getBlockEntity(position);
        return entity!=null && client.getBlockEntityRenderDispatcher().tryExtractRenderState(entity,1.0F,null)!=null;
    }
}

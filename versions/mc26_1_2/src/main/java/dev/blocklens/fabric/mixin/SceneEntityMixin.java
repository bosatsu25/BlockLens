package dev.blocklens.fabric.mixin;

import dev.blocklens.fabric.SceneFilterClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
abstract class SceneEntityMixin {
    @Inject(method="extractEntity",at=@At("HEAD"),cancellable=true)
    private void blocklens$filterEntity(Entity entity,float delta,CallbackInfoReturnable<EntityRenderState> info) {
        if (SceneFilterClient.hiddenEntity(entity)) info.setReturnValue(SceneFilterClient.invisibleState(entity,delta));
    }
}

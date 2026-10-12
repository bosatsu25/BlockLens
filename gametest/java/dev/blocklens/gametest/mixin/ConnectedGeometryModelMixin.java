package dev.blocklens.gametest.mixin;

import dev.blocklens.gametest.OreExtensionFixture;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fixture-only alias guarantees the two collision states actually share one fixed visual model. */
@Mixin(BlockStateModelSet.class)
abstract class ConnectedGeometryModelMixin {
    @Inject(method="get",at=@At("RETURN"),cancellable=true)
    private void blocklens$sharedCollisionControl(BlockState state,CallbackInfoReturnable<BlockStateModel> info){
        var original=OreExtensionFixture.sharedGeometryBase(state);
        if(original!=null)info.setReturnValue(((BlockStateModelSet)(Object)this).get(original));
    }
}

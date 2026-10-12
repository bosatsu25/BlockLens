package dev.blocklens.gametest.mixin;

import dev.blocklens.gametest.ConnectedOreProbe;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets="dev.blocklens.fabric.MinecraftDecorationModel",remap=false)
abstract class ConnectedOreProbeMixin {
    @Inject(method="connectedNeighbors",at=@At("RETURN"),remap=false)
    private static void blocklens$actualNeighbors(BlockAndTintGetter level,BlockPos pos,BlockState state,
            Direction face,@Nullable BlockStateModelPart part,CallbackInfoReturnable<Integer> info){
        ConnectedOreProbe.record(pos.getX(),pos.getY(),pos.getZ(),face.ordinal(),info.getReturnValueI());
    }
}

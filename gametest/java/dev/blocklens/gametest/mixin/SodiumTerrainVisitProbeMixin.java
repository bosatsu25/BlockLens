package dev.blocklens.gametest.mixin;

import dev.blocklens.gametest.SodiumTerrainProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes the caller around renderModel, including a product HEAD cancellation. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask", remap = false)
abstract class SodiumTerrainVisitProbeMixin {
    @ModifyArgs(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;"
            + "Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)"
            + "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;"
                    + "renderModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V"),
            require = 1, remap = false)
    private void blocklens$beforeModel(Args arguments) {
        SodiumTerrainProbe.beforeModel((BlockState) arguments.get(1), (BlockPos) arguments.get(2));
    }

    @Inject(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;"
            + "Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)"
            + "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At(value = "INVOKE", target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;"
                    + "renderModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;"
                    + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V",
                    shift = At.Shift.AFTER), require = 1, remap = false)
    private void blocklens$afterModel(CallbackInfoReturnable<?> callback) {
        SodiumTerrainProbe.afterModel();
    }
}

package dev.blocklens.gametest.mixin;

import dev.blocklens.gametest.BBEPathProbe;
import java.util.function.Predicate;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Counts actual optimized chest emission, not just entry into a potentially skipped method. */
@Pseudo
@Mixin(targets = "betterblockentities.client.chunk.pipeline.BBEBlockRenderer", remap = false)
abstract class BBETerrainProbeMixin {
    @Inject(method = "emitChest", at = @At(value = "INVOKE", target =
            "Lbetterblockentities/client/chunk/pipeline/BBEEmitter;emit(Ljava/util/ArrayList;Ljava/util/function/Predicate;Lnet/caffeinemc/mods/sodium/client/services/PlatformModelEmitter$Bufferer;)V",
            shift = At.Shift.AFTER), require = 1, remap = false)
    private void blocklens$recordTerrainChest(Predicate<Direction> cull, RandomSource random,
            BlockState state, @Coerce Object emitter, BlockEntity entity, CallbackInfo ci) {
        BBEPathProbe.emitted(state);
    }
}

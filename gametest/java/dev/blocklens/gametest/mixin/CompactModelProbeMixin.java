package dev.blocklens.gametest.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import dev.blocklens.gametest.CompactModelProbe;
import java.util.Map;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelManager.class)
abstract class CompactModelProbeMixin {
    @ModifyReturnValue(method = "lambda$loadBlockModels$2", at = @At("RETURN"), order = 2000)
    private static Pair<Identifier, UnbakedModel> blocklens$parsed(Pair<Identifier, UnbakedModel> result,
            @Local(argsOnly = true) Map.Entry<Identifier, Resource> source) {
        if (result != null && result.getSecond() instanceof CuboidModel model) {
            CompactModelProbe.record(result.getFirst(), source.getValue().sourcePackId(), model);
        }
        return result;
    }
}

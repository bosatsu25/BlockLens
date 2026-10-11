package dev.blocklens.fabric.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import dev.blocklens.fabric.MinecraftCompactModels;
import java.util.Map;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Transform the actual result after Fabric's model deserializer has completed. */
@Mixin(ModelManager.class)
abstract class CompactModelMixin {
    @ModifyReturnValue(method = "lambda$loadBlockModels$2", at = @At("RETURN"))
    private static Pair<Identifier, UnbakedModel> blocklens$compact(Pair<Identifier, UnbakedModel> result,
            @Local(argsOnly = true) Map.Entry<Identifier, Resource> source) {
        if (result == null || !(result.getSecond() instanceof CuboidModel model)) return result;
        var changed = MinecraftCompactModels.apply(model, result.getFirst(), source.getValue());
        return changed == model ? result : Pair.of(result.getFirst(), changed);
    }
}

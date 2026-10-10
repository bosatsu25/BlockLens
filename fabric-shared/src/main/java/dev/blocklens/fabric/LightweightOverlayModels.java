package dev.blocklens.fabric;

import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;

/** Three original tiny extra models; no runtime geometry discovery. */
final class LightweightOverlayModels {
    static final ExtraModelKey<BlockStateModel> GLASS = key("glass");
    static final ExtraModelKey<BlockStateModel> PANE = key("pane");
    static final ExtraModelKey<BlockStateModel> KELP = key("kelp");
    private LightweightOverlayModels() { }
    private static Identifier id(String kind) {
        return Identifier.fromNamespaceAndPath("blocklens", "block/lightweight/" + kind);
    }
    private static ExtraModelKey<BlockStateModel> key(String kind) {
        return ExtraModelKey.create(id(kind)::toString);
    }
    static void register(ModelLoadingPlugin.Context context) {
        context.addModel(GLASS, SimpleUnbakedExtraModel.blockStateModel(id("glass")));
        context.addModel(PANE, SimpleUnbakedExtraModel.blockStateModel(id("pane")));
        context.addModel(KELP, SimpleUnbakedExtraModel.blockStateModel(id("kelp")));
    }
}

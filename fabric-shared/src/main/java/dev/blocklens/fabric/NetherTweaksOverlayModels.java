package dev.blocklens.fabric;

import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.resources.Identifier;

/** Two tiny BlockLens-owned extra models reconstruct the M6 14x14 interior and nylium band. */
final class NetherTweaksOverlayModels {
    private static final Identifier INTERIOR_MODEL = id("block/nether/interior_fill");
    private static final Identifier BAND_MODEL = id("block/nether/upper_band");

    static final ExtraModelKey<BlockStateModel> INTERIOR = ExtraModelKey.create(INTERIOR_MODEL::toString);
    static final ExtraModelKey<BlockStateModel> UPPER_BAND = ExtraModelKey.create(BAND_MODEL::toString);

    private NetherTweaksOverlayModels() {
    }

    static void register(ModelLoadingPlugin.Context context) {
        context.addModel(INTERIOR, SimpleUnbakedExtraModel.blockStateModel(INTERIOR_MODEL));
        context.addModel(UPPER_BAND, SimpleUnbakedExtraModel.blockStateModel(BAND_MODEL));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("blocklens", path);
    }
}

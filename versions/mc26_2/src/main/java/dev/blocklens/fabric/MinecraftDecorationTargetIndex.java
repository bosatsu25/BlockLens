package dev.blocklens.fabric;

import dev.blocklens.api.OreHighlightExtensions;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.BlockLensTargetCatalog;
import dev.blocklens.core.state.TargetCapabilityIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/**
 * Resolves the exact current target catalog to session-local 26.2 raw block ids.
 *
 * <p>The historical class name is retained while M4-M7 converges on the already-verified M3 model
 * pipeline. Each raw block id stores a composable capability bit-set, so overlaps are intentional.</p>
 */
public final class MinecraftDecorationTargetIndex {
    private MinecraftDecorationTargetIndex() {
    }

    public static TargetCapabilityIndex build() {
        TargetCapabilityIndex.Builder builder = TargetCapabilityIndex.builder(BuiltInRegistries.BLOCK.size());
        for (CapabilityId capability : CapabilityId.values()) {
            for (String path : BlockLensTargetCatalog.targets(capability)) {
                Block block = requireMinecraftBlock(path);
                builder.bind(BuiltInRegistries.BLOCK.getId(block), capability);
            }
        }
        OreHighlightExtensions.freeze();
        return builder.build();
    }

    private static Block requireMinecraftBlock(String path) {
        Identifier id = Identifier.tryParse("minecraft:" + path);
        if (id == null) {
            throw new IllegalStateException("Invalid BlockLens M0 target id: " + path);
        }
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == null || !id.equals(BuiltInRegistries.BLOCK.getKey(block))) {
            throw new IllegalStateException("BlockLens M0 target is not registered in Minecraft 26.2: " + id);
        }
        return block;
    }
}

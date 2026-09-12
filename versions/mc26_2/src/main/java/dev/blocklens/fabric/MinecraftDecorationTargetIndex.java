package dev.blocklens.fabric;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.DecorationTargetCatalog;
import dev.blocklens.core.state.DecorationStateKind;
import dev.blocklens.core.state.TargetCapabilityIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/** Resolves the exact M0 decoration target catalog to session-local 26.2 raw block ids. */
public final class MinecraftDecorationTargetIndex {
    private MinecraftDecorationTargetIndex() {
    }

    public static TargetCapabilityIndex build() {
        TargetCapabilityIndex.Builder builder = TargetCapabilityIndex.builder(BuiltInRegistries.BLOCK.size());
        for (CapabilityId capability : CapabilityId.values()) {
            if (!DecorationStateKind.isDecorationCapability(capability)) {
                continue;
            }
            for (String path : DecorationTargetCatalog.targets(capability)) {
                Block block = requireMinecraftBlock(path);
                builder.bind(BuiltInRegistries.BLOCK.getId(block), capability);
            }
        }
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

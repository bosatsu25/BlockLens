package dev.blocklens.gametest;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.DecorationTargetCatalog;
import dev.blocklens.core.state.DecorationStateKind;
import dev.blocklens.core.state.TargetCapabilityIndex;
import dev.blocklens.fabric.MinecraftDecorationModelPlugin;
import dev.blocklens.fabric.MinecraftDecorationTargetIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/** Verifies exact M0 decoration targets and the baked-model pipeline on every supported client. */
final class MinecraftDecorationTargetOracle {
    private MinecraftDecorationTargetOracle() {
    }

    static void verify() {
        TargetCapabilityIndex index = MinecraftDecorationTargetIndex.build();
        require(index.bindingCount() == 254,
                "expected 254 M0 decoration target bindings but got " + index.bindingCount());

        int checked = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            if (!DecorationStateKind.isDecorationCapability(capability)) {
                continue;
            }
            for (String path : DecorationTargetCatalog.targets(capability)) {
                Block block = block(path);
                int rawId = BuiltInRegistries.BLOCK.getId(block);
                require(index.contains(rawId, capability),
                        "target index missing " + capability.sourceKey() + " -> minecraft:" + path);
                checked++;
            }
        }
        require(checked == 254, "expected to verify 254 target bindings but got " + checked);
        require(MinecraftDecorationModelPlugin.isModelPipelineReady(),
                "M3 decoration model pipeline was not registered during model loading");
        require(MinecraftDecorationModelPlugin.wrappedModelCount() >= 254,
                "expected at least one wrapped state model per M0 target; wrapped="
                        + MinecraftDecorationModelPlugin.wrappedModelCount());
    }

    private static Block block(String path) {
        Identifier id = Identifier.tryParse("minecraft:" + path);
        if (id == null) {
            throw new AssertionError("Invalid test block id: " + path);
        }
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == null || !id.equals(BuiltInRegistries.BLOCK.getKey(block))) {
            throw new AssertionError("Required M0 target is not registered: " + id);
        }
        return block;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

package dev.blocklens.gametest;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.BlockLensTargetCatalog;
import dev.blocklens.core.state.TargetCapabilityIndex;
import dev.blocklens.fabric.MinecraftDecorationModelPlugin;
import dev.blocklens.fabric.MinecraftDecorationTargetIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/** Verifies the exact all-37 M0 target index and baked-model pipeline on every supported client. */
final class MinecraftDecorationTargetOracle {
    private MinecraftDecorationTargetOracle() {
    }

    static void verify() {
        TargetCapabilityIndex index = MinecraftDecorationTargetIndex.build();
        require(index.bindingCount() == 323,
                "expected 323 all-capability target bindings but got " + index.bindingCount());
        require(index.mappedCount() == 320,
                "expected 320 unique mapped Minecraft targets but got " + index.mappedCount());

        int checked = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            for (String path : BlockLensTargetCatalog.targets(capability)) {
                Block block = block(path);
                int rawId = BuiltInRegistries.BLOCK.getId(block);
                require(index.contains(rawId, capability),
                        "target index missing " + capability.sourceKey() + " -> minecraft:" + path);
                checked++;
            }
        }
        require(checked == 323, "expected to verify 323 target bindings but got " + checked);

        int obsidianId = BuiltInRegistries.BLOCK.getId(block("obsidian"));
        require(index.contains(obsidianId, CapabilityId.OBSIDIAN), "obsidian resource binding missing");
        require(index.contains(obsidianId, CapabilityId.NETHER_TWEAKS), "obsidian Nether Tweaks overlap missing");
        int crimsonStemId = BuiltInRegistries.BLOCK.getId(block("crimson_stem"));
        require(index.contains(crimsonStemId, CapabilityId.LOG), "crimson stem log binding missing");
        require(index.contains(crimsonStemId, CapabilityId.NETHER_TWEAKS), "crimson stem Nether overlap missing");

        require(MinecraftDecorationModelPlugin.isModelPipelineReady(),
                "BlockLens all-capability model pipeline was not registered during model loading");
        require(MinecraftDecorationModelPlugin.wrappedModelCount() >= 320,
                "expected at least one wrapped state model per unique M0 target; wrapped="
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

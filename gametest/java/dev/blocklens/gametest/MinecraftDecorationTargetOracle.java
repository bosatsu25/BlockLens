package dev.blocklens.gametest;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.BlockLensTargetCatalog;
import dev.blocklens.core.state.TargetCapabilityIndex;
import dev.blocklens.fabric.MinecraftDecorationModelPlugin;
import dev.blocklens.fabric.MinecraftDecorationTargetIndex;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

/** Verifies the current exact target index and baked-model pipeline on every supported client. */
final class MinecraftDecorationTargetOracle {
    private MinecraftDecorationTargetOracle() {
    }

    static void verify() {
        TargetCapabilityIndex index = MinecraftDecorationTargetIndex.build();
        require(index.bindingCount() == 366,
                "expected 366 original bindings but got " + index.bindingCount());
        require(index.mappedCount() == 328,
                "expected 328 original targets but got " + index.mappedCount());

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
        require(checked == 366, "expected to verify 366 target bindings but got " + checked);

        int obsidianId = BuiltInRegistries.BLOCK.getId(block("obsidian"));
        require(index.contains(obsidianId, CapabilityId.OBSIDIAN), "obsidian resource binding missing");
        require(index.contains(obsidianId, CapabilityId.NETHER_TWEAKS), "obsidian Nether Tweaks overlap missing");

        int cryingObsidianId = BuiltInRegistries.BLOCK.getId(block("crying_obsidian"));
        require(index.contains(cryingObsidianId, CapabilityId.CRYING_OBSIDIAN),
                "crying obsidian P0 material binding missing");
        require(index.contains(cryingObsidianId, CapabilityId.NETHER_TWEAKS),
                "crying obsidian Nether overlap missing");

        int netherGoldId = BuiltInRegistries.BLOCK.getId(block("nether_gold_ore"));
        require(index.contains(netherGoldId, CapabilityId.NETHER_GOLD_ORE),
                "nether gold P0 material binding missing");
        require(index.contains(netherGoldId, CapabilityId.NETHER_TWEAKS),
                "nether gold Nether overlap missing");

        int netherQuartzId = BuiltInRegistries.BLOCK.getId(block("nether_quartz_ore"));
        require(index.contains(netherQuartzId, CapabilityId.NETHER_QUARTZ_ORE),
                "nether quartz P0 material binding missing");
        require(index.contains(netherQuartzId, CapabilityId.NETHER_TWEAKS),
                "nether quartz Nether overlap missing");

        int tripwireHookId = BuiltInRegistries.BLOCK.getId(block("tripwire_hook"));
        require(index.contains(tripwireHookId, CapabilityId.STRING_TWEAKS),
                "tripwire hook Fine Line parity binding missing");

        int polishedBasaltId = BuiltInRegistries.BLOCK.getId(block("polished_basalt"));
        require(index.contains(polishedBasaltId, CapabilityId.NETHER_TWEAKS),
                "polished basalt Nether parity binding missing");

        int crimsonStemId = BuiltInRegistries.BLOCK.getId(block("crimson_stem"));
        require(index.contains(crimsonStemId, CapabilityId.LOG), "crimson stem log binding missing");
        require(index.contains(crimsonStemId, CapabilityId.NETHER_TWEAKS), "crimson stem Nether overlap missing");

        require(MinecraftDecorationModelPlugin.isModelPipelineReady(),
                "BlockLens current model pipeline was not registered during model loading");
        require(MinecraftDecorationModelPlugin.wrappedModelCount() >= 328,
                "expected at least one wrapped state model per unique current target; wrapped="
                        + MinecraftDecorationModelPlugin.wrappedModelCount());
    }

    private static Block block(String path) {
        Identifier id = Identifier.tryParse("minecraft:" + path);
        if (id == null) throw new AssertionError("Invalid test block id: " + path);
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == null || !id.equals(BuiltInRegistries.BLOCK.getKey(block))) {
            throw new AssertionError("Required BlockLens target is not registered: " + id);
        }
        return block;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

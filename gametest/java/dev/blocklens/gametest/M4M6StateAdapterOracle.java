package dev.blocklens.gametest;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.SemanticState.Axis;
import dev.blocklens.core.state.SemanticState.Facing;
import dev.blocklens.fabric.MinecraftStateAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Real-client semantic oracle for visibility and Nether state families. */
final class M4M6StateAdapterOracle {
    private M4M6StateAdapterOracle() {
    }

    static void verify() {
        BlockState catalyst = block("sculk_catalyst").defaultBlockState()
                .setValue(BlockStateProperties.BLOOM, true);
        SemanticState bloom = MinecraftStateAdapter.interpret(CapabilityId.SCULK_CATALYST, catalyst);
        require(bloom.bloom(), "sculk catalyst bloom semantic missing");

        BlockState tripwire = block("tripwire").defaultBlockState()
                .setValue(BlockStateProperties.NORTH, true)
                .setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.SOUTH, true)
                .setValue(BlockStateProperties.WEST, true)
                .setValue(BlockStateProperties.POWERED, true)
                .setValue(BlockStateProperties.ATTACHED, false);
        SemanticState wire = MinecraftStateAdapter.interpret(CapabilityId.STRING_TWEAKS, tripwire);
        require(wire.connected(Facing.NORTH), "tripwire north connection missing");
        require(!wire.connected(Facing.EAST), "tripwire east connection unexpected");
        require(wire.connected(Facing.SOUTH), "tripwire south connection missing");
        require(wire.connected(Facing.WEST), "tripwire west connection missing");
        require(wire.powered() && !wire.attached(), "tripwire powered/attached semantic mismatch");

        BlockState hook = block("tripwire_hook").defaultBlockState()
                .setValue(BlockStateProperties.POWERED, true)
                .setValue(BlockStateProperties.ATTACHED, true);
        SemanticState hookSemantic = MinecraftStateAdapter.interpret(CapabilityId.STRING_TWEAKS, hook);
        require(!hookSemantic.connected(Facing.NORTH)
                        && !hookSemantic.connected(Facing.EAST)
                        && !hookSemantic.connected(Facing.SOUTH)
                        && !hookSemantic.connected(Facing.WEST),
                "tripwire hook must not invent directional wire connections");
        require(hookSemantic.powered() && hookSemantic.attached(),
                "tripwire hook powered/attached semantic mismatch");

        BlockState stem = block("crimson_stem").defaultBlockState()
                .setValue(BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.X);
        require(MinecraftStateAdapter.interpret(CapabilityId.NETHER_TWEAKS, stem).axis() == Axis.X,
                "Nether Tweaks stem axis semantic mismatch");

        BlockState polishedBasalt = block("polished_basalt").defaultBlockState()
                .setValue(BlockStateProperties.AXIS, net.minecraft.core.Direction.Axis.Z);
        require(MinecraftStateAdapter.interpret(CapabilityId.NETHER_TWEAKS, polishedBasalt).axis() == Axis.Z,
                "P0 polished basalt axis semantic mismatch");

        require(MinecraftStateAdapter.interpret(
                        CapabilityId.NETHER_TWEAKS,
                        block("netherrack").defaultBlockState()).equals(SemanticState.empty()),
                "stateless Nether Tweaks target must use empty semantics");
    }

    private static Block block(String path) {
        Identifier id = Identifier.tryParse("minecraft:" + path);
        if (id == null) throw new AssertionError("invalid test block id: " + path);
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == null || !id.equals(BuiltInRegistries.BLOCK.getKey(block))) {
            throw new AssertionError("required test block is not registered: " + id);
        }
        return block;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

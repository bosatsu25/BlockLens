package dev.blocklens.gametest;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.SemanticState.Axis;
import dev.blocklens.core.state.SemanticState.Facing;
import dev.blocklens.core.state.SemanticState.Half;
import dev.blocklens.core.state.SemanticState.MountFace;
import dev.blocklens.core.state.SemanticState.SlabType;
import dev.blocklens.core.state.SemanticState.StairShape;
import dev.blocklens.fabric.MinecraftStateAdapter;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Shared Minecraft-backed semantic oracle executed by Client GameTest on every supported version.
 *
 * <p>This deliberately runs after the real client has bootstrapped registries. Mapped BlockState
 * behavior is an integration boundary, not a plain-JUnit boundary.</p>
 */
final class MinecraftStateAdapterOracle {
    private MinecraftStateAdapterOracle() {
    }

    static void verify() {
        facingCapabilitiesMapToCommonFacing();
        beehiveAndCampfireMapSecondaryState();
        grindstoneAndFenceGateMapCompoundState();
        axisCapabilitiesProduceTheSameCommonAxisSemantics();
        slabAndPaneConnectionsMapWithoutMinecraftTypesEscapingCommon();
        stairsAndTrapdoorMapExactSourceSemantics();
        visibilityCapabilitiesMapBloomAndAllTripwireFlags();
        statelessCapabilitiesReturnEmptyAndMismatchesFailFast();
    }

    private static void facingCapabilitiesMapToCommonFacing() {
        BlockState anvil = Blocks.ANVIL.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        require(MinecraftStateAdapter.interpret(CapabilityId.ANVIL, anvil).facing() == Facing.WEST,
                "anvil facing semantic mismatch");

        BlockState terracotta = block("white_glazed_terracotta").defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        require(MinecraftStateAdapter.interpret(CapabilityId.GLAZED_TERRACOTTA, terracotta).facing() == Facing.EAST,
                "glazed terracotta facing semantic mismatch");
    }

    private static void beehiveAndCampfireMapSecondaryState() {
        BlockState beehive = Blocks.BEEHIVE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.LEVEL_HONEY, 5);
        SemanticState hive = MinecraftStateAdapter.interpret(CapabilityId.BEEHIVE, beehive);
        require(hive.facing() == Facing.SOUTH && hive.level() == 5,
                "beehive semantic mismatch");

        BlockState campfire = Blocks.CAMPFIRE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.LIT, false);
        SemanticState fire = MinecraftStateAdapter.interpret(CapabilityId.CAMPFIRE, campfire);
        require(fire.facing() == Facing.NORTH && !fire.lit(),
                "campfire semantic mismatch");
    }

    private static void grindstoneAndFenceGateMapCompoundState() {
        BlockState grindstone = Blocks.GRINDSTONE.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        SemanticState grind = MinecraftStateAdapter.interpret(CapabilityId.GRINDSTONE, grindstone);
        require(grind.mountFace() == MountFace.CEILING && grind.facing() == Facing.EAST,
                "grindstone semantic mismatch");

        BlockState gate = Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.OPEN, true)
                .setValue(BlockStateProperties.IN_WALL, true);
        SemanticState fence = MinecraftStateAdapter.interpret(CapabilityId.FENCE_GATE, gate);
        require(fence.facing() == Facing.SOUTH && fence.open() && fence.inWall(),
                "fence gate semantic mismatch");
    }

    private static void axisCapabilitiesProduceTheSameCommonAxisSemantics() {
        BlockState froglight = Blocks.OCHRE_FROGLIGHT.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        require(MinecraftStateAdapter.interpret(CapabilityId.FROGLIGHT, froglight).axis() == Axis.X,
                "froglight axis semantic mismatch");

        BlockState wood = Blocks.OAK_WOOD.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
        require(MinecraftStateAdapter.interpret(CapabilityId.WOOD, wood).axis() == Axis.Y,
                "wood axis semantic mismatch");

        BlockState log = Blocks.OAK_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Z);
        require(MinecraftStateAdapter.interpret(CapabilityId.LOG, log).axis() == Axis.Z,
                "log axis semantic mismatch");
    }

    private static void slabAndPaneConnectionsMapWithoutMinecraftTypesEscapingCommon() {
        BlockState slab = Blocks.OAK_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.SLAB_TYPE,
                        net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);
        require(MinecraftStateAdapter.interpret(CapabilityId.SLABS, slab).slabType() == SlabType.DOUBLE,
                "slab semantic mismatch");

        BlockState pane = block("white_stained_glass_pane").defaultBlockState()
                .setValue(BlockStateProperties.NORTH, true)
                .setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.SOUTH, true)
                .setValue(BlockStateProperties.WEST, false);
        SemanticState connections = MinecraftStateAdapter.interpret(CapabilityId.STAINED_GLASS, pane);
        require(connections.connected(Facing.NORTH), "pane north connection missing");
        require(!connections.connected(Facing.EAST), "pane east connection unexpected");
        require(connections.connected(Facing.SOUTH), "pane south connection missing");
        require(!connections.connected(Facing.WEST), "pane west connection unexpected");

        SemanticState fullGlass = MinecraftStateAdapter.interpret(
                CapabilityId.STAINED_GLASS, block("white_stained_glass").defaultBlockState());
        require(fullGlass.connectionMask() == 0, "full stained glass must not expose pane connections");
    }

    private static void stairsAndTrapdoorMapExactSourceSemantics() {
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.HALF,
                        net.minecraft.world.level.block.state.properties.Half.TOP)
                .setValue(BlockStateProperties.STAIRS_SHAPE,
                        net.minecraft.world.level.block.state.properties.StairsShape.INNER_RIGHT);
        SemanticState stairState = MinecraftStateAdapter.interpret(CapabilityId.STAIRS, stairs);
        require(stairState.facing() == Facing.WEST
                        && stairState.half() == Half.TOP
                        && stairState.stairShape() == StairShape.INNER_RIGHT,
                "stairs semantic mismatch");

        BlockState trapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.HALF,
                        net.minecraft.world.level.block.state.properties.Half.BOTTOM)
                .setValue(BlockStateProperties.OPEN, true);
        SemanticState trapState = MinecraftStateAdapter.interpret(CapabilityId.TRAPDOOR, trapdoor);
        require(trapState.facing() == Facing.NORTH && trapState.half() == Half.BOTTOM && trapState.open(),
                "trapdoor semantic mismatch");
    }

    private static void visibilityCapabilitiesMapBloomAndAllTripwireFlags() {
        require(MinecraftStateAdapter.interpret(
                        CapabilityId.BLUE_ICE, Blocks.BLUE_ICE.defaultBlockState()).equals(SemanticState.empty()),
                "static outline should not invent state semantics");

        BlockState catalyst = Blocks.SCULK_CATALYST.defaultBlockState()
                .setValue(BlockStateProperties.BLOOM, true);
        require(MinecraftStateAdapter.interpret(CapabilityId.SCULK_CATALYST, catalyst).bloom(),
                "sculk catalyst bloom semantic mismatch");

        BlockState tripwire = Blocks.TRIPWIRE.defaultBlockState()
                .setValue(BlockStateProperties.NORTH, true)
                .setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.SOUTH, true)
                .setValue(BlockStateProperties.WEST, true)
                .setValue(BlockStateProperties.POWERED, true)
                .setValue(BlockStateProperties.ATTACHED, false);
        SemanticState stringState = MinecraftStateAdapter.interpret(CapabilityId.STRING_TWEAKS, tripwire);
        require(stringState.connected(Facing.NORTH), "tripwire north connection missing");
        require(!stringState.connected(Facing.EAST), "tripwire east connection unexpected");
        require(stringState.connected(Facing.SOUTH), "tripwire south connection missing");
        require(stringState.connected(Facing.WEST), "tripwire west connection missing");
        require(stringState.powered() && !stringState.attached(), "tripwire power/attached semantic mismatch");
    }

    private static void statelessCapabilitiesReturnEmptyAndMismatchesFailFast() {
        require(MinecraftStateAdapter.interpret(
                        CapabilityId.OBSIDIAN, Blocks.OBSIDIAN.defaultBlockState()).equals(SemanticState.empty()),
                "resource capability must return empty semantics");
        require(MinecraftStateAdapter.interpret(
                        CapabilityId.NETHER_TWEAKS, Blocks.NETHERRACK.defaultBlockState()).equals(SemanticState.empty()),
                "Nether Tweaks must remain stateless until render policy consumes exact targets");

        boolean decorationFailedFast = false;
        try {
            MinecraftStateAdapter.interpret(CapabilityId.STAIRS, Blocks.STONE.defaultBlockState());
        } catch (IllegalArgumentException expected) {
            decorationFailedFast = true;
        }
        require(decorationFailedFast, "mismatched decoration target must fail fast");

        boolean visibilityFailedFast = false;
        try {
            MinecraftStateAdapter.interpret(CapabilityId.SCULK_CATALYST, Blocks.STONE.defaultBlockState());
        } catch (IllegalArgumentException expected) {
            visibilityFailedFast = true;
        }
        require(visibilityFailedFast, "mismatched stateful visibility target must fail fast");
    }

    private static Block block(String path) {
        Identifier id = Identifier.tryParse("minecraft:" + path);
        if (id == null) {
            throw new AssertionError("Invalid test block id: " + path);
        }
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        if (block == null || !id.equals(BuiltInRegistries.BLOCK.getKey(block))) {
            throw new AssertionError("Required test block is not registered: " + id);
        }
        return block;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

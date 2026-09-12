package dev.blocklens.fabric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.SemanticState.Axis;
import dev.blocklens.core.state.SemanticState.Facing;
import dev.blocklens.core.state.SemanticState.Half;
import dev.blocklens.core.state.SemanticState.MountFace;
import dev.blocklens.core.state.SemanticState.SlabType;
import dev.blocklens.core.state.SemanticState.StairShape;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.Test;

/** Same semantic oracle is compiled and executed independently against both Minecraft lines. */
final class MinecraftStateAdapterTest {
    @Test
    void facingCapabilitiesMapToCommonFacing() {
        BlockState anvil = Blocks.ANVIL.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST);
        assertEquals(Facing.WEST, MinecraftStateAdapter.interpret(CapabilityId.ANVIL, anvil).facing());

        BlockState terracotta = Blocks.WHITE_GLAZED_TERRACOTTA.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        assertEquals(Facing.EAST,
                MinecraftStateAdapter.interpret(CapabilityId.GLAZED_TERRACOTTA, terracotta).facing());
    }

    @Test
    void beehiveAndCampfireMapSecondaryState() {
        BlockState beehive = Blocks.BEEHIVE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.HONEY_LEVEL, 5);
        SemanticState hive = MinecraftStateAdapter.interpret(CapabilityId.BEEHIVE, beehive);
        assertEquals(Facing.SOUTH, hive.facing());
        assertEquals(5, hive.level());

        BlockState campfire = Blocks.CAMPFIRE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.LIT, false);
        SemanticState fire = MinecraftStateAdapter.interpret(CapabilityId.CAMPFIRE, campfire);
        assertEquals(Facing.NORTH, fire.facing());
        assertFalse(fire.lit());
    }

    @Test
    void grindstoneAndFenceGateMapCompoundState() {
        BlockState grindstone = Blocks.GRINDSTONE.defaultBlockState()
                .setValue(BlockStateProperties.ATTACH_FACE, AttachFace.CEILING)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
        SemanticState grind = MinecraftStateAdapter.interpret(CapabilityId.GRINDSTONE, grindstone);
        assertEquals(MountFace.CEILING, grind.mountFace());
        assertEquals(Facing.EAST, grind.facing());

        BlockState gate = Blocks.OAK_FENCE_GATE.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .setValue(BlockStateProperties.OPEN, true)
                .setValue(BlockStateProperties.IN_WALL, true);
        SemanticState fence = MinecraftStateAdapter.interpret(CapabilityId.FENCE_GATE, gate);
        assertEquals(Facing.SOUTH, fence.facing());
        assertTrue(fence.open());
        assertTrue(fence.inWall());
    }

    @Test
    void axisCapabilitiesProduceTheSameCommonAxisSemantics() {
        BlockState froglight = Blocks.OCHRE_FROGLIGHT.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        assertEquals(Axis.X,
                MinecraftStateAdapter.interpret(CapabilityId.FROGLIGHT, froglight).axis());

        BlockState wood = Blocks.OAK_WOOD.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
        assertEquals(Axis.Y, MinecraftStateAdapter.interpret(CapabilityId.WOOD, wood).axis());

        BlockState log = Blocks.OAK_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.Z);
        assertEquals(Axis.Z, MinecraftStateAdapter.interpret(CapabilityId.LOG, log).axis());
    }

    @Test
    void slabAndPaneConnectionsMapWithoutMinecraftTypesEscapingCommon() {
        BlockState slab = Blocks.OAK_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.SLAB_TYPE,
                        net.minecraft.world.level.block.state.properties.SlabType.DOUBLE);
        assertEquals(SlabType.DOUBLE,
                MinecraftStateAdapter.interpret(CapabilityId.SLABS, slab).slabType());

        BlockState pane = Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState()
                .setValue(BlockStateProperties.NORTH, true)
                .setValue(BlockStateProperties.EAST, false)
                .setValue(BlockStateProperties.SOUTH, true)
                .setValue(BlockStateProperties.WEST, false);
        SemanticState connections = MinecraftStateAdapter.interpret(CapabilityId.STAINED_GLASS, pane);
        assertTrue(connections.connected(Facing.NORTH));
        assertFalse(connections.connected(Facing.EAST));
        assertTrue(connections.connected(Facing.SOUTH));
        assertFalse(connections.connected(Facing.WEST));

        // Full stained-glass blocks have no pane connection properties and map to no connections.
        SemanticState fullGlass = MinecraftStateAdapter.interpret(
                CapabilityId.STAINED_GLASS, Blocks.WHITE_STAINED_GLASS.defaultBlockState());
        assertEquals(0, fullGlass.connectionMask());
    }

    @Test
    void stairsAndTrapdoorMapExactSourceSemantics() {
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .setValue(BlockStateProperties.HALF,
                        net.minecraft.world.level.block.state.properties.Half.TOP)
                .setValue(BlockStateProperties.STAIRS_SHAPE,
                        net.minecraft.world.level.block.state.properties.StairsShape.INNER_RIGHT);
        SemanticState stairState = MinecraftStateAdapter.interpret(CapabilityId.STAIRS, stairs);
        assertEquals(Facing.WEST, stairState.facing());
        assertEquals(Half.TOP, stairState.half());
        assertEquals(StairShape.INNER_RIGHT, stairState.stairShape());

        BlockState trapdoor = Blocks.OAK_TRAPDOOR.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .setValue(BlockStateProperties.HALF,
                        net.minecraft.world.level.block.state.properties.Half.BOTTOM)
                .setValue(BlockStateProperties.OPEN, true);
        SemanticState trapState = MinecraftStateAdapter.interpret(CapabilityId.TRAPDOOR, trapdoor);
        assertEquals(Facing.NORTH, trapState.facing());
        assertEquals(Half.BOTTOM, trapState.half());
        assertTrue(trapState.open());
    }

    @Test
    void nonDecorationReturnsEmptyAndMismatchedTargetFailsFast() {
        assertEquals(SemanticState.empty(), MinecraftStateAdapter.interpret(
                CapabilityId.OBSIDIAN, Blocks.OBSIDIAN.defaultBlockState()));
        assertThrows(IllegalArgumentException.class, () -> MinecraftStateAdapter.interpret(
                CapabilityId.STAIRS, Blocks.STONE.defaultBlockState()));
        assertThrows(NullPointerException.class, () -> MinecraftStateAdapter.interpret(
                null, Blocks.STONE.defaultBlockState()));
        assertThrows(NullPointerException.class, () -> MinecraftStateAdapter.interpret(
                CapabilityId.STAIRS, null));
    }
}

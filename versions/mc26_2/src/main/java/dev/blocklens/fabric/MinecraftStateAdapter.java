package dev.blocklens.fabric;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.DecorationStateKind;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.SemanticState.Axis;
import dev.blocklens.core.state.SemanticState.Facing;
import dev.blocklens.core.state.SemanticState.Half;
import dev.blocklens.core.state.SemanticState.MountFace;
import dev.blocklens.core.state.SemanticState.SlabType;
import dev.blocklens.core.state.SemanticState.StairShape;
import dev.blocklens.core.state.VisibilityStateKind;
import java.util.Objects;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/** Minecraft 26.2 BlockState -> common semantic-state boundary for all BlockLens capabilities. */
public final class MinecraftStateAdapter {
    private MinecraftStateAdapter() {
    }

    public static SemanticState interpret(CapabilityId capability, BlockState state) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(state, "state");

        VisibilityStateKind visibilityKind = VisibilityStateKind.forCapability(capability);
        if (visibilityKind != VisibilityStateKind.NONE) {
            return switch (visibilityKind) {
                case NONE -> throw new IllegalStateException("unreachable NONE visibility kind");
                case STATIC_OUTLINE -> SemanticState.empty();
                case SCULK_BLOOM -> SemanticState.sculkCatalyst(require(state, BlockStateProperties.BLOOM));
                case TRIPWIRE -> SemanticState.tripwire(
                        booleanOrFalse(state, BlockStateProperties.NORTH),
                        booleanOrFalse(state, BlockStateProperties.EAST),
                        booleanOrFalse(state, BlockStateProperties.SOUTH),
                        booleanOrFalse(state, BlockStateProperties.WEST),
                        require(state, BlockStateProperties.POWERED),
                        require(state, BlockStateProperties.ATTACHED));
            };
        }

        if (capability == CapabilityId.NETHER_TWEAKS) {
            return state.hasProperty(BlockStateProperties.AXIS)
                    ? SemanticState.axis(axis(state.getValue(BlockStateProperties.AXIS)))
                    : SemanticState.empty();
        }

        DecorationStateKind decorationKind = DecorationStateKind.forCapability(capability);
        if (decorationKind == DecorationStateKind.NONE) {
            return SemanticState.empty();
        }
        return switch (decorationKind) {
            case NONE -> throw new IllegalStateException("unreachable NONE decoration kind");
            case FACING -> SemanticState.facing(facing(require(state, BlockStateProperties.HORIZONTAL_FACING)));
            case FACING_HONEY_LEVEL -> SemanticState.beehive(
                    facing(require(state, BlockStateProperties.HORIZONTAL_FACING)),
                    require(state, BlockStateProperties.LEVEL_HONEY));
            case FACING_LIT -> SemanticState.campfire(
                    facing(require(state, BlockStateProperties.HORIZONTAL_FACING)),
                    require(state, BlockStateProperties.LIT));
            case MOUNT_FACE_FACING -> SemanticState.grindstone(
                    mountFace(require(state, BlockStateProperties.ATTACH_FACE)),
                    facing(require(state, BlockStateProperties.HORIZONTAL_FACING)));
            case FENCE_GATE -> SemanticState.fenceGate(
                    facing(require(state, BlockStateProperties.HORIZONTAL_FACING)),
                    require(state, BlockStateProperties.OPEN),
                    require(state, BlockStateProperties.IN_WALL));
            case AXIS -> SemanticState.axis(axis(require(state, BlockStateProperties.AXIS)));
            case SLAB -> SemanticState.slab(slabType(require(state, BlockStateProperties.SLAB_TYPE)));
            case CONNECTIONS -> SemanticState.connections(
                    booleanOrFalse(state, BlockStateProperties.NORTH),
                    booleanOrFalse(state, BlockStateProperties.EAST),
                    booleanOrFalse(state, BlockStateProperties.SOUTH),
                    booleanOrFalse(state, BlockStateProperties.WEST));
            case STAIRS -> SemanticState.stairs(
                    facing(require(state, BlockStateProperties.HORIZONTAL_FACING)),
                    half(require(state, BlockStateProperties.HALF)),
                    stairShape(require(state, BlockStateProperties.STAIRS_SHAPE)));
            case TRAPDOOR -> SemanticState.trapdoor(
                    facing(require(state, BlockStateProperties.HORIZONTAL_FACING)),
                    half(require(state, BlockStateProperties.HALF)),
                    require(state, BlockStateProperties.OPEN));
        };
    }

    private static <T extends Comparable<T>> T require(BlockState state, Property<T> property) {
        if (!state.hasProperty(property)) {
            throw new IllegalArgumentException("BlockState is missing required property: " + property.getName());
        }
        return state.getValue(property);
    }

    private static boolean booleanOrFalse(BlockState state, Property<Boolean> property) {
        return state.hasProperty(property) && state.getValue(property);
    }

    private static Facing facing(Direction direction) {
        return switch (direction) {
            case NORTH -> Facing.NORTH;
            case EAST -> Facing.EAST;
            case SOUTH -> Facing.SOUTH;
            case WEST -> Facing.WEST;
            default -> throw new IllegalArgumentException("Expected horizontal facing, got " + direction);
        };
    }

    private static Axis axis(Direction.Axis axis) {
        return switch (axis) {
            case X -> Axis.X;
            case Y -> Axis.Y;
            case Z -> Axis.Z;
        };
    }

    private static Half half(net.minecraft.world.level.block.state.properties.Half half) {
        return switch (half) {
            case BOTTOM -> Half.BOTTOM;
            case TOP -> Half.TOP;
        };
    }

    private static StairShape stairShape(net.minecraft.world.level.block.state.properties.StairsShape shape) {
        return switch (shape) {
            case STRAIGHT -> StairShape.STRAIGHT;
            case INNER_LEFT -> StairShape.INNER_LEFT;
            case INNER_RIGHT -> StairShape.INNER_RIGHT;
            case OUTER_LEFT -> StairShape.OUTER_LEFT;
            case OUTER_RIGHT -> StairShape.OUTER_RIGHT;
        };
    }

    private static MountFace mountFace(AttachFace face) {
        return switch (face) {
            case FLOOR -> MountFace.FLOOR;
            case WALL -> MountFace.WALL;
            case CEILING -> MountFace.CEILING;
        };
    }

    private static SlabType slabType(net.minecraft.world.level.block.state.properties.SlabType type) {
        return switch (type) {
            case BOTTOM -> SlabType.BOTTOM;
            case TOP -> SlabType.TOP;
            case DOUBLE -> SlabType.DOUBLE;
        };
    }
}

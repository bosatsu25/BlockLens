package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import java.util.Objects;

/**
 * Pack-level decoration decal grammar, applied over the active block texture.
 *
 * <p>The source pack draws its cues as saturated line work on every relevant face, so the state
 * stays readable when only one face of a block is visible. This policy reproduces that
 * information with clean-room glyphs:</p>
 *
 * <ul>
 *   <li>Glazed terracotta: an arrow on all six faces pointing in the placer's look direction
 *       (opposite {@code facing}); the facing face shows it up and the opposite face down.</li>
 *   <li>Log, wood, froglight: an axis-colored frame on both end faces and rails parallel to the
 *       axis on the four side faces (X red, Y green, Z blue).</li>
 *   <li>Slabs: a red edge frame on the face that meets the block boundary (bottom slab: down,
 *       top slab: up); double slabs carry a seam on every side and a double frame on top/bottom.</li>
 *   <li>Stairs and trapdoors: an outline on every quad, colored by stair shape or trapdoor
 *       facing, so step and panel structure reads on uniform textures.</li>
 *   <li>Anvil, beehive, campfire, grindstone, fence gate: an outline on the faces the existing
 *       state cue already selects, in the existing semantic colors.</li>
 *   <li>Stained glass keeps its solid-layer behavior and has no decal.</li>
 * </ul>
 */
public final class DecorationDecalPolicy {
    public static final int AXIS_X = 0xFFFF3030;
    public static final int AXIS_Y = 0xFF30E030;
    public static final int AXIS_Z = 0xFF3080FF;
    public static final int DIRECTION = 0xFFFFE040;
    public static final int HONEY_FULL = 0xFFFF9900;
    public static final int LIT = 0xFFFF7020;
    public static final int UNLIT = 0xFFB0B0B0;
    public static final int MOUNT = 0xFF30E0E0;
    public static final int SLAB = 0xFFFF3030;
    public static final int ARROW = 0xFFFFFFFF;
    public static final int STAIR_STRAIGHT = 0xFF30E030;
    public static final int STAIR_INNER_LEFT = 0xFF30E0E0;
    public static final int STAIR_INNER_RIGHT = 0xFF5090FF;
    public static final int STAIR_OUTER_LEFT = 0xFFFF9030;
    public static final int STAIR_OUTER_RIGHT = 0xFFFF40FF;
    public static final int GATE_OPEN = AXIS_Y;
    public static final int GATE_CLOSED = AXIS_X;

    private DecorationDecalPolicy() {
    }

    public static DecorationDecal decal(DecorationRenderDescriptor descriptor, DecorationQuadFace face) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(face, "face");
        if (face == DecorationQuadFace.NONE) {
            return DecorationDecal.NONE;
        }
        SemanticState state = descriptor.state();
        return switch (descriptor.cue()) {
            case OPAQUE_STAINED_GLASS -> DecorationDecal.NONE;
            case FACING_MARKER -> descriptor.capability() == CapabilityId.GLAZED_TERRACOTTA
                    ? arrow(state.facing(), face)
                    : frameIf(face == face(state.facing()), DIRECTION);
            case FACING_LEVEL_MARKER -> frameIf(face == face(state.facing()),
                    state.level() == 5 ? HONEY_FULL : DIRECTION);
            case FACING_LIT_MARKER -> frameIf(face == face(state.facing()), state.lit() ? LIT : UNLIT);
            case MOUNT_FACE_MARKER -> face == mountFace(state.mountFace())
                    ? frame(MOUNT)
                    : frameIf(face == face(state.facing()), DIRECTION);
            case FENCE_GATE_STATE_MARKER -> frameIf(face == face(state.facing()),
                    state.open() ? GATE_OPEN : GATE_CLOSED);
            case AXIS_MARKER -> axis(state.axis(), face);
            case SLAB_STATE_MARKER -> slab(state.slabType(), face);
            case STAIRS_STATE_MARKER -> state.stairShape() == SemanticState.StairShape.NONE
                    ? DecorationDecal.NONE : frame(stairColor(state.stairShape()));
            case TRAPDOOR_STATE_MARKER -> state.facing() == SemanticState.Facing.NONE
                    ? DecorationDecal.NONE : frame(trapdoorColor(state.facing()));
        };
    }

    private static DecorationDecal arrow(SemanticState.Facing facing, DecorationQuadFace face) {
        DecorationQuadFace front = face(facing);
        if (front == DecorationQuadFace.NONE) {
            return DecorationDecal.NONE;
        }
        DecorationQuadFace look = DecalProjection.opposite(front);
        int turns;
        if (face == front) {
            turns = 0;
        } else if (face == look) {
            turns = 2;
        } else {
            turns = DecalProjection.quarterTurnsToward(face, look);
        }
        return DecorationDecal.of(DecorationGlyph.ARROW, turns, ARROW);
    }

    private static DecorationDecal axis(SemanticState.Axis axis, DecorationQuadFace face) {
        int color = switch (axis) {
            case X -> AXIS_X;
            case Y -> AXIS_Y;
            case Z -> AXIS_Z;
            case NONE -> 0;
        };
        if (color == 0) {
            return DecorationDecal.NONE;
        }
        if (axisOf(face) == axis) {
            return DecorationDecal.of(DecorationGlyph.INSET_FRAME, 0, color);
        }
        int turns = axisOf(DecalProjection.screenUp(face)) == axis ? 0 : 1;
        return DecorationDecal.of(DecorationGlyph.RAILS, turns, color);
    }

    private static DecorationDecal slab(SemanticState.SlabType type, DecorationQuadFace face) {
        boolean vertical = face == DecorationQuadFace.UP || face == DecorationQuadFace.DOWN;
        return switch (type) {
            case BOTTOM -> face == DecorationQuadFace.DOWN
                    ? DecorationDecal.of(DecorationGlyph.EDGE_FRAME, 0, SLAB) : DecorationDecal.NONE;
            case TOP -> face == DecorationQuadFace.UP
                    ? DecorationDecal.of(DecorationGlyph.EDGE_FRAME, 0, SLAB) : DecorationDecal.NONE;
            case DOUBLE -> DecorationDecal.of(
                    vertical ? DecorationGlyph.DOUBLE_FRAME : DecorationGlyph.SEAM_FRAME, 0, SLAB);
            case NONE -> DecorationDecal.NONE;
        };
    }

    private static int stairColor(SemanticState.StairShape shape) {
        return switch (shape) {
            case STRAIGHT -> STAIR_STRAIGHT;
            case INNER_LEFT -> STAIR_INNER_LEFT;
            case INNER_RIGHT -> STAIR_INNER_RIGHT;
            case OUTER_LEFT -> STAIR_OUTER_LEFT;
            case OUTER_RIGHT -> STAIR_OUTER_RIGHT;
            case NONE -> throw new IllegalStateException("unreachable");
        };
    }

    /** Same four-color facing legend as the trapdoor tint grammar. */
    private static int trapdoorColor(SemanticState.Facing facing) {
        return switch (facing) {
            case NORTH -> AXIS_Z;
            case EAST -> AXIS_X;
            case SOUTH -> DIRECTION;
            case WEST -> AXIS_Y;
            case NONE -> throw new IllegalStateException("unreachable");
        };
    }

    private static SemanticState.Axis axisOf(DecorationQuadFace face) {
        return switch (face) {
            case EAST, WEST -> SemanticState.Axis.X;
            case UP, DOWN -> SemanticState.Axis.Y;
            case NORTH, SOUTH -> SemanticState.Axis.Z;
            case NONE -> SemanticState.Axis.NONE;
        };
    }

    private static DecorationQuadFace face(SemanticState.Facing facing) {
        return switch (facing) {
            case NORTH -> DecorationQuadFace.NORTH;
            case EAST -> DecorationQuadFace.EAST;
            case SOUTH -> DecorationQuadFace.SOUTH;
            case WEST -> DecorationQuadFace.WEST;
            case NONE -> DecorationQuadFace.NONE;
        };
    }

    private static DecorationQuadFace mountFace(SemanticState.MountFace mount) {
        return switch (mount) {
            case FLOOR -> DecorationQuadFace.DOWN;
            case CEILING -> DecorationQuadFace.UP;
            case WALL, NONE -> DecorationQuadFace.NONE;
        };
    }

    private static DecorationDecal frame(int color) {
        return DecorationDecal.of(DecorationGlyph.INSET_FRAME, 0, color);
    }

    private static DecorationDecal frameIf(boolean condition, int color) {
        return condition ? frame(color) : DecorationDecal.NONE;
    }
}

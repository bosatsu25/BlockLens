package dev.blocklens.core.render;

import dev.blocklens.core.state.SemanticState;
import java.util.Objects;

/**
 * Shared M3 visual grammar derived from the frozen source behavior without copying source assets.
 *
 * <p>The source pack repeatedly encodes orientation with colored boundaries/markers. BlockLens keeps
 * that user value procedurally: X/Y/Z use red/green/blue, directional/state cues use stable shared
 * colors, and stained glass becomes solid-layer rendered while enabled. Version adapters only map
 * Minecraft directions and apply these instructions to baked quads.</p>
 */
public final class DecorationQuadCuePolicy {
    static final int AXIS_X = 0xFFFF9090;
    static final int AXIS_Y = 0xFF90FF90;
    static final int AXIS_Z = 0xFF9090FF;
    static final int DIRECTION = 0xFFFFFF90;
    static final int HONEY_FULL = 0xFFFFC060;
    static final int LIT = 0xFFFFB060;
    static final int UNLIT = 0xFFC8C8C8;
    static final int OPEN = 0xFF90FF90;
    static final int CLOSED = 0xFFFF9090;
    static final int MOUNT = 0xFF90FFFF;
    static final int SLAB_BOUNDARY = 0xFFFF9090;
    static final int HALF_BOUNDARY = 0xFFFFFF90;

    private DecorationQuadCuePolicy() {
    }

    public static DecorationQuadInstruction instruction(
            DecorationRenderDescriptor descriptor,
            DecorationQuadFace face) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(face, "face");
        SemanticState state = descriptor.state();

        return switch (descriptor.cue()) {
            case OPAQUE_STAINED_GLASS -> DecorationQuadInstruction.OPAQUE;
            case FACING_MARKER -> tintIf(matchesFacing(face, state.facing()), DIRECTION);
            case FACING_LEVEL_MARKER -> tintIf(
                    matchesFacing(face, state.facing()),
                    state.level() == 5 ? HONEY_FULL : DIRECTION);
            case FACING_LIT_MARKER -> tintIf(
                    matchesFacing(face, state.facing()),
                    state.lit() ? LIT : UNLIT);
            case MOUNT_FACE_MARKER -> mountInstruction(state, face);
            case FENCE_GATE_STATE_MARKER -> tintIf(
                    matchesFacing(face, state.facing()),
                    state.open() ? OPEN : CLOSED);
            case AXIS_MARKER -> axisInstruction(state.axis(), face);
            case SLAB_STATE_MARKER -> slabInstruction(state.slabType(), face);
            case STAIRS_STATE_MARKER -> stairsInstruction(state, face);
            case TRAPDOOR_STATE_MARKER -> trapdoorInstruction(state, face);
        };
    }

    private static DecorationQuadInstruction mountInstruction(
            SemanticState state,
            DecorationQuadFace face) {
        if (matchesMount(face, state.mountFace())) {
            return DecorationQuadInstruction.tint(MOUNT);
        }
        return tintIf(matchesFacing(face, state.facing()), DIRECTION);
    }

    private static DecorationQuadInstruction axisInstruction(
            SemanticState.Axis axis,
            DecorationQuadFace face) {
        return switch (axis) {
            case X -> tintIf(face == DecorationQuadFace.EAST || face == DecorationQuadFace.WEST, AXIS_X);
            case Y -> tintIf(face == DecorationQuadFace.UP || face == DecorationQuadFace.DOWN, AXIS_Y);
            case Z -> tintIf(face == DecorationQuadFace.NORTH || face == DecorationQuadFace.SOUTH, AXIS_Z);
            case NONE -> DecorationQuadInstruction.IDENTITY;
        };
    }

    private static DecorationQuadInstruction slabInstruction(
            SemanticState.SlabType type,
            DecorationQuadFace face) {
        boolean boundary = switch (type) {
            case BOTTOM -> face == DecorationQuadFace.UP;
            case TOP -> face == DecorationQuadFace.DOWN;
            case DOUBLE -> face == DecorationQuadFace.UP || face == DecorationQuadFace.DOWN;
            case NONE -> false;
        };
        return tintIf(boundary, SLAB_BOUNDARY);
    }

    private static DecorationQuadInstruction stairsInstruction(
            SemanticState state,
            DecorationQuadFace face) {
        if (matchesFacing(face, state.facing())) {
            return DecorationQuadInstruction.tint(stairShapeColor(state.stairShape()));
        }
        boolean halfBoundary = switch (state.half()) {
            case BOTTOM -> face == DecorationQuadFace.UP;
            case TOP -> face == DecorationQuadFace.DOWN;
            case NONE -> false;
        };
        return tintIf(halfBoundary, HALF_BOUNDARY);
    }

    private static DecorationQuadInstruction trapdoorInstruction(
            SemanticState state,
            DecorationQuadFace face) {
        if (state.open()) {
            return tintIf(matchesFacing(face, state.facing()), facingColor(state.facing()));
        }
        boolean panelFace = switch (state.half()) {
            case BOTTOM -> face == DecorationQuadFace.UP;
            case TOP -> face == DecorationQuadFace.DOWN;
            case NONE -> false;
        };
        return tintIf(panelFace, facingColor(state.facing()));
    }

    private static int stairShapeColor(SemanticState.StairShape shape) {
        return switch (shape) {
            case STRAIGHT -> 0xFF90FF90;
            case INNER_LEFT -> 0xFF90FFFF;
            case INNER_RIGHT -> 0xFF90C0FF;
            case OUTER_LEFT -> 0xFFFFC090;
            case OUTER_RIGHT -> 0xFFFF90FF;
            case NONE -> DIRECTION;
        };
    }

    private static int facingColor(SemanticState.Facing facing) {
        return switch (facing) {
            case NORTH -> 0xFF9090FF;
            case EAST -> 0xFFFF9090;
            case SOUTH -> 0xFFFFFF90;
            case WEST -> 0xFF90FF90;
            case NONE -> DIRECTION;
        };
    }

    private static boolean matchesFacing(
            DecorationQuadFace face,
            SemanticState.Facing facing) {
        return switch (facing) {
            case NORTH -> face == DecorationQuadFace.NORTH;
            case EAST -> face == DecorationQuadFace.EAST;
            case SOUTH -> face == DecorationQuadFace.SOUTH;
            case WEST -> face == DecorationQuadFace.WEST;
            case NONE -> false;
        };
    }

    private static boolean matchesMount(
            DecorationQuadFace face,
            SemanticState.MountFace mountFace) {
        return switch (mountFace) {
            case FLOOR -> face == DecorationQuadFace.DOWN;
            case CEILING -> face == DecorationQuadFace.UP;
            case WALL, NONE -> false;
        };
    }

    private static DecorationQuadInstruction tintIf(boolean condition, int color) {
        return condition ? DecorationQuadInstruction.tint(color) : DecorationQuadInstruction.IDENTITY;
    }
}

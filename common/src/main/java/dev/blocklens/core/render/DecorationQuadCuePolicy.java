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
    private static final DecorationQuadInstruction AXIS_X_TINT = tint(0xFFFF9090);
    private static final DecorationQuadInstruction AXIS_Y_TINT = tint(0xFF90FF90);
    private static final DecorationQuadInstruction AXIS_Z_TINT = tint(0xFF9090FF);
    private static final DecorationQuadInstruction DIRECTION_TINT = tint(0xFFFFFF90);
    private static final DecorationQuadInstruction HONEY_FULL_TINT = tint(0xFFFFC060);
    private static final DecorationQuadInstruction LIT_TINT = tint(0xFFFFB060);
    private static final DecorationQuadInstruction UNLIT_TINT = tint(0xFFC8C8C8);
    private static final DecorationQuadInstruction MOUNT_TINT = tint(0xFF90FFFF);
    private static final DecorationQuadInstruction STAIR_INNER_RIGHT_TINT = tint(0xFF90C0FF);
    private static final DecorationQuadInstruction STAIR_OUTER_LEFT_TINT = tint(0xFFFFC090);
    private static final DecorationQuadInstruction STAIR_OUTER_RIGHT_TINT = tint(0xFFFF90FF);

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
            case FACING_MARKER -> tintIf(matchesFacing(face, state.facing()), DIRECTION_TINT);
            case FACING_LEVEL_MARKER -> tintIf(
                    matchesFacing(face, state.facing()),
                    state.level() == 5 ? HONEY_FULL_TINT : DIRECTION_TINT);
            case FACING_LIT_MARKER -> tintIf(
                    matchesFacing(face, state.facing()),
                    state.lit() ? LIT_TINT : UNLIT_TINT);
            case MOUNT_FACE_MARKER -> mountInstruction(state, face);
            case FENCE_GATE_STATE_MARKER -> tintIf(
                    matchesFacing(face, state.facing()),
                    state.open() ? AXIS_Y_TINT : AXIS_X_TINT);
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
            return MOUNT_TINT;
        }
        return tintIf(matchesFacing(face, state.facing()), DIRECTION_TINT);
    }

    private static DecorationQuadInstruction axisInstruction(
            SemanticState.Axis axis,
            DecorationQuadFace face) {
        return switch (axis) {
            case X -> tintIf(face == DecorationQuadFace.EAST || face == DecorationQuadFace.WEST, AXIS_X_TINT);
            case Y -> tintIf(face == DecorationQuadFace.UP || face == DecorationQuadFace.DOWN, AXIS_Y_TINT);
            case Z -> tintIf(face == DecorationQuadFace.NORTH || face == DecorationQuadFace.SOUTH, AXIS_Z_TINT);
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
        return tintIf(boundary, AXIS_X_TINT);
    }

    private static DecorationQuadInstruction stairsInstruction(
            SemanticState state,
            DecorationQuadFace face) {
        if (matchesFacing(face, state.facing())) {
            return stairShapeInstruction(state.stairShape());
        }
        boolean halfBoundary = switch (state.half()) {
            case BOTTOM -> face == DecorationQuadFace.UP;
            case TOP -> face == DecorationQuadFace.DOWN;
            case NONE -> false;
        };
        return tintIf(halfBoundary, DIRECTION_TINT);
    }

    private static DecorationQuadInstruction trapdoorInstruction(
            SemanticState state,
            DecorationQuadFace face) {
        if (state.open()) {
            return tintIf(matchesFacing(face, state.facing()), facingInstruction(state.facing()));
        }
        boolean panelFace = switch (state.half()) {
            case BOTTOM -> face == DecorationQuadFace.UP;
            case TOP -> face == DecorationQuadFace.DOWN;
            case NONE -> false;
        };
        return tintIf(panelFace, facingInstruction(state.facing()));
    }

    private static DecorationQuadInstruction stairShapeInstruction(SemanticState.StairShape shape) {
        return switch (shape) {
            case STRAIGHT -> AXIS_Y_TINT;
            case INNER_LEFT -> MOUNT_TINT;
            case INNER_RIGHT -> STAIR_INNER_RIGHT_TINT;
            case OUTER_LEFT -> STAIR_OUTER_LEFT_TINT;
            case OUTER_RIGHT -> STAIR_OUTER_RIGHT_TINT;
            case NONE -> DIRECTION_TINT;
        };
    }

    private static DecorationQuadInstruction facingInstruction(SemanticState.Facing facing) {
        return switch (facing) {
            case NORTH -> AXIS_Z_TINT;
            case EAST -> AXIS_X_TINT;
            case SOUTH -> DIRECTION_TINT;
            case WEST -> AXIS_Y_TINT;
            case NONE -> DIRECTION_TINT;
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

    private static DecorationQuadInstruction tintIf(
            boolean condition,
            DecorationQuadInstruction instruction) {
        return condition ? instruction : DecorationQuadInstruction.IDENTITY;
    }

    private static DecorationQuadInstruction tint(int color) {
        return DecorationQuadInstruction.tint(color);
    }
}

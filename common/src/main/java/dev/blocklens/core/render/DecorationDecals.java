package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pack-level decoration decal grammar, drawn over the active block texture.
 *
 * <p>The source pack draws its cues as saturated line work on every relevant face, so the state
 * stays readable when only one face of a block is visible. This policy reproduces that
 * information with clean-room glyphs (see {@code knowledge/current/pack-information-parity.md}):
 * glazed terracotta arrows on all faces in the source template's rotations, axis-colored frames
 * and rails, slab frames on the face that meets the block boundary, and outlines on the faces the
 * tint grammar selects for the other cues. Stained glass has no decal.</p>
 *
 * <p>Glyph rotation is expressed in a fixed viewer frame per face (screen-right x screen-up is the
 * outward normal) and applied to vertex positions, never model UVs. A decal is a packed long:
 * bits 0-2 hold glyph + 1 ({@link #NONE} is 0), bits 3-4 clockwise quarter turns, bit 5
 * {@link #onBoundaryOnly boundary-only}, bits 32-63 the ARGB tint. Face and enum ordinals index
 * static tables, so nothing here allocates per quad.</p>
 */
public final class DecorationDecals {
    public static final int INSET_FRAME = 0;
    public static final int EDGE_FRAME = 1;
    /** Two lines on the left and right glyph edges; one quarter turn makes them horizontal. */
    public static final int RAILS = 2;
    /** Arrow whose tip points to glyph up. */
    public static final int ARROW = 3;
    public static final int SEAM_FRAME = 4;
    public static final int DOUBLE_FRAME = 5;
    public static final int GLYPHS = 6;
    public static final int GLYPH_PIXELS = 32;
    /** One vertical strip in the block atlas; glyph g occupies rows [32g, 32g + 32). */
    public static final String SPRITE = "block/cue/decal_glyphs";
    /** Outward offset in blocks; above Nether overlays (0.03/16) and ore borders (0.02/16). */
    public static final float SURFACE_OFFSET = 1.0F / 256.0F;
    public static final long NONE = 0L;
    private static final long BOUNDARY_ONLY = 1L << 5;

    public static final int AXIS_X = 0xFFFF3030;
    public static final int AXIS_Y = 0xFF30E030;
    public static final int AXIS_Z = 0xFF3080FF;
    public static final int DIRECTION = 0xFFFFE040;
    public static final int HONEY_FULL = 0xFFFF9900;
    public static final int LIT = 0xFFFF7020;
    public static final int UNLIT = 0xFFB0B0B0;
    public static final int MOUNT = 0xFF30E0E0;
    public static final int SLAB = 0xFFFF3030;
    public static final int ARROW_COLOR = 0xFFFFFFFF;

    // Indexed by DecorationQuadFace ordinal: NORTH, EAST, SOUTH, WEST, UP, DOWN.
    private static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3, UP = 4, DOWN = 5;
    private static final int[] SCREEN_UP = {UP, UP, UP, UP, NORTH, SOUTH};
    private static final int[] SCREEN_RIGHT = {WEST, NORTH, EAST, SOUTH, EAST, EAST};
    private static final int[] OPPOSITE = {SOUTH, WEST, NORTH, EAST, DOWN, UP};
    private static final int[] VX = {0, 1, 0, -1, 0, 0};
    private static final int[] VY = {0, 0, 0, 0, 1, -1};
    private static final int[] VZ = {-1, 0, 1, 0, 0, 0};
    /** SemanticState.Axis ordinal (X=1, Y=2, Z=3) of each face's normal. */
    private static final int[] FACE_AXIS = {3, 1, 3, 1, 2, 2};
    // Indexed by enum ordinal minus one (index 0 of each enum is NONE).
    private static final int[] AXIS_COLORS = {AXIS_X, AXIS_Y, AXIS_Z};
    private static final int[] STAIR_COLORS = {0xFF30E030, 0xFF30E0E0, 0xFF5090FF, 0xFFFF9030, 0xFFFF40FF};
    /** Same four-color facing legend as the trapdoor tint grammar: N blue, E red, S yellow, W green. */
    private static final int[] TRAPDOOR_COLORS = {AXIS_Z, AXIS_X, DIRECTION, AXIS_Y};
    private static final int FACES = DecorationQuadFace.values().length;
    private static final ConcurrentHashMap<Long, long[]> INTERNED = new ConcurrentHashMap<>();

    private DecorationDecals() {
    }

    public static long decal(int glyph, int quarterTurns, int argb) {
        if (glyph < 0 || glyph >= GLYPHS || quarterTurns < 0 || quarterTurns > 3 || (argb >>> 24) != 0xFF) {
            throw new IllegalArgumentException("invalid decal " + glyph + "/" + quarterTurns + "/" + Integer.toHexString(argb));
        }
        return (long) argb << 32 | quarterTurns << 3 | glyph + 1;
    }

    public static int glyph(long decal) {
        return (int) (decal & 7) - 1;
    }

    public static int quarterTurns(long decal) {
        return (int) (decal >>> 3 & 3);
    }

    public static int argb(long decal) {
        return (int) (decal >>> 32);
    }

    /** {@code decal}, drawn only on quads that lie in the block-boundary plane of their face. */
    public static long onBoundaryOnly(long decal) {
        return decal == NONE ? NONE : decal | BOUNDARY_ONLY;
    }

    public static boolean boundaryOnly(long decal) {
        return (decal & BOUNDARY_ONLY) != 0L;
    }

    /**
     * Per-face decals for one (capability, state), indexed by {@link DecorationQuadFace} ordinal.
     * Interned and shared across baked models: callers must never write to the returned array.
     */
    public static long[] table(DecorationRenderDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        long key = (long) descriptor.capability().ordinal() << 32 | descriptor.state().packed() & 0xFFFFFFFFL;
        long[] built = build(descriptor);
        long[] previous = INTERNED.putIfAbsent(key, built);
        return previous == null ? built : previous;
    }

    /** Uncached table; {@link #table} is the bake-time entry point. */
    static long[] build(DecorationRenderDescriptor descriptor) {
        long[] built = new long[FACES];
        for (int face = 0; face < SCREEN_UP.length; face++) {
            built[face] = decal(descriptor, face);
        }
        return built;
    }

    public static boolean isEmpty(long[] table) {
        for (long decal : table) {
            if (decal != NONE) return false;
        }
        return true;
    }

    /** Decal for one face ordinal (NORTH..DOWN); see the class documentation for the grammar. */
    static long decal(DecorationRenderDescriptor descriptor, int face) {
        SemanticState state = descriptor.state();
        DecorationVisualCue cue = descriptor.cue();
        int front = state.facing().ordinal() - 1;
        if (cue == DecorationVisualCue.FACING_MARKER) {
            if (descriptor.capability() == CapabilityId.GLAZED_TERRACOTTA) {
                return front < 0 ? NONE : decal(ARROW, arrowTurns(face, front), ARROW_COLOR);
            }
            // The anvil, the only other facing marker: the source pack's top-texture arrow, on the top
            // quad itself and not on the lower steps.
            return front < 0 || face != UP ? NONE
                    : onBoundaryOnly(decal(ARROW, quarterTurnsToward(UP, anvilArrow(front)), ARROW_COLOR));
        }
        if (cue == DecorationVisualCue.FACING_LEVEL_MARKER) {
            return frameIf(face == front, state.level() == 5 ? HONEY_FULL : DIRECTION);
        }
        if (cue == DecorationVisualCue.FACING_LIT_MARKER) {
            return frameIf(face == front, state.lit() ? LIT : UNLIT);
        }
        if (cue == DecorationVisualCue.MOUNT_FACE_MARKER) {
            int mount = state.mountFace() == SemanticState.MountFace.FLOOR ? DOWN
                    : state.mountFace() == SemanticState.MountFace.CEILING ? UP : -1;
            return face == mount ? decal(INSET_FRAME, 0, MOUNT) : frameIf(face == front, DIRECTION);
        }
        if (cue == DecorationVisualCue.FENCE_GATE_STATE_MARKER) {
            return frameIf(face == front, state.open() ? AXIS_Y : AXIS_X);
        }
        if (cue == DecorationVisualCue.AXIS_MARKER) {
            int axis = state.axis().ordinal();
            if (axis == 0) return NONE;
            if (FACE_AXIS[face] == axis) return decal(INSET_FRAME, 0, AXIS_COLORS[axis - 1]);
            // Rails run along glyph up: keep them parallel to the block axis.
            return decal(RAILS, FACE_AXIS[SCREEN_UP[face]] == axis ? 0 : 1, AXIS_COLORS[axis - 1]);
        }
        if (cue == DecorationVisualCue.SLAB_STATE_MARKER) {
            boolean vertical = face == UP || face == DOWN;
            return switch (state.slabType().ordinal()) {
                case 1 -> face == DOWN ? decal(EDGE_FRAME, 0, SLAB) : NONE;
                case 2 -> face == UP ? decal(EDGE_FRAME, 0, SLAB) : NONE;
                case 3 -> decal(vertical ? DOUBLE_FRAME : SEAM_FRAME, 0, SLAB);
                default -> NONE;
            };
        }
        if (cue == DecorationVisualCue.STAIRS_STATE_MARKER) {
            int shape = state.stairShape().ordinal();
            return shape == 0 ? NONE : decal(INSET_FRAME, 0, STAIR_COLORS[shape - 1]);
        }
        if (cue == DecorationVisualCue.TRAPDOOR_STATE_MARKER) {
            return front < 0 ? NONE : decal(INSET_FRAME, 0, TRAPDOOR_COLORS[front]);
        }
        return NONE; // Stained glass keeps only its solid-layer behavior.
    }

    /**
     * Source template_glazed_terracotta rotations: the arrow follows the placer's look direction
     * (opposite {@code facing}); the facing face shows it up and the opposite face down.
     */
    private static int arrowTurns(int face, int front) {
        int look = OPPOSITE[front];
        if (face == front) return 0;
        if (face == look) return 2;
        return quarterTurnsToward(face, look);
    }

    /**
     * World direction of the source pack's anvil arrow: its top texture points toward -u, and the
     * vanilla template_anvil top face turns u by 180 degrees, so the arrow follows the placer's look
     * direction, one counter-clockwise turn from {@code facing}. DecorationDecalOracle checks the
     * 180 degrees against the baked vanilla model's UVs on every supported line.
     */
    static int anvilArrow(int front) {
        return (front + 3) % 4;
    }

    /** Clockwise quarter turns pointing glyph up toward {@code direction}, or -1 if it is normal. */
    public static int quarterTurnsToward(int face, int direction) {
        if (direction == SCREEN_UP[face]) return 0;
        if (direction == SCREEN_RIGHT[face]) return 1;
        if (direction == OPPOSITE[SCREEN_UP[face]]) return 2;
        if (direction == OPPOSITE[SCREEN_RIGHT[face]]) return 3;
        return -1;
    }

    public static int screenUp(int face) {
        return SCREEN_UP[face];
    }

    public static int screenRight(int face) {
        return SCREEN_RIGHT[face];
    }

    public static int opposite(int face) {
        return OPPOSITE[face];
    }

    /** Coordinate that increases toward screen-right on {@code face}; screen-right is always horizontal. */
    public static float right(int face, float x, float y, float z) {
        int r = SCREEN_RIGHT[face];
        return VX[r] * x + VZ[r] * z;
    }

    /** Coordinate that increases toward screen-down on {@code face}. */
    public static float down(int face, float x, float y, float z) {
        int u = SCREEN_UP[face];
        return -(VX[u] * x + VY[u] * y + VZ[u] * z);
    }

    public static int normalX(int face) {
        return VX[face];
    }

    public static int normalY(int face) {
        return VY[face];
    }

    public static int normalZ(int face) {
        return VZ[face];
    }

    /** Glyph-space horizontal coordinate of face point (s, t) after clockwise quarter turns. */
    public static float glyphU(int quarterTurns, float s, float t) {
        return switch (quarterTurns) {
            case 0 -> s;
            case 1 -> t;
            case 2 -> 1.0F - s;
            case 3 -> 1.0F - t;
            default -> throw new IllegalArgumentException("quarter turns must be 0..3: " + quarterTurns);
        };
    }

    /** Glyph-space vertical coordinate of face point (s, t) after clockwise quarter turns. */
    public static float glyphV(int quarterTurns, float s, float t) {
        return switch (quarterTurns) {
            case 0 -> t;
            case 1 -> 1.0F - s;
            case 2 -> 1.0F - t;
            case 3 -> s;
            default -> throw new IllegalArgumentException("quarter turns must be 0..3: " + quarterTurns);
        };
    }

    /**
     * Maps {@code decal}'s glyph onto a quad's own bounds on {@code face} and lifts the quad by
     * {@link #SURFACE_OFFSET} along the face normal.
     *
     * <p>{@code positions} holds x, y, z of four vertices and is moved in place; {@code uvs} receives
     * u, v of each vertex inside the glyph strip {@code [u0, u1] x [v0, v1]}, one vertical cell per
     * glyph, staying a hair inside the cell so a neighbouring glyph is never sampled. A quad without
     * extent on the face changes nothing and returns false.</p>
     */
    public static boolean layout(int face, long decal, float[] positions, float u0, float u1, float v0, float v1,
            float[] uvs) {
        float minRight = Float.POSITIVE_INFINITY, maxRight = Float.NEGATIVE_INFINITY;
        float minDown = Float.POSITIVE_INFINITY, maxDown = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 4; vertex++) {
            float x = positions[3 * vertex], y = positions[3 * vertex + 1], z = positions[3 * vertex + 2];
            minRight = Math.min(minRight, right(face, x, y, z));
            maxRight = Math.max(maxRight, right(face, x, y, z));
            minDown = Math.min(minDown, down(face, x, y, z));
            maxDown = Math.max(maxDown, down(face, x, y, z));
        }
        float width = maxRight - minRight, height = maxDown - minDown;
        if (width < 1.0E-4F || height < 1.0E-4F || boundaryOnly(decal) && !onBoundary(face, positions)) {
            return false;
        }
        int turns = quarterTurns(decal);
        float du = u1 - u0;
        float cell = (v1 - v0) / GLYPHS;
        float glyphV0 = v0 + glyph(decal) * cell + cell / 1024.0F;
        float dv = cell - cell / 512.0F;
        for (int vertex = 0; vertex < 4; vertex++) {
            float x = positions[3 * vertex], y = positions[3 * vertex + 1], z = positions[3 * vertex + 2];
            float s = (right(face, x, y, z) - minRight) / width;
            float t = (down(face, x, y, z) - minDown) / height;
            uvs[2 * vertex] = u0 + glyphU(turns, s, t) * du;
            uvs[2 * vertex + 1] = glyphV0 + glyphV(turns, s, t) * dv;
            positions[3 * vertex] = x + VX[face] * SURFACE_OFFSET;
            positions[3 * vertex + 1] = y + VY[face] * SURFACE_OFFSET;
            positions[3 * vertex + 2] = z + VZ[face] * SURFACE_OFFSET;
        }
        return true;
    }

    /** Whether all four vertices lie in the block-boundary plane of {@code face}. */
    static boolean onBoundary(int face, float[] positions) {
        // A face normal has one non-zero component, +1 or -1: its plane is 1 or 0.
        float plane = (VX[face] + VY[face] + VZ[face] + 1) / 2;
        for (int vertex = 0; vertex < 4; vertex++) {
            float along = VX[face] != 0 ? positions[3 * vertex] : VY[face] != 0 ? positions[3 * vertex + 1]
                    : positions[3 * vertex + 2];
            if (Math.abs(along - plane) > 1.0E-4F) return false;
        }
        return true;
    }

    private static long frameIf(boolean condition, int argb) {
        return condition ? decal(INSET_FRAME, 0, argb) : NONE;
    }
}

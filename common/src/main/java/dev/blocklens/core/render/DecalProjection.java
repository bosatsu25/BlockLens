package dev.blocklens.core.render;

import java.util.Objects;

/**
 * Pure face-frame math that maps a decal glyph onto any axis-aligned model quad.
 *
 * <p>Each face has a fixed viewer frame: the screen-right and screen-up world directions an
 * upright player sees when looking at that face from outside (looking straight down at UP and
 * straight up at DOWN while facing north). Glyph coordinates are derived from vertex positions
 * in that frame, normalized to the quad's own bounds, so a glyph fills every quad of the active
 * model without depending on the model's UV layout. All methods are allocation-free.</p>
 */
public final class DecalProjection {
    /** Outward offset in blocks; above Nether overlays (0.03/16) and ore borders (0.02/16). */
    public static final float SURFACE_OFFSET = 1.0F / 256.0F;

    private DecalProjection() {
    }

    /** World direction that appears as screen-up on {@code face}. */
    public static DecorationQuadFace screenUp(DecorationQuadFace face) {
        return switch (requireFace(face)) {
            case UP -> DecorationQuadFace.NORTH;
            case DOWN -> DecorationQuadFace.SOUTH;
            default -> DecorationQuadFace.UP;
        };
    }

    /** World direction that appears as screen-right on {@code face}. */
    public static DecorationQuadFace screenRight(DecorationQuadFace face) {
        return switch (requireFace(face)) {
            case UP, DOWN, SOUTH -> DecorationQuadFace.EAST;
            case NORTH -> DecorationQuadFace.WEST;
            case EAST -> DecorationQuadFace.NORTH;
            case WEST -> DecorationQuadFace.SOUTH;
            case NONE -> throw new IllegalStateException("unreachable");
        };
    }

    /** Coordinate that increases toward screen-right on {@code face}. */
    public static float right(DecorationQuadFace face, float x, float y, float z) {
        return switch (requireFace(face)) {
            case UP, DOWN, SOUTH -> x;
            case NORTH -> -x;
            case EAST -> -z;
            case WEST -> z;
            case NONE -> throw new IllegalStateException("unreachable");
        };
    }

    /** Coordinate that increases toward screen-down on {@code face}. */
    public static float down(DecorationQuadFace face, float x, float y, float z) {
        return switch (requireFace(face)) {
            case UP -> z;
            case DOWN -> -z;
            default -> -y;
        };
    }

    /**
     * Clockwise quarter turns that make glyph-up point toward {@code direction} on {@code face},
     * or {@code -1} when the direction is normal to the face.
     */
    public static int quarterTurnsToward(DecorationQuadFace face, DecorationQuadFace direction) {
        Objects.requireNonNull(direction, "direction");
        DecorationQuadFace up = screenUp(face);
        DecorationQuadFace right = screenRight(face);
        if (direction == up) return 0;
        if (direction == right) return 1;
        if (direction == opposite(up)) return 2;
        if (direction == opposite(right)) return 3;
        return -1;
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

    public static int normalX(DecorationQuadFace face) {
        return switch (requireFace(face)) {
            case EAST -> 1;
            case WEST -> -1;
            default -> 0;
        };
    }

    public static int normalY(DecorationQuadFace face) {
        return switch (requireFace(face)) {
            case UP -> 1;
            case DOWN -> -1;
            default -> 0;
        };
    }

    public static int normalZ(DecorationQuadFace face) {
        return switch (requireFace(face)) {
            case SOUTH -> 1;
            case NORTH -> -1;
            default -> 0;
        };
    }

    public static DecorationQuadFace opposite(DecorationQuadFace face) {
        return switch (requireFace(face)) {
            case NORTH -> DecorationQuadFace.SOUTH;
            case SOUTH -> DecorationQuadFace.NORTH;
            case EAST -> DecorationQuadFace.WEST;
            case WEST -> DecorationQuadFace.EAST;
            case UP -> DecorationQuadFace.DOWN;
            case DOWN -> DecorationQuadFace.UP;
            case NONE -> throw new IllegalStateException("unreachable");
        };
    }

    private static DecorationQuadFace requireFace(DecorationQuadFace face) {
        if (Objects.requireNonNull(face, "face") == DecorationQuadFace.NONE) {
            throw new IllegalArgumentException("a decal needs a real block face");
        }
        return face;
    }
}

package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import java.util.Objects;

/**
 * Vanilla targets present in the supplied Glowing Ores border-pack CTM inventory.
 * The border is a clean-room exposed-face overlay, not a copied OptiFine CTM tile.
 * Existing individual ore highlight settings own its enable/disable lifecycle.
 */
public final class ResourceBorderPolicy {
    private ResourceBorderPolicy() { }

    /** The connected-border sprite is a 16 x 16 sheet with one tile per eight-bit neighbor mask. */
    public static final int SHEET_TILES = 16;

    // Neighbor bit -> steps along the face's screen-right and screen-down directions.
    private static final int[] NEIGHBOR_RIGHT = {0, 1, 0, -1, 1, 1, -1, -1};
    private static final int[] NEIGHBOR_DOWN = {-1, 0, 1, 0, -1, 1, 1, -1};
    // [face][3 * bit + axis]: block offset of each neighbor, faces by DecorationQuadFace ordinal.
    private static final int[][] NEIGHBOR_OFFSETS = new int[6][24];

    static {
        for (int face = 0; face < 6; face++) {
            int right = DecorationDecals.screenRight(face);
            int down = DecorationDecals.opposite(DecorationDecals.screenUp(face));
            for (int bit = 0; bit < 8; bit++) {
                NEIGHBOR_OFFSETS[face][3 * bit] = NEIGHBOR_RIGHT[bit] * DecorationDecals.normalX(right)
                        + NEIGHBOR_DOWN[bit] * DecorationDecals.normalX(down);
                NEIGHBOR_OFFSETS[face][3 * bit + 1] = NEIGHBOR_RIGHT[bit] * DecorationDecals.normalY(right)
                        + NEIGHBOR_DOWN[bit] * DecorationDecals.normalY(down);
                NEIGHBOR_OFFSETS[face][3 * bit + 2] = NEIGHBOR_RIGHT[bit] * DecorationDecals.normalZ(right)
                        + NEIGHBOR_DOWN[bit] * DecorationDecals.normalZ(down);
            }
        }
    }

    /**
     * Block offset along {@code axis} (0 = x, 1 = y, 2 = z) from a block to neighbor {@code bit} of
     * its {@code face} (DecorationQuadFace ordinal), in {@link #connectedBorderMask}'s bit order:
     * screen-up, right, down, left, then the diagonals clockwise from up-right.
     */
    public static int neighborOffset(int face, int bit, int axis) {
        return NEIGHBOR_OFFSETS[face][3 * bit + axis];
    }

    /** Sheet column of the tile for {@code neighbors}; the row is {@link #sheetRow}. */
    public static int sheetColumn(int neighbors) {
        return neighbors % SHEET_TILES;
    }

    public static int sheetRow(int neighbors) {
        return neighbors / SHEET_TILES;
    }

    /**
     * Corner index 0..3 of a vertex that lies on a corner of the unit-cube face {@code face}, or -1.
     * A quad covers the whole face when its four vertices yield all four corners.
     */
    public static int unitCubeCorner(int face, float x, float y, float z) {
        if ((x != 0 && x != 1) || (y != 0 && y != 1) || (z != 0 && z != 1)) {
            return -1;
        }
        int nx = DecorationDecals.normalX(face), ny = DecorationDecals.normalY(face);
        float plane = nx != 0 ? x : ny != 0 ? y : z;
        if (plane != (nx + ny + DecorationDecals.normalZ(face) > 0 ? 1 : 0)) {
            return -1;
        }
        return nx != 0 ? (int) y + 2 * (int) z : ny != 0 ? (int) x + 2 * (int) z : (int) x + 2 * (int) y;
    }

    /** Cardinal bits0..3 and following diagonal bits4..7, clockwise in face UV space. */
    public static int connectedBorderMask(int neighbors) {
        if (neighbors < 0 || neighbors > 255) throw new IllegalArgumentException("Expected eight neighbor bits");
        int visible = ~neighbors & 15;
        for (int side = 0; side < 4; side++) {
            int adjacent = (1 << side) | (1 << ((side + 1) % 4)) | (1 << (side + 4));
            if ((neighbors & adjacent) != adjacent) visible |= 1 << (side + 4);
        }
        return visible;
    }

    public static boolean supports(CapabilityId capability) {
        return switch (Objects.requireNonNull(capability, "capability")) {
            case COAL_ORE, DEEPSLATE_COAL_ORE,
                    COPPER_ORE, DEEPSLATE_COPPER_ORE,
                    DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
                    EMERALD_ORE, DEEPSLATE_EMERALD_ORE,
                    GOLD_ORE, DEEPSLATE_GOLD_ORE,
                    IRON_ORE, DEEPSLATE_IRON_ORE,
                    LAPIS_ORE, DEEPSLATE_LAPIS_ORE,
                    REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE,
                    NETHER_GOLD_ORE, NETHER_QUARTZ_ORE -> true;
            default -> false;
        };
    }
}

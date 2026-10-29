package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.*;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigCodec;
import dev.blocklens.core.CapabilityId;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

final class ResourceBorderPolicyTest {
    private static final EnumSet<CapabilityId> ORES = EnumSet.of(
            CapabilityId.COAL_ORE, CapabilityId.DEEPSLATE_COAL_ORE,
            CapabilityId.COPPER_ORE, CapabilityId.DEEPSLATE_COPPER_ORE,
            CapabilityId.DIAMOND_ORE, CapabilityId.DEEPSLATE_DIAMOND_ORE,
            CapabilityId.EMERALD_ORE, CapabilityId.DEEPSLATE_EMERALD_ORE,
            CapabilityId.GOLD_ORE, CapabilityId.DEEPSLATE_GOLD_ORE,
            CapabilityId.IRON_ORE, CapabilityId.DEEPSLATE_IRON_ORE,
            CapabilityId.LAPIS_ORE, CapabilityId.DEEPSLATE_LAPIS_ORE,
            CapabilityId.REDSTONE_ORE, CapabilityId.DEEPSLATE_REDSTONE_ORE,
            CapabilityId.NETHER_GOLD_ORE, CapabilityId.NETHER_QUARTZ_ORE);

    @Test void onlyReviewedVanillaOresReceiveTheCleanRoomBorder() {
        assertEquals(18, ORES.size());
        for (CapabilityId capability : CapabilityId.values()) {
            assertEquals(ORES.contains(capability), ResourceBorderPolicy.supports(capability),
                    capability.sourceKey());
            if (ORES.contains(capability)) {
                assertEquals(1, ResourceTargetCatalog.targets(capability).size());
                assertEquals(0xFF, ResourceHighlightCue.forCapability(capability).accentArgb() >>> 24);
            }
        }
        assertFalse(ResourceBorderPolicy.supports(CapabilityId.ANCIENT_DEBRIS));
        assertFalse(ResourceBorderPolicy.supports(CapabilityId.OBSIDIAN));
        assertFalse(ResourceBorderPolicy.supports(CapabilityId.CRYING_OBSIDIAN));
        assertThrows(NullPointerException.class, () -> ResourceBorderPolicy.supports(null));
    }

    @Test void originalOreTogglesStayIndependentAfterConfigRoundTrip() {
        var defaults = BlockLensConfig.defaults();
        for (CapabilityId selected : ORES) {
            assertFalse(defaults.isEnabled(selected));
            var enabled = defaults.withEnabled(selected, true);
            assertEquals(defaults.enabledMask() | (1L << selected.bitPosition()),
                    enabled.enabledMask());
            var restored = BlockLensConfigCodec.decode(BlockLensConfigCodec.encode(enabled));
            assertEquals(enabled.enabledMask(), restored.enabledMask());
            for (CapabilityId other : ORES) {
                assertEquals(selected == other, restored.isEnabled(other), other.sourceKey());
            }
        }
        assertEquals(61, CapabilityId.values().length);
    }
    @Test void joinedEdgesAndMissingDiagonalsRetainOnlyRequiredCorners() {
        assertEquals(255, ResourceBorderPolicy.connectedBorderMask(0));
        assertEquals(0, ResourceBorderPolicy.connectedBorderMask(255));
        assertEquals(240, ResourceBorderPolicy.connectedBorderMask(15));
        assertEquals(252, ResourceBorderPolicy.connectedBorderMask(3));
        assertEquals(236, ResourceBorderPolicy.connectedBorderMask(3 | 16));
        assertEquals(255, ResourceBorderPolicy.connectedBorderMask(240));
    }

    @Test void allEightNeighborCasesProduceExactly47CanonicalBoundaries() {
        var shapes = new java.util.HashSet<Integer>();
        for (int neighbors = 0; neighbors < 256; neighbors++) {
            int border = ResourceBorderPolicy.connectedBorderMask(neighbors);
            shapes.add(border);
            for (int side = 0; side < 4; side++) {
                assertEquals((neighbors & (1 << side)) == 0, (border & (1 << side)) != 0);
                boolean corner = (neighbors & (1 << side)) == 0
                        || (neighbors & (1 << ((side + 1) % 4))) == 0
                        || (neighbors & (1 << (side + 4))) == 0;
                assertEquals(corner, (border & (1 << (side + 4))) != 0);
            }
        }
        assertEquals(47, shapes.size());
    }

    @Test void invalidNeighborMasksAreRejected() {
        for (int mask : new int[] {-1, 256, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class, () -> ResourceBorderPolicy.connectedBorderMask(mask));
        }
    }

    @Test void connectionsAreIndependentAndNeverRegisterAsAnOreMaterial() {
        var id=CapabilityId.CONNECTED_ORE_EDGES;
        assertEquals(62,id.bitPosition());assertEquals(CapabilityId.Category.RESOURCE,id.category());
        assertFalse(id.defaultEnabled());assertFalse(id.isResourceHighlight());assertFalse(ResourceBorderPolicy.supports(id));
        var defaults=BlockLensConfig.defaults();var enabled=defaults.withEnabled(id,true);
        assertEquals(defaults.enabledMask()|(1L<<62),enabled.enabledMask());
        assertEquals(enabled,BlockLensConfigCodec.decode(BlockLensConfigCodec.encode(enabled)));
        for(var ore:ORES)assertFalse(enabled.isEnabled(ore));
        assertFalse(new dev.blocklens.core.extension.OreExtensionCatalog().register(1,"example:ore",id.sourceKey()));
    }

    // Faces by DecorationQuadFace ordinal; steps as Minecraft's Direction.
    private static final int N = 0, E = 1, S = 2, W = 3, U = 4, D = 5;
    private static final int[][] STEP = {{0, 0, -1}, {1, 0, 0}, {0, 0, 1}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}};

    @Test
    void neighborOffsetsMatchTheDirectionTableTheRendererUsedToCarry() {
        // MinecraftDecorationModel's former table: u = NORTH->WEST, EAST->NORTH, WEST->SOUTH, else EAST;
        // v = UP->SOUTH, DOWN->NORTH, else DOWN; neighbor = u * U[bit] + v * V[bit].
        int[] u = {W, N, E, S, E, E};
        int[] v = {D, D, D, D, S, N};
        int[] stepsU = {0, 1, 0, -1, 1, 1, -1, -1};
        int[] stepsV = {-1, 0, 1, 0, -1, 1, 1, -1};
        for (int face = 0; face < 6; face++) {
            for (int bit = 0; bit < 8; bit++) {
                for (int axis = 0; axis < 3; axis++) {
                    int expected = STEP[u[face]][axis] * stepsU[bit] + STEP[v[face]][axis] * stepsV[bit];
                    assertEquals(expected, ResourceBorderPolicy.neighborOffset(face, bit, axis),
                            "face " + face + " bit " + bit + " axis " + axis);
                }
            }
        }
        // On the north face, screen-up is up and screen-right is west.
        assertArrayEquals(new int[] {0, 1, 0}, offset(N, 0));
        assertArrayEquals(new int[] {-1, 0, 0}, offset(N, 1));
        assertArrayEquals(new int[] {-1, 1, 0}, offset(N, 4));
    }

    private static int[] offset(int face, int bit) {
        return new int[] {ResourceBorderPolicy.neighborOffset(face, bit, 0),
                ResourceBorderPolicy.neighborOffset(face, bit, 1), ResourceBorderPolicy.neighborOffset(face, bit, 2)};
    }

    @Test
    void sheetTilesFollowTheNeighborMask() {
        assertEquals(16, ResourceBorderPolicy.SHEET_TILES);
        for (int mask = 0; mask < 256; mask++) {
            assertEquals(mask, ResourceBorderPolicy.sheetRow(mask) * 16 + ResourceBorderPolicy.sheetColumn(mask));
            assertTrue(ResourceBorderPolicy.sheetColumn(mask) < 16 && ResourceBorderPolicy.sheetRow(mask) < 16);
        }
        assertEquals(5, ResourceBorderPolicy.sheetColumn(37));
        assertEquals(2, ResourceBorderPolicy.sheetRow(37));
    }

    @Test
    void unitCubeCornersMatchTheRendererCheckOnEveryLatticePoint() {
        float[] values = {-1, 0, 0.5f, 1, 2};
        for (int face = 0; face < 6; face++) {
            int corners = 0;
            for (float x : values) {
                for (float y : values) {
                    for (float z : values) {
                        int corner = ResourceBorderPolicy.unitCubeCorner(face, x, y, z);
                        assertEquals(formerCorner(face, x, y, z), corner, "face " + face + " at " + x + "," + y + "," + z);
                        if (corner >= 0) {
                            corners |= 1 << corner;
                        }
                    }
                }
            }
            assertEquals(15, corners, "each face has four distinct corners");
        }
        assertEquals(-1, ResourceBorderPolicy.unitCubeCorner(N, 0, 0, 1), "south plane is not the north face");
        assertEquals(3, ResourceBorderPolicy.unitCubeCorner(E, 1, 1, 1));
    }

    /** The check MinecraftDecorationModel ran per vertex with Direction axis and axis direction. */
    private static int formerCorner(int face, float x, float y, float z) {
        if ((x != 0 && x != 1) || (y != 0 && y != 1) || (z != 0 && z != 1)) return -1;
        int axis = face == E || face == W ? 0 : face == U || face == D ? 1 : 2;
        boolean positive = face == E || face == S || face == U;
        float plane = axis == 0 ? x : axis == 1 ? y : z;
        if (plane != (positive ? 1 : 0)) return -1;
        return axis == 0 ? (int) y + 2 * (int) z : axis == 1 ? (int) x + 2 * (int) z : (int) x + 2 * (int) y;
    }
}

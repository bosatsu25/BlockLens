package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.DecorationLines.Line;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.SemanticState.Facing;
import dev.blocklens.core.state.SemanticState.Half;
import dev.blocklens.core.state.SemanticState.StairShape;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

final class DecorationLinesTest {
    private static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3, UP = 4, DOWN = 5;
    private static final Facing[] FACINGS = {Facing.NORTH, Facing.EAST, Facing.SOUTH, Facing.WEST};
    private static final StairShape[] SHAPES = {StairShape.STRAIGHT, StairShape.INNER_LEFT,
            StairShape.INNER_RIGHT, StairShape.OUTER_LEFT, StairShape.OUTER_RIGHT};
    private static final String BLANK = "................";
    private static final String GREEN_RIGHT = "...............G";
    private static final String GREEN_LEFT = "G...............";

    // Uncached, so no interned result from an earlier test can stand in for the code under test.
    private static Line[] stairs(Facing facing, Half half, StairShape shape) {
        return DecorationLines.build(DecorationRenderDescriptor.of(CapabilityId.STAIRS,
                SemanticState.stairs(facing, half, shape)));
    }

    private static Line[] trapdoor(Facing facing, Half half, boolean open) {
        return DecorationLines.build(DecorationRenderDescriptor.of(CapabilityId.TRAPDOOR,
                SemanticState.trapdoor(facing, half, open)));
    }

    private static String[] repeat(String row, int count) {
        String[] rows = new String[count];
        java.util.Arrays.fill(rows, row);
        return rows;
    }

    private static String[] rows(Object... parts) {
        List<String> rows = new ArrayList<>();
        for (Object part : parts) {
            if (part instanceof String row) rows.add(row);
            else for (String row : (String[]) part) rows.add(row);
        }
        assertEquals(16, rows.size());
        return rows.toArray(String[]::new);
    }

    /**
     * The plane as seen from outside, top row first: G green, A amber, - dashed, M metal. Each cell
     * belongs to at most one line.
     */
    private static String[] raster(Line[] lines, int face, int depth) {
        char[][] grid = new char[16][16];
        for (char[] row : grid) java.util.Arrays.fill(row, '.');
        for (Line line : lines) {
            if (line.face() != face || line.depth() != depth) continue;
            char c = line.dashed() ? '-' : line.argb() == DecorationLines.GREEN ? 'G'
                    : line.argb() == DecorationLines.AMBER ? 'A' : line.argb() == DecorationLines.METAL ? 'M' : '?';
            assertTrue(line.dashed() ? line.argb() == DecorationLines.AMBER : c != '?', line.toString());
            for (int t = line.bottom(); t < line.top(); t++) {
                for (int s = line.left(); s < line.right(); s++) {
                    assertEquals('.', grid[15 - t][s], "lines overlap at " + s + "," + t + " in " + line);
                    grid[15 - t][s] = c;
                }
            }
        }
        String[] rows = new String[16];
        for (int i = 0; i < 16; i++) rows[i] = new String(grid[i]);
        return rows;
    }

    private static Set<String> planes(Line[] lines) {
        Set<String> planes = new TreeSet<>();
        for (Line line : lines) planes.add(line.face() + "@" + line.depth());
        return planes;
    }

    @Test
    void straightStairsOutlineTheBackAndBaseAndDashTheHiddenStep() {
        Line[] lines = stairs(Facing.EAST, Half.BOTTOM, StairShape.STRAIGHT);
        assertEquals(13, lines.length, "one quad per merged run");
        assertEquals(Set.of("0@0", "1@0", "2@0", "4@0", "5@0"), planes(lines));
        assertArrayEquals(rows("GGGGGGGGGGGGGGGG", repeat("G..............G", 6), "G--------------G",
                repeat("G..............G", 7), "GGGGGGGGGGGGGGGG"), raster(lines, EAST, 0));
        assertArrayEquals(rows("AAAAAAAAAAAAAAAG", repeat("A......-.......G", 14), "AAAAAAAAAAAAAAAG"),
                raster(lines, DOWN, 0));
        assertArrayEquals(repeat(GREEN_RIGHT, 16), raster(lines, UP, 0), "the step top meets the back face");
        assertArrayEquals(repeat(GREEN_LEFT, 16), raster(lines, NORTH, 0));
        assertArrayEquals(repeat(GREEN_RIGHT, 16), raster(lines, SOUTH, 0));
        assertArrayEquals(repeat(BLANK, 16), raster(lines, WEST, 0), "the front shows the plain block");
        assertArrayEquals(repeat(BLANK, 16), raster(lines, UP, 8), "the lower tread never meets the back face");
    }

    @Test
    void innerStairsDashAnLOnEveryFullFace() {
        Line[] lines = stairs(Facing.EAST, Half.BOTTOM, StairShape.INNER_LEFT);
        assertEquals(20, lines.length);
        assertArrayEquals(rows("GGGGGGGGGGGGGGGG", repeat("G......-.......G", 6), "G-------.......G",
                repeat("G..............G", 7), "GGGGGGGGGGGGGGGG"), raster(lines, EAST, 0));
        assertArrayEquals(rows("GAAAAAAAAAAAAAAA", repeat("G.......-......A", 6), "G.......-------A",
                repeat("G..............A", 7), "GAAAAAAAAAAAAAAA"), raster(lines, NORTH, 0),
                "the left side is full on an inner-left stair");
        assertArrayEquals(rows("AAAAAAAAAAAAAAAG", repeat("A......-.......G", 6), "A-------.......G",
                repeat("A..............G", 7), "AAAAAAAAAAAAAAAG"), raster(lines, DOWN, 0));
        assertArrayEquals(repeat(GREEN_RIGHT, 16), raster(lines, SOUTH, 0));
    }

    @Test
    void outerStairsOutlineTheBackSilhouetteAndDashOnlyTheBase() {
        Line[] lines = stairs(Facing.EAST, Half.BOTTOM, StairShape.OUTER_LEFT);
        assertEquals(17, lines.length);
        assertArrayEquals(rows("........GGGGGGGG", repeat("........G......G", 7), "GGGGGGGGG......G",
                repeat("G..............G", 6), "GGGGGGGGGGGGGGGG"), raster(lines, EAST, 0),
                "the silhouette closes its inner corner and hides nothing behind a partial face");
        assertArrayEquals(rows("AAAAAAAAAAAAAAAG", repeat("A..............G", 6), "A.......-------G",
                repeat("A......-.......G", 7), "AAAAAAAAAAAAAAAG"), raster(lines, DOWN, 0));
        assertArrayEquals(rows(repeat(BLANK, 8), repeat(GREEN_RIGHT, 8)), raster(lines, SOUTH, 0));
        assertArrayEquals(rows(repeat(GREEN_RIGHT, 8), repeat(BLANK, 8)), raster(lines, SOUTH, 8),
                "the step's inner side is a surface of its own");
        assertArrayEquals(rows(repeat(GREEN_RIGHT, 8), repeat(BLANK, 8)), raster(lines, UP, 0));
        assertArrayEquals(rows(repeat(BLANK, 8), repeat(GREEN_RIGHT, 8)), raster(lines, UP, 8));
        assertArrayEquals(repeat(GREEN_LEFT, 16), raster(lines, NORTH, 0));
    }

    @Test
    void stairsTurnWithTheirFacingMirrorBetweenLeftAndRightAndFlipBetweenHalves() {
        for (StairShape shape : SHAPES) {
            for (Half half : List.of(Half.BOTTOM, Half.TOP)) {
                for (int i = 0; i < FACINGS.length; i++) {
                    Set<String> turned = cells(stairs(FACINGS[(i + 1) % 4], half, shape));
                    assertEquals(turned, rotated(cells(stairs(FACINGS[i], half, shape))), shape + "/" + half);
                }
            }
        }
        StairShape[] mirrored = {StairShape.STRAIGHT, StairShape.INNER_RIGHT, StairShape.INNER_LEFT,
                StairShape.OUTER_RIGHT, StairShape.OUTER_LEFT};
        for (int i = 0; i < SHAPES.length; i++) {
            for (Half half : List.of(Half.BOTTOM, Half.TOP)) {
                assertEquals(cells(stairs(Facing.EAST, half, mirrored[i])),
                        mirroredZ(cells(stairs(Facing.EAST, half, SHAPES[i]))), SHAPES[i] + "/" + half);
            }
            assertEquals(cells(stairs(Facing.NORTH, Half.TOP, SHAPES[i])),
                    flipped(cells(stairs(Facing.NORTH, Half.BOTTOM, SHAPES[i]))), SHAPES[i].toString());
        }
    }

    @Test
    void everyStairStateHasDisjointLinesAndOnlyFullFacesAreDashed() {
        for (Facing facing : FACINGS) {
            for (Half half : List.of(Half.BOTTOM, Half.TOP)) {
                for (StairShape shape : SHAPES) {
                    Line[] lines = stairs(facing, half, shape);
                    int dashedPlanes = 0;
                    for (String plane : planes(lines)) {
                        int face = Integer.parseInt(plane.substring(0, 1));
                        int depth = Integer.parseInt(plane.substring(2));
                        String[] raster = raster(lines, face, depth);
                        boolean dashed = String.join("", raster).contains("-");
                        if (dashed) {
                            dashedPlanes++;
                            assertEquals(0, depth, "hidden edges show only through boundary faces");
                            assertTrue(raster[0].indexOf('.') < 0 && raster[15].indexOf('.') < 0,
                                    "a dashed face is a full outlined square");
                        }
                    }
                    int expected = shape == StairShape.STRAIGHT ? 2
                            : shape == StairShape.INNER_LEFT || shape == StairShape.INNER_RIGHT ? 3 : 1;
                    assertEquals(expected, dashedPlanes, facing + "/" + half + "/" + shape);
                }
            }
        }
    }

    @Test
    void trapdoorsMarkHingesOnThePivotEdgeAndTheHandleOppositeIt() {
        Line[] closed = trapdoor(Facing.NORTH, Half.BOTTOM, false);
        assertEquals(9, closed.length);
        assertEquals(Set.of("0@0", "2@0", "4@13", "5@0"), planes(closed));
        String handle = ".....MMMMMM.....";
        String hinges = "...MM......MM...";
        assertArrayEquals(rows(handle, handle, repeat(BLANK, 13), hinges), raster(closed, UP, 13),
                "a north-facing trapdoor pivots on its south edge");
        assertArrayEquals(rows(hinges, repeat(BLANK, 13), handle, handle), raster(closed, DOWN, 0));
        assertArrayEquals(rows(repeat(BLANK, 13), repeat(hinges, 3)), raster(closed, SOUTH, 0));
        assertArrayEquals(rows(repeat(BLANK, 13), repeat(handle, 3)), raster(closed, NORTH, 0));

        Line[] open = trapdoor(Facing.NORTH, Half.BOTTOM, true);
        assertEquals(Set.of("0@13", "2@0", "4@0", "5@0"), planes(open));
        assertArrayEquals(rows(handle, handle, repeat(BLANK, 13), hinges), raster(open, SOUTH, 0),
                "an open trapdoor pivots on its bottom edge");
        assertArrayEquals(rows(handle, handle, repeat(BLANK, 13), hinges), raster(open, NORTH, 13));
        assertArrayEquals(rows(repeat(hinges, 3), repeat(BLANK, 13)), raster(open, DOWN, 0));
        assertArrayEquals(rows(repeat(BLANK, 13), repeat(handle, 3)), raster(open, UP, 0));

        Line[] top = trapdoor(Facing.NORTH, Half.TOP, false);
        assertArrayEquals(rows(handle, handle, repeat(BLANK, 13), hinges), raster(top, UP, 0));
        assertArrayEquals(rows(repeat(hinges, 3), repeat(BLANK, 13)), raster(top, SOUTH, 0));
        for (boolean isOpen : new boolean[] {false, true}) {
            assertEquals(cells(trapdoor(Facing.NORTH, Half.TOP, isOpen)),
                    flipped(cells(trapdoor(Facing.NORTH, Half.BOTTOM, isOpen))), "open=" + isOpen);
            for (Half half : List.of(Half.BOTTOM, Half.TOP)) {
                for (int i = 0; i < FACINGS.length; i++) {
                    assertEquals(cells(trapdoor(FACINGS[(i + 1) % 4], half, isOpen)),
                            rotated(cells(trapdoor(FACINGS[i], half, isOpen))), half + "/" + isOpen);
                }
            }
        }
    }

    @Test
    void onlyStairsAndTrapdoorsWithAFullStateDrawLinesAndLinesAreInterned() {
        var descriptor = DecorationRenderDescriptor.of(CapabilityId.STAIRS,
                SemanticState.stairs(Facing.SOUTH, Half.TOP, StairShape.OUTER_RIGHT));
        Line[] lines = DecorationLines.lines(descriptor);
        assertSame(lines, DecorationLines.lines(DecorationRenderDescriptor.of(CapabilityId.STAIRS,
                SemanticState.stairs(Facing.SOUTH, Half.TOP, StairShape.OUTER_RIGHT))));
        assertNotSame(lines, DecorationLines.lines(DecorationRenderDescriptor.of(CapabilityId.STAIRS,
                SemanticState.stairs(Facing.SOUTH, Half.TOP, StairShape.OUTER_LEFT))));
        Line[] fresh = DecorationLines.build(descriptor);
        assertNotSame(lines, fresh);
        assertArrayEquals(lines, fresh);
        assertTrue(lines.length > 0);
        Line[] trapdoor = DecorationLines.lines(DecorationRenderDescriptor.of(CapabilityId.TRAPDOOR,
                SemanticState.trapdoor(Facing.WEST, Half.BOTTOM, false)));
        assertArrayEquals(trapdoor(Facing.WEST, Half.BOTTOM, false), trapdoor);
        // The interning key keeps the capability: one state read as stairs and as a trapdoor differs.
        SemanticState shared = SemanticState.stairs(Facing.WEST, Half.TOP, StairShape.INNER_RIGHT);
        Line[] asStairs = DecorationLines.lines(DecorationRenderDescriptor.of(CapabilityId.STAIRS, shared));
        Line[] asTrapdoor = DecorationLines.lines(DecorationRenderDescriptor.of(CapabilityId.TRAPDOOR, shared));
        assertArrayEquals(DecorationLines.build(DecorationRenderDescriptor.of(CapabilityId.STAIRS, shared)), asStairs);
        assertArrayEquals(DecorationLines.build(DecorationRenderDescriptor.of(CapabilityId.TRAPDOOR, shared)), asTrapdoor);
        assertEquals(0, DecorationLines.build(DecorationRenderDescriptor.of(CapabilityId.STAIRS,
                SemanticState.empty())).length);
        assertEquals(0, stairs(Facing.SOUTH, Half.NONE, StairShape.STRAIGHT).length);
        assertEquals(0, stairs(Facing.SOUTH, Half.TOP, StairShape.NONE).length);
        assertEquals(0, stairs(Facing.NONE, Half.TOP, StairShape.STRAIGHT).length);
        assertEquals(0, trapdoor(Facing.NONE, Half.TOP, true).length);
        assertEquals(0, trapdoor(Facing.EAST, Half.NONE, true).length);
        assertEquals(0, DecorationLines.lines(DecorationRenderDescriptor.of(CapabilityId.TRAPDOOR,
                SemanticState.trapdoor(Facing.NONE, Half.TOP, true))).length);
        assertEquals(0, DecorationLines.lines(DecorationRenderDescriptor.of(CapabilityId.SLABS,
                SemanticState.stairs(Facing.SOUTH, Half.TOP, StairShape.STRAIGHT))).length);
        assertThrows(NullPointerException.class, () -> DecorationLines.lines(null));
    }

    @Test
    void lineRectanglesMustLieInsideOneFacePlane() {
        new Line(UP, 15, 0, 0, 16, 16, false, DecorationLines.GREEN);
        int[][] invalid = {{-1, 0, 0, 0, 1, 1}, {6, 0, 0, 0, 1, 1}, {UP, -1, 0, 0, 1, 1}, {UP, 16, 0, 0, 1, 1},
                {UP, 0, -1, 0, 1, 1}, {UP, 0, 0, -1, 1, 1}, {UP, 0, 1, 0, 1, 1}, {UP, 0, 0, 1, 1, 1},
                {UP, 0, 0, 0, 17, 1}, {UP, 0, 0, 0, 1, 17}};
        for (int[] args : invalid) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Line(args[0], args[1], args[2], args[3], args[4], args[5], false, DecorationLines.GREEN),
                    java.util.Arrays.toString(args));
        }
    }

    @Test
    void verticesWindCounterClockwiseFromOutsideAndLiftOffTheFace() {
        float[] positions = new float[12];
        for (int face = 0; face < 6; face++) {
            for (int depth : new int[] {0, 8, 13}) {
                Line line = new Line(face, depth, 2, 3, 9, 7, false, DecorationLines.GREEN);
                DecorationLines.vertices(line, positions);
                float[] e1 = {positions[3] - positions[0], positions[4] - positions[1], positions[5] - positions[2]};
                float[] e2 = {positions[6] - positions[3], positions[7] - positions[4], positions[8] - positions[5]};
                float[] normal = {e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]};
                int[] outward = {DecorationDecals.normalX(face), DecorationDecals.normalY(face), DecorationDecals.normalZ(face)};
                for (int axis = 0; axis < 3; axis++) {
                    assertEquals(outward[axis], (int) Math.signum(normal[axis]), "winding of face " + face);
                }
                for (int vertex = 0; vertex < 4; vertex++) {
                    float along = DecorationDecals.normalX(face) * positions[3 * vertex]
                            + DecorationDecals.normalY(face) * positions[3 * vertex + 1]
                            + DecorationDecals.normalZ(face) * positions[3 * vertex + 2];
                    float plane = DecorationDecals.normalX(face) + DecorationDecals.normalY(face)
                            + DecorationDecals.normalZ(face) > 0 ? 1 - depth / 16.0F : -(depth / 16.0F);
                    assertEquals(plane + DecorationDecals.SURFACE_OFFSET, along, 1.0E-6F, "lift of face " + face);
                }
                // Top-left, bottom-left, bottom-right, top-right in the face's viewer frame.
                assertEquals(2 / 16.0F, DecorationDecals.right(face, positions[3], positions[4], positions[5])
                        - minRight(face), 1.0E-6F);
                assertEquals(9 / 16.0F, DecorationDecals.right(face, positions[6], positions[7], positions[8])
                        - minRight(face), 1.0E-6F);
                assertEquals(1 - 7 / 16.0F, DecorationDecals.down(face, positions[0], positions[1], positions[2])
                        - minDown(face), 1.0E-6F);
                assertEquals(1 - 3 / 16.0F, DecorationDecals.down(face, positions[3], positions[4], positions[5])
                        - minDown(face), 1.0E-6F);
            }
        }
        Line east = new Line(EAST, 0, 0, 0, 16, 1, false, DecorationLines.GREEN);
        DecorationLines.vertices(east, positions);
        float lifted = 1 + DecorationDecals.SURFACE_OFFSET;
        assertArrayEquals(new float[] {lifted, 1 / 16.0F, 1, lifted, 0, 1, lifted, 0, 0, lifted, 1 / 16.0F, 0},
                positions, 1.0E-6F, "east: screen right is north, so the left edge sits at z = 1");
    }

    private static float minRight(int face) {
        float min = Float.POSITIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            min = Math.min(min, DecorationDecals.right(face, corner & 1, corner >> 1 & 1, corner >> 2 & 1));
        }
        return min;
    }

    private static float minDown(int face) {
        float min = Float.POSITIVE_INFINITY;
        for (int corner = 0; corner < 8; corner++) {
            min = Math.min(min, DecorationDecals.down(face, corner & 1, corner >> 1 & 1, corner >> 2 & 1));
        }
        return min;
    }

    @Test
    void solidLinesSampleTheOpaqueHalfAndDashesKeepOnePhasePerBlock() {
        float u0 = 0.25F, u1 = 0.5F, v0 = 0.125F, v1 = 0.125F + 7 * 0.0625F;
        float cell = 0.0625F;
        float glyph = v0 + DecorationDecals.LINES * cell;
        float[] uvs = new float[8];
        DecorationLines.uvs(new Line(UP, 0, 3, 4, 5, 9, false, DecorationLines.AMBER), u0, u1, v0, v1, uvs);
        assertArrayEquals(new float[] {u0 + 0.0625F, glyph + 0.125F * cell, u0 + 0.0625F, glyph + 0.375F * cell,
                u0 + 0.1875F, glyph + 0.375F * cell, u0 + 0.1875F, glyph + 0.125F * cell}, uvs, 1.0E-6F);

        DecorationLines.uvs(new Line(UP, 0, 1, 7, 15, 8, true, DecorationLines.AMBER), u0, u1, v0, v1, uvs);
        assertArrayEquals(new float[] {u0 + 0.25F / 16, glyph + 0.875F * cell, u0 + 0.25F / 16, glyph + 0.625F * cell,
                u0 + 0.25F * 15 / 16, glyph + 0.625F * cell, u0 + 0.25F * 15 / 16, glyph + 0.875F * cell}, uvs, 1.0E-6F,
                "a horizontal dash run follows s");

        DecorationLines.uvs(new Line(UP, 0, 7, 2, 8, 9, true, DecorationLines.AMBER), u0, u1, v0, v1, uvs);
        assertArrayEquals(new float[] {u0 + 0.25F * 9 / 16, glyph + 0.625F * cell, u0 + 0.25F * 2 / 16, glyph + 0.625F * cell,
                u0 + 0.25F * 2 / 16, glyph + 0.875F * cell, u0 + 0.25F * 9 / 16, glyph + 0.875F * cell}, uvs, 1.0E-6F,
                "a vertical dash run follows t");

        DecorationLines.uvs(new Line(UP, 0, 0, 0, 16, 1, true, DecorationLines.AMBER), u0, u1, v0, v1, uvs);
        assertEquals(u0 + 0.25F / 1024, uvs[0], 1.0E-7F, "u stays inside the glyph at a block edge");
        assertEquals(u1 - 0.25F / 1024, uvs[4], 1.0E-7F);
        DecorationLines.uvs(new Line(UP, 0, 4, 4, 5, 5, true, DecorationLines.AMBER), u0, u1, v0, v1, uvs);
        assertEquals(u0 + 0.25F * 4 / 16, uvs[0], 1.0E-7F, "a single cell runs along s");
    }

    @Test
    void linesFitTheVanillaShapesAndNotAReshapedBlock() {
        for (Facing facing : FACINGS) {
            for (Half half : List.of(Half.BOTTOM, Half.TOP)) {
                for (StairShape shape : SHAPES) {
                    var surfaces = new DecorationLines.Surfaces();
                    boolean[] solid = DecorationLines.solid(facing.ordinal() - 1, half == Half.TOP, shape);
                    for (int octant = 0; octant < 8; octant++) {
                        if (!solid[octant]) continue;
                        int x = (octant & 1) * 8, y = (octant >> 1 & 1) * 8, z = (octant >> 2 & 1) * 8;
                        box(surfaces, x, y, z, x + 8, y + 8, z + 8);
                    }
                    assertTrue(DecorationLines.fits(stairs(facing, half, shape), surfaces), facing + "/" + half + "/" + shape);
                }
                for (boolean open : new boolean[] {false, true}) {
                    int[] panel = trapdoorPanel(facing, half, open, 3);
                    var vanilla = new DecorationLines.Surfaces();
                    box(vanilla, panel);
                    assertTrue(DecorationLines.fits(trapdoor(facing, half, open), vanilla), facing + "/" + half + "/" + open);
                    var thin = new DecorationLines.Surfaces();
                    box(thin, trapdoorPanel(facing, half, open, 1));
                    assertFalse(DecorationLines.fits(trapdoor(facing, half, open), thin),
                            "a 1/16 panel leaves the 3/16 marks in the air: " + facing + "/" + half + "/" + open);
                }
            }
        }
        Line[] straight = stairs(Facing.EAST, Half.BOTTOM, StairShape.STRAIGHT);
        assertFalse(DecorationLines.fits(straight, new DecorationLines.Surfaces()), "no geometry, no lines");
        var slab = new DecorationLines.Surfaces();
        box(slab, 0, 0, 0, 16, 8, 16);
        assertFalse(DecorationLines.fits(straight, slab), "a slab model under stairs lacks the step");
        assertTrue(DecorationLines.fits(new Line[0], slab));
    }

    @Test
    void surfacesCoverOnlyWholeCellsOfAxisAlignedQuadsOnTheSixteenthGrid() {
        Line top = new Line(UP, 0, 0, 0, 16, 1, false, DecorationLines.GREEN);
        Line[] row = {top};
        var exact = new DecorationLines.Surfaces();
        exact.add(UP, new float[] {0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0});
        assertTrue(DecorationLines.fits(row, exact));
        var nearlyOnGrid = new DecorationLines.Surfaces();
        nearlyOnGrid.add(UP, new float[] {0, 0.99999994F, 1, 1, 0.99999994F, 1, 1, 0.99999994F, 0.9375F, 0, 0.99999994F, 0.9375F});
        assertTrue(DecorationLines.fits(row, nearlyOnGrid), "bake rounding still lands on the grid; UP's bottom row is the south edge");
        var offGrid = new DecorationLines.Surfaces();
        offGrid.add(UP, new float[] {0, 0.97F, 0, 0, 0.97F, 1, 1, 0.97F, 1, 1, 0.97F, 0});
        assertFalse(DecorationLines.fits(new Line[] {new Line(UP, 0, 0, 0, 1, 1, false, DecorationLines.GREEN)}, offGrid));
        var tilted = new DecorationLines.Surfaces();
        tilted.add(UP, new float[] {0, 1, 0, 0, 1, 1, 1, 0.5F, 1, 1, 0.5F, 0});
        assertFalse(DecorationLines.fits(row, tilted), "a sloped quad is not a face plane");
        var skewed = new DecorationLines.Surfaces();
        skewed.add(UP, new float[] {0.5F, 1, 0, 0, 1, 0.5F, 0.5F, 1, 1, 1, 1, 0.5F});
        assertFalse(DecorationLines.fits(new Line[] {new Line(UP, 0, 7, 7, 9, 9, false, DecorationLines.GREEN)}, skewed),
                "a diamond is not an axis-aligned rectangle");
        var partial = new DecorationLines.Surfaces();
        partial.add(UP, new float[] {0, 1, 0.96875F, 0, 1, 1, 0.5F, 1, 1, 0.5F, 1, 0.96875F});
        assertFalse(DecorationLines.fits(row, partial), "half a cell is not a covered cell");
        var wrongFace = new DecorationLines.Surfaces();
        wrongFace.add(DOWN, new float[] {0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0});
        wrongFace.add(6, new float[] {0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0});
        wrongFace.add(-1, new float[] {0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0});
        assertFalse(DecorationLines.fits(row, wrongFace), "a quad covers only its own face's planes");
        var deep = new DecorationLines.Surfaces();
        deep.add(UP, new float[] {0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0});
        assertFalse(DecorationLines.fits(new Line[] {new Line(UP, 15, 0, 0, 1, 1, false, DecorationLines.GREEN)}, deep),
                "the plane at the opposite boundary is outside the face's depths");
        var east = new DecorationLines.Surfaces();
        east.add(EAST, new float[] {1, 0.25F, 1, 1, 0, 1, 1, 0, 0.5F, 1, 0.25F, 0.5F});
        assertTrue(DecorationLines.fits(new Line[] {new Line(EAST, 0, 0, 0, 8, 4, false, DecorationLines.GREEN)}, east),
                "east: screen right is north, so z 1 to 0.5 is s 0 to 8");
        assertFalse(DecorationLines.fits(new Line[] {new Line(EAST, 0, 8, 0, 9, 4, false, DecorationLines.GREEN)}, east));
        assertFalse(DecorationLines.fits(new Line[] {new Line(EAST, 0, 0, 4, 8, 5, false, DecorationLines.GREEN)}, east));
    }

    /** The panel box in sixteenths of a trapdoor {@code thickness} sixteenths thick. */
    private static int[] trapdoorPanel(Facing facing, Half half, boolean open, int thickness) {
        if (!open) {
            return half == Half.TOP ? new int[] {0, 16 - thickness, 0, 16, 16, 16} : new int[] {0, 0, 0, 16, thickness, 16};
        }
        return switch (facing) {
            case NORTH -> new int[] {0, 0, 16 - thickness, 16, 16, 16};
            case SOUTH -> new int[] {0, 0, 0, 16, 16, thickness};
            case EAST -> new int[] {0, 0, 0, thickness, 16, 16};
            default -> new int[] {16 - thickness, 0, 0, 16, 16, 16};
        };
    }

    private static void box(DecorationLines.Surfaces surfaces, int[] box) {
        box(surfaces, box[0], box[1], box[2], box[3], box[4], box[5]);
    }

    /** The six faces of a box given in sixteenths, as a baked model emits them. */
    private static void box(DecorationLines.Surfaces surfaces, int x0, int y0, int z0, int x1, int y1, int z1) {
        float a = x0 / 16.0F, b = y0 / 16.0F, c = z0 / 16.0F, d = x1 / 16.0F, e = y1 / 16.0F, f = z1 / 16.0F;
        surfaces.add(UP, new float[] {a, e, c, a, e, f, d, e, f, d, e, c});
        surfaces.add(DOWN, new float[] {a, b, f, a, b, c, d, b, c, d, b, f});
        surfaces.add(NORTH, new float[] {d, e, c, d, b, c, a, b, c, a, e, c});
        surfaces.add(SOUTH, new float[] {a, e, f, a, b, f, d, b, f, d, e, f});
        surfaces.add(WEST, new float[] {a, e, c, a, b, c, a, b, f, a, e, f});
        surfaces.add(EAST, new float[] {d, e, f, d, b, f, d, b, c, d, e, c});
    }

    /** Cells in world space, doubled sixteenths so cell centers are integers: face|dashed|argb|x,y,z. */
    private static Set<String> cells(Line[] lines) {
        Set<String> cells = new TreeSet<>();
        for (Line line : lines) {
            for (int t = line.bottom(); t < line.top(); t++) {
                for (int s = line.left(); s < line.right(); s++) {
                    int[] center = new int[3];
                    for (int axis = 0; axis < 3; axis++) {
                        center[axis] = DecorationLines.world(line.face(), axis, s, t, line.depth())
                                + DecorationLines.world(line.face(), axis, s + 1, t + 1, line.depth());
                    }
                    cells.add(cell(line.face(), line, center[0], center[1], center[2]));
                }
            }
        }
        return cells;
    }

    private static String cell(int face, Line line, int x, int y, int z) {
        return face + "|" + line.dashed() + "|" + Integer.toHexString(line.argb()) + "|" + x + "," + y + "," + z;
    }

    private interface CellMap {
        String map(int face, String style, int x, int y, int z);
    }

    private static Set<String> mapped(Set<String> cells, CellMap map) {
        Set<String> result = new TreeSet<>();
        for (String cell : cells) {
            String[] parts = cell.split("\\|");
            String[] xyz = parts[3].split(",");
            result.add(map.map(Integer.parseInt(parts[0]), parts[1] + "|" + parts[2],
                    Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2])));
        }
        return result;
    }

    /** A quarter turn clockwise seen from above: north to east, (x, z) to (1 - z, x). */
    private static Set<String> rotated(Set<String> cells) {
        return mapped(cells, (face, style, x, y, z) ->
                (face < 4 ? (face + 1) % 4 : face) + "|" + style + "|" + (32 - z) + "," + y + "," + x);
    }

    private static Set<String> mirroredZ(Set<String> cells) {
        return mapped(cells, (face, style, x, y, z) ->
                (face == NORTH ? SOUTH : face == SOUTH ? NORTH : face) + "|" + style + "|" + x + "," + y + "," + (32 - z));
    }

    private static Set<String> flipped(Set<String> cells) {
        return mapped(cells, (face, style, x, y, z) ->
                (face == UP ? DOWN : face == DOWN ? UP : face) + "|" + style + "|" + x + "," + (32 - y) + "," + z);
    }
}

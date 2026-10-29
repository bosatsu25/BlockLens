package dev.blocklens.core.render;

import static dev.blocklens.core.render.DecorationDecals.ARROW;
import static dev.blocklens.core.render.DecorationDecals.DOUBLE_FRAME;
import static dev.blocklens.core.render.DecorationDecals.EDGE_FRAME;
import static dev.blocklens.core.render.DecorationDecals.INSET_FRAME;
import static dev.blocklens.core.render.DecorationDecals.NONE;
import static dev.blocklens.core.render.DecorationDecals.RAILS;
import static dev.blocklens.core.render.DecorationDecals.SEAM_FRAME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.DecorationStateKind;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.SemanticState.Axis;
import dev.blocklens.core.state.SemanticState.Facing;
import dev.blocklens.core.state.SemanticState.Half;
import dev.blocklens.core.state.SemanticState.MountFace;
import dev.blocklens.core.state.SemanticState.SlabType;
import dev.blocklens.core.state.SemanticState.StairShape;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class DecorationDecalsTest {
    private static final int NORTH = 0, EAST = 1, SOUTH = 2, WEST = 3, UP = 4, DOWN = 5;
    private static final int[] FACES = {NORTH, EAST, SOUTH, WEST, UP, DOWN};

    @Test
    void tableOrdinalsMatchTheEnumsTheyIndex() {
        assertEquals(List.of(DecorationQuadFace.NORTH, DecorationQuadFace.EAST, DecorationQuadFace.SOUTH,
                DecorationQuadFace.WEST, DecorationQuadFace.UP, DecorationQuadFace.DOWN, DecorationQuadFace.NONE),
                List.of(DecorationQuadFace.values()));
        assertEquals(List.of(Facing.NONE, Facing.NORTH, Facing.EAST, Facing.SOUTH, Facing.WEST), List.of(Facing.values()));
        assertEquals(List.of(Axis.NONE, Axis.X, Axis.Y, Axis.Z), List.of(Axis.values()));
        assertEquals(List.of(SlabType.NONE, SlabType.BOTTOM, SlabType.TOP, SlabType.DOUBLE), List.of(SlabType.values()));
        assertEquals(List.of(StairShape.NONE, StairShape.STRAIGHT, StairShape.INNER_LEFT, StairShape.INNER_RIGHT,
                StairShape.OUTER_LEFT, StairShape.OUTER_RIGHT), List.of(StairShape.values()));
    }

    @Test
    void decalsPackGlyphTurnsAndColorAndRejectInvalidValues() {
        for (int glyph = 0; glyph < DecorationDecals.GLYPHS; glyph++) {
            for (int turns = 0; turns < 4; turns++) {
                long decal = DecorationDecals.decal(glyph, turns, 0xFF102030);
                assertTrue(decal != NONE);
                assertEquals(glyph, DecorationDecals.glyph(decal));
                assertEquals(turns, DecorationDecals.quarterTurns(decal));
                assertEquals(0xFF102030, DecorationDecals.argb(decal));
            }
        }
        assertEquals(-1, DecorationDecals.glyph(NONE));
        assertThrows(IllegalArgumentException.class, () -> DecorationDecals.decal(-1, 0, 0xFFFFFFFF));
        assertThrows(IllegalArgumentException.class, () -> DecorationDecals.decal(DecorationDecals.GLYPHS, 0, 0xFFFFFFFF));
        assertThrows(IllegalArgumentException.class, () -> DecorationDecals.decal(ARROW, -1, 0xFFFFFFFF));
        assertThrows(IllegalArgumentException.class, () -> DecorationDecals.decal(ARROW, 4, 0xFFFFFFFF));
        assertThrows(IllegalArgumentException.class, () -> DecorationDecals.decal(ARROW, 0, 0x80FFFFFF));
        assertThrows(IllegalArgumentException.class, () -> DecorationDecals.decal(ARROW, 0, 0x00FFFFFF));
    }

    // --- Viewer frames and projection -------------------------------------------------------

    @Test
    void viewerFramesMatchAnUprightPlayerLookingAtEachFace() {
        int[][] frames = {{NORTH, UP, WEST}, {EAST, UP, NORTH}, {SOUTH, UP, EAST}, {WEST, UP, SOUTH},
                {UP, NORTH, EAST}, {DOWN, SOUTH, EAST}};
        for (int[] frame : frames) {
            assertEquals(frame[1], DecorationDecals.screenUp(frame[0]), "up of " + frame[0]);
            assertEquals(frame[2], DecorationDecals.screenRight(frame[0]), "right of " + frame[0]);
        }
    }

    @Test
    void everyFrameIsRightHandedWithItsOutwardNormal() {
        for (int face : FACES) {
            int[] right = vector(DecorationDecals.screenRight(face));
            int[] up = vector(DecorationDecals.screenUp(face));
            int[] cross = {right[1] * up[2] - right[2] * up[1], right[2] * up[0] - right[0] * up[2],
                    right[0] * up[1] - right[1] * up[0]};
            assertEquals(List.of(cross[0], cross[1], cross[2]), List.of(DecorationDecals.normalX(face),
                    DecorationDecals.normalY(face), DecorationDecals.normalZ(face)), "mirrored glyphs on " + face);
        }
    }

    @Test
    void coordinatesIncreaseTowardScreenRightAndScreenDown() {
        float x = 0.25F, y = 0.5F, z = 0.75F;
        for (int face : FACES) {
            int[] right = vector(DecorationDecals.screenRight(face));
            int[] up = vector(DecorationDecals.screenUp(face));
            float baseRight = DecorationDecals.right(face, x, y, z);
            float baseDown = DecorationDecals.down(face, x, y, z);
            assertEquals(baseRight + 1, DecorationDecals.right(face, x + right[0], y + right[1], z + right[2]), 1.0E-6F);
            assertEquals(baseDown, DecorationDecals.down(face, x + right[0], y + right[1], z + right[2]), 1.0E-6F);
            assertEquals(baseDown - 1, DecorationDecals.down(face, x + up[0], y + up[1], z + up[2]), 1.0E-6F);
            assertEquals(baseRight, DecorationDecals.right(face, x + up[0], y + up[1], z + up[2]), 1.0E-6F);
        }
        for (int face : FACES) {
            assertEquals(0.0F, DecorationDecals.right(face, 0, 0, 0), 0.0F, "origin right on " + face);
            assertEquals(0.0F, DecorationDecals.down(face, 0, 0, 0), 0.0F, "origin down on " + face);
            assertEquals(0, DecorationDecals.normalY(DecorationDecals.screenRight(face)), "screen-right is horizontal");
        }
        assertEquals(0.25F, DecorationDecals.right(UP, x, y, z), 1.0E-6F);
        assertEquals(0.75F, DecorationDecals.down(UP, x, y, z), 1.0E-6F);
        assertEquals(-0.75F, DecorationDecals.down(DOWN, x, y, z), 1.0E-6F);
    }

    @Test
    void quarterTurnsPointGlyphUpTowardEachInPlaneDirection() {
        for (int face : FACES) {
            int up = DecorationDecals.screenUp(face);
            int right = DecorationDecals.screenRight(face);
            assertEquals(0, DecorationDecals.quarterTurnsToward(face, up));
            assertEquals(1, DecorationDecals.quarterTurnsToward(face, right));
            assertEquals(2, DecorationDecals.quarterTurnsToward(face, DecorationDecals.opposite(up)));
            assertEquals(3, DecorationDecals.quarterTurnsToward(face, DecorationDecals.opposite(right)));
            assertEquals(-1, DecorationDecals.quarterTurnsToward(face, face));
            assertEquals(-1, DecorationDecals.quarterTurnsToward(face, DecorationDecals.opposite(face)));
        }
        assertEquals(List.of(SOUTH, WEST, NORTH, EAST, DOWN, UP), List.of(DecorationDecals.opposite(NORTH),
                DecorationDecals.opposite(EAST), DecorationDecals.opposite(SOUTH), DecorationDecals.opposite(WEST),
                DecorationDecals.opposite(UP), DecorationDecals.opposite(DOWN)));
    }

    @Test
    void glyphUpAppearsOnTheRotatedScreenEdge() {
        float[][] up = {{0.5F, 0.0F}, {1.0F, 0.5F}, {0.5F, 1.0F}, {0.0F, 0.5F}};
        float[][] left = {{0.0F, 0.5F}, {0.5F, 0.0F}, {1.0F, 0.5F}, {0.5F, 1.0F}};
        for (int turns = 0; turns < 4; turns++) {
            assertEquals(0.5F, DecorationDecals.glyphU(turns, up[turns][0], up[turns][1]), 1.0E-6F);
            assertEquals(0.0F, DecorationDecals.glyphV(turns, up[turns][0], up[turns][1]), 1.0E-6F);
            assertEquals(0.0F, DecorationDecals.glyphU(turns, left[turns][0], left[turns][1]), 1.0E-6F);
            assertEquals(0.5F, DecorationDecals.glyphV(turns, left[turns][0], left[turns][1]), 1.0E-6F);
        }
        float[][] expected = {{0.2F, 0.7F}, {0.7F, 0.8F}, {0.8F, 0.3F}, {0.3F, 0.2F}};
        for (int turns = 0; turns < 4; turns++) {
            assertEquals(expected[turns][0], DecorationDecals.glyphU(turns, 0.2F, 0.7F), 1.0E-6F);
            assertEquals(expected[turns][1], DecorationDecals.glyphV(turns, 0.2F, 0.7F), 1.0E-6F);
        }
        for (int invalid : new int[] {-1, 4}) {
            assertThrows(IllegalArgumentException.class, () -> DecorationDecals.glyphU(invalid, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> DecorationDecals.glyphV(invalid, 0, 0));
        }
        assertTrue(DecorationDecals.SURFACE_OFFSET > 0.03F / 16.0F && DecorationDecals.SURFACE_OFFSET < 1.0F / 64.0F);
    }

    // --- Pack-level grammar -----------------------------------------------------------------

    @Test
    void glazedTerracottaMatchesTheSourceTemplateForFacingSouth() {
        // Source template_glazed_terracotta rotations at blockstate y=0 (facing=south):
        // north 180, east 90, south 0, west 270, up 0, down 180 degrees.
        int[] expected = {2, 1, 0, 3, 0, 2};
        for (int face : FACES) {
            long decal = decal(CapabilityId.GLAZED_TERRACOTTA, SemanticState.facing(Facing.SOUTH), face);
            assertEquals(ARROW, DecorationDecals.glyph(decal));
            assertEquals(expected[face], DecorationDecals.quarterTurns(decal), "face " + face);
            assertEquals(DecorationDecals.ARROW_COLOR, DecorationDecals.argb(decal));
        }
    }

    @Test
    void glazedTerracottaArrowFollowsThePlacersLookDirectionForEveryFacing() {
        Facing[] facings = {Facing.NORTH, Facing.EAST, Facing.SOUTH, Facing.WEST};
        for (int front = 0; front < 4; front++) {
            int look = DecorationDecals.opposite(front);
            for (int face : FACES) {
                int expected = face == front ? UP : face == look ? DOWN : look;
                long decal = decal(CapabilityId.GLAZED_TERRACOTTA, SemanticState.facing(facings[front]), face);
                assertEquals(expected, pointing(face, decal), facings[front] + "/" + face);
            }
        }
        assertEquals(NONE, decal(CapabilityId.GLAZED_TERRACOTTA, SemanticState.empty(), UP));
    }

    @Test
    void anvilDrawsOnlyThePacksTopArrowInThePlacersLookDirection() {
        Facing[] facings = {Facing.NORTH, Facing.EAST, Facing.SOUTH, Facing.WEST};
        // Vanilla turns the placer's look direction clockwise into facing, so the look is one
        // counter-clockwise turn back: facing=east was placed looking north.
        int[] look = {WEST, NORTH, EAST, SOUTH};
        for (int front = 0; front < 4; front++) {
            for (int face : FACES) {
                long decal = decal(CapabilityId.ANVIL, SemanticState.facing(facings[front]), face);
                if (face != UP) {
                    assertEquals(NONE, decal, facings[front] + "/" + face);
                    continue;
                }
                assertEquals(ARROW, DecorationDecals.glyph(decal));
                assertEquals(DecorationDecals.ARROW_COLOR, DecorationDecals.argb(decal));
                assertTrue(DecorationDecals.boundaryOnly(decal), "only the top quad, not the lower steps");
                assertEquals(look[front], pointing(face, decal), facings[front].name());
            }
        }
        assertFalse(DecorationDecals.boundaryOnly(decal(CapabilityId.GLAZED_TERRACOTTA,
                SemanticState.facing(Facing.NORTH), UP)));
    }

    @Test
    void boundaryOnlyKeepsTheDecalAndSkipsInnerQuads() {
        long plain = DecorationDecals.decal(ARROW, 3, 0xFF102030);
        long outer = DecorationDecals.onBoundaryOnly(plain);
        assertTrue(DecorationDecals.boundaryOnly(outer));
        assertFalse(DecorationDecals.boundaryOnly(plain));
        assertEquals(ARROW, DecorationDecals.glyph(outer));
        assertEquals(3, DecorationDecals.quarterTurns(outer));
        assertEquals(0xFF102030, DecorationDecals.argb(outer));
        assertEquals(NONE, DecorationDecals.onBoundaryOnly(NONE));
        assertFalse(DecorationDecals.boundaryOnly(NONE));

        float[] top = {0.2F, 1, 0, 0.8F, 1, 0, 0.8F, 1, 1, 0.2F, 1, 1};
        float[] step = {0.2F, 0.25F, 0.1F, 0.8F, 0.25F, 0.1F, 0.8F, 0.25F, 0.9F, 0.2F, 0.25F, 0.9F};
        float[] stepCopy = step.clone();
        float[] uvs = {9, 9, 9, 9, 9, 9, 9, 9};
        assertFalse(DecorationDecals.layout(UP, outer, step, 0, 1, 0, 1, uvs), "inner step quad");
        assertArrayEquals(stepCopy, step);
        assertArrayEquals(new float[] {9, 9, 9, 9, 9, 9, 9, 9}, uvs);
        assertTrue(DecorationDecals.layout(UP, plain, step.clone(), 0, 1, 0, 1, uvs), "a plain decal draws on every quad");
        assertTrue(DecorationDecals.layout(UP, outer, top, 0, 1, 0, 1, uvs));
        assertEquals(1 + DecorationDecals.SURFACE_OFFSET, top[1], 1.0E-7F);
    }

    @Test
    void boundaryPlanesFollowEachFaceNormal() {
        float[][] planes = {
                {0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0},     // north: z = 0
                {1, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0},     // east: x = 1
                {0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1},     // south: z = 1
                {0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0},     // west: x = 0
                {0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1},     // up: y = 1
                {0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1}};    // down: y = 0
        for (int face : FACES) {
            assertTrue(DecorationDecals.onBoundary(face, planes[face]), "face " + face);
            assertFalse(DecorationDecals.onBoundary(DecorationDecals.opposite(face), planes[face]), "opposite " + face);
            float[] inset = planes[face].clone();
            int axis = DecorationDecals.normalX(face) != 0 ? 0 : DecorationDecals.normalY(face) != 0 ? 1 : 2;
            inset[3 * 2 + axis] += DecorationDecals.normalX(face) + DecorationDecals.normalY(face)
                    + DecorationDecals.normalZ(face) > 0 ? -0.01F : 0.01F;
            assertFalse(DecorationDecals.onBoundary(face, inset), "one vertex off the plane, face " + face);
        }
        float[] nearlyTop = {0, 1 - 5.0E-5F, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1};
        assertTrue(DecorationDecals.onBoundary(UP, nearlyTop), "baked float noise stays on the plane");
        float[] atTolerance = {0, 1.0E-4F, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1};
        assertTrue(DecorationDecals.onBoundary(DOWN, atTolerance), "exactly the tolerance is still on the plane");
        float[] pastTolerance = {0, 2.0E-4F, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1};
        assertFalse(DecorationDecals.onBoundary(DOWN, pastTolerance));
    }

    @Test
    void axisBlocksFrameTheirEndsAndRunRailsAlongTheAxis() {
        Map<Axis, Integer> colors = Map.of(Axis.X, DecorationDecals.AXIS_X, Axis.Y, DecorationDecals.AXIS_Y,
                Axis.Z, DecorationDecals.AXIS_Z);
        for (CapabilityId capability : List.of(CapabilityId.LOG, CapabilityId.WOOD, CapabilityId.FROGLIGHT)) {
            for (Map.Entry<Axis, Integer> entry : colors.entrySet()) {
                for (int face : FACES) {
                    long decal = decal(capability, SemanticState.axis(entry.getKey()), face);
                    assertEquals((int) entry.getValue(), DecorationDecals.argb(decal));
                    if (axis(face) == entry.getKey()) {
                        assertEquals(INSET_FRAME, DecorationDecals.glyph(decal));
                        assertEquals(0, DecorationDecals.quarterTurns(decal));
                    } else {
                        assertEquals(RAILS, DecorationDecals.glyph(decal));
                        assertEquals(entry.getKey(), axis(pointing(face, decal)), capability + "/" + face);
                    }
                }
            }
            for (int face : FACES) {
                assertEquals(NONE, decal(capability, SemanticState.axis(Axis.NONE), face));
            }
        }
        assertEquals(1, DecorationDecals.quarterTurns(decal(CapabilityId.LOG, SemanticState.axis(Axis.X), UP)));
        assertEquals(0, DecorationDecals.quarterTurns(decal(CapabilityId.LOG, SemanticState.axis(Axis.Z), UP)));
        assertEquals(1, DecorationDecals.quarterTurns(decal(CapabilityId.LOG, SemanticState.axis(Axis.Z), EAST)));
        assertEquals(0, DecorationDecals.quarterTurns(decal(CapabilityId.LOG, SemanticState.axis(Axis.Y), NORTH)));
    }

    @Test
    void slabsMarkTheFaceThatMeetsTheBlockBoundaryLikeTheSourcePack() {
        for (int face : FACES) {
            long bottom = decal(CapabilityId.SLABS, SemanticState.slab(SlabType.BOTTOM), face);
            long top = decal(CapabilityId.SLABS, SemanticState.slab(SlabType.TOP), face);
            long doubled = decal(CapabilityId.SLABS, SemanticState.slab(SlabType.DOUBLE), face);
            assertEquals(face == DOWN ? EDGE_FRAME : -1, DecorationDecals.glyph(bottom), "bottom/" + face);
            assertEquals(face == UP ? EDGE_FRAME : -1, DecorationDecals.glyph(top), "top/" + face);
            assertEquals(face == UP || face == DOWN ? DOUBLE_FRAME : SEAM_FRAME, DecorationDecals.glyph(doubled));
            assertEquals(DecorationDecals.SLAB, DecorationDecals.argb(doubled));
            assertEquals(NONE, decal(CapabilityId.SLABS, SemanticState.slab(SlabType.NONE), face));
        }
        assertEquals(DecorationDecals.SLAB, DecorationDecals.argb(decal(CapabilityId.SLABS, SemanticState.slab(SlabType.TOP), UP)));
    }

    @Test
    void stairsAndTrapdoorsOutlineEveryFaceInTheirLegend() {
        int[] stairColors = {0xFF30E030, 0xFF30E0E0, 0xFF5090FF, 0xFFFF9030, 0xFFFF40FF};
        StairShape[] shapes = {StairShape.STRAIGHT, StairShape.INNER_LEFT, StairShape.INNER_RIGHT,
                StairShape.OUTER_LEFT, StairShape.OUTER_RIGHT};
        for (int i = 0; i < shapes.length; i++) {
            for (Half half : List.of(Half.BOTTOM, Half.TOP)) {
                for (int face : FACES) {
                    assertFrame(decal(CapabilityId.STAIRS, SemanticState.stairs(Facing.EAST, half, shapes[i]), face),
                            stairColors[i]);
                }
            }
        }
        assertEquals(NONE, decal(CapabilityId.STAIRS, SemanticState.empty(), UP));
        int[] trapdoorColors = {DecorationDecals.AXIS_Z, DecorationDecals.AXIS_X, DecorationDecals.DIRECTION,
                DecorationDecals.AXIS_Y};
        Facing[] facings = {Facing.NORTH, Facing.EAST, Facing.SOUTH, Facing.WEST};
        for (int i = 0; i < facings.length; i++) {
            for (boolean open : new boolean[] {false, true}) {
                for (int face : FACES) {
                    assertFrame(decal(CapabilityId.TRAPDOOR, SemanticState.trapdoor(facings[i], Half.TOP, open), face),
                            trapdoorColors[i]);
                }
            }
        }
        assertEquals(NONE, decal(CapabilityId.TRAPDOOR, SemanticState.empty(), UP));
    }

    @Test
    void facingCuesOutlineOnlyTheFacesTheTintGrammarAlreadySelects() {
        assertOnlyFrame(CapabilityId.BEEHIVE, SemanticState.beehive(Facing.NORTH, 5), NORTH, DecorationDecals.HONEY_FULL);
        assertOnlyFrame(CapabilityId.BEEHIVE, SemanticState.beehive(Facing.NORTH, 4), NORTH, DecorationDecals.DIRECTION);
        assertOnlyFrame(CapabilityId.CAMPFIRE, SemanticState.campfire(Facing.EAST, true), EAST, DecorationDecals.LIT);
        assertOnlyFrame(CapabilityId.CAMPFIRE, SemanticState.campfire(Facing.EAST, false), EAST, DecorationDecals.UNLIT);
        assertOnlyFrame(CapabilityId.FENCE_GATE, SemanticState.fenceGate(Facing.SOUTH, true, false), SOUTH,
                DecorationDecals.AXIS_Y);
        assertOnlyFrame(CapabilityId.FENCE_GATE, SemanticState.fenceGate(Facing.SOUTH, false, true), SOUTH,
                DecorationDecals.AXIS_X);
        assertOnlyFrame(CapabilityId.GRINDSTONE, SemanticState.grindstone(MountFace.WALL, Facing.NORTH), NORTH,
                DecorationDecals.DIRECTION);
        for (int face : FACES) {
            assertEquals(NONE, decal(CapabilityId.ANVIL, SemanticState.empty(), face));
        }
        SemanticState floor = SemanticState.grindstone(MountFace.FLOOR, Facing.EAST);
        SemanticState ceiling = SemanticState.grindstone(MountFace.CEILING, Facing.EAST);
        assertFrame(decal(CapabilityId.GRINDSTONE, floor, DOWN), DecorationDecals.MOUNT);
        assertFrame(decal(CapabilityId.GRINDSTONE, floor, EAST), DecorationDecals.DIRECTION);
        assertEquals(NONE, decal(CapabilityId.GRINDSTONE, floor, UP));
        assertFrame(decal(CapabilityId.GRINDSTONE, ceiling, UP), DecorationDecals.MOUNT);
        assertEquals(NONE, decal(CapabilityId.GRINDSTONE, ceiling, DOWN));
        assertEquals(NONE, decal(CapabilityId.GRINDSTONE, ceiling, WEST));
    }

    @Test
    void stainedGlassHasNoDecalAndTablesAreInternedPerCapabilityAndState() {
        var glass = DecorationRenderDescriptor.of(CapabilityId.STAINED_GLASS, SemanticState.connections(true, false, true, false));
        assertTrue(DecorationDecals.isEmpty(DecorationDecals.table(glass)));
        long[] bottom = DecorationDecals.table(DecorationRenderDescriptor.of(CapabilityId.SLABS, SemanticState.slab(SlabType.BOTTOM)));
        assertFalse(DecorationDecals.isEmpty(bottom), "one marked face among six makes a table non-empty");
        assertEquals(DecorationQuadFace.values().length, bottom.length);
        assertEquals(NONE, bottom[DecorationQuadFace.NONE.ordinal()]);
        assertEquals(EDGE_FRAME, DecorationDecals.glyph(bottom[DOWN]));
        assertSame(bottom, DecorationDecals.table(DecorationRenderDescriptor.of(CapabilityId.SLABS, SemanticState.slab(SlabType.BOTTOM))));
        assertNotSame(bottom, DecorationDecals.table(DecorationRenderDescriptor.of(CapabilityId.SLABS, SemanticState.slab(SlabType.TOP))));
        assertNotSame(DecorationDecals.table(DecorationRenderDescriptor.of(CapabilityId.LOG, SemanticState.axis(Axis.Y))),
                DecorationDecals.table(DecorationRenderDescriptor.of(CapabilityId.WOOD, SemanticState.axis(Axis.Y))));
        assertThrows(NullPointerException.class, () -> DecorationDecals.table(null));
        long[] fresh = DecorationDecals.build(DecorationRenderDescriptor.of(CapabilityId.SLABS, SemanticState.slab(SlabType.BOTTOM)));
        assertNotSame(bottom, fresh);
        assertEquals(List.of(bottom[0], bottom[1], bottom[2], bottom[3], bottom[4], bottom[5], bottom[6]),
                List.of(fresh[0], fresh[1], fresh[2], fresh[3], fresh[4], fresh[5], fresh[6]));
        assertEquals(EDGE_FRAME, DecorationDecals.glyph(fresh[DOWN]), "uncached build fills every face");
        int decorated = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            if (!DecorationStateKind.isDecorationCapability(capability)) continue;
            long[] table = DecorationDecals.table(DecorationRenderDescriptor.of(capability,
                    SemanticState.stairs(Facing.NORTH, Half.TOP, StairShape.STRAIGHT)));
            assertEquals(NONE, table[DecorationQuadFace.NONE.ordinal()], capability.sourceKey());
            decorated++;
        }
        assertEquals(13, decorated);
    }

    private static long decal(CapabilityId capability, SemanticState state, int face) {
        long[] table = DecorationDecals.table(DecorationRenderDescriptor.of(capability, state));
        assertEquals(table[face], DecorationDecals.decal(DecorationRenderDescriptor.of(capability, state), face));
        return table[face];
    }

    private static int pointing(int face, long decal) {
        int up = DecorationDecals.screenUp(face);
        int right = DecorationDecals.screenRight(face);
        return switch (DecorationDecals.quarterTurns(decal)) {
            case 0 -> up;
            case 1 -> right;
            case 2 -> DecorationDecals.opposite(up);
            default -> DecorationDecals.opposite(right);
        };
    }

    private static Axis axis(int face) {
        return face == EAST || face == WEST ? Axis.X : face == UP || face == DOWN ? Axis.Y : Axis.Z;
    }

    private static int[] vector(int direction) {
        return new int[] {DecorationDecals.normalX(direction), DecorationDecals.normalY(direction),
                DecorationDecals.normalZ(direction)};
    }

    private static void assertOnlyFrame(CapabilityId capability, SemanticState state, int marked, int color) {
        for (int face : FACES) {
            long decal = decal(capability, state, face);
            if (face == marked) {
                assertFrame(decal, color);
            } else {
                assertEquals(NONE, decal, capability + "/" + face);
            }
        }
    }

    private static void assertFrame(long decal, int color) {
        assertEquals(INSET_FRAME, DecorationDecals.glyph(decal));
        assertEquals(0, DecorationDecals.quarterTurns(decal));
        assertEquals(color, DecorationDecals.argb(decal));
    }

    @Test
    void layoutMapsTheGlyphOntoTheQuadBoundsAndLiftsIt() {
        // North face of a full block: screen-right is west (-x), screen-down is -y.
        float[] positions = {0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0};
        float[] uvs = new float[8];
        long decal = DecorationDecals.decal(RAILS, 0, 0xFFFFFFFF);
        assertTrue(DecorationDecals.layout(0, decal, positions, 0.25F, 0.75F, 0.0F, 0.6F, uvs));
        float cell = 0.6F / DecorationDecals.GLYPHS;
        float top = RAILS * cell + cell / 1024.0F;
        float span = cell - cell / 512.0F;
        // (x, y) = (0,0) is right-bottom on the north face: s = 1, t = 1.
        assertArrayEquals(new float[] {0.75F, top + span, 0.25F, top + span, 0.25F, top, 0.75F, top}, uvs, 1.0E-6F);
        float lift = -DecorationDecals.SURFACE_OFFSET;
        assertArrayEquals(new float[] {0, 0, lift, 1, 0, lift, 1, 1, lift, 0, 1, lift}, positions, 1.0E-7F);
    }

    @Test
    void layoutRotatesWithTheDecalAndUsesTheQuadsOwnBounds() {
        for (int turns = 0; turns < 4; turns++) {
            // Upper half of the top face (a top slab's face), lifted along +y.
            float[] positions = {0, 1, 0, 1, 1, 0, 1, 1, 0.5F, 0, 1, 0.5F};
            float[] uvs = new float[8];
            long decal = DecorationDecals.decal(ARROW, turns, 0xFFFFFFFF);
            assertTrue(DecorationDecals.layout(4, decal, positions, 0, 1, 0, 6, uvs));
            for (int vertex = 0; vertex < 4; vertex++) {
                float x = vertex == 1 || vertex == 2 ? 1 : 0;
                float z = vertex >= 2 ? 0.5F : 0;
                float s = x;                 // up face: screen-right is east (+x)
                float t = (z - 0) / 0.5F;    // screen-down is south (+z), over the quad's own extent
                assertEquals(DecorationDecals.glyphU(turns, s, t), uvs[2 * vertex], 1.0E-6F, "u turns " + turns);
                float expectedV = ARROW + 1.0F / 1024.0F + DecorationDecals.glyphV(turns, s, t) * (1 - 1.0F / 512.0F);
                assertEquals(expectedV, uvs[2 * vertex + 1], 1.0E-5F, "v turns " + turns);
                assertEquals(1 + DecorationDecals.SURFACE_OFFSET, positions[3 * vertex + 1], 1.0E-7F);
            }
        }
    }

    @Test
    void layoutLeavesADegenerateQuadUntouched() {
        float[] line = {0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0};
        float[] lineCopy = line.clone();
        float[] uvs = {9, 9, 9, 9, 9, 9, 9, 9};
        assertFalse(DecorationDecals.layout(0, DecorationDecals.decal(RAILS, 0, 0xFFFFFFFF), line, 0, 1, 0, 1, uvs));
        assertArrayEquals(lineCopy, line);
        assertArrayEquals(new float[] {9, 9, 9, 9, 9, 9, 9, 9}, uvs);
        float[] column = {0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0};
        assertFalse(DecorationDecals.layout(0, DecorationDecals.decal(RAILS, 0, 0xFFFFFFFF), column, 0, 1, 0, 1, uvs));
    }

    @Test
    void layoutUsesTheSpriteOffsetsAndAHalfSizeQuadOnAnXFace() {
        // East face: screen-right is north (-z), screen-down is -y. Quad covers z 0.25..0.75, y 0..0.5.
        float[] positions = {1, 0, 0.25F, 1, 0, 0.75F, 1, 0.5F, 0.75F, 1, 0.5F, 0.25F};
        float[] uvs = new float[8];
        assertTrue(DecorationDecals.layout(1, DecorationDecals.decal(RAILS, 0, 0xFFFFFFFF), positions,
                0.25F, 0.75F, 0.1F, 0.7F, uvs));
        float cell = (0.7F - 0.1F) / DecorationDecals.GLYPHS;
        float top = 0.1F + RAILS * cell + cell / 1024.0F;
        float bottom = top + (cell - cell / 512.0F);
        assertArrayEquals(new float[] {0.75F, bottom, 0.25F, bottom, 0.25F, top, 0.75F, top}, uvs, 1.0E-6F);
        float lifted = 1 + DecorationDecals.SURFACE_OFFSET;
        assertArrayEquals(new float[] {lifted, 0, 0.25F, lifted, 0, 0.75F, lifted, 0.5F, 0.75F, lifted, 0.5F, 0.25F},
                positions, 1.0E-7F);
    }

    @Test
    void layoutAcceptsAQuadExactlyAtTheMinimumExtent() {
        float min = 1.0E-4F;
        float[] narrow = {0, 0, 0, min, 0, 0, min, 1, 0, 0, 1, 0};
        assertTrue(DecorationDecals.layout(0, DecorationDecals.decal(RAILS, 0, 0xFFFFFFFF), narrow, 0, 1, 0, 1,
                new float[8]), "width at the minimum still draws");
        float[] flat = {0, 0, 0, 1, 0, 0, 1, min, 0, 0, min, 0};
        assertTrue(DecorationDecals.layout(0, DecorationDecals.decal(RAILS, 0, 0xFFFFFFFF), flat, 0, 1, 0, 1,
                new float[8]), "height at the minimum still draws");
    }
}

package dev.blocklens.core.render;

import static dev.blocklens.core.render.DecorationQuadFace.DOWN;
import static dev.blocklens.core.render.DecorationQuadFace.EAST;
import static dev.blocklens.core.render.DecorationQuadFace.NONE;
import static dev.blocklens.core.render.DecorationQuadFace.NORTH;
import static dev.blocklens.core.render.DecorationQuadFace.SOUTH;
import static dev.blocklens.core.render.DecorationQuadFace.UP;
import static dev.blocklens.core.render.DecorationQuadFace.WEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

final class DecorationDecalPolicyTest {
    private static final List<DecorationQuadFace> FACES = List.of(NORTH, EAST, SOUTH, WEST, UP, DOWN);

    /**
     * Glyph-up world direction after the decal's quarter turns, in the face's viewer frame.
     */
    private static DecorationQuadFace pointing(DecorationQuadFace face, DecorationDecal decal) {
        DecorationQuadFace up = DecalProjection.screenUp(face);
        DecorationQuadFace right = DecalProjection.screenRight(face);
        return switch (decal.quarterTurns()) {
            case 0 -> up;
            case 1 -> right;
            case 2 -> DecalProjection.opposite(up);
            default -> DecalProjection.opposite(right);
        };
    }

    private static DecorationDecal decal(CapabilityId capability, SemanticState state, DecorationQuadFace face) {
        return DecorationDecalPolicy.decal(DecorationRenderDescriptor.of(capability, state), face);
    }

    @Test
    void glazedTerracottaMatchesTheSourceTemplateForFacingSouth() {
        // Source template_glazed_terracotta rotations at blockstate y=0 (facing=south):
        // up 0, down 180, north 180, south 0, east 90, west 270 degrees.
        Map<DecorationQuadFace, Integer> expected = Map.of(UP, 0, DOWN, 2, NORTH, 2, SOUTH, 0, EAST, 1, WEST, 3);
        SemanticState south = SemanticState.facing(Facing.SOUTH);
        for (DecorationQuadFace face : FACES) {
            DecorationDecal decal = decal(CapabilityId.GLAZED_TERRACOTTA, south, face);
            assertEquals(DecorationGlyph.ARROW, decal.glyph(), face.name());
            assertEquals(expected.get(face), decal.quarterTurns(), face.name());
            assertEquals(DecorationDecalPolicy.ARROW, decal.argb());
        }
    }

    @Test
    void glazedTerracottaArrowFollowsThePlacersLookDirectionForEveryFacing() {
        Map<Facing, DecorationQuadFace> front = Map.of(
                Facing.NORTH, NORTH, Facing.EAST, EAST, Facing.SOUTH, SOUTH, Facing.WEST, WEST);
        for (Map.Entry<Facing, DecorationQuadFace> entry : front.entrySet()) {
            DecorationQuadFace look = DecalProjection.opposite(entry.getValue());
            SemanticState state = SemanticState.facing(entry.getKey());
            for (DecorationQuadFace face : FACES) {
                DecorationQuadFace expected = face == entry.getValue() ? UP : face == look ? DOWN : look;
                assertEquals(expected, pointing(face, decal(CapabilityId.GLAZED_TERRACOTTA, state, face)),
                        entry.getKey() + "/" + face);
            }
        }
        assertFalse(decal(CapabilityId.GLAZED_TERRACOTTA, SemanticState.empty(), UP).present());
    }

    @Test
    void axisBlocksFrameTheirEndsAndRunRailsAlongTheAxis() {
        Map<Axis, Integer> colors = Map.of(Axis.X, DecorationDecalPolicy.AXIS_X,
                Axis.Y, DecorationDecalPolicy.AXIS_Y, Axis.Z, DecorationDecalPolicy.AXIS_Z);
        for (CapabilityId capability : List.of(CapabilityId.LOG, CapabilityId.WOOD, CapabilityId.FROGLIGHT)) {
            for (Map.Entry<Axis, Integer> entry : colors.entrySet()) {
                SemanticState state = SemanticState.axis(entry.getKey());
                for (DecorationQuadFace face : FACES) {
                    DecorationDecal decal = decal(capability, state, face);
                    assertEquals(entry.getValue(), decal.argb(), capability + "/" + entry.getKey() + "/" + face);
                    if (axis(face) == entry.getKey()) {
                        assertEquals(DecorationGlyph.INSET_FRAME, decal.glyph());
                        assertEquals(0, decal.quarterTurns());
                    } else {
                        assertEquals(DecorationGlyph.RAILS, decal.glyph());
                        // Rails run along glyph up, so glyph up must be parallel to the block axis.
                        assertEquals(entry.getKey(), axis(pointing(face, decal)), capability + "/" + face);
                    }
                }
            }
            for (DecorationQuadFace face : FACES) {
                assertFalse(decal(capability, SemanticState.axis(Axis.NONE), face).present());
            }
        }
        assertEquals(0, decal(CapabilityId.LOG, SemanticState.axis(Axis.Y), NORTH).quarterTurns());
        assertEquals(1, decal(CapabilityId.LOG, SemanticState.axis(Axis.X), UP).quarterTurns());
        assertEquals(0, decal(CapabilityId.LOG, SemanticState.axis(Axis.Z), UP).quarterTurns());
        assertEquals(1, decal(CapabilityId.LOG, SemanticState.axis(Axis.Z), EAST).quarterTurns());
    }

    @Test
    void slabsMarkTheFaceThatMeetsTheBlockBoundaryLikeTheSourcePack() {
        for (DecorationQuadFace face : FACES) {
            DecorationDecal bottom = decal(CapabilityId.SLABS, SemanticState.slab(SlabType.BOTTOM), face);
            DecorationDecal top = decal(CapabilityId.SLABS, SemanticState.slab(SlabType.TOP), face);
            DecorationDecal doubled = decal(CapabilityId.SLABS, SemanticState.slab(SlabType.DOUBLE), face);
            assertEquals(face == DOWN, bottom.present(), "bottom/" + face);
            assertEquals(face == UP, top.present(), "top/" + face);
            boolean vertical = face == UP || face == DOWN;
            assertEquals(vertical ? DecorationGlyph.DOUBLE_FRAME : DecorationGlyph.SEAM_FRAME, doubled.glyph());
            assertEquals(DecorationDecalPolicy.SLAB, doubled.argb());
            assertEquals(0, doubled.quarterTurns());
            assertFalse(decal(CapabilityId.SLABS, SemanticState.slab(SlabType.NONE), face).present());
        }
        DecorationDecal bottomFrame = decal(CapabilityId.SLABS, SemanticState.slab(SlabType.BOTTOM), DOWN);
        assertEquals(DecorationGlyph.EDGE_FRAME, bottomFrame.glyph());
        assertEquals(DecorationDecalPolicy.SLAB, bottomFrame.argb());
        assertEquals(DecorationGlyph.EDGE_FRAME, decal(CapabilityId.SLABS, SemanticState.slab(SlabType.TOP), UP).glyph());
    }

    @Test
    void stairsOutlineEveryFaceInTheShapeColor() {
        Map<StairShape, Integer> colors = Map.of(
                StairShape.STRAIGHT, DecorationDecalPolicy.STAIR_STRAIGHT,
                StairShape.INNER_LEFT, DecorationDecalPolicy.STAIR_INNER_LEFT,
                StairShape.INNER_RIGHT, DecorationDecalPolicy.STAIR_INNER_RIGHT,
                StairShape.OUTER_LEFT, DecorationDecalPolicy.STAIR_OUTER_LEFT,
                StairShape.OUTER_RIGHT, DecorationDecalPolicy.STAIR_OUTER_RIGHT);
        assertEquals(colors.size(), colors.values().stream().distinct().count(), "shape colors must differ");
        for (Map.Entry<StairShape, Integer> entry : colors.entrySet()) {
            for (Half half : List.of(Half.BOTTOM, Half.TOP)) {
                SemanticState state = SemanticState.stairs(Facing.EAST, half, entry.getKey());
                for (DecorationQuadFace face : FACES) {
                    assertFrame(decal(CapabilityId.STAIRS, state, face), entry.getValue());
                }
            }
        }
        assertFalse(decal(CapabilityId.STAIRS, SemanticState.empty(), UP).present());
    }

    @Test
    void trapdoorsOutlineEveryFaceWithTheFacingLegend() {
        Map<Facing, Integer> colors = Map.of(Facing.NORTH, DecorationDecalPolicy.AXIS_Z,
                Facing.EAST, DecorationDecalPolicy.AXIS_X, Facing.SOUTH, DecorationDecalPolicy.DIRECTION,
                Facing.WEST, DecorationDecalPolicy.AXIS_Y);
        for (Map.Entry<Facing, Integer> entry : colors.entrySet()) {
            for (boolean open : new boolean[] {false, true}) {
                SemanticState state = SemanticState.trapdoor(entry.getKey(), Half.TOP, open);
                for (DecorationQuadFace face : FACES) {
                    assertFrame(decal(CapabilityId.TRAPDOOR, state, face), entry.getValue());
                }
            }
        }
        assertFalse(decal(CapabilityId.TRAPDOOR, SemanticState.empty(), UP).present());
    }

    @Test
    void facingCuesOutlineOnlyTheFacesTheTintGrammarAlreadySelects() {
        assertOnlyFacingFrame(CapabilityId.ANVIL, SemanticState.facing(Facing.WEST), WEST, DecorationDecalPolicy.DIRECTION);
        assertOnlyFacingFrame(CapabilityId.BEEHIVE, SemanticState.beehive(Facing.NORTH, 5), NORTH,
                DecorationDecalPolicy.HONEY_FULL);
        assertOnlyFacingFrame(CapabilityId.BEEHIVE, SemanticState.beehive(Facing.NORTH, 4), NORTH,
                DecorationDecalPolicy.DIRECTION);
        assertOnlyFacingFrame(CapabilityId.CAMPFIRE, SemanticState.campfire(Facing.EAST, true), EAST,
                DecorationDecalPolicy.LIT);
        assertOnlyFacingFrame(CapabilityId.CAMPFIRE, SemanticState.campfire(Facing.EAST, false), EAST,
                DecorationDecalPolicy.UNLIT);
        assertOnlyFacingFrame(CapabilityId.FENCE_GATE, SemanticState.fenceGate(Facing.SOUTH, true, false), SOUTH,
                DecorationDecalPolicy.GATE_OPEN);
        assertOnlyFacingFrame(CapabilityId.FENCE_GATE, SemanticState.fenceGate(Facing.SOUTH, false, true), SOUTH,
                DecorationDecalPolicy.GATE_CLOSED);
        assertOnlyFacingFrame(CapabilityId.GRINDSTONE, SemanticState.grindstone(MountFace.WALL, Facing.NORTH), NORTH,
                DecorationDecalPolicy.DIRECTION);
        for (DecorationQuadFace face : FACES) {
            assertFalse(decal(CapabilityId.ANVIL, SemanticState.empty(), face).present());
        }
    }

    @Test
    void grindstoneMarksItsMountFaceAndFacing() {
        SemanticState floor = SemanticState.grindstone(MountFace.FLOOR, Facing.EAST);
        SemanticState ceiling = SemanticState.grindstone(MountFace.CEILING, Facing.EAST);
        assertFrame(decal(CapabilityId.GRINDSTONE, floor, DOWN), DecorationDecalPolicy.MOUNT);
        assertFrame(decal(CapabilityId.GRINDSTONE, floor, EAST), DecorationDecalPolicy.DIRECTION);
        assertFalse(decal(CapabilityId.GRINDSTONE, floor, UP).present());
        assertFrame(decal(CapabilityId.GRINDSTONE, ceiling, UP), DecorationDecalPolicy.MOUNT);
        assertFalse(decal(CapabilityId.GRINDSTONE, ceiling, DOWN).present());
        assertFalse(decal(CapabilityId.GRINDSTONE, ceiling, WEST).present());
    }

    @Test
    void stainedGlassKeepsOnlyItsSolidLayerBehavior() {
        SemanticState state = SemanticState.connections(true, false, true, false);
        for (DecorationQuadFace face : FACES) {
            assertSame(DecorationDecal.NONE, decal(CapabilityId.STAINED_GLASS, state, face));
        }
    }

    @Test
    void faceWithoutDirectionNeverCarriesADecal() {
        for (CapabilityId capability : CapabilityId.values()) {
            if (!DecorationStateKind.isDecorationCapability(capability)) continue;
            assertSame(DecorationDecal.NONE, decal(capability, SemanticState.stairs(Facing.NORTH, Half.TOP,
                    StairShape.STRAIGHT), NONE), capability.sourceKey());
        }
        var descriptor = DecorationRenderDescriptor.of(CapabilityId.LOG, SemanticState.axis(Axis.X));
        assertThrows(NullPointerException.class, () -> DecorationDecalPolicy.decal(null, UP));
        assertThrows(NullPointerException.class, () -> DecorationDecalPolicy.decal(descriptor, null));
    }

    @Test
    void decalColorsAreOpaqueAndAxisColorsAreDistinct() {
        int[] colors = {DecorationDecalPolicy.AXIS_X, DecorationDecalPolicy.AXIS_Y, DecorationDecalPolicy.AXIS_Z,
                DecorationDecalPolicy.DIRECTION, DecorationDecalPolicy.HONEY_FULL, DecorationDecalPolicy.LIT,
                DecorationDecalPolicy.UNLIT, DecorationDecalPolicy.MOUNT, DecorationDecalPolicy.SLAB,
                DecorationDecalPolicy.ARROW};
        for (int color : colors) {
            assertEquals(0xFF, color >>> 24);
        }
        assertTrue(red(DecorationDecalPolicy.AXIS_X) > green(DecorationDecalPolicy.AXIS_X)
                && red(DecorationDecalPolicy.AXIS_X) > blue(DecorationDecalPolicy.AXIS_X));
        assertTrue(green(DecorationDecalPolicy.AXIS_Y) > red(DecorationDecalPolicy.AXIS_Y)
                && green(DecorationDecalPolicy.AXIS_Y) > blue(DecorationDecalPolicy.AXIS_Y));
        assertTrue(blue(DecorationDecalPolicy.AXIS_Z) > red(DecorationDecalPolicy.AXIS_Z)
                && blue(DecorationDecalPolicy.AXIS_Z) > green(DecorationDecalPolicy.AXIS_Z));
    }

    private static void assertOnlyFacingFrame(CapabilityId capability, SemanticState state,
            DecorationQuadFace marked, int color) {
        for (DecorationQuadFace face : FACES) {
            DecorationDecal decal = decal(capability, state, face);
            if (face == marked) {
                assertFrame(decal, color);
            } else {
                assertFalse(decal.present(), capability + "/" + face);
            }
        }
    }

    private static void assertFrame(DecorationDecal decal, int color) {
        assertEquals(DecorationGlyph.INSET_FRAME, decal.glyph());
        assertEquals(0, decal.quarterTurns());
        assertEquals(color, decal.argb());
    }

    private static Axis axis(DecorationQuadFace face) {
        return switch (face) {
            case EAST, WEST -> Axis.X;
            case UP, DOWN -> Axis.Y;
            case NORTH, SOUTH -> Axis.Z;
            case NONE -> Axis.NONE;
        };
    }

    private static int red(int argb) {
        return (argb >>> 16) & 0xFF;
    }

    private static int green(int argb) {
        return (argb >>> 8) & 0xFF;
    }

    private static int blue(int argb) {
        return argb & 0xFF;
    }
}

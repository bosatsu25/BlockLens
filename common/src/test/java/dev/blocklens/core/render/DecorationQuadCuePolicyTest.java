package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.SemanticState.Axis;
import dev.blocklens.core.state.SemanticState.Facing;
import dev.blocklens.core.state.SemanticState.Half;
import dev.blocklens.core.state.SemanticState.MountFace;
import dev.blocklens.core.state.SemanticState.SlabType;
import dev.blocklens.core.state.SemanticState.StairShape;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class DecorationQuadCuePolicyTest {
    @Test
    void axisUsesSourceDerivedRgbGrammarOnlyOnAxisEnds() {
        assertTint(CapabilityId.LOG, SemanticState.axis(Axis.X), DecorationQuadFace.EAST, 0xFFFF9090);
        assertTint(CapabilityId.LOG, SemanticState.axis(Axis.X), DecorationQuadFace.WEST, 0xFFFF9090);
        assertIdentity(CapabilityId.LOG, SemanticState.axis(Axis.X), DecorationQuadFace.UP);

        assertTint(CapabilityId.WOOD, SemanticState.axis(Axis.Y), DecorationQuadFace.UP, 0xFF90FF90);
        assertTint(CapabilityId.WOOD, SemanticState.axis(Axis.Y), DecorationQuadFace.DOWN, 0xFF90FF90);
        assertIdentity(CapabilityId.WOOD, SemanticState.axis(Axis.Y), DecorationQuadFace.NORTH);

        assertTint(CapabilityId.FROGLIGHT, SemanticState.axis(Axis.Z), DecorationQuadFace.NORTH, 0xFF9090FF);
        assertTint(CapabilityId.FROGLIGHT, SemanticState.axis(Axis.Z), DecorationQuadFace.SOUTH, 0xFF9090FF);
        assertIdentity(CapabilityId.FROGLIGHT, SemanticState.axis(Axis.Z), DecorationQuadFace.EAST);
    }

    @Test
    void facingAndSecondaryStateCuesAreDeterministic() {
        assertTint(CapabilityId.ANVIL, SemanticState.facing(Facing.NORTH), DecorationQuadFace.NORTH, 0xFFFFFF90);
        assertIdentity(CapabilityId.ANVIL, SemanticState.facing(Facing.NORTH), DecorationQuadFace.SOUTH);

        assertTint(CapabilityId.BEEHIVE, SemanticState.beehive(Facing.EAST, 4), DecorationQuadFace.EAST, 0xFFFFFF90);
        assertTint(CapabilityId.BEEHIVE, SemanticState.beehive(Facing.EAST, 5), DecorationQuadFace.EAST, 0xFFFFC060);

        assertTint(CapabilityId.CAMPFIRE, SemanticState.campfire(Facing.SOUTH, true), DecorationQuadFace.SOUTH, 0xFFFFB060);
        assertTint(CapabilityId.CAMPFIRE, SemanticState.campfire(Facing.SOUTH, false), DecorationQuadFace.SOUTH, 0xFFC8C8C8);

        assertTint(CapabilityId.FENCE_GATE,
                SemanticState.fenceGate(Facing.WEST, true, false), DecorationQuadFace.WEST, 0xFF90FF90);
        assertTint(CapabilityId.FENCE_GATE,
                SemanticState.fenceGate(Facing.WEST, false, true), DecorationQuadFace.WEST, 0xFFFF9090);
    }

    @Test
    void grindstoneDistinguishesMountAndFacing() {
        SemanticState floor = SemanticState.grindstone(MountFace.FLOOR, Facing.NORTH);
        assertTint(CapabilityId.GRINDSTONE, floor, DecorationQuadFace.DOWN, 0xFF90FFFF);
        assertTint(CapabilityId.GRINDSTONE, floor, DecorationQuadFace.NORTH, 0xFFFFFF90);

        SemanticState ceiling = SemanticState.grindstone(MountFace.CEILING, Facing.EAST);
        assertTint(CapabilityId.GRINDSTONE, ceiling, DecorationQuadFace.UP, 0xFF90FFFF);
        assertTint(CapabilityId.GRINDSTONE, ceiling, DecorationQuadFace.EAST, 0xFFFFFF90);

        SemanticState wall = SemanticState.grindstone(MountFace.WALL, Facing.SOUTH);
        assertTint(CapabilityId.GRINDSTONE, wall, DecorationQuadFace.SOUTH, 0xFFFFFF90);
        assertIdentity(CapabilityId.GRINDSTONE, wall, DecorationQuadFace.UP);
    }

    @Test
    void slabBoundaryCueTracksBottomTopAndDouble() {
        assertTint(CapabilityId.SLABS, SemanticState.slab(SlabType.BOTTOM), DecorationQuadFace.UP, 0xFFFF9090);
        assertIdentity(CapabilityId.SLABS, SemanticState.slab(SlabType.BOTTOM), DecorationQuadFace.DOWN);

        assertTint(CapabilityId.SLABS, SemanticState.slab(SlabType.TOP), DecorationQuadFace.DOWN, 0xFFFF9090);
        assertIdentity(CapabilityId.SLABS, SemanticState.slab(SlabType.TOP), DecorationQuadFace.UP);

        assertTint(CapabilityId.SLABS, SemanticState.slab(SlabType.DOUBLE), DecorationQuadFace.UP, 0xFFFF9090);
        assertTint(CapabilityId.SLABS, SemanticState.slab(SlabType.DOUBLE), DecorationQuadFace.DOWN, 0xFFFF9090);
    }

    @Test
    void stainedGlassForcesSolidWithoutChangingBaseColor() {
        DecorationRenderDescriptor descriptor = DecorationRenderDescriptor.of(
                CapabilityId.STAINED_GLASS,
                SemanticState.connections(false, false, false, false));
        for (DecorationQuadFace face : DecorationQuadFace.values()) {
            DecorationQuadInstruction instruction = DecorationQuadCuePolicy.instruction(descriptor, face);
            assertTrue(instruction.forceSolid());
            assertFalse(instruction.changesColor());
            assertEquals(DecorationQuadInstruction.IDENTITY_COLOR, instruction.multiplyArgb());
        }
    }

    @Test
    void everyStairShapeHasDistinctFacingCueAndHalfBoundary() {
        Set<Integer> colors = new java.util.HashSet<>();
        for (StairShape shape : new StairShape[]{
                StairShape.STRAIGHT,
                StairShape.INNER_LEFT,
                StairShape.INNER_RIGHT,
                StairShape.OUTER_LEFT,
                StairShape.OUTER_RIGHT}) {
            SemanticState bottom = SemanticState.stairs(Facing.NORTH, Half.BOTTOM, shape);
            DecorationQuadInstruction facing = instruction(CapabilityId.STAIRS, bottom, DecorationQuadFace.NORTH);
            assertTrue(facing.changesColor());
            assertTrue(colors.add(facing.multiplyArgb()), () -> "duplicate stair shape color: " + shape);
            assertTint(CapabilityId.STAIRS, bottom, DecorationQuadFace.UP, 0xFFFFFF90);
            assertIdentity(CapabilityId.STAIRS, bottom, DecorationQuadFace.DOWN);

            SemanticState top = SemanticState.stairs(Facing.NORTH, Half.TOP, shape);
            assertTint(CapabilityId.STAIRS, top, DecorationQuadFace.DOWN, 0xFFFFFF90);
            assertIdentity(CapabilityId.STAIRS, top, DecorationQuadFace.UP);
        }
        assertEquals(5, colors.size());
    }

    @Test
    void closedTrapdoorUsesPanelFaceAndOpenTrapdoorUsesFacingFace() {
        SemanticState closedNorthBottom = SemanticState.trapdoor(Facing.NORTH, Half.BOTTOM, false);
        assertTint(CapabilityId.TRAPDOOR, closedNorthBottom, DecorationQuadFace.UP, 0xFF9090FF);
        assertIdentity(CapabilityId.TRAPDOOR, closedNorthBottom, DecorationQuadFace.NORTH);

        SemanticState closedEastTop = SemanticState.trapdoor(Facing.EAST, Half.TOP, false);
        assertTint(CapabilityId.TRAPDOOR, closedEastTop, DecorationQuadFace.DOWN, 0xFFFF9090);

        SemanticState openSouth = SemanticState.trapdoor(Facing.SOUTH, Half.BOTTOM, true);
        assertTint(CapabilityId.TRAPDOOR, openSouth, DecorationQuadFace.SOUTH, 0xFFFFFF90);
        assertIdentity(CapabilityId.TRAPDOOR, openSouth, DecorationQuadFace.UP);

        SemanticState openWest = SemanticState.trapdoor(Facing.WEST, Half.TOP, true);
        assertTint(CapabilityId.TRAPDOOR, openWest, DecorationQuadFace.WEST, 0xFF90FF90);
    }

    @Test
    void incompleteSemanticFallbackBranchesRemainSafeAndIdentityBased() {
        SemanticState empty = SemanticState.empty();
        assertIdentity(CapabilityId.ANVIL, empty, DecorationQuadFace.NORTH);
        assertIdentity(CapabilityId.LOG, empty, DecorationQuadFace.EAST);
        assertIdentity(CapabilityId.SLABS, empty, DecorationQuadFace.UP);
        assertIdentity(CapabilityId.GRINDSTONE, SemanticState.facing(Facing.NORTH), DecorationQuadFace.UP);

        SemanticState stairWithoutShape = SemanticState.facing(Facing.NORTH);
        assertTint(CapabilityId.STAIRS, stairWithoutShape, DecorationQuadFace.NORTH, 0xFFFFFF90);
        assertIdentity(CapabilityId.STAIRS, empty, DecorationQuadFace.UP);

        SemanticState trapdoorWithoutHalf = SemanticState.facing(Facing.SOUTH);
        assertIdentity(CapabilityId.TRAPDOOR, trapdoorWithoutHalf, DecorationQuadFace.UP);

        SemanticState closedWithoutFacing = SemanticState.trapdoor(Facing.NONE, Half.BOTTOM, false);
        assertTint(CapabilityId.TRAPDOOR, closedWithoutFacing, DecorationQuadFace.UP, 0xFFFFFF90);

        SemanticState openWithoutFacing = SemanticState.trapdoor(Facing.NONE, Half.BOTTOM, true);
        assertIdentity(CapabilityId.TRAPDOOR, openWithoutFacing, DecorationQuadFace.NORTH);
    }

    @Test
    void untouchedFacesRemainIdentityAndInstructionContractIsOpaqueArgb() {
        DecorationQuadInstruction identity = DecorationQuadInstruction.IDENTITY;
        assertFalse(identity.changesColor());
        assertFalse(identity.forceSolid());
        assertEquals(0xFFFFFFFF, identity.multiplyArgb());

        DecorationQuadInstruction opaque = DecorationQuadInstruction.OPAQUE;
        assertFalse(opaque.changesColor());
        assertTrue(opaque.forceSolid());

        assertThrows(IllegalArgumentException.class,
                () -> new DecorationQuadInstruction(0x80FFFFFF, false));
        assertThrows(NullPointerException.class,
                () -> DecorationQuadCuePolicy.instruction(null, DecorationQuadFace.NORTH));
        assertThrows(NullPointerException.class,
                () -> DecorationQuadCuePolicy.instruction(
                        DecorationRenderDescriptor.of(CapabilityId.ANVIL, SemanticState.facing(Facing.NORTH)),
                        null));
    }

    @Test
    void repeatedHotPathLookupsReusePrebuiltInstructions() {
        DecorationRenderDescriptor stairs = DecorationRenderDescriptor.of(
                CapabilityId.STAIRS,
                SemanticState.stairs(Facing.NORTH, Half.TOP, StairShape.OUTER_RIGHT));
        DecorationRenderDescriptor trapdoor = DecorationRenderDescriptor.of(
                CapabilityId.TRAPDOOR,
                SemanticState.trapdoor(Facing.WEST, Half.BOTTOM, true));

        assertSame(
                DecorationQuadCuePolicy.instruction(stairs, DecorationQuadFace.NORTH),
                DecorationQuadCuePolicy.instruction(stairs, DecorationQuadFace.NORTH));
        assertSame(
                DecorationQuadCuePolicy.instruction(trapdoor, DecorationQuadFace.WEST),
                DecorationQuadCuePolicy.instruction(trapdoor, DecorationQuadFace.WEST));
        assertSame(
                DecorationQuadInstruction.IDENTITY,
                DecorationQuadCuePolicy.instruction(stairs, DecorationQuadFace.SOUTH));
    }

    private static DecorationQuadInstruction instruction(
            CapabilityId capability,
            SemanticState state,
            DecorationQuadFace face) {
        return DecorationQuadCuePolicy.instruction(DecorationRenderDescriptor.of(capability, state), face);
    }

    private static void assertTint(
            CapabilityId capability,
            SemanticState state,
            DecorationQuadFace face,
            int expected) {
        DecorationQuadInstruction instruction = instruction(capability, state, face);
        assertEquals(expected, instruction.multiplyArgb());
        assertTrue(instruction.changesColor());
        assertFalse(instruction.forceSolid());
    }

    private static void assertIdentity(
            CapabilityId capability,
            SemanticState state,
            DecorationQuadFace face) {
        assertEquals(DecorationQuadInstruction.IDENTITY, instruction(capability, state, face));
    }
}

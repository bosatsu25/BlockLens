package dev.blocklens.core.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.state.SemanticState.Axis;
import dev.blocklens.core.state.SemanticState.Facing;
import dev.blocklens.core.state.SemanticState.Half;
import dev.blocklens.core.state.SemanticState.MountFace;
import dev.blocklens.core.state.SemanticState.SlabType;
import dev.blocklens.core.state.SemanticState.StairShape;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class SemanticStateTest {
    @Test
    void emptyIsCanonicalAndContainsNoSemantics() {
        SemanticState empty = SemanticState.empty();
        assertSame(empty, SemanticState.empty());
        assertEquals(0, empty.packed());
        assertEquals(Facing.NONE, empty.facing());
        assertEquals(Axis.NONE, empty.axis());
        assertEquals(Half.NONE, empty.half());
        assertEquals(StairShape.NONE, empty.stairShape());
        assertEquals(MountFace.NONE, empty.mountFace());
        assertEquals(SlabType.NONE, empty.slabType());
        assertEquals(0, empty.connectionMask());
        assertEquals(-1, empty.level());
        assertFalse(empty.open());
        assertFalse(empty.lit());
        assertFalse(empty.inWall());
        assertFalse(empty.powered());
        assertFalse(empty.attached());
        assertFalse(empty.connected(Facing.NONE));
    }

    @Test
    void facingAndAxisFactoriesRoundTripAllValues() {
        for (Facing facing : Facing.values()) {
            SemanticState state = SemanticState.facing(facing);
            assertEquals(facing, state.facing());
            assertEquals(Axis.NONE, state.axis());
        }
        for (Axis axis : Axis.values()) {
            SemanticState state = SemanticState.axis(axis);
            assertEquals(axis, state.axis());
            assertEquals(Facing.NONE, state.facing());
        }
    }

    @Test
    void beehiveRoundTripsEveryFacingAndHoneyLevel() {
        Set<Integer> packed = new HashSet<>();
        for (Facing facing : cardinalFacings()) {
            for (int honey = 0; honey <= 5; honey++) {
                SemanticState state = SemanticState.beehive(facing, honey);
                assertEquals(facing, state.facing());
                assertEquals(honey, state.level());
                assertTrue(packed.add(state.packed()));
            }
        }
        assertEquals(24, packed.size());
    }

    @Test
    void campfireRoundTripsFacingAndLit() {
        for (Facing facing : cardinalFacings()) {
            SemanticState unlit = SemanticState.campfire(facing, false);
            SemanticState lit = SemanticState.campfire(facing, true);
            assertEquals(facing, unlit.facing());
            assertFalse(unlit.lit());
            assertEquals(facing, lit.facing());
            assertTrue(lit.lit());
            assertNotEquals(unlit, lit);
        }
    }

    @Test
    void grindstoneRoundTripsAllMountFacesAndFacings() {
        for (MountFace face : new MountFace[]{MountFace.FLOOR, MountFace.WALL, MountFace.CEILING}) {
            for (Facing facing : cardinalFacings()) {
                SemanticState state = SemanticState.grindstone(face, facing);
                assertEquals(face, state.mountFace());
                assertEquals(facing, state.facing());
            }
        }
    }

    @Test
    void fenceGateRoundTripsAllBooleanCombinations() {
        for (Facing facing : cardinalFacings()) {
            for (boolean open : new boolean[]{false, true}) {
                for (boolean inWall : new boolean[]{false, true}) {
                    SemanticState state = SemanticState.fenceGate(facing, open, inWall);
                    assertEquals(facing, state.facing());
                    assertEquals(open, state.open());
                    assertEquals(inWall, state.inWall());
                }
            }
        }
    }

    @Test
    void slabRoundTripsEverySourceType() {
        for (SlabType type : new SlabType[]{SlabType.BOTTOM, SlabType.TOP, SlabType.DOUBLE}) {
            assertEquals(type, SemanticState.slab(type).slabType());
        }
    }

    @Test
    void connectionsUseStableCardinalBitMask() {
        for (int expectedMask = 0; expectedMask <= SemanticState.CONNECTION_MASK; expectedMask++) {
            SemanticState state = SemanticState.connections(
                    (expectedMask & SemanticState.CONNECT_NORTH) != 0,
                    (expectedMask & SemanticState.CONNECT_EAST) != 0,
                    (expectedMask & SemanticState.CONNECT_SOUTH) != 0,
                    (expectedMask & SemanticState.CONNECT_WEST) != 0);
            assertEquals(expectedMask, state.connectionMask());
            assertEquals((expectedMask & SemanticState.CONNECT_NORTH) != 0, state.connected(Facing.NORTH));
            assertEquals((expectedMask & SemanticState.CONNECT_EAST) != 0, state.connected(Facing.EAST));
            assertEquals((expectedMask & SemanticState.CONNECT_SOUTH) != 0, state.connected(Facing.SOUTH));
            assertEquals((expectedMask & SemanticState.CONNECT_WEST) != 0, state.connected(Facing.WEST));
            assertFalse(state.connected(Facing.NONE));
        }
    }

    @Test
    void stairsCoverAllFortySourceSemanticCombinations() {
        Set<Integer> packed = new HashSet<>();
        for (Facing facing : cardinalFacings()) {
            for (Half half : new Half[]{Half.BOTTOM, Half.TOP}) {
                for (StairShape shape : new StairShape[]{
                        StairShape.STRAIGHT,
                        StairShape.INNER_LEFT,
                        StairShape.INNER_RIGHT,
                        StairShape.OUTER_LEFT,
                        StairShape.OUTER_RIGHT}) {
                    SemanticState state = SemanticState.stairs(facing, half, shape);
                    assertEquals(facing, state.facing());
                    assertEquals(half, state.half());
                    assertEquals(shape, state.stairShape());
                    assertTrue(packed.add(state.packed()));
                }
            }
        }
        assertEquals(40, packed.size());
    }

    @Test
    void trapdoorCoversAllSixteenSourceSemanticCombinations() {
        Set<Integer> packed = new HashSet<>();
        for (Facing facing : cardinalFacings()) {
            for (Half half : new Half[]{Half.BOTTOM, Half.TOP}) {
                for (boolean open : new boolean[]{false, true}) {
                    SemanticState state = SemanticState.trapdoor(facing, half, open);
                    assertEquals(facing, state.facing());
                    assertEquals(half, state.half());
                    assertEquals(open, state.open());
                    assertTrue(packed.add(state.packed()));
                }
            }
        }
        assertEquals(16, packed.size());
    }

    @Test
    void poweredAndAttachedAreIndependentFlags() {
        SemanticState none = SemanticState.poweredAttached(false, false);
        SemanticState powered = SemanticState.poweredAttached(true, false);
        SemanticState attached = SemanticState.poweredAttached(false, true);
        SemanticState both = SemanticState.poweredAttached(true, true);
        assertFalse(none.powered());
        assertFalse(none.attached());
        assertTrue(powered.powered());
        assertFalse(powered.attached());
        assertFalse(attached.powered());
        assertTrue(attached.attached());
        assertTrue(both.powered());
        assertTrue(both.attached());
    }

    @Test
    void equalityHashAndStringArePackedValueBased() {
        SemanticState first = SemanticState.stairs(Facing.WEST, Half.TOP, StairShape.OUTER_LEFT);
        SemanticState same = SemanticState.stairs(Facing.WEST, Half.TOP, StairShape.OUTER_LEFT);
        SemanticState other = SemanticState.stairs(Facing.WEST, Half.TOP, StairShape.OUTER_RIGHT);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, other);
        assertNotEquals(first, null);
        assertNotEquals(first, "not a semantic state");
        assertTrue(first.toString().contains(Integer.toUnsignedString(first.packed())));
    }

    @Test
    void genericCreationRejectsInvalidValues() {
        assertThrows(NullPointerException.class, () -> SemanticState.facing(null));
        assertThrows(NullPointerException.class, () -> SemanticState.axis(null));
        assertThrows(NullPointerException.class, () -> SemanticState.stairs(null, Half.BOTTOM, StairShape.STRAIGHT));
        assertThrows(NullPointerException.class, () -> SemanticState.stairs(Facing.NORTH, null, StairShape.STRAIGHT));
        assertThrows(NullPointerException.class, () -> SemanticState.stairs(Facing.NORTH, Half.BOTTOM, null));
        assertThrows(NullPointerException.class, () -> SemanticState.grindstone(null, Facing.NORTH));
        assertThrows(NullPointerException.class, () -> SemanticState.slab(null));
        assertThrows(NullPointerException.class, () -> SemanticState.empty().connected(null));
        assertThrows(IllegalArgumentException.class, () -> SemanticState.beehive(Facing.NORTH, -1));
        assertThrows(IllegalArgumentException.class, () -> SemanticState.beehive(Facing.NORTH, 6));
        assertThrows(IllegalArgumentException.class, () -> SemanticState.create(
                Facing.NONE, Axis.NONE, Half.NONE, StairShape.NONE, MountFace.NONE, SlabType.NONE,
                1 << 5, -1, false, false, false, false, false));
    }

    private static Facing[] cardinalFacings() {
        return new Facing[]{Facing.NORTH, Facing.EAST, Facing.SOUTH, Facing.WEST};
    }
}

package dev.blocklens.core.render;

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
import org.junit.jupiter.api.Test;

final class FaceDecalTableTest {
    private static final List<SemanticState> STATES = List.of(SemanticState.empty(),
            SemanticState.facing(Facing.NORTH), SemanticState.facing(Facing.WEST), SemanticState.axis(Axis.X),
            SemanticState.axis(Axis.Z), SemanticState.beehive(Facing.EAST, 5),
            SemanticState.campfire(Facing.WEST, true), SemanticState.grindstone(MountFace.FLOOR, Facing.NORTH),
            SemanticState.fenceGate(Facing.NORTH, true, false),
            SemanticState.stairs(Facing.SOUTH, Half.BOTTOM, StairShape.OUTER_LEFT),
            SemanticState.trapdoor(Facing.WEST, Half.TOP, true), SemanticState.slab(SlabType.DOUBLE),
            SemanticState.connections(true, true, false, false));

    @Test
    void compiledTablesMatchThePolicyForEveryCapabilityStateAndFace() {
        int compiled = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            if (!DecorationStateKind.isDecorationCapability(capability)) continue;
            for (SemanticState state : STATES) {
                var descriptor = DecorationRenderDescriptor.of(capability, state);
                FaceDecalTable table = FaceDecalTable.compile(descriptor);
                boolean anyPresent = false;
                for (DecorationQuadFace face : DecorationQuadFace.values()) {
                    DecorationDecal expected = DecorationDecalPolicy.decal(descriptor, face);
                    assertEquals(expected, table.decal(face), capability.sourceKey() + "/" + face);
                    anyPresent |= expected.present();
                }
                assertEquals(!anyPresent, table.isEmpty(), capability.sourceKey() + " " + state);
                compiled++;
            }
        }
        assertEquals(13 * STATES.size(), compiled);
    }

    @Test
    void uncachedBuildsComputeContentAndEmptinessThemselves() {
        var glass = FaceDecalTable.build(DecorationRenderDescriptor.of(CapabilityId.STAINED_GLASS,
                SemanticState.connections(true, false, false, false)));
        var bottom = FaceDecalTable.build(DecorationRenderDescriptor.of(CapabilityId.SLABS,
                SemanticState.slab(SlabType.BOTTOM)));
        var anvil = FaceDecalTable.build(DecorationRenderDescriptor.of(CapabilityId.ANVIL,
                SemanticState.facing(Facing.EAST)));
        assertTrue(glass.isEmpty());
        assertFalse(bottom.isEmpty(), "one marked face among six must make the table non-empty");
        assertFalse(anvil.isEmpty());
        assertEquals(DecorationGlyph.EDGE_FRAME, bottom.decal(DecorationQuadFace.DOWN).glyph());
        assertFalse(bottom.decal(DecorationQuadFace.UP).present());
        assertEquals(DecorationGlyph.INSET_FRAME, anvil.decal(DecorationQuadFace.EAST).glyph());
        assertNotSame(bottom, FaceDecalTable.build(DecorationRenderDescriptor.of(CapabilityId.SLABS,
                SemanticState.slab(SlabType.BOTTOM))));
    }

    @Test
    void tablesAreInternedPerCapabilityAndState() {
        var first = FaceDecalTable.compile(DecorationRenderDescriptor.of(CapabilityId.LOG, SemanticState.axis(Axis.Y)));
        var second = FaceDecalTable.compile(DecorationRenderDescriptor.of(CapabilityId.LOG, SemanticState.axis(Axis.Y)));
        var otherState = FaceDecalTable.compile(DecorationRenderDescriptor.of(CapabilityId.LOG, SemanticState.axis(Axis.X)));
        var otherCapability = FaceDecalTable.compile(DecorationRenderDescriptor.of(CapabilityId.WOOD, SemanticState.axis(Axis.Y)));
        assertSame(first, second);
        assertNotSame(first, otherState);
        assertNotSame(first, otherCapability);
        int before = FaceDecalTable.internedCount();
        FaceDecalTable.compile(DecorationRenderDescriptor.of(CapabilityId.LOG, SemanticState.axis(Axis.Y)));
        assertEquals(before, FaceDecalTable.internedCount());
        assertTrue(before >= 3);
    }

    @Test
    void emptinessIsReportedForGlassAndAbsentStates() {
        assertTrue(FaceDecalTable.compile(DecorationRenderDescriptor.of(CapabilityId.STAINED_GLASS,
                SemanticState.connections(false, false, false, false))).isEmpty());
        assertTrue(FaceDecalTable.compile(DecorationRenderDescriptor.of(CapabilityId.SLABS,
                SemanticState.slab(SlabType.NONE))).isEmpty());
        assertFalse(FaceDecalTable.compile(DecorationRenderDescriptor.of(CapabilityId.SLABS,
                SemanticState.slab(SlabType.BOTTOM))).isEmpty());
    }

    @Test
    void nullContractsAreRejected() {
        assertThrows(NullPointerException.class, () -> FaceDecalTable.compile(null));
        var table = FaceDecalTable.compile(DecorationRenderDescriptor.of(CapabilityId.ANVIL,
                SemanticState.facing(Facing.NORTH)));
        assertThrows(NullPointerException.class, () -> table.decal(null));
    }
}

package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.*;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.DecorationStateKind;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.SemanticState.*;
import java.util.List;
import org.junit.jupiter.api.Test;

final class FaceInstructionTableTest {
    @Test void precompiledInstructionsMatchTheExistingPolicyForEveryCapabilityAndFace() {
        var states = List.of(SemanticState.empty(), SemanticState.facing(Facing.NORTH),
                SemanticState.facing(Facing.SOUTH), SemanticState.axis(Axis.X),
                SemanticState.axis(Axis.Y), SemanticState.axis(Axis.Z),
                SemanticState.beehive(Facing.EAST, 5), SemanticState.campfire(Facing.WEST, false),
                SemanticState.grindstone(MountFace.FLOOR, Facing.NORTH),
                SemanticState.fenceGate(Facing.NORTH, true, false),
                SemanticState.stairs(Facing.SOUTH, Half.BOTTOM, StairShape.INNER_RIGHT),
                SemanticState.trapdoor(Facing.WEST, Half.TOP, true), SemanticState.slab(SlabType.DOUBLE));
        int total = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            if (!DecorationStateKind.isDecorationCapability(capability)) continue;
            for (SemanticState state : states) {
                var descriptor = DecorationRenderDescriptor.of(capability, state);
                var compiled = FaceInstructionTable.compile(descriptor);
                for (DecorationQuadFace face : DecorationQuadFace.values()) {
                    assertSame(DecorationQuadCuePolicy.instruction(descriptor, face),
                            compiled.instruction(face), capability.sourceKey() + "/" + face);
                }
                total++;
            }
        }
        assertEquals(13 * states.size(), total);
    }

    @Test void nullContractsAreRejected() {
        assertThrows(NullPointerException.class, () -> FaceInstructionTable.compile(null));
        var table = FaceInstructionTable.compile(DecorationRenderDescriptor.of(
                CapabilityId.ANVIL, SemanticState.facing(Facing.NORTH)));
        assertThrows(NullPointerException.class, () -> table.instruction(null));
    }
}

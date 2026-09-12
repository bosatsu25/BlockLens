package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import java.util.List;
import org.junit.jupiter.api.Test;

final class VisibilityQuadCuePolicyTest {
    @Test
    void everyM4CueBecomesAnEmissiveTexturePreservingTint() {
        List<VisibilityRenderDescriptor> descriptors = List.of(
                VisibilityRenderPolicy.describeEnabled(CapabilityId.BLUE_ICE, SemanticState.empty()),
                VisibilityRenderPolicy.describeEnabled(CapabilityId.DEAD_CORAL, SemanticState.empty()),
                VisibilityRenderPolicy.describeEnabled(CapabilityId.POWDER_SNOW, SemanticState.empty()),
                VisibilityRenderPolicy.describeEnabled(
                        CapabilityId.SCULK_CATALYST, SemanticState.sculkCatalyst(false)),
                VisibilityRenderPolicy.describeEnabled(
                        CapabilityId.SCULK_CATALYST, SemanticState.sculkCatalyst(true)),
                VisibilityRenderPolicy.describeEnabled(
                        CapabilityId.STRING_TWEAKS,
                        SemanticState.tripwire(true, false, true, false, false, true)),
                VisibilityRenderPolicy.describeEnabled(
                        CapabilityId.STRING_TWEAKS,
                        SemanticState.tripwire(false, true, false, true, true, false)));

        for (VisibilityRenderDescriptor descriptor : descriptors) {
            DecorationQuadInstruction instruction = VisibilityQuadCuePolicy.instruction(descriptor);
            assertEquals(descriptor.cue().accentArgb(), instruction.multiplyArgb());
            assertTrue(instruction.changesColor());
            assertTrue(instruction.emissive());
            assertFalse(instruction.forceSolid());
        }
    }

    @Test
    void enabledDescriptorBuilderFailsClosedOutsideM4() {
        assertThrows(IllegalArgumentException.class,
                () -> VisibilityRenderPolicy.describeEnabled(CapabilityId.ANVIL, SemanticState.empty()));
        assertThrows(NullPointerException.class,
                () -> VisibilityRenderPolicy.describeEnabled(null, SemanticState.empty()));
        assertThrows(NullPointerException.class,
                () -> VisibilityRenderPolicy.describeEnabled(CapabilityId.BLUE_ICE, null));
        assertThrows(NullPointerException.class, () -> VisibilityQuadCuePolicy.instruction(null));
    }
}

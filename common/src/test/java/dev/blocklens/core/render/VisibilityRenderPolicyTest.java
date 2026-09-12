package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class VisibilityRenderPolicyTest {
    private static final Set<CapabilityId> M4 = EnumSet.of(
            CapabilityId.BLUE_ICE,
            CapabilityId.DEAD_CORAL,
            CapabilityId.POWDER_SNOW,
            CapabilityId.SCULK_CATALYST,
            CapabilityId.STRING_TWEAKS);

    @Test
    void staticOutlineCapabilitiesUseSourceDerivedAccents() {
        BlockLensConfig config = enabledM4();
        assertDescriptor(
                CapabilityId.BLUE_ICE,
                SemanticState.empty(),
                config,
                VisibilityVisualCue.BLUE_ICE_OUTLINE,
                0xFF0000FF,
                VisibilityVisualCue.Mode.OUTLINE);
        assertDescriptor(
                CapabilityId.DEAD_CORAL,
                SemanticState.empty(),
                config,
                VisibilityVisualCue.DEAD_CORAL_OUTLINE,
                0xFFD962A3,
                VisibilityVisualCue.Mode.OUTLINE);
        assertDescriptor(
                CapabilityId.POWDER_SNOW,
                SemanticState.empty(),
                config,
                VisibilityVisualCue.POWDER_SNOW_OUTLINE,
                0xFFA5F6FF,
                VisibilityVisualCue.Mode.OUTLINE);
    }

    @Test
    void sculkBloomSelectsDistinctSourceDerivedCue() {
        BlockLensConfig config = enabledM4();
        VisibilityRenderDescriptor idle = VisibilityRenderPolicy.describe(
                CapabilityId.SCULK_CATALYST, SemanticState.sculkCatalyst(false), config).orElseThrow();
        VisibilityRenderDescriptor bloom = VisibilityRenderPolicy.describe(
                CapabilityId.SCULK_CATALYST, SemanticState.sculkCatalyst(true), config).orElseThrow();

        assertEquals(VisibilityVisualCue.SCULK_CATALYST_IDLE, idle.cue());
        assertEquals(0xFF29DFEB, idle.cue().accentArgb());
        assertEquals(VisibilityVisualCue.SCULK_CATALYST_BLOOM, bloom.cue());
        assertEquals(0xFF16DEEC, bloom.cue().accentArgb());
        assertFalse(idle.state().bloom());
        assertTrue(bloom.state().bloom());
    }

    @Test
    void allSixtyFourTripwireStatesRetainShapeAndUsePoweredColorOnly() {
        BlockLensConfig config = enabledM4();
        int count = 0;
        for (int connections = 0; connections <= SemanticState.CONNECTION_MASK; connections++) {
            for (boolean powered : new boolean[]{false, true}) {
                for (boolean attached : new boolean[]{false, true}) {
                    SemanticState state = SemanticState.tripwire(
                            (connections & SemanticState.CONNECT_NORTH) != 0,
                            (connections & SemanticState.CONNECT_EAST) != 0,
                            (connections & SemanticState.CONNECT_SOUTH) != 0,
                            (connections & SemanticState.CONNECT_WEST) != 0,
                            powered,
                            attached);
                    VisibilityRenderDescriptor descriptor = VisibilityRenderPolicy.describe(
                            CapabilityId.STRING_TWEAKS, state, config).orElseThrow();

                    assertEquals(connections, descriptor.state().connectionMask());
                    assertEquals(powered, descriptor.state().powered());
                    assertEquals(attached, descriptor.state().attached());
                    assertEquals(VisibilityVisualCue.Mode.FINE_LINE, descriptor.cue().mode());
                    assertEquals(
                            powered ? VisibilityVisualCue.STRING_POWERED : VisibilityVisualCue.STRING_UNPOWERED,
                            descriptor.cue());
                    assertEquals(powered ? 0xFFFF0000 : 0xFF00FF00, descriptor.cue().accentArgb());
                    count++;
                }
            }
        }
        assertEquals(64, count);
    }

    @Test
    void eachM4ToggleIsIndependentAndOffRestoresNoDescriptor() {
        BlockLensConfig allOn = enabledM4();
        for (CapabilityId capability : M4) {
            SemanticState state = stateFor(capability);
            assertTrue(VisibilityRenderPolicy.describe(capability, state, allOn).isPresent());

            BlockLensConfig oneOff = allOn.withEnabled(capability, false);
            assertTrue(VisibilityRenderPolicy.describe(capability, state, oneOff).isEmpty());
            for (CapabilityId other : M4) {
                if (other != capability) {
                    assertTrue(VisibilityRenderPolicy.describe(other, stateFor(other), oneOff).isPresent());
                }
            }
        }
    }

    @Test
    void M3AndM4CanBeEnabledTogetherWithoutExclusion() {
        BlockLensConfig config = enabledM4();
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.category() == CapabilityId.Category.DECORATION) {
                config = config.withEnabled(capability, true);
            }
        }

        for (CapabilityId capability : M4) {
            assertTrue(config.isEnabled(capability));
            assertTrue(VisibilityRenderPolicy.describe(capability, stateFor(capability), config).isPresent());
        }
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.category() == CapabilityId.Category.DECORATION) {
                assertTrue(config.isEnabled(capability));
            }
        }
    }

    @Test
    void nonM4CapabilitiesProduceNoDescriptorAndInvalidDescriptorIsRejected() {
        BlockLensConfig config = BlockLensConfig.defaults().withEnabled(CapabilityId.ANVIL, true);
        assertTrue(VisibilityRenderPolicy.describe(CapabilityId.ANVIL, null, config).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new VisibilityRenderDescriptor(
                CapabilityId.ANVIL,
                VisibilityVisualCue.BLUE_ICE_OUTLINE,
                SemanticState.empty()));
        assertThrows(NullPointerException.class,
                () -> VisibilityRenderPolicy.describe(null, SemanticState.empty(), config));
        assertThrows(NullPointerException.class,
                () -> VisibilityRenderPolicy.describe(CapabilityId.BLUE_ICE, SemanticState.empty(), null));
        assertThrows(NullPointerException.class,
                () -> VisibilityRenderPolicy.describe(CapabilityId.BLUE_ICE, null, enabledM4()));
    }

    private static void assertDescriptor(
            CapabilityId capability,
            SemanticState state,
            BlockLensConfig config,
            VisibilityVisualCue expectedCue,
            int expectedArgb,
            VisibilityVisualCue.Mode expectedMode) {
        VisibilityRenderDescriptor descriptor = VisibilityRenderPolicy.describe(capability, state, config).orElseThrow();
        assertEquals(capability, descriptor.capability());
        assertEquals(state, descriptor.state());
        assertEquals(expectedCue, descriptor.cue());
        assertEquals(expectedArgb, descriptor.cue().accentArgb());
        assertEquals(expectedMode, descriptor.cue().mode());
    }

    private static BlockLensConfig enabledM4() {
        BlockLensConfig config = BlockLensConfig.defaults();
        for (CapabilityId capability : M4) {
            config = config.withEnabled(capability, true);
        }
        return config;
    }

    private static SemanticState stateFor(CapabilityId capability) {
        return switch (capability) {
            case SCULK_CATALYST -> SemanticState.sculkCatalyst(true);
            case STRING_TWEAKS -> SemanticState.tripwire(true, true, false, true, true, false);
            case BLUE_ICE, DEAD_CORAL, POWDER_SNOW -> SemanticState.empty();
            default -> throw new IllegalArgumentException("not M4: " + capability);
        };
    }
}

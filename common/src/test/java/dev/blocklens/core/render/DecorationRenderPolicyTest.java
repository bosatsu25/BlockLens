package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.DecorationStateKind;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.SemanticState.Axis;
import dev.blocklens.core.state.SemanticState.Facing;
import dev.blocklens.core.state.SemanticState.Half;
import dev.blocklens.core.state.SemanticState.MountFace;
import dev.blocklens.core.state.SemanticState.SlabType;
import dev.blocklens.core.state.SemanticState.StairShape;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class DecorationRenderPolicyTest {
    @Test
    void exactlyThirteenCapabilitiesHaveVisualCues() {
        Set<CapabilityId> mapped = EnumSet.noneOf(CapabilityId.class);
        for (CapabilityId capability : CapabilityId.values()) {
            if (DecorationStateKind.isDecorationCapability(capability)) {
                mapped.add(capability);
                assertEquals(expectedCue(capability), DecorationVisualCue.forCapability(capability));
            }
        }
        assertEquals(13, mapped.size());
        assertThrows(IllegalArgumentException.class,
                () -> DecorationVisualCue.forCapability(CapabilityId.OBSIDIAN));
        assertThrows(NullPointerException.class,
                () -> DecorationVisualCue.forCapability(null));
    }

    @Test
    void disabledDecorationAndNonDecorationProduceNoDescriptor() {
        BlockLensConfig defaults = BlockLensConfig.defaults();
        assertFalse(DecorationRenderPolicy.describe(
                CapabilityId.STAIRS,
                SemanticState.stairs(Facing.NORTH, Half.BOTTOM, StairShape.STRAIGHT),
                defaults).isPresent());
        assertFalse(DecorationRenderPolicy.describe(
                CapabilityId.OBSIDIAN,
                SemanticState.empty(),
                defaults).isPresent());
    }

    @Test
    void allThirteenCapabilitiesCanBeEnabledSimultaneously() {
        BlockLensConfig config = BlockLensConfig.defaults();
        for (CapabilityId capability : CapabilityId.values()) {
            if (DecorationStateKind.isDecorationCapability(capability)) {
                config = config.withEnabled(capability, true);
            }
        }

        int descriptors = 0;
        for (CapabilityId capability : CapabilityId.values()) {
            if (!DecorationStateKind.isDecorationCapability(capability)) {
                continue;
            }
            Optional<DecorationRenderDescriptor> descriptor = DecorationRenderPolicy.describe(
                    capability,
                    validState(capability),
                    config);
            assertTrue(descriptor.isPresent(), capability::sourceKey);
            assertEquals(capability, descriptor.orElseThrow().capability());
            assertEquals(expectedCue(capability), descriptor.orElseThrow().cue());
            descriptors++;
        }
        assertEquals(13, descriptors);
    }

    @Test
    void stainedGlassOpaqueCueAllowsFullBlockWithNoPaneConnections() {
        BlockLensConfig enabled = BlockLensConfig.defaults().withEnabled(CapabilityId.STAINED_GLASS, true);
        DecorationRenderDescriptor descriptor = DecorationRenderPolicy.describe(
                CapabilityId.STAINED_GLASS,
                SemanticState.connections(false, false, false, false),
                enabled).orElseThrow();
        assertEquals(DecorationVisualCue.OPAQUE_STAINED_GLASS, descriptor.cue());
        assertEquals(0, descriptor.state().connectionMask());
    }

    @Test
    void enabledCapabilitiesRejectSemanticallyIncompleteState() {
        for (CapabilityId capability : CapabilityId.values()) {
            if (!DecorationStateKind.isDecorationCapability(capability)
                    || capability == CapabilityId.STAINED_GLASS) {
                continue;
            }
            BlockLensConfig enabled = BlockLensConfig.defaults().withEnabled(capability, true);
            assertThrows(IllegalArgumentException.class,
                    () -> DecorationRenderPolicy.describe(capability, SemanticState.empty(), enabled),
                    capability::sourceKey);
        }
    }

    @Test
    void descriptorRejectsMismatchedCueAndNullContracts() {
        SemanticState state = SemanticState.facing(Facing.NORTH);
        assertThrows(IllegalArgumentException.class,
                () -> new DecorationRenderDescriptor(
                        CapabilityId.ANVIL,
                        DecorationVisualCue.AXIS_MARKER,
                        state));
        assertThrows(NullPointerException.class,
                () -> DecorationRenderPolicy.describe(null, state, BlockLensConfig.defaults()));
        assertThrows(NullPointerException.class,
                () -> DecorationRenderPolicy.describe(CapabilityId.ANVIL, state, null));

        BlockLensConfig enabled = BlockLensConfig.defaults().withEnabled(CapabilityId.ANVIL, true);
        assertThrows(NullPointerException.class,
                () -> DecorationRenderPolicy.describe(CapabilityId.ANVIL, null, enabled));
    }

    private static DecorationVisualCue expectedCue(CapabilityId capability) {
        return switch (capability) {
            case ANVIL, GLAZED_TERRACOTTA -> DecorationVisualCue.FACING_MARKER;
            case BEEHIVE -> DecorationVisualCue.FACING_LEVEL_MARKER;
            case CAMPFIRE -> DecorationVisualCue.FACING_LIT_MARKER;
            case GRINDSTONE -> DecorationVisualCue.MOUNT_FACE_MARKER;
            case FENCE_GATE -> DecorationVisualCue.FENCE_GATE_STATE_MARKER;
            case FROGLIGHT, WOOD, LOG -> DecorationVisualCue.AXIS_MARKER;
            case SLABS -> DecorationVisualCue.SLAB_STATE_MARKER;
            case STAINED_GLASS -> DecorationVisualCue.OPAQUE_STAINED_GLASS;
            case STAIRS -> DecorationVisualCue.STAIRS_STATE_MARKER;
            case TRAPDOOR -> DecorationVisualCue.TRAPDOOR_STATE_MARKER;
            default -> throw new IllegalArgumentException(capability.sourceKey());
        };
    }

    private static SemanticState validState(CapabilityId capability) {
        return switch (capability) {
            case ANVIL, GLAZED_TERRACOTTA -> SemanticState.facing(Facing.NORTH);
            case BEEHIVE -> SemanticState.beehive(Facing.EAST, 4);
            case CAMPFIRE -> SemanticState.campfire(Facing.SOUTH, true);
            case GRINDSTONE -> SemanticState.grindstone(MountFace.WALL, Facing.WEST);
            case FENCE_GATE -> SemanticState.fenceGate(Facing.NORTH, true, true);
            case FROGLIGHT, WOOD, LOG -> SemanticState.axis(Axis.Z);
            case SLABS -> SemanticState.slab(SlabType.TOP);
            case STAINED_GLASS -> SemanticState.connections(true, false, true, false);
            case STAIRS -> SemanticState.stairs(Facing.EAST, Half.BOTTOM, StairShape.OUTER_LEFT);
            case TRAPDOOR -> SemanticState.trapdoor(Facing.WEST, Half.TOP, true);
            default -> throw new IllegalArgumentException(capability.sourceKey());
        };
    }
}

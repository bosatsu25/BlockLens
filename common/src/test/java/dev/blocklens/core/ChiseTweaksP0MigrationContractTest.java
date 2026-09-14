package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.render.NetherTweaksTargetCatalog;
import dev.blocklens.core.render.ResourceTargetCatalog;
import dev.blocklens.core.render.VisibilityTargetCatalog;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Executable contract for Issue #22 P0: ChiseTweaks visual overlap absorbed into BlockLens. */
final class ChiseTweaksP0MigrationContractTest {
    @Test
    void materialHighlightsMapToBlockLensResourceCapabilities() {
        Set<CapabilityId> expected = Set.of(
                CapabilityId.OBSIDIAN,
                CapabilityId.CRYING_OBSIDIAN,
                CapabilityId.ANCIENT_DEBRIS,
                CapabilityId.DIAMOND_ORE,
                CapabilityId.DEEPSLATE_DIAMOND_ORE,
                CapabilityId.GOLD_ORE,
                CapabilityId.DEEPSLATE_GOLD_ORE,
                CapabilityId.EMERALD_ORE,
                CapabilityId.DEEPSLATE_EMERALD_ORE,
                CapabilityId.COAL_ORE,
                CapabilityId.DEEPSLATE_COAL_ORE,
                CapabilityId.IRON_ORE,
                CapabilityId.DEEPSLATE_IRON_ORE,
                CapabilityId.COPPER_ORE,
                CapabilityId.DEEPSLATE_COPPER_ORE,
                CapabilityId.LAPIS_ORE,
                CapabilityId.DEEPSLATE_LAPIS_ORE,
                CapabilityId.REDSTONE_ORE,
                CapabilityId.DEEPSLATE_REDSTONE_ORE,
                CapabilityId.NETHER_GOLD_ORE,
                CapabilityId.NETHER_QUARTZ_ORE);

        Set<CapabilityId> actual = java.util.Arrays.stream(CapabilityId.values())
                .filter(capability -> capability.category() == CapabilityId.Category.RESOURCE)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        assertEquals(expected, actual);
        assertEquals(21, ResourceTargetCatalog.totalBindingCount());
    }

    @Test
    void hiddenSurfaceTraceReusesExistingBlockLensVisibilityCapabilities() {
        assertEquals(Set.of("blue_ice"), Set.copyOf(VisibilityTargetCatalog.targets(CapabilityId.BLUE_ICE)));
        assertEquals(Set.of("powder_snow"), Set.copyOf(VisibilityTargetCatalog.targets(CapabilityId.POWDER_SNOW)));
        assertEquals(Set.of("sculk_catalyst"), Set.copyOf(VisibilityTargetCatalog.targets(CapabilityId.SCULK_CATALYST)));
        assertEquals(20, VisibilityTargetCatalog.targets(CapabilityId.DEAD_CORAL).size());
    }

    @Test
    void fineThreadTraceMapsToOneStringTweaksCapability() {
        assertEquals(Set.of("tripwire", "tripwire_hook"),
                Set.copyOf(VisibilityTargetCatalog.targets(CapabilityId.STRING_TWEAKS)));
    }

    @Test
    void netherPaletteUsesAdditiveTargetUnion() {
        assertEquals(27, NetherTweaksTargetCatalog.m0Targets().size());
        assertEquals(28, NetherTweaksTargetCatalog.totalBindingCount());
        assertTrue(NetherTweaksTargetCatalog.contains("obsidian"),
                "BlockLens M0 target must remain present");
        assertTrue(NetherTweaksTargetCatalog.contains("polished_basalt"),
                "ChiseTweaks-only Nether target must be absorbed");
    }

    @Test
    void originalM0CapabilityBitsRemainStable() {
        assertEquals(36, CapabilityId.STRING_TWEAKS.ordinal());
        assertEquals(37, CapabilityId.CRYING_OBSIDIAN.ordinal());
        assertEquals(38, CapabilityId.NETHER_GOLD_ORE.ordinal());
        assertEquals(39, CapabilityId.NETHER_QUARTZ_ORE.ordinal());
    }
}

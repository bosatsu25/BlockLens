package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.core.CapabilityId;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class FullTargetCatalogTest {
    @Test
    void everyCapabilityHasAnExactCompiledTargetContract() {
        for (CapabilityId capability : CapabilityId.values()) {
            assertFalse(BlockLensTargetCatalog.targets(capability).isEmpty(), capability.sourceKey());
        }
        assertEquals(37, CapabilityId.values().length);
        assertEquals(323, BlockLensTargetCatalog.totalBindingCount());
    }

    @Test
    void resourceScopeIsExactlyEighteenSingleTargetBindings() {
        assertEquals(18, ResourceTargetCatalog.totalBindingCount());
        for (CapabilityId capability : CapabilityId.values()) {
            if (capability.category() == CapabilityId.Category.RESOURCE) {
                assertEquals(1, ResourceTargetCatalog.targets(capability).size(), capability.sourceKey());
            } else {
                assertTrue(ResourceTargetCatalog.targets(capability).isEmpty(), capability.sourceKey());
            }
        }
        assertTrue(ResourceTargetCatalog.contains(CapabilityId.OBSIDIAN, "obsidian"));
        assertFalse(ResourceTargetCatalog.contains(CapabilityId.OBSIDIAN, "crying_obsidian"));
    }

    @Test
    void netherTweaksScopeIsExactAndOverlapsRemainComposable() {
        assertEquals(27, NetherTweaksTargetCatalog.totalBindingCount());
        assertEquals(27, new HashSet<>(NetherTweaksTargetCatalog.targets(CapabilityId.NETHER_TWEAKS)).size());
        assertEquals(
                "e20ab1991f3b4da58f67b94585ec79f5bb73e6d872090480cac96c56f8b73e40",
                NetherTweaksTargetCatalog.DIRECT_PATH_SET_SHA256);
        assertTrue(NetherTweaksTargetCatalog.contains("obsidian"));
        assertTrue(NetherTweaksTargetCatalog.contains("crimson_stem"));
        assertTrue(NetherTweaksTargetCatalog.contains("warped_stem"));
        assertFalse(NetherTweaksTargetCatalog.contains("ancient_debris"));
        assertTrue(NetherTweaksTargetCatalog.targets(CapabilityId.STRING_TWEAKS).isEmpty());

        Set<String> uniqueTargets = new HashSet<>();
        for (CapabilityId capability : CapabilityId.values()) {
            uniqueTargets.addAll(BlockLensTargetCatalog.targets(capability));
        }
        assertEquals(320, uniqueTargets.size());
        assertTrue(BlockLensTargetCatalog.targets(CapabilityId.OBSIDIAN).contains("obsidian"));
        assertTrue(BlockLensTargetCatalog.targets(CapabilityId.NETHER_TWEAKS).contains("obsidian"));
        assertTrue(BlockLensTargetCatalog.targets(CapabilityId.LOG).contains("crimson_stem"));
        assertTrue(BlockLensTargetCatalog.targets(CapabilityId.NETHER_TWEAKS).contains("crimson_stem"));
    }

    @Test
    void nullsAreRejectedAndCatalogsDoNotBroadenTargets() {
        assertThrows(NullPointerException.class, () -> BlockLensTargetCatalog.targets(null));
        assertThrows(NullPointerException.class, () -> ResourceTargetCatalog.targets(null));
        assertThrows(NullPointerException.class, () -> ResourceTargetCatalog.contains(CapabilityId.DIAMOND_ORE, null));
        assertThrows(NullPointerException.class, () -> NetherTweaksTargetCatalog.targets(null));
        assertThrows(NullPointerException.class, () -> NetherTweaksTargetCatalog.contains(null));
        assertFalse(BlockLensTargetCatalog.targets(CapabilityId.DIAMOND_ORE).contains("deepslate_diamond_ore"));
    }
}

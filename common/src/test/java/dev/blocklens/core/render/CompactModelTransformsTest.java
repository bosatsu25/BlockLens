package dev.blocklens.core.render;

import static dev.blocklens.core.render.CompactModelTransforms.Context.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class CompactModelTransformsTest {
    @Test void ordinaryBlockTransformIncludesLeftHandAndShelf() {
        var p = CompactModelTransforms.find("minecraft", "block/block", "vanilla", true, false, false);
        assertNotNull(p);
        assertEquals(.28f, p.get(FIRST_RIGHT).sx());
        assertEquals(225f, p.get(FIRST_LEFT).ry());
        assertEquals(.3f / 16, p.get(FIRST_RIGHT).ty());
        assertEquals(1f, p.get(SHELF).sx());
        assertThrows(UnsupportedOperationException.class, () -> p.clear());
    }
    @Test void itemHelperParentIsResolvedWithoutBundlingTheHelper() {
        var p = CompactModelTransforms.find("minecraft", "item/blaze_rod", "vanilla", false, true, false);
        assertNotNull(p);
        assertEquals(.4f, p.get(FIRST_RIGHT).sx());
        assertEquals(.85f, p.get(THIRD_RIGHT).sx());
        assertNull(p.get(GROUND), "Inherited ground pose must remain owned by the active parent");
        assertEquals(90f, p.get(FIRST_LEFT).ry());
    }
    @Test void toolAndStateExceptionsAreIndependent() {
        var p = CompactModelTransforms.find("minecraft", "item/shield_blocking", "vanilla", false, false, true);
        assertNotNull(p);
        assertEquals(.95f, p.get(FIRST_RIGHT).sx());
        assertEquals(-10f / 16, p.get(FIRST_RIGHT).tx());
        assertEquals(5f / 16, p.get(FIRST_LEFT).tx());
        assertNull(CompactModelTransforms.find("minecraft", "item/shield_blocking", "vanilla", true, true, false));
        var spear = CompactModelTransforms.find("minecraft", "item/spear_in_hand", "vanilla", false, false, true);
        assertEquals(1f, spear.get(FIRST_RIGHT).sx());
        assertEquals(.68f, spear.get(FIRST_RIGHT).sz());
    }
    @Test void parentChangedBoneKeepsItsHeadException() {
        var p = CompactModelTransforms.find("minecraft", "item/bone", "vanilla", false, true, false);
        assertNotNull(p);
        assertEquals(-45f, p.get(HEAD).rz());
        assertEquals(.4f, p.get(FIRST_RIGHT).sx());
    }
    @Test void modernBedAliasesKeepTheBlockGroup() {
        var p = CompactModelTransforms.find("minecraft", "block/template_bed_head", "vanilla", true, false, false);
        assertNotNull(p);
        assertEquals(.27f, p.get(FIRST_RIGHT).sx());
        assertEquals(p, CompactModelTransforms.find("minecraft", "block/template_bed_foot", "vanilla", true, false, false));
    }
    @Test void unownedUnknownAndDisabledModelsAreUnchanged() {
        assertNull(CompactModelTransforms.find("minecraft", "block/block", "vanilla", false, false, false));
        assertNull(CompactModelTransforms.find("minecraft", "block/block", "file/external", true, true, true));
        assertNull(CompactModelTransforms.find("example", "block/block", "vanilla", true, true, true));
        assertNull(CompactModelTransforms.find("minecraft", "block/unknown", "vanilla", true, true, true));
        assertNull(CompactModelTransforms.find(null, null, null, true, true, true));
    }
    @Test void newControlsAppendWithoutChangingExistingBitsOrDefaults() {
        var keys = dev.blocklens.core.CapabilityId.bySourceKey();
        String[] added = {"others.compact_blocks", "others.compact_items", "others.compact_tools"};
        for (int i = 0; i < added.length; i++) {
            var id = keys.get(added[i]);
            assertNotNull(id);
            assertEquals(58 + i, id.bitPosition());
            assertFalse(id.defaultEnabled());
            var loaded = dev.blocklens.core.BlockLensConfigCodec.decode(added[i] + "=true\n");
            assertTrue(loaded.isEnabled(id));
        }
        assertEquals(57, keys.get("outline.pale_oak_neutral").bitPosition());
        assertEquals(55, keys.get("builder.pattern_consistency").bitPosition());
    }
    @Test void disabledGroupsAndNullPathStayOutsideSelection() {
        assertNull(CompactModelTransforms.find("minecraft", "block/block", "vanilla", false, true, true));
        assertNull(CompactModelTransforms.find("minecraft", "item/generated", "vanilla", true, false, true));
        assertNull(CompactModelTransforms.find("minecraft", "item/handheld", "vanilla", true, true, false));
        assertNull(CompactModelTransforms.find("minecraft", null, "vanilla", true, true, true));
    }
    @Test void helperReplacementDoesNotFreezeParentOwnedContexts() {
        for (String role : new String[]{"item/bamboo", "item/blaze_rod", "item/breeze_rod", "item/debug_stick", "item/stick", "item/bone"}) {
            var profile = CompactModelTransforms.find("minecraft", role, "vanilla", false, true, false);
            assertNull(profile.get(GROUND), role);
            assertNull(profile.get(FIXED), role);
            if (!role.equals("item/bone")) assertNull(profile.get(HEAD), role);
        }
    }
}

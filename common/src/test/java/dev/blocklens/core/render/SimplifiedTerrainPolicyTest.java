package dev.blocklens.core.render;

import static org.junit.jupiter.api.Assertions.*;
import dev.blocklens.core.scene.EaterClearPreset;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

final class SimplifiedTerrainPolicyTest {
    @Test void independentSavedControlDoesNotReuseRetiredBitsOrHidePreset() {
        var id=dev.blocklens.core.CapabilityId.SIMPLIFIED_TERRAIN;
        assertEquals(61,id.bitPosition());assertFalse(id.defaultEnabled());
        var before=dev.blocklens.core.BlockLensConfig.defaults();var after=before.withEnabled(id,true);
        assertFalse(after.isEnabled(dev.blocklens.core.CapabilityId.BLOCK_FILTER));
        assertSame(before.sceneFilterOptions(),after.sceneFilterOptions());
        assertTrue(dev.blocklens.core.BlockLensConfigCodec.decode(dev.blocklens.core.BlockLensConfigCodec.encode(after)).isEnabled(id));
        assertTrue(VisualResourceReloadPolicy.requiresReload(before,after));
        assertTrue(VisualResourceReloadPolicy.requiresReload(after,before));
    }
    @Test void frozenSourceInventoryAndSpecializedFamiliesRemainBounded() {
        assertEquals(207, SimplifiedTerrainPolicy.sourcePaths().size());
        assertEquals(207, new HashSet<>(SimplifiedTerrainPolicy.sourcePaths()).size());
        for (String id : EaterClearPreset.sourceIds())
            assertEquals(SimplifiedTerrainPolicy.Kind.CUBE,
                    SimplifiedTerrainPolicy.kind("minecraft", id.substring(10)));
        for (String path : new String[]{"big_dripleaf", "big_dripleaf_stem", "small_dripleaf",
                "pointed_dripstone", "vine", "glow_lichen", "moss_carpet", "leaf_litter"})
            assertEquals(SimplifiedTerrainPolicy.Kind.SURFACE, SimplifiedTerrainPolicy.kind("minecraft",path));
        assertEquals(SimplifiedTerrainPolicy.Kind.SHORT_GRASS,SimplifiedTerrainPolicy.kind("minecraft","short_grass"));
        assertEquals(SimplifiedTerrainPolicy.Kind.TALL_GRASS,SimplifiedTerrainPolicy.kind("minecraft","tall_grass"));
        assertEquals(SimplifiedTerrainPolicy.Kind.BUSH,SimplifiedTerrainPolicy.kind("minecraft","bush"));
        assertThrows(UnsupportedOperationException.class, () -> SimplifiedTerrainPolicy.sourcePaths().clear());
    }
    @Test void disabledExternalAndUnknownInputsNeverReplaceActiveModels() {
        assertEquals(SimplifiedTerrainPolicy.Kind.CUBE,SimplifiedTerrainPolicy.select(true,"minecraft","stone","vanilla"));
        for (boolean enabled : new boolean[]{true,false}) {
            assertEquals(SimplifiedTerrainPolicy.Kind.NONE,SimplifiedTerrainPolicy.select(enabled,"other","stone","vanilla"));
            assertEquals(SimplifiedTerrainPolicy.Kind.NONE,SimplifiedTerrainPolicy.select(enabled,"minecraft","diamond_ore","vanilla"));
            assertEquals(SimplifiedTerrainPolicy.Kind.NONE,SimplifiedTerrainPolicy.select(enabled,"minecraft","stone","external"));
            assertEquals(SimplifiedTerrainPolicy.Kind.NONE,SimplifiedTerrainPolicy.select(enabled,"minecraft","stone",null));
        }
        assertEquals(SimplifiedTerrainPolicy.Kind.NONE,SimplifiedTerrainPolicy.select(false,"minecraft","stone","vanilla"));
        assertEquals(SimplifiedTerrainPolicy.Kind.NONE,SimplifiedTerrainPolicy.kind(null,"stone"));
        assertEquals(SimplifiedTerrainPolicy.Kind.NONE,SimplifiedTerrainPolicy.kind("minecraft",null));
    }
    @Test void generatedMasksAreOpaqueOrSparseWithoutCopiedArtwork() {
        for (var kind : SimplifiedTerrainPolicy.Kind.values()) {
            int visible=0; var tones=new HashSet<Integer>();
            for(int y=0;y<16;y++) for(int x=0;x<16;x++) {
                int pixel=SimplifiedTerrainPolicy.pixel(kind,x,y), alpha=pixel>>>24;
                assertTrue(alpha==0 || alpha==255);
                if(alpha==255) { visible++; int gray=pixel&255; if(kind==SimplifiedTerrainPolicy.Kind.CUBE || kind==SimplifiedTerrainPolicy.Kind.SURFACE) assertTrue(gray>=35 && gray<=49); else assertEquals(135,gray);
                    assertEquals(gray,pixel>>>8&255);assertEquals(gray,pixel>>>16&255);tones.add(gray); }
            }
            assertEquals(switch(kind) {case NONE->0;case CUBE,SURFACE->256;case SHORT_GRASS->40;case TALL_GRASS->32;case BUSH->38;},visible);
            if(kind==SimplifiedTerrainPolicy.Kind.CUBE || kind==SimplifiedTerrainPolicy.Kind.SURFACE) assertTrue(tones.size()>4,"Flat fill lost clean-room grain");
        }
        assertEquals(0xFF232323,SimplifiedTerrainPolicy.pixel(SimplifiedTerrainPolicy.Kind.CUBE,0,0));
        assertEquals(0xFF2D2D2D,SimplifiedTerrainPolicy.pixel(SimplifiedTerrainPolicy.Kind.CUBE,1,0));
        assertEquals(0xFF313131,SimplifiedTerrainPolicy.pixel(SimplifiedTerrainPolicy.Kind.CUBE,0,1));
        assertEquals(0xFF272727,SimplifiedTerrainPolicy.pixel(SimplifiedTerrainPolicy.Kind.CUBE,3,5));
        assertEquals(0xFF2E2E2E,SimplifiedTerrainPolicy.pixel(SimplifiedTerrainPolicy.Kind.SURFACE,5,3));
        assertThrows(IllegalArgumentException.class,()->SimplifiedTerrainPolicy.pixel(null,0,0));
        assertThrows(IllegalArgumentException.class,()->SimplifiedTerrainPolicy.pixel(SimplifiedTerrainPolicy.Kind.CUBE,16,0));
        assertThrows(IllegalArgumentException.class,()->SimplifiedTerrainPolicy.pixel(SimplifiedTerrainPolicy.Kind.CUBE,0,-1));
        assertThrows(IllegalArgumentException.class,()->SimplifiedTerrainPolicy.pixel(SimplifiedTerrainPolicy.Kind.CUBE,-1,0));
        assertThrows(IllegalArgumentException.class,()->SimplifiedTerrainPolicy.pixel(SimplifiedTerrainPolicy.Kind.CUBE,0,16));
    }
}
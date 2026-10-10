package dev.blocklens.core.analyzer;

import static org.junit.jupiter.api.Assertions.*;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import org.junit.jupiter.api.Test;

final class AnalyzerOptionsContractTest {
    @Test void documentedLimitsAndDefaultsAreStableForEveryField() {
        int[][] minimum = {{16,16,8,5},{1,1,1,5},{1,1,1,5},{1,1,1,5},{1,1,1,5}};
        int[][] maximum = {{256,256,128,100},{8,5,24,100},{32,16,16,100},{32,16,16,100},{32,16,24,100}};
        int[][] defaults = {{64,64,64,20},{5,3,12,10},{16,8,8,20},{16,8,8,20},{16,8,12,20}};
        var options = AnalyzerOptions.defaults();
        for (int kind = 0; kind < 5; kind++) {
            for (int field = 0; field < 4; field++) {
                assertEquals(minimum[kind][field], AnalyzerOptions.minimum(kind, field));
                assertEquals(maximum[kind][field], AnalyzerOptions.maximum(kind, field));
                assertEquals(defaults[kind][field], options.value(kind, field));
                final int analyzer = kind;
                final int option = field;
                assertThrows(IllegalArgumentException.class,
                        () -> options.with(analyzer, option, minimum[analyzer][option] - 1));
                assertThrows(IllegalArgumentException.class,
                        () -> options.with(analyzer, option, maximum[analyzer][option] + 1));
                assertEquals(minimum[kind][field], options.with(kind, field, minimum[kind][field]).value(kind, field));
                assertEquals(maximum[kind][field], options.with(kind, field, maximum[kind][field]).value(kind, field));
            }
        }
    }

    @Test void sphereAxesRemainLinkedWhileOtherFieldsRemainIndependent() {
        var original = AnalyzerOptions.defaults();
        var sphere = original.with(0, 1, 128);
        assertEquals(128, sphere.value(0, 0));
        assertEquals(128, sphere.value(0, 1));
        assertEquals(64, sphere.value(0, 2));
        var markers = original.with(0, 2, 128);
        assertEquals(64, markers.value(0, 0));
        assertEquals(64, markers.value(0, 1));
        assertEquals(128, markers.value(0, 2));
        var lava = original.with(1, 1, 5);
        assertEquals(5, lava.value(1, 0));
        assertEquals(5, lava.value(1, 1));
        assertEquals(3, original.value(1, 1));
        assertEquals(original, AnalyzerOptions.defaults());
        assertEquals(original.hashCode(), AnalyzerOptions.defaults().hashCode());
        assertNotEquals(original, lava);
        assertNotEquals(original, null);
        assertNotEquals(original, "options");
        assertEquals(original, original.with(1, 1, 3));
    }

    @Test void analyzerMaskNeverAbsorbsLegacyControlsAndEachAnalyzerIsIndependent() {
        var original = BlockLensConfig.defaults();
        long originalMask = original.enabledMask();
        assertEquals(0, AnalyzerCatalog.mask(original));
        assertEquals(-1, AnalyzerCatalog.index(CapabilityId.ANVIL));
        var all = original;
        for (int kind = 0; kind < 5; kind++) {
            CapabilityId capability = AnalyzerCatalog.CAPABILITIES.get(kind);
            assertEquals(kind, AnalyzerCatalog.index(capability));
            var single = original.withEnabled(capability, true).withEnabled(CapabilityId.ANVIL, true);
            assertEquals(1 << kind, AnalyzerCatalog.mask(single));
            assertEquals(0, AnalyzerCatalog.mask(single.withEnabled(capability, false)));
            all = all.withEnabled(capability, true);
        }
        assertEquals(31, AnalyzerCatalog.mask(all));
        assertEquals(originalMask, original.enabledMask());
    }

    @Test void optionKeysAndImmutableCatalogKeepConfigurationOwnershipExplicit() {
        String[] keys = {"analyzer.debris_analyzer", "analyzer.lava_analyzer", "analyzer.beacon_range",
                "analyzer.lightning_rod_range", "analyzer.villager_job_site_links"};
        String[] fields = {"radius", "vertical_radius", "markers", "interval"};
        for (int kind = 0; kind < 5; kind++) {
            for (int field = 0; field < 4; field++) {
                assertEquals(keys[kind] + "." + fields[field], AnalyzerCatalog.optionKey(kind, field));
            }
        }
        assertThrows(UnsupportedOperationException.class,
                () -> AnalyzerCatalog.CAPABILITIES.add(CapabilityId.ANVIL));
        assertThrows(UnsupportedOperationException.class, () -> AnalyzerCatalog.FIELDS.add("extra"));
    }
}
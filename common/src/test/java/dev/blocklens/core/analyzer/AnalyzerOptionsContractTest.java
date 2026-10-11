package dev.blocklens.core.analyzer;

import static org.junit.jupiter.api.Assertions.*;
import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import org.junit.jupiter.api.Test;

final class AnalyzerOptionsContractTest {
    @Test void documentedLimitsAndDefaultsAreStableForEveryField() {
        int[][] minimum = {{1,1,1,5},{1,1,1,5},{1,1,1,5},{1,1,1,5}};
        int[][] maximum = {{8,5,24,100},{32,16,16,100},{32,16,16,100},{32,16,24,100}};
        int[][] defaults = {{5,3,12,10},{16,8,8,20},{16,8,8,20},{16,8,12,20}};
        var options = AnalyzerOptions.defaults();
        for (int kind = 0; kind < 4; kind++) {
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

    @Test void analyzerFieldsRemainIndependentAndImmutable() {
        var original = AnalyzerOptions.defaults();
        var lavaRadius = original.with(0, 0, 8);
        assertEquals(8, lavaRadius.value(0, 0));
        assertEquals(3, lavaRadius.value(0, 1));
        var beaconMarkers = original.with(1, 2, 16);
        assertEquals(8, beaconMarkers.value(1, 2));
        assertEquals(16, original.value(1, 2));
        assertEquals(original, AnalyzerOptions.defaults());
        assertEquals(original.hashCode(), AnalyzerOptions.defaults().hashCode());
        assertNotEquals(original, lavaRadius);
        assertNotEquals(original, null);
        assertNotEquals(original, "options");
        assertEquals(original, original.with(0, 1, 3));
    }

    @Test void analyzerMaskNeverAbsorbsLegacyControlsAndEachAnalyzerIsIndependent() {
        var original = BlockLensConfig.defaults();
        long originalMask = original.enabledMask();
        assertEquals(0, AnalyzerCatalog.mask(original));
        assertEquals(-1, AnalyzerCatalog.index(CapabilityId.ANVIL));
        var all = original;
        for (int kind = 0; kind < 4; kind++) {
            CapabilityId capability = AnalyzerCatalog.CAPABILITIES.get(kind);
            assertEquals(kind, AnalyzerCatalog.index(capability));
            var single = original.withEnabled(capability, true).withEnabled(CapabilityId.ANVIL, true);
            assertEquals(1 << kind, AnalyzerCatalog.mask(single));
            assertEquals(0, AnalyzerCatalog.mask(single.withEnabled(capability, false)));
            all = all.withEnabled(capability, true);
        }
        assertEquals(15, AnalyzerCatalog.mask(all));
        assertEquals(originalMask, original.enabledMask());
    }

    @Test void optionKeysAndImmutableCatalogKeepConfigurationOwnershipExplicit() {
        String[] keys = {"analyzer.lava_analyzer", "analyzer.beacon_range",
                "analyzer.lightning_rod_range", "analyzer.villager_job_site_links"};
        String[] fields = {"radius", "vertical_radius", "markers", "interval"};
        for (int kind = 0; kind < 4; kind++) {
            for (int field = 0; field < 4; field++) {
                assertEquals(keys[kind] + "." + fields[field], AnalyzerCatalog.optionKey(kind, field));
            }
        }
        assertThrows(UnsupportedOperationException.class,
                () -> AnalyzerCatalog.CAPABILITIES.add(CapabilityId.ANVIL));
        assertThrows(UnsupportedOperationException.class, () -> AnalyzerCatalog.FIELDS.add("extra"));
    }
}
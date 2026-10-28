package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.*;

import dev.blocklens.core.analyzer.AnalyzerOptions;
import dev.blocklens.core.scene.SceneFilterOptions;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class BlockLensConfigTest {

    @Test
    void equalsAndHashCodeAdhereToContractForDefaults() {
        var first = BlockLensConfig.defaults();
        var second = BlockLensConfig.defaults();

        // Reflexive
        assertEquals(first, first);
        assertEquals(first.hashCode(), first.hashCode());

        // Symmetric
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());

        // Non-nullity
        assertNotEquals(null, first);
        assertNotEquals("non-config", first);
    }

    @Test
    void equalsIsSensitiveToEveryConfigDimension() {
        var baseline = BlockLensConfig.defaults();

        // 1. Capability enabledMask dimension
        for (CapabilityId capability : CapabilityId.values()) {
            boolean current = baseline.isEnabled(capability);
            var mutated = baseline.withEnabled(capability, !current);
            assertNotEquals(baseline, mutated, "Config must not equal when " + capability + " is toggled");
            assertNotEquals(mutated, baseline);
            assertEquals(mutated, baseline.withEnabled(capability, !current));
            assertEquals(mutated.hashCode(), baseline.withEnabled(capability, !current).hashCode());
        }

        // 2. ComfortOptions dimension
        var mutatedComfort = baseline.withComfortOptions(new ComfortOptions(2, 40, 40, 40));
        assertNotEquals(baseline, mutatedComfort);
        assertEquals(mutatedComfort, baseline.withComfortOptions(new ComfortOptions(2, 40, 40, 40)));
        assertEquals(mutatedComfort.hashCode(), baseline.withComfortOptions(new ComfortOptions(2, 40, 40, 40)).hashCode());

        // 3. AnalyzerOptions dimension
        var mutatedAnalyzer = baseline.withAnalyzerOptions(AnalyzerOptions.defaults().with(0, 0, 8));
        assertNotEquals(baseline, mutatedAnalyzer);
        assertEquals(mutatedAnalyzer, baseline.withAnalyzerOptions(AnalyzerOptions.defaults().with(0, 0, 8)));
        assertEquals(mutatedAnalyzer.hashCode(), baseline.withAnalyzerOptions(AnalyzerOptions.defaults().with(0, 0, 8)).hashCode());

        // 4. SceneFilterOptions dimension
        var mutatedScene = baseline.withSceneFilterOptions(SceneFilterOptions.defaults().withEntityPreset(1));
        assertNotEquals(baseline, mutatedScene);
        assertEquals(mutatedScene, baseline.withSceneFilterOptions(SceneFilterOptions.defaults().withEntityPreset(1)));
        assertEquals(mutatedScene.hashCode(), baseline.withSceneFilterOptions(SceneFilterOptions.defaults().withEntityPreset(1)).hashCode());
    }

    @Test
    void fromOverridesProducesEquivalentConfig() {
        var defaults = BlockLensConfig.defaults();
        var fromOverrides = BlockLensConfig.fromOverrides(Map.of());

        assertEquals(defaults, fromOverrides);
        assertEquals(defaults.hashCode(), fromOverrides.hashCode());
        assertEquals(defaults.enabledMask(), fromOverrides.enabledMask());
        assertEquals(defaults.comfortOptions(), fromOverrides.comfortOptions());
        assertEquals(defaults.analyzerOptions(), fromOverrides.analyzerOptions());
        assertEquals(defaults.sceneFilterOptions(), fromOverrides.sceneFilterOptions());
    }

    @Test
    void asMapReturnsUnmodifiableView() {
        var config = BlockLensConfig.defaults();
        var map = config.asMap();
        assertThrows(UnsupportedOperationException.class, () -> map.put(CapabilityId.ANVIL, true));
    }

    @Test
    void nullArgumentsAreRejected() {
        var config = BlockLensConfig.defaults();
        assertThrows(NullPointerException.class, () -> config.isEnabled(null));
        assertThrows(NullPointerException.class, () -> config.withEnabled(null, true));
        assertThrows(NullPointerException.class, () -> config.withComfortOptions(null));
        assertThrows(NullPointerException.class, () -> config.withAnalyzerOptions(null));
        assertThrows(NullPointerException.class, () -> config.withSceneFilterOptions(null));
        assertThrows(NullPointerException.class, () -> BlockLensConfig.fromOverrides(null));
        assertThrows(NullPointerException.class, () -> config.sameCapabilities(null));
    }

    @Test
    void twoWordMaskKeepsBitsPastSixtyThreeApart() {
        long[] words = new long[2];
        for (int position : new int[] {0, 63, 64, 127}) {
            BlockLensConfig.set(words, position);
        }
        assertArrayEquals(new long[] {1L | Long.MIN_VALUE, 1L | Long.MIN_VALUE}, words);
        // Bit 64 is word 1's bit 0, never word 0's: the alias Java's shift masking would create.
        long[] high = new long[2];
        BlockLensConfig.set(high, 64);
        assertArrayEquals(new long[] {0L, 1L}, high);
        assertTrue(BlockLensConfig.isSet(0L, 1L, 64));
        assertFalse(BlockLensConfig.isSet(1L, 0L, 64));
        assertTrue(BlockLensConfig.isSet(Long.MIN_VALUE, 0L, 63));
        assertFalse(BlockLensConfig.isSet(0L, Long.MIN_VALUE, 63));
        assertTrue(BlockLensConfig.isSet(0L, Long.MIN_VALUE, 127));
        for (int outside : new int[] {-1, 128}) {
            assertThrows(IllegalStateException.class, () -> BlockLensConfig.set(new long[2], outside));
            assertThrows(IllegalStateException.class, () -> BlockLensConfig.isSet(-1L, -1L, outside));
        }
    }

    @Test
    void capabilityComparisonsCoverBothWordsAndIgnoreOtherOptions() {
        var defaults = BlockLensConfig.defaults();
        var allOn = defaults;
        for (CapabilityId capability : CapabilityId.values()) {
            allOn = allOn.withEnabled(capability, true);
        }
        assertEquals(CapabilityId.values().length, allOn.enabledCount());
        assertEquals(0L, allOn.enabledMaskHigh(), "every current capability fits word 0");
        assertEquals(Long.bitCount(defaults.enabledMask()), defaults.enabledCount());

        var otherOptions = defaults.withComfortOptions(new ComfortOptions(2, 40, 40, 40));
        assertTrue(defaults.sameCapabilities(otherOptions));
        assertNotEquals(defaults, otherOptions);
        assertFalse(defaults.sameCapabilities(allOn));
        assertFalse(allOn.sameCapabilities(defaults));
    }
}

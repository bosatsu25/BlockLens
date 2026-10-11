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
        var mutatedAnalyzer = baseline.withAnalyzerOptions(AnalyzerOptions.defaults().with(0, 0, 32));
        assertNotEquals(baseline, mutatedAnalyzer);
        assertEquals(mutatedAnalyzer, baseline.withAnalyzerOptions(AnalyzerOptions.defaults().with(0, 0, 32)));
        assertEquals(mutatedAnalyzer.hashCode(), baseline.withAnalyzerOptions(AnalyzerOptions.defaults().with(0, 0, 32)).hashCode());

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
    }
}

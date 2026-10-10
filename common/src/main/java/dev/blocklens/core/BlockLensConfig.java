package dev.blocklens.core;

import dev.blocklens.core.analyzer.AnalyzerOptions;
import dev.blocklens.core.scene.SceneFilterOptions;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Native BlockLens configuration independent of Minecraft/Fabric APIs. */
public final class BlockLensConfig {
    private final EnumMap<CapabilityId, Boolean> enabled;
    private final long enabledMask;
    private final ComfortOptions comfortOptions;
    private final AnalyzerOptions analyzerOptions;
    private final SceneFilterOptions sceneFilterOptions;

    private BlockLensConfig(EnumMap<CapabilityId, Boolean> enabled, ComfortOptions comfortOptions,
            AnalyzerOptions analyzerOptions, SceneFilterOptions sceneFilterOptions) {
        this.enabled = enabled;
        this.analyzerOptions = Objects.requireNonNull(analyzerOptions, "analyzerOptions");
        this.comfortOptions = Objects.requireNonNull(comfortOptions, "comfortOptions");
        this.sceneFilterOptions = Objects.requireNonNull(sceneFilterOptions, "sceneFilterOptions");
        long mask = 0L;
        for (CapabilityId capability : CapabilityId.values()) {
            if (Boolean.TRUE.equals(enabled.get(capability))) {
                mask |= bit(capability);
            }
        }
        this.enabledMask = mask;
    }

    public static BlockLensConfig defaults() {
        EnumMap<CapabilityId, Boolean> values = new EnumMap<>(CapabilityId.class);
        for (CapabilityId capability : CapabilityId.values()) {
            values.put(capability, capability.defaultEnabled());
        }
        return new BlockLensConfig(values, ComfortOptions.defaults(), AnalyzerOptions.defaults(), SceneFilterOptions.defaults());
    }

    static BlockLensConfig fromOverrides(Map<CapabilityId, Boolean> overrides) {
        return fromOverrides(overrides, ComfortOptions.defaults());
    }

    static BlockLensConfig fromOverrides(Map<CapabilityId, Boolean> overrides, ComfortOptions comfortOptions) {
        Objects.requireNonNull(overrides, "overrides");
        EnumMap<CapabilityId, Boolean> values = new EnumMap<>(CapabilityId.class);
        for (CapabilityId capability : CapabilityId.values()) {
            values.put(capability, overrides.getOrDefault(capability, capability.defaultEnabled()));
        }
        return new BlockLensConfig(values, comfortOptions, AnalyzerOptions.defaults(), SceneFilterOptions.defaults());
    }

    public boolean isEnabled(CapabilityId capability) {
        return (enabledMask & bit(Objects.requireNonNull(capability, "capability"))) != 0L;
    }

    /** Immutable primitive capability mask for render/runtime hot paths. */
    public long enabledMask() {
        return enabledMask;
    }

    public BlockLensConfig withEnabled(CapabilityId capability, boolean value) {
        Objects.requireNonNull(capability, "capability");
        EnumMap<CapabilityId, Boolean> copy = new EnumMap<>(enabled);
        copy.put(capability, value);
        return new BlockLensConfig(copy, comfortOptions, analyzerOptions, sceneFilterOptions);
    }

    public ComfortOptions comfortOptions() {
        return comfortOptions;
    }

    public BlockLensConfig withComfortOptions(ComfortOptions options) {
        return new BlockLensConfig(new EnumMap<>(enabled), options, analyzerOptions, sceneFilterOptions);
    }

    public AnalyzerOptions analyzerOptions() { return analyzerOptions; }

    public BlockLensConfig withAnalyzerOptions(AnalyzerOptions options) {
        return new BlockLensConfig(new EnumMap<>(enabled), comfortOptions, options, sceneFilterOptions);
    }

    public SceneFilterOptions sceneFilterOptions() { return sceneFilterOptions; }

    public BlockLensConfig withSceneFilterOptions(SceneFilterOptions options) {
        return new BlockLensConfig(new EnumMap<>(enabled), comfortOptions, analyzerOptions, options);
    }

    public Map<CapabilityId, Boolean> asMap() {
        return Collections.unmodifiableMap(enabled);
    }

    private static long bit(CapabilityId capability) {
        int ordinal = capability.ordinal();
        if (ordinal >= Long.SIZE) {
            throw new IllegalStateException("BlockLens capability count exceeds 64-bit config mask capacity");
        }
        return 1L << ordinal;
    }
}

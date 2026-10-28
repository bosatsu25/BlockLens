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
    // Bits 0-63 and 64-127 of the enabled-capability mask (CapabilityId.bitPosition()).
    private final long enabledMask;
    private final long enabledMaskHigh;
    private final ComfortOptions comfortOptions;
    private final AnalyzerOptions analyzerOptions;
    private final SceneFilterOptions sceneFilterOptions;

    private BlockLensConfig(EnumMap<CapabilityId, Boolean> enabled, ComfortOptions comfortOptions,
            AnalyzerOptions analyzerOptions, SceneFilterOptions sceneFilterOptions) {
        this.enabled = enabled;
        this.analyzerOptions = Objects.requireNonNull(analyzerOptions, "analyzerOptions");
        this.comfortOptions = Objects.requireNonNull(comfortOptions, "comfortOptions");
        this.sceneFilterOptions = Objects.requireNonNull(sceneFilterOptions, "sceneFilterOptions");
        long[] words = new long[2];
        for (CapabilityId capability : CapabilityId.values()) {
            if (Boolean.TRUE.equals(enabled.get(capability))) {
                set(words, capability.bitPosition());
            }
        }
        this.enabledMask = words[0];
        this.enabledMaskHigh = words[1];
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
        return isSet(enabledMask, enabledMaskHigh, Objects.requireNonNull(capability, "capability").bitPosition());
    }

    /**
     * Word 0 (bits 0–63) of the enabled-capability mask, for render/runtime hot paths. Combine it
     * only with {@link CapabilityId#renderBit()}; capabilities past bit 63 are in
     * {@link #enabledMaskHigh()}.
     */
    public long enabledMask() {
        return enabledMask;
    }

    /** Word 1 (bits 64–127) of the enabled-capability mask. */
    public long enabledMaskHigh() {
        return enabledMaskHigh;
    }

    /** Number of enabled capabilities across both mask words. */
    public int enabledCount() {
        return Long.bitCount(enabledMask) + Long.bitCount(enabledMaskHigh);
    }

    /** Whether both configs enable exactly the same capabilities, ignoring the other option groups. */
    public boolean sameCapabilities(BlockLensConfig other) {
        Objects.requireNonNull(other, "other");
        return enabledMask == other.enabledMask && enabledMaskHigh == other.enabledMaskHigh;
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BlockLensConfig other)) {
            return false;
        }
        return sameCapabilities(other)
                && this.comfortOptions.equals(other.comfortOptions)
                && this.analyzerOptions.equals(other.analyzerOptions)
                && this.sceneFilterOptions.equals(other.sceneFilterOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabledMask, enabledMaskHigh, comfortOptions, analyzerOptions, sceneFilterOptions);
    }

    public Map<CapabilityId, Boolean> asMap() {
        return Collections.unmodifiableMap(enabled);
    }

    /** Sets {@code position} in the two-word mask {@code words}. */
    static void set(long[] words, int position) {
        checkPosition(position);
        words[position >>> 6] |= 1L << (position & 63);
    }

    /** Whether {@code position} is set in the two-word mask {@code (low, high)}. */
    static boolean isSet(long low, long high, int position) {
        checkPosition(position);
        return ((position < Long.SIZE ? low : high) & (1L << (position & 63))) != 0L;
    }

    private static void checkPosition(int position) {
        if (position < 0 || position >= CapabilityId.MASK_BITS) {
            throw new IllegalStateException("Capability bit " + position + " exceeds the "
                    + CapabilityId.MASK_BITS + "-bit config mask");
        }
    }
}


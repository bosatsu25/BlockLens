package dev.blocklens.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Native BlockLens configuration independent of Minecraft/Fabric APIs. */
public final class BlockLensConfig {
    private final EnumMap<CapabilityId, Boolean> enabled;
    private final long enabledMask;
    private final ComfortOptions comfortOptions;

    private BlockLensConfig(EnumMap<CapabilityId, Boolean> enabled, ComfortOptions comfortOptions) {
        this.enabled = enabled;
        this.comfortOptions = Objects.requireNonNull(comfortOptions, "comfortOptions");
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
        return new BlockLensConfig(values, ComfortOptions.defaults());
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
        return new BlockLensConfig(values, comfortOptions);
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
        return new BlockLensConfig(copy, comfortOptions);
    }

    public ComfortOptions comfortOptions() {
        return comfortOptions;
    }

    public BlockLensConfig withComfortOptions(ComfortOptions options) {
        return new BlockLensConfig(new EnumMap<>(enabled), options);
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

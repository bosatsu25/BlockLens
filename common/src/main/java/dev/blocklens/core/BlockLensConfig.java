package dev.blocklens.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Native BlockLens configuration independent of Minecraft/Fabric APIs. */
public final class BlockLensConfig {
    private final EnumMap<CapabilityId, Boolean> enabled;

    private BlockLensConfig(EnumMap<CapabilityId, Boolean> enabled) {
        this.enabled = enabled;
    }

    public static BlockLensConfig defaults() {
        EnumMap<CapabilityId, Boolean> values = new EnumMap<>(CapabilityId.class);
        for (CapabilityId capability : CapabilityId.values()) {
            values.put(capability, capability.defaultEnabled());
        }
        return new BlockLensConfig(values);
    }

    static BlockLensConfig fromOverrides(Map<CapabilityId, Boolean> overrides) {
        Objects.requireNonNull(overrides, "overrides");
        EnumMap<CapabilityId, Boolean> values = new EnumMap<>(CapabilityId.class);
        for (CapabilityId capability : CapabilityId.values()) {
            values.put(capability, overrides.getOrDefault(capability, capability.defaultEnabled()));
        }
        return new BlockLensConfig(values);
    }

    public boolean isEnabled(CapabilityId capability) {
        return enabled.get(Objects.requireNonNull(capability, "capability"));
    }

    public BlockLensConfig withEnabled(CapabilityId capability, boolean value) {
        Objects.requireNonNull(capability, "capability");
        EnumMap<CapabilityId, Boolean> copy = new EnumMap<>(enabled);
        copy.put(capability, value);
        return new BlockLensConfig(copy);
    }

    public Map<CapabilityId, Boolean> asMap() {
        return Collections.unmodifiableMap(enabled);
    }
}

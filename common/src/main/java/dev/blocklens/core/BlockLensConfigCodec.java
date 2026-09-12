package dev.blocklens.core;

import java.util.EnumMap;
import java.util.Map;

/**
 * Deterministic, dependency-free codec for the initial BlockLens configuration.
 * Unknown or malformed keys fail soft and leave the current default in place.
 */
public final class BlockLensConfigCodec {
    private BlockLensConfigCodec() {
    }

    public static String encode(BlockLensConfig config) {
        StringBuilder output = new StringBuilder();
        output.append("# BlockLens native config v1\n");
        for (CapabilityId capability : CapabilityId.values()) {
            output.append(capability.sourceKey())
                    .append('=')
                    .append(config.isEnabled(capability))
                    .append('\n');
        }
        return output.toString();
    }

    public static BlockLensConfig decode(String text) {
        EnumMap<CapabilityId, Boolean> overrides = new EnumMap<>(CapabilityId.class);
        if (text == null || text.isBlank()) {
            return BlockLensConfig.defaults();
        }

        String[] lines = text.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int separator = line.indexOf('=');
            if (separator == -1) {
                continue;
            }

            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            CapabilityId capability = CapabilityId.bySourceKey().get(key);
            if (capability == null) {
                continue;
            }

            if ("true".equalsIgnoreCase(value)) {
                overrides.put(capability, true);
            } else if ("false".equalsIgnoreCase(value)) {
                overrides.put(capability, false);
            }
        }
        return BlockLensConfig.fromOverrides(overrides);
    }

    public static Map<CapabilityId, Boolean> decodeToMap(String text) {
        return decode(text).asMap();
    }
}

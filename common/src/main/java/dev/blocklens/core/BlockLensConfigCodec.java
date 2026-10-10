package dev.blocklens.core;

import dev.blocklens.core.analyzer.AnalyzerCatalog;
import dev.blocklens.core.analyzer.AnalyzerOptions;
import dev.blocklens.core.scene.SceneFilterCatalog;
import dev.blocklens.core.scene.SceneFilterOptions;
import java.util.EnumMap;
import java.util.Locale;
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
        ComfortOptions options = config.comfortOptions();
        output.append("others.low_fire.size=").append(options.fireSize()).append('\n');
        output.append("others.handheld_size.blocks_percent=").append(options.blocksPercent()).append('\n');
        output.append("others.handheld_size.items_percent=").append(options.itemsPercent()).append('\n');
        output.append("others.handheld_size.tools_percent=").append(options.toolsPercent()).append('\n');
        for(int kind=0;kind<5;kind++) for(int field=0;field<4;field++) {
            if(kind==0 && field==1) continue;
            output.append(AnalyzerCatalog.optionKey(kind,field)).append('=')
                    .append(config.analyzerOptions().value(kind,field)).append('\n');
        }
        for (int kind = 0; kind < 2; kind++) {
            var rule = config.sceneFilterOptions().rule(kind);
            output.append(SceneFilterCatalog.optionKey(kind,0)).append('=').append(rule.mode()).append('\n');
            output.append(SceneFilterCatalog.optionKey(kind,1)).append('=').append(String.join(",",rule.blacklist())).append('\n');
            output.append(SceneFilterCatalog.optionKey(kind,2)).append('=').append(String.join(",",rule.whitelist())).append('\n');
        }
        return output.toString();
    }

    public static BlockLensConfig decode(String text) {
        EnumMap<CapabilityId, Boolean> overrides = new EnumMap<>(CapabilityId.class);
        if (text == null || text.isBlank()) {
            return BlockLensConfig.defaults();
        }

        AnalyzerOptions analyzers = AnalyzerOptions.defaults();
        SceneFilterOptions scene = SceneFilterOptions.defaults();
        ComfortOptions defaults = ComfortOptions.defaults();
        int fireSize = defaults.fireSize();
        int blocksPercent = defaults.blocksPercent();
        int itemsPercent = defaults.itemsPercent();
        int toolsPercent = defaults.toolsPercent();
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
                for (int kind = 0; kind < 2; kind++) {
                    try {
                        if (key.equals(SceneFilterCatalog.optionKey(kind,0))) {
                            scene = scene.withMode(kind,SceneFilterOptions.Mode.valueOf(value.toUpperCase(Locale.ROOT)));
                        } else if (key.equals(SceneFilterCatalog.optionKey(kind,1))) {
                            scene = scene.withList(kind,false,value);
                        } else if (key.equals(SceneFilterCatalog.optionKey(kind,2))) {
                            scene = scene.withList(kind,true,value);
                        }
                    } catch (IllegalArgumentException invalid) {
                        // Malformed/oversized replacement retains the last valid bounded draft.
                    }
                }
                for(int kind=0;kind<5;kind++) for(int field=0;field<4;field++) {
                    if(kind==0 && field==1) continue;
                    if(key.equals(AnalyzerCatalog.optionKey(kind,field))) {
                        int parsed=bounded(value,AnalyzerOptions.minimum(kind,field),AnalyzerOptions.maximum(kind,field),
                                analyzers.value(kind,field));
                        analyzers=analyzers.with(kind,field,parsed);
                    }
                }
                switch (key) {
                    case "others.low_fire.size" -> fireSize = bounded(value, 0, 2, fireSize);
                    case "others.handheld_size.blocks_percent" -> blocksPercent = bounded(value, 40, 100, blocksPercent);
                    case "others.handheld_size.items_percent" -> itemsPercent = bounded(value, 40, 100, itemsPercent);
                    case "others.handheld_size.tools_percent" -> toolsPercent = bounded(value, 40, 100, toolsPercent);
                    default -> { }
                }
                continue;
            }

            if ("true".equalsIgnoreCase(value)) {
                overrides.put(capability, true);
            } else if ("false".equalsIgnoreCase(value)) {
                overrides.put(capability, false);
            }
        }
        return BlockLensConfig.fromOverrides(overrides,
                new ComfortOptions(fireSize, blocksPercent, itemsPercent, toolsPercent))
                .withAnalyzerOptions(analyzers).withSceneFilterOptions(scene);
    }

    private static int bounded(String text, int min, int max, int fallback) {
        try {
            int value = Integer.parseInt(text);
            return value >= min && value <= max ? value : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static Map<CapabilityId, Boolean> decodeToMap(String text) {
        return decode(text).asMap();
    }
}

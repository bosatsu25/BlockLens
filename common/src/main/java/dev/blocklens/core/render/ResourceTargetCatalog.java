package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Exact M0 target scope for the 18 resource/highlight capabilities. */
public final class ResourceTargetCatalog {
    private static final Map<CapabilityId, List<String>> TARGETS;

    static {
        EnumMap<CapabilityId, List<String>> targets = new EnumMap<>(CapabilityId.class);
        targets.put(CapabilityId.OBSIDIAN, List.of("obsidian"));
        targets.put(CapabilityId.ANCIENT_DEBRIS, List.of("ancient_debris"));
        targets.put(CapabilityId.DIAMOND_ORE, List.of("diamond_ore"));
        targets.put(CapabilityId.DEEPSLATE_DIAMOND_ORE, List.of("deepslate_diamond_ore"));
        targets.put(CapabilityId.GOLD_ORE, List.of("gold_ore"));
        targets.put(CapabilityId.DEEPSLATE_GOLD_ORE, List.of("deepslate_gold_ore"));
        targets.put(CapabilityId.EMERALD_ORE, List.of("emerald_ore"));
        targets.put(CapabilityId.DEEPSLATE_EMERALD_ORE, List.of("deepslate_emerald_ore"));
        targets.put(CapabilityId.COAL_ORE, List.of("coal_ore"));
        targets.put(CapabilityId.DEEPSLATE_COAL_ORE, List.of("deepslate_coal_ore"));
        targets.put(CapabilityId.IRON_ORE, List.of("iron_ore"));
        targets.put(CapabilityId.DEEPSLATE_IRON_ORE, List.of("deepslate_iron_ore"));
        targets.put(CapabilityId.COPPER_ORE, List.of("copper_ore"));
        targets.put(CapabilityId.DEEPSLATE_COPPER_ORE, List.of("deepslate_copper_ore"));
        targets.put(CapabilityId.LAPIS_ORE, List.of("lapis_ore"));
        targets.put(CapabilityId.DEEPSLATE_LAPIS_ORE, List.of("deepslate_lapis_ore"));
        targets.put(CapabilityId.REDSTONE_ORE, List.of("redstone_ore"));
        targets.put(CapabilityId.DEEPSLATE_REDSTONE_ORE, List.of("deepslate_redstone_ore"));
        TARGETS = Collections.unmodifiableMap(targets);
    }

    private ResourceTargetCatalog() {
    }

    public static List<String> targets(CapabilityId capability) {
        Objects.requireNonNull(capability, "capability");
        return TARGETS.getOrDefault(capability, List.of());
    }

    public static boolean contains(CapabilityId capability, String minecraftPath) {
        Objects.requireNonNull(minecraftPath, "minecraftPath");
        return targets(capability).contains(minecraftPath);
    }

    public static int totalBindingCount() {
        return TARGETS.values().stream().mapToInt(List::size).sum();
    }
}

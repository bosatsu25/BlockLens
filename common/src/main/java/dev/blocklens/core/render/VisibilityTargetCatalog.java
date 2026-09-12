package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Exact M0 target scope for the five M4 outline/fine-visibility capabilities. */
public final class VisibilityTargetCatalog {
    private static final Map<CapabilityId, List<String>> TARGETS;

    static {
        EnumMap<CapabilityId, List<String>> targets = new EnumMap<>(CapabilityId.class);
        targets.put(CapabilityId.BLUE_ICE, List.of("blue_ice"));
        targets.put(CapabilityId.DEAD_CORAL, List.of(
                "dead_brain_coral", "dead_brain_coral_block", "dead_brain_coral_fan", "dead_brain_coral_wall_fan",
                "dead_bubble_coral", "dead_bubble_coral_block", "dead_bubble_coral_fan", "dead_bubble_coral_wall_fan",
                "dead_fire_coral", "dead_fire_coral_block", "dead_fire_coral_fan", "dead_fire_coral_wall_fan",
                "dead_horn_coral", "dead_horn_coral_block", "dead_horn_coral_fan", "dead_horn_coral_wall_fan",
                "dead_tube_coral", "dead_tube_coral_block", "dead_tube_coral_fan", "dead_tube_coral_wall_fan"));
        targets.put(CapabilityId.POWDER_SNOW, List.of("powder_snow"));
        targets.put(CapabilityId.SCULK_CATALYST, List.of("sculk_catalyst"));
        targets.put(CapabilityId.STRING_TWEAKS, List.of("tripwire"));
        TARGETS = Collections.unmodifiableMap(targets);
    }

    private VisibilityTargetCatalog() {
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

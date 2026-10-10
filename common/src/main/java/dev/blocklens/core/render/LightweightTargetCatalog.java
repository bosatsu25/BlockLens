package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Fixed vanilla identities; renderer-only controls never add world-scan targets. */
public final class LightweightTargetCatalog {
    private static final List<String> GLASS = glassTargets();
    private static final List<String> KELP = List.of("kelp", "kelp_plant");
    private static final List<String> WHITE_CONCRETE = List.of("white_concrete");

    private LightweightTargetCatalog() {
    }

    public static List<String> targets(CapabilityId capability) {
        return switch (Objects.requireNonNull(capability, "capability")) {
            case GLASS_HIGHLIGHT -> GLASS;
            case KELP_HIGHLIGHT -> KELP;
            case BRIGHT_CONCRETE -> WHITE_CONCRETE;
            default -> List.of();
        };
    }

    public static int totalBindingCount() {
        return GLASS.size() + KELP.size() + WHITE_CONCRETE.size();
    }

    private static List<String> glassTargets() {
        ArrayList<String> targets = new ArrayList<>(35);
        targets.addAll(List.of("glass", "tinted_glass", "glass_pane"));
        for (String color : List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink",
                "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            targets.add(color + "_stained_glass");
            targets.add(color + "_stained_glass_pane");
        }
        return List.copyOf(targets);
    }
}

package dev.blocklens.core.ui;

import dev.blocklens.core.CapabilityId;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

/** Stable settings presentation; never changes persisted keys or capability bit positions. */
public final class SettingsCatalog {
    private static final EnumMap<CapabilityId.Category, List<CapabilityId>> CATEGORIES =
            new EnumMap<>(CapabilityId.Category.class);

    static {
        for (CapabilityId.Category category : CapabilityId.Category.values()) {
            CATEGORIES.put(category, Arrays.stream(CapabilityId.values())
                    .filter(capability -> capability.category() == category).toList());
        }
    }

    private SettingsCatalog() {
    }

    public static List<CapabilityId> capabilities(CapabilityId.Category category) {
        return CATEGORIES.get(Objects.requireNonNull(category, "category"));
    }

    public static String nameKey(CapabilityId capability) {
        String key = Objects.requireNonNull(capability, "capability").sourceKey();
        return "blocklens.capability." + key.substring(key.indexOf('.') + 1);
    }

    public static String descriptionKey(CapabilityId capability) {
        return nameKey(capability) + ".description";
    }
}

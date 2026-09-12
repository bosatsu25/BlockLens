package dev.blocklens.core.state;

import dev.blocklens.core.CapabilityId;
import java.util.Objects;

/** Common product policy describing the semantic shape required by each M4 capability. */
public enum VisibilityStateKind {
    NONE,
    STATIC_OUTLINE,
    SCULK_BLOOM,
    TRIPWIRE;

    public static VisibilityStateKind forCapability(CapabilityId capability) {
        return switch (Objects.requireNonNull(capability, "capability")) {
            case BLUE_ICE, DEAD_CORAL, POWDER_SNOW -> STATIC_OUTLINE;
            case SCULK_CATALYST -> SCULK_BLOOM;
            case STRING_TWEAKS -> TRIPWIRE;
            default -> NONE;
        };
    }

    public static boolean isVisibilityCapability(CapabilityId capability) {
        return forCapability(capability) != NONE;
    }
}

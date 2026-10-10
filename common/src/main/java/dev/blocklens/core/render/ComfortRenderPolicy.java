package dev.blocklens.core.render;

import dev.blocklens.core.ComfortOptions;
import java.util.Objects;

/** Pure bounded first-person transform policy. */
public final class ComfortRenderPolicy {
    private ComfortRenderPolicy() { }

    public static float fireScaleY(int preset) {
        return switch (preset) {
            case 0 -> .78f;
            case 1 -> .56f;
            case 2 -> .34f;
            default -> throw new IllegalArgumentException("invalid fire preset");
        };
    }

    public static float fireOffsetY(int preset) {
        return switch (preset) {
            case 0 -> -.14f;
            case 1 -> -.24f;
            case 2 -> -.34f;
            default -> throw new IllegalArgumentException("invalid fire preset");
        };
    }

    public static float heldScale(ComfortOptions options, boolean block, boolean tool, boolean shield) {
        Objects.requireNonNull(options, "options");
        if (shield) return .95f;
        return (block ? options.blocksPercent() : tool ? options.toolsPercent() : options.itemsPercent()) / 100f;
    }
}

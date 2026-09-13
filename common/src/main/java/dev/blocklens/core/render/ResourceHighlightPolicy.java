package dev.blocklens.core.render;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import java.util.Objects;
import java.util.Optional;

/** Shared independent-toggle policy for the 18 M5 resource highlights. */
public final class ResourceHighlightPolicy {
    private ResourceHighlightPolicy() {
    }

    public static Optional<ResourceHighlightCue> cue(
            CapabilityId capability,
            BlockLensConfig config) {
        Objects.requireNonNull(capability, "capability");
        Objects.requireNonNull(config, "config");
        if (capability.category() != CapabilityId.Category.RESOURCE || !config.isEnabled(capability)) {
            return Optional.empty();
        }
        return Optional.of(ResourceHighlightCue.forCapability(capability));
    }
}

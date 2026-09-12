package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import java.util.List;
import java.util.Objects;

/**
 * Single exact-target entry point for all 37 BlockLens capabilities.
 *
 * <p>Catalogs stay split by semantic family for maintainability, while version adapters consume
 * this one compiled/static view. No suffix matching, resource scanning, or runtime discovery is
 * performed.</p>
 */
public final class BlockLensTargetCatalog {
    private BlockLensTargetCatalog() {
    }

    public static List<String> targets(CapabilityId capability) {
        Objects.requireNonNull(capability, "capability");
        List<String> decoration = DecorationTargetCatalog.targets(capability);
        if (!decoration.isEmpty()) {
            return decoration;
        }
        List<String> visibility = VisibilityTargetCatalog.targets(capability);
        if (!visibility.isEmpty()) {
            return visibility;
        }
        List<String> resource = ResourceTargetCatalog.targets(capability);
        if (!resource.isEmpty()) {
            return resource;
        }
        return NetherTweaksTargetCatalog.targets(capability);
    }

    /** Number of capability-to-target bindings. Overlapping targets are counted once per capability. */
    public static int totalBindingCount() {
        return DecorationTargetCatalog.totalBindingCount()
                + VisibilityTargetCatalog.totalBindingCount()
                + ResourceTargetCatalog.totalBindingCount()
                + NetherTweaksTargetCatalog.totalBindingCount();
    }
}

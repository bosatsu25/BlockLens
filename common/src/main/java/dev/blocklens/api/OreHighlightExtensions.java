package dev.blocklens.api;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.extension.OreExtensionCatalog;
import java.util.Map;

/**
 * Optional client-side resource highlight API. Register exact mod block IDs during client
 * initialization, before the first model bake. API v1 accepts existing gaming.* resource keys.
 * Invalid, incompatible, conflicting or late registrations return false without affecting other IDs.
 */
public final class OreHighlightExtensions {
    private static final OreExtensionCatalog CATALOG = new OreExtensionCatalog();

    private OreHighlightExtensions() { }

    public static boolean register(int apiVersion, String blockId, String capabilityKey) {
        return CATALOG.register(apiVersion, blockId, capabilityKey);
    }

    /** Model-loader boundary; integrations should call register, not freeze the registry themselves. */
    public static Map<String, CapabilityId> freeze() {
        return CATALOG.freeze();
    }
}

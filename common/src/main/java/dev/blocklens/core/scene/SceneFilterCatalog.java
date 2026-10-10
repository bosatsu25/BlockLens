package dev.blocklens.core.scene;

import dev.blocklens.core.CapabilityId;
import java.util.List;

public final class SceneFilterCatalog {
    public static final List<CapabilityId> CAPABILITIES = List.of(CapabilityId.BLOCK_FILTER, CapabilityId.ENTITY_FILTER);
    public static final List<String> FIELDS = List.of("mode", "blacklist", "whitelist");
    private SceneFilterCatalog() { }
    public static int index(CapabilityId capability) { return CAPABILITIES.indexOf(capability); }
    public static String optionKey(int kind, int field) {
        return CAPABILITIES.get(kind).sourceKey() + "." + FIELDS.get(field);
    }
}

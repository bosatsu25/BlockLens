package dev.blocklens.core.render;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class VisualResourceReloadPolicyTest {
    @Test void everyDecodePolicyReloadsInBothDirectionsAndOtherFlagsDoNot() {
        var original = BlockLensConfig.defaults();
        for (var id : CapabilityId.values()) {
            var changed = original.withEnabled(id, !original.isEnabled(id));
            boolean expected = id == CapabilityId.PALE_OAK_NEUTRAL || id == CapabilityId.COMPACT_BLOCKS
                    || id == CapabilityId.COMPACT_ITEMS || id == CapabilityId.COMPACT_TOOLS
                    || id == CapabilityId.SIMPLIFIED_TERRAIN || id == CapabilityId.CONNECTED_ORE_EDGES;
            assertEquals(expected, VisualResourceReloadPolicy.requiresReload(original, changed), id.sourceKey());
            assertEquals(expected, VisualResourceReloadPolicy.requiresReload(changed, original), id.sourceKey());
        }
        assertFalse(VisualResourceReloadPolicy.requiresReload(original, original));
    }
}

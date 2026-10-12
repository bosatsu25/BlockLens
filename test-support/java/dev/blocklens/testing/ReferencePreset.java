package dev.blocklens.testing;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import java.util.Set;

/** Historical visual fixture; this must not redefine the product defaults. */
public final class ReferencePreset {
    private ReferencePreset() { }

    public static BlockLensConfig historical() {
        Set<CapabilityId> enabled = Set.of(CapabilityId.BLUE_ICE, CapabilityId.DEAD_CORAL,
                CapabilityId.POWDER_SNOW, CapabilityId.SCULK_CATALYST, CapabilityId.STRING_TWEAKS);
        BlockLensConfig config = BlockLensConfig.defaults();
        for (CapabilityId capability : CapabilityId.values()) {
            config = config.withEnabled(capability, enabled.contains(capability));
        }
        return config;
    }
}

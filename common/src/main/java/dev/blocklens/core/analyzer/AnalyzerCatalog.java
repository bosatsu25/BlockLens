package dev.blocklens.core.analyzer;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.CapabilityId;
import java.util.List;

public final class AnalyzerCatalog {
    public static final List<CapabilityId> CAPABILITIES = List.of(CapabilityId.DEBRIS_ANALYZER,
            CapabilityId.LAVA_ANALYZER,CapabilityId.BEACON_RANGE,CapabilityId.LIGHTNING_ROD_RANGE,
            CapabilityId.VILLAGER_JOB_SITE_LINKS);
    public static final List<String> FIELDS = List.of("radius","vertical_radius","markers","interval");
    private AnalyzerCatalog() { }
    public static int index(CapabilityId capability) { return CAPABILITIES.indexOf(capability); }
    public static int mask(BlockLensConfig config) {
        int mask=0;
        for(int i=0;i<CAPABILITIES.size();i++) if(config.isEnabled(CAPABILITIES.get(i))) mask|=1<<i;
        return mask;
    }
    public static String optionKey(int kind,int field) {
        return CAPABILITIES.get(kind).sourceKey()+"."+FIELDS.get(field);
    }
}

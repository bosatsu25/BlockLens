package dev.blocklens.gametest;

import dev.blocklens.core.render.SimplifiedTerrainPolicy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Test-only bounded evidence of real model emission, with no world coordinates or data. */
public final class SimplifiedTerrainProbe {
    private static final Set<String> ROLES=Set.of("stone","oak_slab","oak_leaves","big_dripleaf","big_dripleaf_stem",
            "small_dripleaf","pointed_dripstone","vine","glow_lichen","moss_carpet","leaf_litter","short_grass","tall_grass","bush");
    private static final Map<String,SimplifiedTerrainPolicy.Kind> SEEN=new ConcurrentHashMap<>();
    private static volatile boolean active;
    private SimplifiedTerrainProbe() { }
    public static void start() { SEEN.clear();active=true; }
    public static void stop() { active=false;SEEN.clear(); }
    public static void record(String namespace,String path,SimplifiedTerrainPolicy.Kind kind,boolean enabled) {
        if(active && enabled && namespace.equals("minecraft") && ROLES.contains(path)) SEEN.put(path,kind);
    }
    public static boolean seen(String role,SimplifiedTerrainPolicy.Kind kind) { return SEEN.get(role)==kind; }
}
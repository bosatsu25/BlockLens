package dev.blocklens.core.render;

import dev.blocklens.core.scene.EaterClearPreset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Bounded clean-room style policy; no archive data or runtime resource discovery. */
public final class SimplifiedTerrainPolicy {
    public enum Kind { NONE, CUBE, SURFACE, SHORT_GRASS, TALL_GRASS, BUSH }
    private static final Set<String> CUBES = Set.copyOf(EaterClearPreset.sourceIds().stream()
            .map(id -> id.substring("minecraft:".length())).toList());
    private static final Set<String> SURFACES = Set.of("big_dripleaf", "big_dripleaf_stem", "small_dripleaf",
            "pointed_dripstone", "vine", "glow_lichen", "moss_carpet", "leaf_litter");
    private static final List<String> PATHS = paths();
    private SimplifiedTerrainPolicy() { }
    private static List<String> paths() {
        var paths = new ArrayList<>(CUBES); paths.addAll(SURFACES);
        paths.addAll(List.of("short_grass", "tall_grass", "bush")); paths.sort(String::compareTo);
        return List.copyOf(paths);
    }
    public static List<String> sourcePaths() { return PATHS; }
    public static Kind kind(String namespace, String path) {
        if (!"minecraft".equals(namespace) || path == null) return Kind.NONE;
        if (CUBES.contains(path)) return Kind.CUBE;
        if (SURFACES.contains(path)) return Kind.SURFACE;
        return switch (path) {
            case "short_grass" -> Kind.SHORT_GRASS;
            case "tall_grass" -> Kind.TALL_GRASS;
            case "bush" -> Kind.BUSH;
            default -> Kind.NONE;
        };
    }
    public static Kind select(boolean enabled, String namespace, String path, String owner) {
        return enabled && "vanilla".equals(owner) ? kind(namespace,path) : Kind.NONE;
    }
    /** Original procedural pattern and tuft masks; fully transparent texels contain no RGB. */
    public static int pixel(Kind kind, int x, int y) {
        if (kind == null || x < 0 || x >= 16 || y < 0 || y >= 16)
            throw new IllegalArgumentException("Invalid procedural texel");
        boolean visible = kind == Kind.CUBE || kind == Kind.SURFACE;
        if (kind == Kind.SHORT_GRASS) visible = (x >= 7 && x <= 10 && y >= 8) || (x >= 5 && x <= 8 && y >= 6 && y <= 7);
        else if (kind == Kind.TALL_GRASS) visible = x == 7 || x == 8;
        else if (kind == Kind.BUSH) visible = (x >= 4 && x <= 11 && y >= 10 && y <= 13) || (x >= 6 && x <= 11 && y == 9);
        if (!visible) return 0;
        int gray = kind == Kind.CUBE || kind == Kind.SURFACE ? 35 + (x * 7 + y * 11 + (x ^ y) * 3) % 15 : 135;
        return 0xFF000000 | gray << 16 | gray << 8 | gray;
    }
}
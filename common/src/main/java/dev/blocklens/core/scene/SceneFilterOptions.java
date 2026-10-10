package dev.blocklens.core.scene;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** Immutable, deterministic and bounded native filter rules; no Minecraft or registry discovery. */
public record SceneFilterOptions(Rule blocks, Rule entities) {
    public static final int MAX_IDS = 512;
    public static final int MAX_ID_CHARS = 256;
    public static final int MAX_TOTAL_CHARS = 48 * 1024;
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    public enum Mode { NONE, BLACKLIST, WHITELIST }

    public record Rule(Mode mode, List<String> blacklist, List<String> whitelist) {
        public Rule {
            Objects.requireNonNull(mode, "mode");
            blacklist = normalize(blacklist);
            whitelist = normalize(whitelist);
        }
        public static Rule empty() { return new Rule(Mode.NONE, List.of(), List.of()); }
        public List<String> list(boolean white) { return white ? whitelist : blacklist; }
    }

    public SceneFilterOptions {
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(entities, "entities");
        int total = encodedLength(blocks.blacklist()) + encodedLength(blocks.whitelist())
                + encodedLength(entities.blacklist()) + encodedLength(entities.whitelist());
        if (total > MAX_TOTAL_CHARS) throw new IllegalArgumentException("filter text limit exceeded");
    }

    public static SceneFilterOptions defaults() { return new SceneFilterOptions(Rule.empty(), Rule.empty()); }

    public Rule rule(int kind) {
        return switch (kind) {
            case 0 -> blocks;
            case 1 -> entities;
            default -> throw new IllegalArgumentException("unknown filter kind");
        };
    }

    private SceneFilterOptions withRule(int kind, Rule rule) {
        return switch (kind) {
            case 0 -> new SceneFilterOptions(rule, entities);
            case 1 -> new SceneFilterOptions(blocks, rule);
            default -> throw new IllegalArgumentException("unknown filter kind");
        };
    }

    public SceneFilterOptions withMode(int kind, Mode mode) {
        Rule current = rule(kind);
        return withRule(kind, new Rule(mode, current.blacklist(), current.whitelist()));
    }

    public SceneFilterOptions withList(int kind, boolean white, String text) {
        Rule current = rule(kind);
        List<String> parsed = parseList(text);
        return withRule(kind, new Rule(current.mode(), white ? current.blacklist() : parsed,
                white ? parsed : current.whitelist()));
    }

    public SceneFilterOptions withEntityPreset(int preset) {
        List<String> build = List.of("minecraft:item", "minecraft:experience_orb", "minecraft:area_effect_cloud");
        Rule rule = switch (preset) {
            case 0 -> new Rule(Mode.BLACKLIST, build, List.of());
            case 1 -> new Rule(Mode.WHITELIST, List.of(), List.of("minecraft:armor_stand", "minecraft:item_frame",
                    "minecraft:glow_item_frame", "minecraft:minecart", "minecraft:hopper_minecart", "minecraft:chest_minecart"));
            case 2 -> new Rule(Mode.BLACKLIST, List.of("minecraft:item", "minecraft:experience_orb",
                    "minecraft:area_effect_cloud", "minecraft:marker"), List.of());
            case 3 -> Rule.empty();
            default -> throw new IllegalArgumentException("unknown entity preset");
        };
        return new SceneFilterOptions(blocks, rule);
    }

    public static List<String> parseList(String text) {
        Objects.requireNonNull(text, "text");
        if (text.length() > MAX_TOTAL_CHARS) throw new IllegalArgumentException("filter text limit exceeded");
        if (text.isEmpty()) return List.of();
        return normalize(List.of(text.split(",", -1)));
    }

    public static List<String> normalize(List<String> ids) {
        Objects.requireNonNull(ids, "ids");
        if (ids.size() > MAX_IDS) throw new IllegalArgumentException("filter ID count exceeded");
        TreeSet<String> result = new TreeSet<>();
        for (String raw : ids) {
            Objects.requireNonNull(raw, "id");
            if (raw.length() > MAX_ID_CHARS) throw new IllegalArgumentException("filter ID length exceeded");
            for (int i = 0; i < raw.length(); i++) {
                if (Character.isISOControl(raw.charAt(i))) throw new IllegalArgumentException("control in filter ID");
            }
            String value = raw.trim().toLowerCase(Locale.ROOT);
            if (value.length() > MAX_ID_CHARS || !ID.matcher(value).matches()) {
                throw new IllegalArgumentException("invalid namespaced filter ID");
            }
            for (String segment : value.substring(value.indexOf(':') + 1).split("/", -1)) {
                if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                    throw new IllegalArgumentException("invalid filter ID segment");
                }
            }
            result.add(value);
        }
        return List.copyOf(result);
    }

    private static int encodedLength(List<String> ids) {
        int result = Math.max(0, ids.size() - 1);
        for (String id : ids) result += id.length();
        return result;
    }
}

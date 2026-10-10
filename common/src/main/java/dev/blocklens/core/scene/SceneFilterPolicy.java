package dev.blocklens.core.scene;

/** Renderer-independent visibility decision, including the required local-player exception. */
public final class SceneFilterPolicy {
    private SceneFilterPolicy() { }

    public static boolean hidden(boolean enabled, SceneFilterOptions.Mode mode,
            boolean inBlacklist, boolean inWhitelist, boolean localPlayer) {
        if (!enabled || localPlayer) return false;
        return switch (mode) {
            case NONE -> false;
            case BLACKLIST -> inBlacklist;
            case WHITELIST -> !inWhitelist;
        };
    }
}

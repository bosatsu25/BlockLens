package dev.blocklens.core;

import java.util.Objects;

/** Small common runtime state shared by version-specific Fabric adapters. */
public final class BlockLensRuntime {
    private static volatile BlockLensConfig config = BlockLensConfig.defaults();
    private static volatile String minecraftVersion = "uninitialized";

    private BlockLensRuntime() {
    }

    public static void initialize(String version, BlockLensConfig loadedConfig) {
        minecraftVersion = Objects.requireNonNull(version, "version");
        config = Objects.requireNonNull(loadedConfig, "loadedConfig");
    }

    public static BlockLensConfig config() {
        return config;
    }

    public static String minecraftVersion() {
        return minecraftVersion;
    }
}

package dev.blocklens.core;

import java.util.Objects;

/** Small common runtime state shared by version-specific Fabric adapters. */
public final class BlockLensRuntime {
    private static volatile BlockLensConfig config = BlockLensConfig.defaults();
    private static volatile String minecraftVersion = "uninitialized";
    private static volatile long initializationNanos = 0L;

    private BlockLensRuntime() {
    }

    public static void initialize(String version, BlockLensConfig loadedConfig) {
        initialize(version, loadedConfig, 0L);
    }

    public static void initialize(String version, BlockLensConfig loadedConfig, long measuredInitializationNanos) {
        if (measuredInitializationNanos < 0L) {
            throw new IllegalArgumentException("measuredInitializationNanos must be >= 0");
        }
        minecraftVersion = Objects.requireNonNull(version, "version");
        config = Objects.requireNonNull(loadedConfig, "loadedConfig");
        initializationNanos = measuredInitializationNanos;
    }

    public static BlockLensConfig config() {
        return config;
    }

    public static String minecraftVersion() {
        return minecraftVersion;
    }

    public static long initializationNanos() {
        return initializationNanos;
    }
}

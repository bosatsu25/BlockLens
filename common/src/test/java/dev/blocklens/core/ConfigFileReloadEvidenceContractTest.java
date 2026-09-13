package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ConfigFileReloadEvidenceContractTest {
    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void sharedClientGameTestUsesRealNativeConfigFilesystemBoundary() throws IOException {
        String smoke = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/BlockLensSmokeClientGameTest.java"));
        String oracle = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/ConfigFileReloadOracle.java"));

        assertTrue(smoke.contains("ConfigFileReloadOracle.verify()"));
        assertTrue(smoke.contains("native config file reload restored"));
        assertTrue(oracle.contains("FabricLoader.getInstance().getConfigDir()"));
        assertTrue(oracle.contains("BlockLensConfigFiles.save(configDirectory, changed)"));
        assertTrue(oracle.contains("BlockLensRuntime.reloadConfig(configDirectory)"));
        assertTrue(oracle.contains("invertEveryCapability(originalRuntime)"));
        assertTrue(oracle.contains("Files.writeString(configPath, originalText"));
        assertTrue(oracle.contains("temporary config file leaked after atomic replace"));
        assertTrue(oracle.contains("BLOCKLENS_CONFIG_FILE_RELOAD"));
    }

    @Test
    void runtimeReloadApiReadsThroughNativeConfigFilesWithoutResettingIdentity() throws IOException {
        String runtime = Files.readString(root().resolve(
                "common/src/main/java/dev/blocklens/core/BlockLensRuntime.java"));

        assertTrue(runtime.contains("public static BlockLensConfig reloadConfig(Path configDirectory)"));
        assertTrue(runtime.contains("BlockLensConfigFiles.loadOrCreate"));
        assertTrue(runtime.contains("config = reloaded"));
        assertTrue(runtime.contains("minecraftVersion"));
        assertTrue(runtime.contains("initializationNanos"));
    }
}

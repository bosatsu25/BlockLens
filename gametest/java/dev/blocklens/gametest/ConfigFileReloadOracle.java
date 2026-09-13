package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensConfigCodec;
import dev.blocklens.core.BlockLensConfigFiles;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** Real native-config filesystem acceptance shared by both supported client targets. */
final class ConfigFileReloadOracle {
    private ConfigFileReloadOracle() {
    }

    static void verify() {
        Path configDirectory = FabricLoader.getInstance().getConfigDir();
        Path configPath = configDirectory.resolve(BlockLensConfigFiles.FILE_NAME);
        Path temporaryPath = configDirectory.resolve(BlockLensConfigFiles.FILE_NAME + ".tmp");
        require(Files.isRegularFile(configPath), "native config must exist before reload acceptance");

        BlockLensConfig originalRuntime = BlockLensRuntime.config();
        String originalVersion = BlockLensRuntime.minecraftVersion();
        long originalInitializationNanos = BlockLensRuntime.initializationNanos();

        try {
            String originalText = Files.readString(configPath, StandardCharsets.UTF_8);
            BlockLensConfig changed = invertEveryCapability(originalRuntime);
            require(changed.enabledMask() != originalRuntime.enabledMask(),
                    "reload acceptance config must differ from the initial runtime mask");

            try {
                BlockLensConfigFiles.save(configDirectory, changed);
                require(Files.isRegularFile(configPath), "native config disappeared after save");
                require(!Files.exists(temporaryPath), "temporary config file leaked after atomic replace");

                String persistedText = Files.readString(configPath, StandardCharsets.UTF_8);
                require(persistedText.equals(BlockLensConfigCodec.encode(changed)),
                        "persisted native config text differs from the canonical codec output");
                BlockLensConfig persisted = BlockLensConfigCodec.decode(persistedText);
                require(persisted.asMap().equals(changed.asMap()),
                        "persisted native config does not contain the changed 37-capability state");

                BlockLensConfig reloaded = BlockLensRuntime.reloadConfig(configDirectory);
                require(reloaded.asMap().equals(changed.asMap()),
                        "runtime reload did not publish the persisted native config");
                require(BlockLensRuntime.config().enabledMask() == changed.enabledMask(),
                        "runtime enabled mask differs after native config reload");
                require(BlockLensRuntime.minecraftVersion().equals(originalVersion),
                        "native config reload changed runtime Minecraft version identity");
                require(BlockLensRuntime.initializationNanos() == originalInitializationNanos,
                        "native config reload changed the startup timing measurement");

                System.out.println("BLOCKLENS_CONFIG_FILE_RELOAD minecraft=" + originalVersion
                        + " capabilities=" + CapabilityId.values().length
                        + " mask=" + Long.toUnsignedString(changed.enabledMask())
                        + " result=PASS");
            } finally {
                Files.writeString(configPath, originalText, StandardCharsets.UTF_8);
                Files.deleteIfExists(temporaryPath);
                BlockLensConfig restored = BlockLensRuntime.reloadConfig(configDirectory);
                require(restored.asMap().equals(originalRuntime.asMap()),
                        "native config acceptance failed to restore the original runtime config");
                require(BlockLensRuntime.minecraftVersion().equals(originalVersion),
                        "runtime Minecraft version identity changed while restoring config");
                require(BlockLensRuntime.initializationNanos() == originalInitializationNanos,
                        "startup timing changed while restoring config");
            }
        } catch (IOException exception) {
            throw new AssertionError("native config file reload acceptance failed", exception);
        }
    }

    private static BlockLensConfig invertEveryCapability(BlockLensConfig original) {
        BlockLensConfig changed = original;
        for (CapabilityId capability : CapabilityId.values()) {
            changed = changed.withEnabled(capability, !original.isEnabled(capability));
        }
        return changed;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

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
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Shared client smoke test compiled and executed against every supported Minecraft version.
 * It intentionally validates only M1 runtime contracts; rendering parity is added with later milestones.
 */
public final class BlockLensSmokeClientGameTest implements FabricClientGameTest {
    private static final int EXPECTED_CAPABILITY_COUNT = 37;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> verifyRuntime("initial client launch"));

        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientLevel().waitForChunksDownload();
            context.waitTicks(20);
            context.runOnClient(client -> verifyRuntime("singleplayer world joined"));
        }

        // The world context closing exercises the disconnect path. M1 has no world-owned
        // renderer state yet, so the contract here is that common runtime/config state remains valid.
        context.waitTicks(5);
        context.runOnClient(client -> verifyRuntime("singleplayer world closed"));
    }

    private static void verifyRuntime(String phase) {
        require(CapabilityId.values().length == EXPECTED_CAPABILITY_COUNT,
                phase + ": expected 37 capabilities");

        String minecraftVersion = FabricLoader.getInstance()
                .getModContainer("minecraft")
                .orElseThrow(() -> new AssertionError(phase + ": Minecraft mod container unavailable"))
                .getMetadata()
                .getVersion()
                .getFriendlyString();
        require(minecraftVersion.equals(BlockLensRuntime.minecraftVersion()),
                phase + ": runtime version mismatch; expected " + minecraftVersion
                        + " but was " + BlockLensRuntime.minecraftVersion());

        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(BlockLensConfigFiles.FILE_NAME);
        require(Files.isRegularFile(configPath),
                phase + ": native config was not created at " + configPath);

        try {
            String encoded = Files.readString(configPath, StandardCharsets.UTF_8);
            BlockLensConfig persisted = BlockLensConfigCodec.decode(encoded);
            require(persisted.asMap().equals(BlockLensRuntime.config().asMap()),
                    phase + ": persisted config differs from runtime config");
        } catch (IOException exception) {
            throw new AssertionError(phase + ": failed to read native config", exception);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

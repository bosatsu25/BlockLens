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

/** Shared client smoke and integration oracle executed against every supported Minecraft version. */
public final class BlockLensSmokeClientGameTest implements FabricClientGameTest {
    private static final int EXPECTED_CAPABILITY_COUNT = 37;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            verifyRuntime("initial client launch", true);
            MinecraftStateAdapterOracle.verify();
            M4M6StateAdapterOracle.verify();
            MinecraftDecorationTargetOracle.verify();
        });

        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            context.waitTicks(20);
            context.runOnClient(client -> verifyRuntime("singleplayer world joined", false));
            M3VisualParityOracle.verify(context, singleplayer);
            M7FullParityOracle.verify(context, singleplayer);
        }

        context.runOnClient(client -> verifyRuntime("singleplayer world closed", false));
    }

    private static void verifyRuntime(String phase, boolean reportBaseline) {
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
        require(BlockLensRuntime.initializationNanos() >= 0L,
                phase + ": initialization timing must be non-negative");

        Path configPath = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(BlockLensConfigFiles.FILE_NAME);
        require(Files.isRegularFile(configPath), phase + ": native config was not created at " + configPath);

        try {
            String encoded = Files.readString(configPath, StandardCharsets.UTF_8);
            BlockLensConfig persisted = BlockLensConfigCodec.decode(encoded);
            require(persisted.asMap().equals(BlockLensRuntime.config().asMap()),
                    phase + ": persisted config differs from runtime config");
        } catch (IOException exception) {
            throw new AssertionError(phase + ": failed to read native config", exception);
        }

        if (reportBaseline) {
            System.out.println("BLOCKLENS_M1_BASELINE minecraft=" + minecraftVersion
                    + " initNanos=" + BlockLensRuntime.initializationNanos()
                    + " capabilities=" + EXPECTED_CAPABILITY_COUNT);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

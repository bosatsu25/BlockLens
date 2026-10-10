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
    private static final int EXPECTED_CAPABILITY_COUNT = 57;

    @Override
    public void runTest(ClientGameTestContext context) {
        if ("external".equals(System.getProperty("blocklens.test.focus", "all"))) {
            ExternalCompatibilityOracle.verify(context);
            return;
        }
        if ("m8".equals(System.getProperty("blocklens.test.focus", "all"))) {
            verifyM8Only(context);
            return;
        }
        if ("scene".equals(System.getProperty("blocklens.test.focus","all"))) {
            verifySceneOnly(context);
            return;
        }
        context.runOnClient(client -> {
            PackagedRuntimeOracle.verify();
            verifyRuntime("initial client launch", true);
            ConfigFileReloadOracle.verify();
            verifyRuntime("native config file reload restored", false);
            MinecraftStateAdapterOracle.verify();
            M4M6StateAdapterOracle.verify();
            MinecraftDecorationTargetOracle.verify();
        });
        SettingsStateReloadOracle.verify(context);
        ResponsiveSettingsScreenOracle.verify(context);
        SettingsEditingOracle.verify(context);
        ComfortSettingsOracle.verify(context);
        AnalyzerSettingsOracle.verify(context);
        SceneFilterSettingsOracle.verify(context);
        SettingsVisualOracle.verify(context);

        BlockLensConfig disconnectBaseline=BlockLensRuntime.config();
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            context.waitTicks(20);
            context.runOnClient(client -> verifyRuntime("singleplayer world joined", false));
            M3VisualParityOracle.verify(context, singleplayer);
            M7FullParityOracle.verify(context, singleplayer);
            M5DarkAreaVisualOracle.verify(context, singleplayer);
            M5ActiveResourcePackOracle.verify(context, singleplayer);
            LightweightVisualOracle.verify(context, singleplayer);
            AnalyzerVisualOracle.verify(context, singleplayer);
            SceneFilterVisualOracle.verify(context, singleplayer);
            BuilderAssistOracle.verify(context, singleplayer);
            OreExtensionVisualOracle.verify(context, singleplayer);
            M8PerformanceBaselineOracle.verify(context, singleplayer);
            AnalyzerVisualOracle.prepareDisconnect(context, singleplayer);
            SceneFilterVisualOracle.prepareDisconnect(context);
            BuilderAssistOracle.prepareDisconnect(context, singleplayer);
        }

        BuilderAssistOracle.verifyDisconnected(context);
        context.runOnClient(client -> {
            require(dev.blocklens.fabric.AnalyzerClient.engine().markers().isEmpty(), "disconnect retained analyzer markers");
            require(dev.blocklens.fabric.SceneFilterClient.compiledLookupCount()==0,"disconnect retained compiled scene rules");
            require(dev.blocklens.fabric.MinecraftOreExtensions.compiledModelCount() == 0
                    && dev.blocklens.fabric.MinecraftOreExtensions.tagCandidateCount() == 0,
                    "disconnect retained ore extension models or synchronized tags");
            BlockLensRuntime.installConfig(disconnectBaseline);
            verifyRuntime("singleplayer world closed", false);
            System.out.println("BLOCKLENS_ANALYZER_DISCONNECT nonemptyBeforeClose=true emptyAfterClose=true");
            System.out.println("BLOCKLENS_SCENE_DISCONNECT nonemptyBeforeClose=true emptyAfterClose=true");
            System.out.println("BLOCKLENS_ORE_DISCONNECT cleared=true");
        });
    }

    /** Opt-in experiment with fresh world history; never substitutes for the full graph. */
    private static void verifyM8Only(ClientGameTestContext context) {
        require(Boolean.getBoolean("blocklens.test.packagedRuntime"),"M8 experiments require packaged runtime");
        context.runOnClient(client -> PackagedRuntimeOracle.verify());
        System.out.println("BLOCKLENS_M8_FOCUSED nativeScope=m8 fullGraph=false");
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            context.waitTicks(20);
            M8PerformanceBaselineOracle.verify(context, singleplayer);
        }
    }

    /** Opt-in local diagnosis only; hosted CI and the normal launcher always run the full graph. */
    private static void verifySceneOnly(ClientGameTestContext context) {
        context.runOnClient(c->{PackagedRuntimeOracle.verify();verifyRuntime("focused scene client",true);});
        SceneFilterSettingsOracle.verify(context);
        var original=BlockLensRuntime.config();
        try(TestSingleplayerContext world=context.worldBuilder().create()) {
            context.waitTicks(20);
            SceneFilterVisualOracle.verify(context,world);
            AnalyzerVisualOracle.prepareDisconnect(context,world);
            SceneFilterVisualOracle.prepareDisconnect(context);
        }
        context.runOnClient(c->{
            require(dev.blocklens.fabric.SceneFilterClient.compiledLookupCount()==0,"disconnect retained scene rules");
            require(dev.blocklens.fabric.AnalyzerClient.engine().markers().isEmpty(),"disconnect retained analyzer markers");
            BlockLensRuntime.installConfig(original);
            System.out.println("BLOCKLENS_SCENE_FOCUSED nativeScope=scene fullGraph=false disconnect=true");
        });
    }

    private static void verifyRuntime(String phase, boolean reportBaseline) {
        require(CapabilityId.values().length == EXPECTED_CAPABILITY_COUNT,
                phase + ": expected 57 capabilities");

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

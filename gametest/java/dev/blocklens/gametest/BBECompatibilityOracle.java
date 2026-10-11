package dev.blocklens.gametest;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;

/** Opt-in exact rendering-stack regression; it supplements, never replaces, the full CI graph. */
final class BBECompatibilityOracle {
    private BBECompatibilityOracle() { }
    static void verify(ClientGameTestContext context) {
        requireVersion("minecraft", "26.3");
        requireVersion("betterblockentities", "1.3.9+mc26.3");
        requireVersion("sodium", "0.9.2+mc26.3");
        String stack = System.getProperty("blocklens.test.bbeStack", "");
        if (stack.equals("cele-render")) {
            requireVersion("iris", "1.11.7+mc26.3");
            requireVersion("continuity", "3.0.1+26.3");
        } else {
            require(stack.equals("bbe-sodium"), "unknown BBE fixture stack");
            require(!FabricLoader.getInstance().isModLoaded("iris")
                    && !FabricLoader.getInstance().isModLoaded("continuity"), "minimal BBE fixture contains unrequested renderers");
        }
        require(Boolean.getBoolean("blocklens.test.packagedRuntime"), "BBE regression requires the built distribution JAR");
        context.runOnClient(c -> PackagedRuntimeOracle.verify());
        BBEPathProbe.reset();
        try {
            verifyConfiguration();
            try (var world = context.worldBuilder().create()) {
                context.waitTicks(20);
                // Includes closed single/left/right UI Save/Apply, OFF restoration, reload,
                // trapped/ender negatives, lid animation, working glass/kelp and comfort transforms.
                LightweightVisualOracle.verify(context, world);
                OreBorderVisualOracle.verify(context, world);
                require(BBEPathProbe.enabled() > 0 && BBEPathProbe.disabled() > 0,
                        "chest verification bypassed BBE optimized terrain emission");
                verifyConfiguration();
                var output = FabricLoader.getInstance().getGameDir().resolve("bbe-compatibility");
                Files.createDirectories(output);
                Files.writeString(output.resolve("metrics.properties"),
                        "minecraft=26.3\nstack=" + stack + "\noptimizedRegularOn=" + BBEPathProbe.enabled()
                        + "\noptimizedRegularOff=" + BBEPathProbe.disabled()
                        + "\nclosedChestUi=true\nglassKelpRegression=true\noreBorders=true\noptimizationsRetained=true\n",
                        StandardCharsets.UTF_8);
                System.out.println("BLOCKLENS_BBE_COMPATIBILITY stack=" + stack
                        + " optimizedRegularOn=" + BBEPathProbe.enabled() + " optimizedRegularOff=" + BBEPathProbe.disabled());
            }
        } catch (IOException exception) {
            throw new AssertionError("BBE fixture evidence/configuration could not be verified", exception);
        } finally {
            System.out.println("BLOCKLENS_BBE_PATHS enabled=" + BBEPathProbe.enabled() + " disabled=" + BBEPathProbe.disabled());
            BBEPathProbe.reset();
        }
    }
    private static void verifyConfiguration() throws IOException {
        var directory = FabricLoader.getInstance().getConfigDir();
        var settings = JsonParser.parseString(Files.readString(directory.resolve("BBEConfig.json")))
                .getAsJsonObject().getAsJsonArray("bbe.config.storage.main");
        boolean master = false, chest = false;
        for (var entry : settings) {
            var option = entry.getAsJsonObject();
            if (option.get("option").getAsString().equals("optimize.master")) master = option.get("value").getAsBoolean();
            if (option.get("option").getAsString().equals("optimize.chest")) chest = option.get("value").getAsBoolean();
        }
        require(master && chest, "BBE optimization must remain enabled throughout the regression");
        require(Files.readString(directory.resolve("iris.properties")).contains("enableShaders=false"),
                "fixture must match the supplied shader-OFF setting");
    }
    private static void requireVersion(String id, String expected) {
        String actual = FabricLoader.getInstance().getModContainer(id)
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("");
        require(expected.equals(actual), "unexpected fixture dependency: " + id + "=" + actual);
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}

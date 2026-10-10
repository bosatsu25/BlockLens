package dev.blocklens.gametest;

import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/** Dependency-free probe used for vanilla OpenGL, Vulkan and reference-pack tests. */
final class ExternalShaderProbe {
    private ExternalShaderProbe() { }

    static boolean verify(Properties manifest) {
        ExternalCompatibilityOracle.require(!FabricLoader.getInstance().isModLoaded("iris")
                        && !FabricLoader.getInstance().isModLoaded("sodium"),
                "Default backend profile unexpectedly contains a third-party renderer");
        ExternalCompatibilityOracle.require("false".equals(manifest.getProperty("shaderExpected")),
                "This profile cannot verify an active shader");
        return false;
    }
}

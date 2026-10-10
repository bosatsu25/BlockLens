package dev.blocklens.gametest;

import java.util.Properties;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;

/** Exact-version, test-only typed Iris API; never bundled or discovered with reflection. */
final class ExternalShaderProbe {
    private ExternalShaderProbe() { }

    static boolean verify(Properties manifest) {
        boolean active = IrisApi.getInstance().isShaderPackInUse();
        if (active) {
            ExternalCompatibilityOracle.require(Iris.getCurrentPackName().equals(manifest.getProperty("shaderPack")),
                    "The active shader pack differs from the pinned representative");
            var pack = Iris.getCurrentPack().orElseThrow(() -> new AssertionError("Active shader has no loaded pack"));
            var options = pack.getShaderPackOptions().getOptionValues();
            int verified = 0;
            for (String name : manifest.stringPropertyNames()) {
                if (!name.startsWith("shaderOption.")) continue;
                String option = name.substring("shaderOption.".length());
                ExternalCompatibilityOracle.require(manifest.getProperty(name)
                                .equals(options.getStringValueOrDefault(option)),
                        "An actual shader option differs from the recorded LOW profile");
                verified++;
            }
            ExternalCompatibilityOracle.require(verified == 14, "The complete shader profile was not verified");
        }
        return active;
    }
}

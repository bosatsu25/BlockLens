package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SharedTerrainRendererContractTest {
    private static final Path ROOT = Path.of(System.getProperty("blocklens.repoRoot"));

    @Test
    void oneTerrainImplementationUsesOnlyThinVersionLightingAdapters() throws IOException {
        for (String file : SupportedVersionFixtures.RENDERER_FILES) {
            assertTrue(Files.isRegularFile(SupportedVersionFixtures.sharedSource(ROOT, file)), "shared " + file + " is missing");
            for (String module : SupportedVersionFixtures.MODULES) {
                assertFalse(Files.exists(SupportedVersionFixtures.sourceRoot(ROOT, module).resolve(file)), module + " " + file);
            }
        }
        String renderer = SupportedVersionFixtures.rendererText(ROOT);
        assertTrue(renderer.contains("MinecraftQuadLighting.disableDirectionalShade(quad)"));
        assertFalse(renderer.contains("quad.diffuseShade("));
        assertFalse(renderer.contains("quad.shadeDirectionOverride("));
        assertFalse(renderer.contains("CapabilityId.KELP_HIGHLIGHT.ordinal()"));
        for (String adapter : List.of(SupportedVersionFixtures.SHARED_PRE_26_3,
                "versions/mc26_3/src/main/java")) {
            assertTrue(Files.isRegularFile(ROOT.resolve(adapter
                    + "/dev/blocklens/fabric/MinecraftQuadLighting.java")), adapter);
        }
        String convention = Files.readString(ROOT.resolve("gradle/version-module.gradle"));
        for (String file : SupportedVersionFixtures.RENDERER_FILES) {
            String compiled = "'dev/blocklens/fabric/" + file.replace(".java", ".class") + "'";
            assertTrue(convention.contains(compiled), "the runtime JAR audit must require " + compiled);
        }
    }
}

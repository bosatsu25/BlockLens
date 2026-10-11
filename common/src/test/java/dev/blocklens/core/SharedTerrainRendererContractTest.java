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
        Path shared = ROOT.resolve("fabric-shared/src/main/java/dev/blocklens/fabric/MinecraftDecorationModel.java");
        assertTrue(Files.isRegularFile(shared), "shared renderer is missing");
        String model = Files.readString(shared);
        assertTrue(model.contains("MinecraftQuadLighting.disableDirectionalShade(quad)"));
        assertFalse(model.contains("quad.diffuseShade("));
        assertFalse(model.contains("quad.shadeDirectionOverride("));
        assertFalse(model.contains("CapabilityId.KELP_HIGHLIGHT.ordinal()"));
        for (String module : SupportedVersionFixtures.MODULES) {
            assertFalse(Files.exists(ROOT.resolve("versions/" + module
                    + "/src/main/java/dev/blocklens/fabric/MinecraftDecorationModel.java")), module);
        }
        for (String adapter : List.of("versions/mc26_1_2/src/main/java",
                "versions/mc26_2/src/compat/java", "versions/mc26_3/src/main/java")) {
            assertTrue(Files.isRegularFile(ROOT.resolve(adapter
                    + "/dev/blocklens/fabric/MinecraftQuadLighting.java")), adapter);
        }
    }
}

package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ModMenuConfigEntryContractTest {
    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void bothMinecraftTargetsExposeTheExistingSettingsScreenThroughOptionalModMenu() throws IOException {
        String rootBuild = Files.readString(root().resolve("build.gradle"));
        assertTrue(rootBuild.contains("https://maven.terraformersmc.com/"));

        for (String module : new String[] {"mc26_1_2", "mc26_2"}) {
            Path moduleRoot = root().resolve("versions").resolve(module);
            String metadata = Files.readString(moduleRoot.resolve("src/main/resources/fabric.mod.json"));
            String moduleBuild = Files.readString(moduleRoot.resolve("build.gradle"));
            String bridge = Files.readString(moduleRoot.resolve(
                    "src/main/java/dev/blocklens/fabric/BlockLensModMenu.java"));

            assertTrue(metadata.contains("\"modmenu\""));
            assertTrue(metadata.contains("dev.blocklens.fabric.BlockLensModMenu"));
            assertTrue(metadata.indexOf("\"modmenu\"") < metadata.indexOf("\"depends\""));
            assertFalse(metadata.substring(metadata.indexOf("\"depends\"")).contains("\"modmenu\""));
            assertTrue(moduleBuild.contains("compileOnly \"com.terraformersmc:modmenu:${blockLensModMenuVersion}\""));
            assertTrue(bridge.contains("implements ModMenuApi"));
            assertTrue(bridge.contains("return BlockLensConfigScreen::new;"));
        }
    }
}

package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ModMenuConfigEntrypointContractTest {
    private static final String[] VERSIONS = {"mc26_1_2", "mc26_2"};

    @Test
    void bothVersionsExposeSettingsThroughOptionalModMenuEntrypoint() throws IOException {
        Path root = Path.of(System.getProperty("blocklens.repoRoot"));
        for (String version : VERSIONS) {
            Path module = root.resolve("versions").resolve(version);
            String metadata = Files.readString(module.resolve("src/main/resources/fabric.mod.json"));
            String adapter = Files.readString(module.resolve(
                    "src/main/java/dev/blocklens/fabric/BlockLensModMenu.java"));

            assertTrue(metadata.contains("\"modmenu\""), version);
            assertTrue(metadata.contains("dev.blocklens.fabric.BlockLensModMenu"), version);
            String depends = metadata.substring(metadata.indexOf("\"depends\""));
            assertFalse(depends.contains("\"modmenu\""), "Mod Menu must remain optional");
            assertTrue(adapter.contains("implements ModMenuApi"), version);
            assertTrue(adapter.contains("BlockLensConfigScreen::new"), version);
        }
    }
}

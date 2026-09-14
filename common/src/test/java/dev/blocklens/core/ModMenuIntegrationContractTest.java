package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ModMenuIntegrationContractTest {
    @Test
    void bothMinecraftLinesExposeOptionalModMenuConfigBridge() throws Exception {
        Path root = Path.of(System.getProperty("blocklens.repoRoot"));
        for (String module : new String[] {"mc26_1_2", "mc26_2"}) {
            Path version = root.resolve("versions").resolve(module);
            String metadata = Files.readString(version.resolve("src/main/resources/fabric.mod.json"));
            assertTrue(metadata.contains("\"modmenu\""));
            assertTrue(metadata.contains("dev.blocklens.fabric.BlockLensModMenu"));
            int depends = metadata.indexOf("\"depends\"");
            assertTrue(depends >= 0);
            assertFalse(metadata.substring(depends).contains("\"modmenu\""));

            String bridge = Files.readString(version.resolve(
                    "src/main/java/dev/blocklens/fabric/BlockLensModMenu.java"));
            assertTrue(bridge.contains("implements ModMenuApi"));
            assertTrue(bridge.contains("return BlockLensConfigScreen::new;"));
        }
    }
}

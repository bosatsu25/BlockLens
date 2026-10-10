package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ResponsiveSettingsScreenContractTest {
    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void bothAdaptersExposeTheSameBoundedResizeAwareSettingsSurface() throws IOException {
        for (String module : SupportedVersionFixtures.MODULES) {
            String screen = Files.readString(SupportedVersionFixtures.sourceRoot(root(), module).resolve("BlockLensConfigScreen.java"));
            String client = Files.readString(SupportedVersionFixtures.sourceRoot(root(), module).resolve("BlockLensClient.java"));
            assertTrue(screen.contains("extends AbstractBlockLensConfigScreen"));
            assertTrue(screen.contains("protected void showParent(Screen parent)"));
            assertTrue(client.contains("KeyMappingHelper.registerKeyMapping(OPEN_SETTINGS)"));
            assertTrue(client.contains("ClientTickEvents.END_CLIENT_TICK.register"));
        }
    }

    @Test
    void realClientOracleCoversCompactStandardAndWideSizes() throws IOException {
        String oracle = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/ResponsiveSettingsScreenOracle.java"));
        String smoke = Files.readString(root().resolve(
                "gametest/java/dev/blocklens/gametest/BlockLensSmokeClientGameTest.java"));
        String english = Files.readString(root().resolve(
                "common/src/main/resources/assets/blocklens/lang/en_us.json"));
        String japanese = Files.readString(root().resolve(
                "common/src/main/resources/assets/blocklens/lang/ja_jp.json"));

        assertTrue(smoke.contains("ResponsiveSettingsScreenOracle.verify(context)"));
        for (String size : new String[] {"320, 240", "640, 360", "854, 480", "1920, 1080"}) {
            assertTrue(oracle.contains(size));
        }
        assertTrue(oracle.contains("widget registry changed"));
        assertTrue(oracle.contains("keyboard focus escaped"));
        for (String translation : new String[] {
                "key.blocklens.settings",
                "blocklens.save_failed"}) {
            assertTrue(english.contains(translation));
            assertTrue(japanese.contains(translation));
        }
    }
}

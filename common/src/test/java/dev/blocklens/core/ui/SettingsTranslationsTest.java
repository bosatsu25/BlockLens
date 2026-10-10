package dev.blocklens.core.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import dev.blocklens.core.CapabilityId;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** A missing translation would expose an internal key in a real settings row. */
final class SettingsTranslationsTest {
    @Test
    void everySettingsControlAndDescriptionHasEnglishAndJapaneseText() throws IOException {
        Path root = Path.of(System.getProperty("blocklens.repoRoot"));
        for (String locale : new String[] {"en_us", "ja_jp"}) {
            String translations = Files.readString(root.resolve(
                    "common/src/main/resources/assets/blocklens/lang/" + locale + ".json"));
            for (CapabilityId capability : CapabilityId.values()) {
                requireText(translations, SettingsCatalog.nameKey(capability));
                requireText(translations, SettingsCatalog.descriptionKey(capability));
            }
            for (String key : new String[] {"blocklens.settings.apply", "blocklens.settings.discard",
                    "blocklens.settings.unsaved", "blocklens.settings.saved_error",
                    "blocklens.state.enabled", "blocklens.state.disabled", "blocklens.settings.details",
                    "blocklens.settings.back", "blocklens.comfort.fire.0", "blocklens.comfort.fire.1",
                    "blocklens.comfort.fire.2", "blocklens.comfort.held.0", "blocklens.comfort.held.1",
                    "blocklens.comfort.held.2", "blocklens.comfort.draft_hint"}) {
                requireText(translations, key);
            }
        }
    }

    private static void requireText(String translations, String key) {
        String quoted = java.util.regex.Pattern.quote(key);
        assertTrue(java.util.regex.Pattern.compile("\\\"" + quoted + "\\\"\\s*:\\s*\\\"[^\\\"]+\\\"")
                .matcher(translations).find(), () -> "Missing settings translation: " + key);
    }
}

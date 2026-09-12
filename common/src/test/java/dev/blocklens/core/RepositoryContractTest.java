package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RepositoryContractTest {
    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void everyCapabilityHasEnglishAndJapaneseName() throws IOException {
        String english = Files.readString(root().resolve("common/src/main/resources/assets/blocklens/lang/en_us.json"));
        String japanese = Files.readString(root().resolve("common/src/main/resources/assets/blocklens/lang/ja_jp.json"));

        for (CapabilityId capability : CapabilityId.values()) {
            String id = capability.sourceKey().substring(capability.sourceKey().indexOf('.') + 1);
            String key = "\"blocklens.capability." + id + "\"";
            assertTrue(english.contains(key), () -> "Missing English key: " + key);
            assertTrue(japanese.contains(key), () -> "Missing Japanese key: " + key);
        }
    }

    @Test
    void bothVersionModulesRemainThinExactClientOnlyAdapters() throws IOException {
        assertVersionModule("mc26_1_2", "rootProject.minecraft_26_1_2", "rootProject.fabric_api_26_1_2");
        assertVersionModule("mc26_2", "rootProject.minecraft_26_2", "rootProject.fabric_api_26_2");
    }

    @Test
    void startupSourceContainsNoNetworkTelemetryOrReflectionDiscovery() throws IOException {
        List<String> banned = List.of(
                "Class.forName(",
                "ServiceLoader.load(",
                "java.net.http",
                "java.net.URL",
                "HttpClient.newHttpClient",
                "Telemetry",
                "UpdateChecker");

        for (Path sourceRoot : List.of(
                root().resolve("common/src/main/java"),
                root().resolve("versions/mc26_1_2/src/main/java"),
                root().resolve("versions/mc26_2/src/main/java"))) {
            try (var paths = Files.walk(sourceRoot)) {
                for (Path file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    for (String marker : banned) {
                        assertFalse(source.contains(marker), () -> "Forbidden startup/runtime marker '" + marker + "' in " + file);
                    }
                }
            }
        }
    }

    private static void assertVersionModule(String module, String minecraftProperty, String apiProperty) throws IOException {
        Path moduleRoot = root().resolve("versions").resolve(module);
        String build = Files.readString(moduleRoot.resolve("build.gradle"));
        String metadata = Files.readString(moduleRoot.resolve("src/main/resources/fabric.mod.json"));

        assertTrue(build.contains(minecraftProperty));
        assertTrue(build.contains(apiProperty));
        assertTrue(build.contains("gradle/version-module.gradle"));
        assertTrue(metadata.contains("\"id\": \"blocklens\""));
        assertTrue(metadata.contains("\"environment\": \"client\""));
        assertTrue(metadata.contains("\"minecraft\": \"=${minecraft_version}\""));
        assertTrue(metadata.contains("dev.blocklens.fabric.BlockLensClient"));
        assertFalse(metadata.contains("\"main\":"));
        assertFalse(metadata.contains("\"server\":"));
    }
}

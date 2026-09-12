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
    void clientSmokeIsSharedAndExecutedForEveryVersion() throws IOException {
        Path gameTestSource = root().resolve("gametest/java/dev/blocklens/gametest/BlockLensSmokeClientGameTest.java");
        Path gameTestMetadata = root().resolve("gametest/resources/fabric.mod.json");
        String source = Files.readString(gameTestSource);
        String metadata = Files.readString(gameTestMetadata);
        String convention = Files.readString(root().resolve("gradle/version-module.gradle"));
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));

        assertTrue(source.contains("implements FabricClientGameTest"));
        assertTrue(source.contains("EXPECTED_CAPABILITY_COUNT = 37"));
        assertTrue(source.contains("context.worldBuilder().create()"));
        assertTrue(source.contains("persisted.asMap().equals(BlockLensRuntime.config().asMap())"));
        assertTrue(metadata.contains("\"fabric-client-gametest\""));
        assertTrue(metadata.contains("dev.blocklens.gametest.BlockLensSmokeClientGameTest"));

        assertTrue(convention.contains("enableClientGameTests = true"));
        assertTrue(convention.contains("rootProject.file('gametest/java')"));
        assertTrue(convention.contains("rootProject.file('gametest/resources')"));
        assertTrue(ci.contains(":versions:${{ matrix.module }}:runClientGameTest"));

        assertFalse(Files.exists(root().resolve("versions/mc26_1_2/src/gametest")));
        assertFalse(Files.exists(root().resolve("versions/mc26_2/src/gametest")));
    }

    @Test
    void runtimeArtifactAuditsStayEnabled() throws IOException {
        String convention = Files.readString(root().resolve("gradle/version-module.gradle"));
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));

        assertTrue(convention.contains("forbiddenPayloadMarkers"));
        assertTrue(convention.contains("respackopts.json5"));
        assertTrue(convention.contains("Runtime JAR contains forbidden residue entry"));
        assertTrue(ci.contains("Verify reproducible runtime JAR"));
        assertTrue(ci.contains("first_sha"));
        assertTrue(ci.contains("second_sha"));
    }

    @Test
    void engineeringGraphLoopAndBilingualReadmesRemainDocumented() throws IOException {
        String agents = Files.readString(root().resolve("AGENTS.md"));
        String index = Files.readString(root().resolve("knowledge/index.md"));
        String graphLoop = Files.readString(root().resolve("knowledge/current/engineering-loop.md"));
        String englishReadme = Files.readString(root().resolve("README.md"));
        String japaneseReadme = Files.readString(root().resolve("README_ja.md"));

        assertTrue(agents.contains("## Engineering Graph Loop"));
        assertTrue(agents.contains("DIAGNOSE -> FIX -> VERIFY"));
        assertTrue(agents.contains("DONE`, `BLOCKED`, or explicitly scoped `PARTIAL`"));

        assertTrue(index.contains("current/engineering-loop.md"));
        assertTrue(graphLoop.contains("DISCOVER"));
        assertTrue(graphLoop.contains("IMPLEMENT"));
        assertTrue(graphLoop.contains("SELF REVIEW"));
        assertTrue(graphLoop.contains("ISSUE UPDATE"));
        assertTrue(graphLoop.contains("DIAGNOSE"));
        assertTrue(graphLoop.contains("PARTIAL"));
        assertTrue(graphLoop.contains("```mermaid"));

        assertTrue(englishReadme.contains("[日本語](README_ja.md)"));
        assertTrue(japaneseReadme.contains("[English](README.md)"));
        assertTrue(englishReadme.contains("## Engineering Graph Loop"));
        assertTrue(japaneseReadme.contains("## Engineering Graph Loop"));
        assertTrue(englishReadme.contains("```mermaid"));
        assertTrue(japaneseReadme.contains("```mermaid"));
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

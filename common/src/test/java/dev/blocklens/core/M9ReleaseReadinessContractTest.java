package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class M9ReleaseReadinessContractTest {
    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void repositoryOwnsAndPackagesTheReleaseIcon() throws IOException {
        Path icon = root().resolve("common/src/main/resources/assets/blocklens/icon.png");
        assertTrue(Files.isRegularFile(icon));
        assertTrue(Files.size(icon) > 1_000L, "release icon must not be an empty placeholder");

        byte[] bytes = Files.readAllBytes(icon);
        assertTrue(bytes.length >= 8);
        assertArrayEquals(
                new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a},
                java.util.Arrays.copyOf(bytes, 8));

        for (String module : SupportedVersionFixtures.MODULES) {
            String metadata = Files.readString(root().resolve(
                    "versions/" + module + "/src/main/resources/fabric.mod.json"));
            assertTrue(metadata.contains("\"icon\": \"assets/blocklens/icon.png\""));
            assertTrue(metadata.contains("\"environment\": \"client\""));
        }

        String versionModule = Files.readString(root().resolve("gradle/version-module.gradle"));
        assertTrue(versionModule.contains("assets/blocklens/lang/en_us.json"));
        assertTrue(versionModule.contains("assets/blocklens/lang/ja_jp.json"));
    }

    @Test
    void releaseWorkflowPublishesOnlyTheExactSuccessfulMainCiArtifacts() throws IOException {
        String release = Files.readString(root().resolve(".github/workflows/release.yml"));
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));

        assertTrue(release.contains("workflow_run:"));
        assertTrue(release.contains("- CI"));
        assertTrue(release.contains("github.event.workflow_run.conclusion == 'success'"));
        assertTrue(release.contains("github.event.workflow_run.head_branch == 'main'"));
        assertTrue(release.contains("github.event.workflow_run.event == 'push'"));
        assertTrue(release.contains("actions: read"));
        assertTrue(release.contains("contents: write"));
        assertTrue(release.contains("ref: ${{ github.event.workflow_run.head_sha }}"));
        assertTrue(release.contains("RUN_ID: ${{ github.event.workflow_run.id }}"));
        assertTrue(ci.contains("archive: false"));
        assertTrue(release.contains("Download the exact raw JAR artifacts produced by successful CI"));
        assertTrue(release.contains("/actions/artifacts/${id}/zip"));
        assertTrue(release.contains("curl --fail --location --silent --show-error"));
        assertTrue(release.contains("dist/raw-26.1.2.jar"));
        assertTrue(release.contains("dist/raw-26.2.jar"));
        assertFalse(release.contains("gh run download"));
        assertTrue(release.contains("gh release view"));
        assertTrue(release.contains("gh release create"));
        assertTrue(release.contains("runtime_jar_baseline_bytes"));
        assertTrue(release.contains("runtime_jar_release_budget_bytes"));
        assertTrue(release.contains("runtime_jar_hard_max_bytes"));
        assertTrue(release.contains("SHA256SUMS.txt"));
        assertTrue(release.contains("assets/blocklens/icon.png"));
        assertTrue(release.contains("no duplicate release"));

        assertTrue(release.contains("40 independently configurable capabilities"));
        assertTrue(release.contains("37-capability source baseline"));
        assertTrue(release.contains("Crying Obsidian, Nether Gold Ore, and Nether Quartz Ore"));
        assertTrue(release.contains("Tripwire Hook coverage in String Tweaks"));
        assertTrue(release.contains("Polished Basalt coverage in Nether Tweaks"));
        assertFalse(release.contains("with **37 independently configurable capabilities**"));
    }

    @Test
    void releaseVersionAndArtifactBudgetsAreExplicit() throws IOException {
        String properties = Files.readString(root().resolve("gradle.properties"));
        String english = Files.readString(root().resolve("README.md"));
        String japanese = Files.readString(root().resolve("README_ja.md"));

        assertTrue(properties.contains("mod_version=0.3.0"));
        assertTrue(properties.contains("v0.1.0 is the published 37-capability source baseline"));
        assertTrue(properties.contains("v0.2.2 adds the missing Mod Menu settings entrypoint"));
        assertTrue(properties.contains("v0.3.0 ships 57 capabilities across Minecraft 26.1.2, 26.2 and 26.3"));

        assertTrue(properties.contains("runtime_jar_baseline_bytes=204800"));
        assertTrue(properties.contains("runtime_jar_release_budget_bytes=204800"));
        assertTrue(properties.contains("runtime_jar_hard_max_bytes=1183432"));
        assertTrue(properties.contains("user-facing product ceiling is now 200 KiB"));

        assertTrue(english.contains("v0.1.0"));
        assertTrue(japanese.contains("v0.1.0"));
        assertTrue(english.contains("v0.2.0"));
        assertTrue(japanese.contains("v0.2.0"));
        assertTrue(english.contains("95,333 B"));
        assertTrue(japanese.contains("95,333 B"));
        assertTrue(english.contains("96,257 B"));
        assertTrue(japanese.contains("96,257 B"));
        assertTrue(english.contains("96,248 B"));
        assertTrue(japanese.contains("96,248 B"));
        assertTrue(english.contains("34802548055"));
        assertTrue(japanese.contains("34802548055"));
    }

    @Test
    void userDocumentationExplainsTheEngineeringFoundationAndSupportBoundary() throws IOException {
        String english = Files.readString(root().resolve("README.md"));
        String japanese = Files.readString(root().resolve("README_ja.md"));

        for (String document : new String[] {english, japanese}) {
            assertTrue(document.contains("Java 25"));
            assertTrue(document.contains("Gradle 9.5.1"));
            assertTrue(document.contains("Fabric Loom 1.17.19"));
            assertTrue(document.contains("JUnit"));
            assertTrue(document.contains("JaCoCo 0.8.15"));
            assertTrue(document.contains("PIT 1.19.0"));
            assertTrue(document.contains("Fabric Client GameTest"));
            assertTrue(document.contains("GitHub Actions"));
            assertTrue(document.contains("SHA-256"));
            assertTrue(document.contains("95,333 B"));
            assertTrue(document.contains("96,257 B"));
            assertTrue(document.contains("100 KiB"));
            assertTrue(document.contains("shader-OFF"));
            assertTrue(document.contains("Vulkan"));
            assertTrue(document.contains("release-readiness.md"));
            assertTrue(document.contains("source-raw-parity-audit.md"));
            assertTrue(document.contains("chisetweaks-migration.md"));
        }
    }
}

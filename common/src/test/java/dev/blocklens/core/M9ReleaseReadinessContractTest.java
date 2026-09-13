package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

        for (String module : new String[] {"mc26_1_2", "mc26_2"}) {
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

        assertTrue(release.contains("workflow_run:"));
        assertTrue(release.contains("- CI"));
        assertTrue(release.contains("github.event.workflow_run.conclusion == 'success'"));
        assertTrue(release.contains("github.event.workflow_run.head_branch == 'main'"));
        assertTrue(release.contains("github.event.workflow_run.event == 'push'"));
        assertTrue(release.contains("actions: read"));
        assertTrue(release.contains("contents: write"));
        assertTrue(release.contains("ref: ${{ github.event.workflow_run.head_sha }}"));
        assertTrue(release.contains("RUN_ID: ${{ github.event.workflow_run.id }}"));
        assertTrue(release.contains("gh run download"));
        assertTrue(release.contains("gh release view"));
        assertTrue(release.contains("gh release create"));
        assertTrue(release.contains("runtime_jar_baseline_bytes"));
        assertTrue(release.contains("runtime_jar_release_budget_bytes"));
        assertTrue(release.contains("runtime_jar_hard_max_bytes"));
        assertTrue(release.contains("SHA256SUMS.txt"));
        assertTrue(release.contains("assets/blocklens/icon.png"));
        assertTrue(release.contains("No duplicate release"));
    }

    @Test
    void releaseVersionAndArtifactBudgetsAreExplicit() throws IOException {
        String properties = Files.readString(root().resolve("gradle.properties"));

        assertTrue(properties.contains("mod_version=0.1.0"));
        assertTrue(properties.contains("runtime_jar_baseline_bytes=95333"));
        assertTrue(properties.contains("runtime_jar_release_budget_bytes=102400"));
        assertTrue(properties.contains("runtime_jar_hard_max_bytes=1183432"));
        assertTrue(properties.contains("PR #19 measurement run 34766720469"));
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
            assertTrue(document.contains("100 KiB"));
            assertTrue(document.contains("shader-OFF"));
            assertTrue(document.contains("Vulkan"));
            assertTrue(document.contains("release-readiness.md"));
        }
    }
}

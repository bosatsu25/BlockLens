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
    void mainMergePublishesOnlyAfterTheFullMatrixPassedOnTheSameTree() throws IOException {
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));
        assertFalse(Files.exists(root().resolve(".github/workflows/release.yml")),
                "release runs inside CI so it can depend on the verification jobs directly");

        // An independent successful workflow is insufficient: both compatibility
        // matrices must be required jobs in the same verification run.
        assertTrue(ci.contains("uses: ./.github/workflows/bbe-compatibility.yml"),
                "publishing CI omits the optimized chest rendering paths");
        assertTrue(ci.contains("uses: ./.github/workflows/external-compatibility.yml"),
                "publishing CI omits the advertised shader/backend profiles");
        assertFalse(ci.contains("continue-on-error:"), "required CI failures must block release");
        for (String workflow : new String[] {"bbe-compatibility.yml", "external-compatibility.yml"}) {
            String required = Files.readString(root().resolve(".github/workflows/" + workflow));
            assertTrue(required.contains("workflow_call:"), "matrix must run inside authorizing CI");
            assertFalse(required.contains("continue-on-error:"));
        }

        String verified = job(ci, "verified");
        assertTrue(verified.contains("name: verified / full matrix"));
        for (String required : new String[] {"common-quality", "version-build", "external-compatibility",
                "bbe-compatibility", "connected-compatibility", "connected-native"}) {
            assertTrue(verified.contains(required), "verified must require " + required);
        }
        assertFalse(verified.contains("always()"), "verified succeeds only when every required job succeeded");

        String plan = job(ci, "plan");
        assertTrue(plan.contains("scripts/plan_ci.py"));
        assertTrue(plan.contains("check_name=verified%20%2F%20full%20matrix"));
        assertTrue(plan.contains("git rev-parse 'HEAD^{tree}'"), "reuse requires the identical tree");
        assertTrue(plan.contains("releases/latest"), "a main push is judged on the changes since the latest release");
        assertFalse(plan.contains("github.event.before"), "a replaced pending run must not lose its merge");
        assertTrue(ci.contains("group: blocklens-ci-${{ github.event_name == 'workflow_dispatch' && 'manual-' || '' }}${{ github.ref }}"),
                "manual runs must not replace a pending main push");

        String release = job(ci, "release");
        assertTrue(release.contains("needs.plan.outputs.release == 'true'"));
        assertTrue(release.contains("needs.release-build.result == 'success'"));
        assertTrue(release.contains("needs.verified.result == 'success'"));
        assertTrue(release.contains("needs.plan.outputs.full == 'false' && needs.verified.result == 'skipped'"));
        assertTrue(release.contains("!cancelled()"));
        assertTrue(release.contains("contents: write"));
        assertTrue(release.contains("gh release create"));
        assertTrue(release.contains("--target \"$GITHUB_SHA\""));
        assertTrue(release.contains("no duplicate release"));
        assertTrue(release.contains("runtime_jar_baseline_bytes"));
        assertTrue(release.contains("runtime_jar_release_budget_bytes"));
        assertTrue(release.contains("runtime_jar_hard_max_bytes"));
        assertTrue(release.contains("SHA256SUMS.txt"));
        assertTrue(release.contains("assets/blocklens/icon.png"));
        assertTrue(release.contains("Remove JARs from previous releases"));
        assertFalse(ci.substring(0, ci.indexOf("\njobs:")).contains("contents: write"),
                "only the release job may write repository contents");

        String releaseBuild = job(ci, "release-build");
        assertTrue(releaseBuild.contains("-Pmod_version=\"$VERSION\""));
        assertTrue(releaseBuild.contains("versionSmokeContract"));
        assertTrue(releaseBuild.contains("verifyRuntimeJarBudget"));
    }

    private static String job(String workflow, String id) {
        int start = workflow.indexOf("\n  " + id + ":\n");
        assertTrue(start >= 0, "missing CI job " + id);
        int end = workflow.indexOf("\n  ", start + id.length() + 4);
        while (end >= 0 && workflow.charAt(end + 3) == ' ') {
            end = workflow.indexOf("\n  ", end + 1);
        }
        return end < 0 ? workflow.substring(start) : workflow.substring(start, end);
    }

    @Test
    void releaseVersionAndArtifactBudgetsAreExplicit() throws IOException {
        String properties = Files.readString(root().resolve("gradle.properties"));
        String english = Files.readString(root().resolve("README.md"));
        String japanese = Files.readString(root().resolve("README_ja.md"));

        // The base of the automatic patch line: a main merge publishes the first free v0.4.N >= 0.4.7.
        assertTrue(properties.contains("mod_version=0.4.7"));
        assertTrue(english.contains("**Current implementation:** BlockLens **v0.4.7** provides **"
                + CapabilityId.values().length + " independently configurable capabilities**"));
        assertTrue(japanese.contains("**現行実装:** BlockLens **v0.4.7**"));
        assertTrue(japanese.contains("**独立設定可能な" + CapabilityId.values().length + "機能**"));
        assertFalse(english.contains("**Current stable release:** BlockLens **v0.4.4**"));
        assertFalse(japanese.contains("**現在の安定版:** BlockLens **v0.4.4**"));
        assertTrue(properties.contains("v0.1.0 is the published 37-capability source baseline"));
        assertTrue(properties.contains("v0.2.2 adds the missing Mod Menu settings entrypoint"));
        assertTrue(properties.contains("v0.3.0 shipped 57 capabilities across Minecraft 26.1.2, 26.2 and 26.3"));
        assertTrue(properties.contains("v0.4.0 keeps 55"));

        assertTrue(properties.contains("runtime_jar_baseline_bytes=256000"));
        assertTrue(properties.contains("runtime_jar_release_budget_bytes=256000"));
        assertTrue(properties.contains("runtime_jar_hard_max_bytes=1183432"));
        assertTrue(properties.contains("user-facing product ceiling is now 250 KiB"));

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

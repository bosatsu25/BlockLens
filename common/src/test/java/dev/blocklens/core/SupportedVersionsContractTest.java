package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

final class SupportedVersionsContractTest {
    private static final List<String> VERSIONS = List.of("26.1.2", "26.2", "26.3");

    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void everySupportedVersionHasAnExactModuleAndFullCiEntry() throws IOException {
        String settings = Files.readString(root().resolve("settings.gradle"));
        String properties = Files.readString(root().resolve("gradle.properties"));
        String ci = Files.readString(root().resolve(".github/workflows/ci.yml"));
        String build = Files.readString(root().resolve("build.gradle"));
        for (String version : VERSIONS) {
            String suffix = version.replace('.', '_');
            String module = "mc" + suffix;
            assertTrue(settings.contains("include 'versions:" + module + "'"), version);
            assertTrue(properties.contains("minecraft_" + suffix + "=" + version), version);
            assertTrue(ci.contains("module: " + module), version);
            assertTrue(ci.contains("minecraft: '" + version + "'"), version);
            assertTrue(build.contains(":versions:" + module + ":test"), version);
            assertTrue(build.contains(":versions:" + module + ":verifyRuntimeJarBudget"), version);
            String metadata = Files.readString(root().resolve("versions/" + module
                    + "/src/main/resources/fabric.mod.json"));
            assertTrue(metadata.contains("\"minecraft\": \"=${minecraft_version}\""), version);
            assertTrue(metadata.contains("\"environment\": \"client\""), version);
        }
        assertTrue(ci.contains(":versions:${{ matrix.module }}:runClientGameTest"));
        assertTrue(ci.contains("Collect settings UI visual evidence"));
        assertTrue(ci.contains("Expected exactly ten settings UI screenshots"));
        assertTrue(ci.contains("SDL_VIDEO_FORCE_EGL: ${{ matrix.module == 'mc26_3' && '1' || '0' }}"));
        int eglSetup = ci.indexOf("name: Install SDL3 EGL runtime");
        assertTrue(eglSetup >= 0 && eglSetup < ci.indexOf("name: Run client GameTest"));
        String eglStep = ci.substring(eglSetup, ci.indexOf("name: Run client GameTest"));
        assertTrue(eglStep.contains("if: matrix.module == 'mc26_3'"));
        assertTrue(eglStep.contains("sudo apt-get install --no-install-recommends -y libegl1 libgl1-mesa-dri"));
    }

    @Test
    void releaseArtifactSelectionAndChecksumsIncludeEverySupportedVersion() throws IOException {
        String release = Files.readString(root().resolve(".github/workflows/release.yml"));
        for (String version : VERSIONS) {
            assertTrue(release.contains("resolve_name '" + version + "'"), version);
            assertTrue(release.contains("dist/raw-" + version + ".jar"), version);
            assertTrue(release.contains("BlockLens-" + version + "-v${version}.jar"), version);
        }
    }

    @Test
    void modernRendererIsSharedWithOnlyAnExplicitLightingBoundary() throws IOException {
        String modern = Files.readString(root().resolve("versions/mc26_2/src/main/java/"
                + "dev/blocklens/fabric/MinecraftDecorationModel.java"));
        String thirdBuild = Files.readString(root().resolve("versions/mc26_3/build.gradle"));
        assertTrue(thirdBuild.contains("versions/mc26_2/src/main/java"));
        assertTrue(thirdBuild.contains("versions/mc26_2/src/gametest/java"));
        assertFalse(Files.exists(root().resolve("versions/mc26_3/src/main/java/"
                + "dev/blocklens/fabric/MinecraftDecorationModel.java")));
        assertTrue(modern.contains("MinecraftQuadLighting.disableDirectionalShade(quad)"));
        assertTrue(modern.contains("quad.emissive(true)"));
        assertTrue(modern.contains("quad.ambientOcclusion(TriState.FALSE)"));
        String older = Files.readString(root().resolve("versions/mc26_2/src/compat/java/"
                + "dev/blocklens/fabric/MinecraftQuadLighting.java"));
        String newer = Files.readString(root().resolve("versions/mc26_3/src/main/java/"
                + "dev/blocklens/fabric/MinecraftQuadLighting.java"));
        assertTrue(older.contains("quad.diffuseShade(false)"));
        assertTrue(newer.contains("quad.shadeDirectionOverride(Direction.UP)"));
    }

    @Test
    void settingsInputUsesMinecraftConstantsAcrossGlfwAndSdl() throws IOException {
        Pattern backendNumbers = Pattern.compile(
                "new KeyEvent\\(\\s*\\d+|new MouseButtonInfo\\(\\s*\\d+"
                        + "|event\\.key\\(\\)\\s*==\\s*\\d+|case 26[6-9]\\s*->");
        for (Path directory : List.of(root().resolve("fabric-shared/src/main/java"),
                root().resolve("gametest/java"))) {
            try (var paths = Files.walk(directory)) {
                for (Path file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                    assertFalse(backendNumbers.matcher(Files.readString(file)).find(),
                            "Backend-specific input number in " + file.getFileName());
                }
            }
        }
    }
}

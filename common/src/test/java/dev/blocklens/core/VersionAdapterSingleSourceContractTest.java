package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Keeps every supported Minecraft line on one copy of each behavior.
 *
 * <p>A Fabric adapter whose code is the same for two lines belongs in a shared source root;
 * version trees may hold only thin bridges whose code actually differs. This turns accidental
 * copy-and-diverge drift between 26.1.2, 26.2 and 26.3 into a deterministic failure.</p>
 */
final class VersionAdapterSingleSourceContractTest {
    private static final int MAX_VERSION_BRIDGE_LINES = 60;
    private static final List<String> MAIN_VERSION_ROOTS = List.of(
            "versions/mc26_1_2/src/main/java",
            "versions/mc26_2/src/main/java",
            "versions/mc26_3/src/main/java",
            SupportedVersionFixtures.SHARED_PRE_26_3);
    private static final List<String> GAMETEST_VERSION_ROOTS = List.of(
            "versions/mc26_1_2/src/gametest/java",
            "versions/mc26_2/src/gametest/java",
            "versions/mc26_3/src/gametest/java",
            SupportedVersionFixtures.GAMETEST_PRE_26_3);

    private static Path root() {
        return Path.of(System.getProperty("blocklens.repoRoot"));
    }

    @Test
    void identicalAdapterCodeExistsOnlyOnceAcrossVersionRoots() throws IOException {
        assertNoNormalizedDuplicates(MAIN_VERSION_ROOTS);
        assertNoNormalizedDuplicates(GAMETEST_VERSION_ROOTS);
    }

    @Test
    void versionRootsNeverShadowSharedSources() throws IOException {
        assertNoShadowing(SupportedVersionFixtures.SHARED_MAIN, MAIN_VERSION_ROOTS);
        assertNoShadowing("gametest/java", GAMETEST_VERSION_ROOTS);
    }

    @Test
    void versionSpecificMainSourcesStayThinBridges() throws IOException {
        for (String relativeRoot : MAIN_VERSION_ROOTS) {
            for (Map.Entry<String, Path> source : javaSources(root().resolve(relativeRoot)).entrySet()) {
                long lines = Files.readString(source.getValue()).lines().count();
                assertTrue(lines <= MAX_VERSION_BRIDGE_LINES, () -> relativeRoot + "/" + source.getKey()
                        + " has " + lines + " lines; move shared behavior to " + SupportedVersionFixtures.SHARED_MAIN);
            }
        }
    }

    @Test
    void normalizationIgnoresOnlyCommentsAndWhitespace() {
        String first = "/** 26.1.2 */\nfinal class A { int x = 1; // note\n}";
        String second = "// 26.2\nfinal class A {\n    int x = 1;\n}\n";
        String different = "final class A { int x = 2; }";
        assertTrue(normalize(first).equals(normalize(second)));
        assertFalse(normalize(first).equals(normalize(different)));
        assertTrue(normalize("String s = \"a//b\";").contains("a//b"));
    }

    private static void assertNoNormalizedDuplicates(List<String> relativeRoots) throws IOException {
        Map<String, List<String>> owners = new HashMap<>();
        Map<String, List<String>> normalizedVariants = new HashMap<>();
        for (String relativeRoot : relativeRoots) {
            for (Map.Entry<String, Path> source : javaSources(root().resolve(relativeRoot)).entrySet()) {
                String normalized = normalize(Files.readString(source.getValue()));
                List<String> variants = normalizedVariants.computeIfAbsent(source.getKey(), ignored -> new ArrayList<>());
                List<String> roots = owners.computeIfAbsent(source.getKey(), ignored -> new ArrayList<>());
                roots.add(relativeRoot);
                if (variants.contains(normalized)) {
                    fail(source.getKey() + " is identical in " + roots + "; keep one copy in a shared source root");
                }
                variants.add(normalized);
            }
        }
    }

    private static void assertNoShadowing(String sharedRoot, List<String> versionRoots) throws IOException {
        var shared = javaSources(root().resolve(sharedRoot)).keySet();
        for (String relativeRoot : versionRoots) {
            for (String relative : javaSources(root().resolve(relativeRoot)).keySet()) {
                assertFalse(shared.contains(relative), () -> relativeRoot + "/" + relative
                        + " duplicates the class compiled from " + sharedRoot);
            }
        }
    }

    private static Map<String, Path> javaSources(Path directory) throws IOException {
        Map<String, Path> sources = new HashMap<>();
        if (!Files.isDirectory(directory)) {
            return sources;
        }
        try (var paths = Files.walk(directory)) {
            for (Path file : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                sources.put(directory.relativize(file).toString().replace('\\', '/'), file);
            }
        }
        return sources;
    }

    /** Removes comments and whitespace outside string/char literals. */
    static String normalize(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
                while (i < source.length() && source.charAt(i) != '\n') i++;
            } else if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? source.length() : end + 2;
            } else if (c == '"' || c == '\'') {
                int end = i + 1;
                while (end < source.length() && source.charAt(end) != c) {
                    end += source.charAt(end) == '\\' ? 2 : 1;
                }
                end = Math.min(end + 1, source.length());
                out.append(source, i, end);
                i = end;
            } else {
                if (!Character.isWhitespace(c)) out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}

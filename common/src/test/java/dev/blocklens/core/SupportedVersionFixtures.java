package dev.blocklens.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Expected adapter coverage; 26.3 compiles the compatible 26.2 adapters independently.
 *
 * <p>Fabric glue is resolved in the same order Gradle composes each module: the version tree,
 * the shared pre-26.3 bridge for 26.1.2/26.2, then the code shared by every line.</p>
 */
final class SupportedVersionFixtures {
    static final List<String> MODULES = List.of("mc26_1_2", "mc26_2", "mc26_3");
    static final List<String> PRE_26_3_MODULES = List.of("mc26_1_2", "mc26_2");
    static final String SHARED_MAIN = "fabric-shared/src/main/java";
    static final String SHARED_PRE_26_3 = "fabric-shared/src/pre26_3/java";
    static final String GAMETEST_PRE_26_3 = "gametest/pre26_3/java";
    private static final String FABRIC_PACKAGE = "dev/blocklens/fabric";

    private SupportedVersionFixtures() {
    }

    static Path sourceRoot(Path root, String module) {
        String adapter = module.equals("mc26_3") ? "mc26_2" : module;
        return root.resolve("versions").resolve(adapter).resolve("src/main/java/dev/blocklens/fabric");
    }

    /** Resolves the single compiled source of {@code fileName} for one supported module. */
    static Path adapterSource(Path root, String module, String fileName) {
        Path versionSpecific = sourceRoot(root, module).resolve(fileName);
        if (Files.isRegularFile(versionSpecific)) {
            return versionSpecific;
        }
        if (PRE_26_3_MODULES.contains(module)) {
            Path older = root.resolve(SHARED_PRE_26_3).resolve(FABRIC_PACKAGE).resolve(fileName);
            if (Files.isRegularFile(older)) {
                return older;
            }
        }
        return root.resolve(SHARED_MAIN).resolve(FABRIC_PACKAGE).resolve(fileName);
    }

    static Path sharedSource(Path root, String fileName) {
        return root.resolve(SHARED_MAIN).resolve(FABRIC_PACKAGE).resolve(fileName);
    }

    static Path rendererSource(Path root) {
        return sharedSource(root, "MinecraftDecorationModel.java");
    }

    static Path testSourceRoot(Path root, String module) {
        String adapter = module.equals("mc26_3") ? "mc26_2" : module;
        return root.resolve("versions").resolve(adapter).resolve("src/gametest");
    }
}

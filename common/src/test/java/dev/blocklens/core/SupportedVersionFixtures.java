package dev.blocklens.core;

import java.nio.file.Path;
import java.util.List;

/** Expected adapter coverage; 26.3 compiles the compatible 26.2 adapters independently. */
final class SupportedVersionFixtures {
    static final List<String> MODULES = List.of("mc26_1_2", "mc26_2", "mc26_3");

    private SupportedVersionFixtures() {
    }

    static Path sourceRoot(Path root, String module) {
        String adapter = module.equals("mc26_3") ? "mc26_2" : module;
        return root.resolve("versions").resolve(adapter).resolve("src/main/java/dev/blocklens/fabric");
    }

    static Path testSourceRoot(Path root, String module) {
        String adapter = module.equals("mc26_3") ? "mc26_2" : module;
        return root.resolve("versions").resolve(adapter).resolve("src/gametest");
    }
}

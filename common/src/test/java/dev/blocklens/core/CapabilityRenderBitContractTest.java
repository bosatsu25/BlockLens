package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Keeps capability bits in render paths on {@link CapabilityId#renderBit()} across every source tree. */
final class CapabilityRenderBitContractTest {
    @Test
    void mainSourcesNeverShiftByARawBitPosition() throws IOException {
        // Raw "1L << capability.bitPosition()" aliases past bit 63; render paths use renderBit().
        Path root = Path.of(System.getProperty("blocklens.repoRoot"));
        Pattern rawShift = Pattern.compile("<<\\s*[\\w.()]*bitPosition\\(\\)");
        List<Path> sources = new ArrayList<>();
        for (String tree : List.of("common/src/main/java", "fabric-shared/src/main/java", "versions", "gametest/java")) {
            try (Stream<Path> files = Files.walk(root.resolve(tree))) {
                files.filter(path -> path.toString().endsWith(".java")).forEach(sources::add);
            }
        }
        assertTrue(sources.size() > 100, "source trees were not found");
        for (Path source : sources) {
            assertFalse(rawShift.matcher(Files.readString(source)).find(), root.relativize(source).toString());
        }
    }
}

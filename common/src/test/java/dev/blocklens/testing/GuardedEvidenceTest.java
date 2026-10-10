package dev.blocklens.testing;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class GuardedEvidenceTest {
    @TempDir Path directory;

    @Test
    void preservesRawSamplesWhenGuardFails() throws IOException {
        Path evidence = directory.resolve("samples.txt");
        AssertionError failure = assertThrows(AssertionError.class, () ->
                GuardedEvidence.publishBeforeChecks(
                        () -> Files.writeString(evidence, "allocatedBytes=36183336,36712856,36683960\n"),
                        () -> { throw new AssertionError("coarse guard exceeded"); }));
        assertEquals("coarse guard exceeded", failure.getMessage());
        assertTrue(Files.readString(evidence).contains("36183336"));
    }

    @Test
    void checksSeeAlreadyPublishedEvidence() throws IOException {
        Path evidence = directory.resolve("samples.txt");
        GuardedEvidence.publishBeforeChecks(() -> Files.writeString(evidence, "raw=1,2,3\n"),
                () -> assertTrue(Files.isRegularFile(evidence)));
    }

    @Test
    void writeFailurePreventsMisleadingValidation() {
        AtomicBoolean checked = new AtomicBoolean();
        assertThrows(IOException.class, () -> GuardedEvidence.publishBeforeChecks(
                () -> { throw new IOException("fixture write failed"); }, () -> checked.set(true)));
        assertFalse(checked.get());
    }
}

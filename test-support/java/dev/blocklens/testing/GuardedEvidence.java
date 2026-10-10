package dev.blocklens.testing;

import java.io.IOException;

/** Test-only evidence must survive a failing regression guard. */
public final class GuardedEvidence {
    private GuardedEvidence() {
    }

    @FunctionalInterface
    public interface Writer {
        void write() throws IOException;
    }

    public static void publishBeforeChecks(Writer writer, Runnable checks) throws IOException {
        writer.write();
        checks.run();
    }
}

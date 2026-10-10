package dev.blocklens.core.render;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RetainedValueTest {
    @Test void concurrentFirstLookupsPublishOneCompleteValue() throws Exception {
        var value = new RetainedValue<String>();
        var calls = new AtomicInteger();
        var start = new CountDownLatch(1);
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var workers = Executors.newFixedThreadPool(12)) {
            var results = new ArrayList<java.util.concurrent.Future<String>>();
            for (int i = 0; i < 12; i++) results.add(workers.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return value.get("key", key -> {
                    calls.incrementAndGet();
                    entered.countDown();
                    try {
                        if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("loader timeout");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                    return "loaded";
                });
            }));
            start.countDown();
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            release.countDown();
            for (var result : results) assertEquals("loaded", result.get(5, TimeUnit.SECONDS));
            assertEquals(1, calls.get());
            assertEquals("loaded", value.get("key", key -> { throw new AssertionError("reloaded"); }));
        } finally {
            release.countDown();
        }
    }
    @Test void missingResultIsRetainedButThrownLookupCanRetry() {
        var missing = new RetainedValue<String>();
        assertNull(missing.get("key", key -> null));
        assertNull(missing.get("key", key -> { throw new AssertionError("missing value retried"); }));
        var retry = new RetainedValue<String>();
        assertThrows(IllegalStateException.class, () -> retry.get("key", key -> { throw new IllegalStateException(); }));
        assertEquals("retry", retry.get("key", key -> "retry"));
    }
}

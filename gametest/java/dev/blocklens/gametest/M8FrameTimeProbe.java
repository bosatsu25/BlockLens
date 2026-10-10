package dev.blocklens.gametest;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

/** Test-only main render-pass sampler used by the M8 real-client oracle. */
final class M8FrameTimeProbe {
    private static final Object LOCK = new Object();
    static final int MAX_TIMING_SAMPLES = 1024;
    private static final List<Long> SAMPLES = new ArrayList<>();

    private static volatile boolean capture;
    private static volatile long frameStartNanos;
    private static volatile long renderedFrames;
    private static boolean installed;

    private M8FrameTimeProbe() {
    }

    static void install() {
        synchronized (LOCK) {
            if (installed) {
                return;
            }
            LevelRenderEvents.START_MAIN.register(context -> {
                if (capture) {
                    frameStartNanos = System.nanoTime();
                }
            });
            LevelRenderEvents.END_MAIN.register(context -> {
                renderedFrames++;
                if (!capture) {
                    return;
                }
                long start = frameStartNanos;
                if (start <= 0L) {
                    return;
                }
                long elapsed = System.nanoTime() - start;
                frameStartNanos = 0L;
                if (elapsed <= 0L) {
                    return;
                }
                synchronized (LOCK) {
                    if (capture && SAMPLES.size() < MAX_TIMING_SAMPLES) {
                        SAMPLES.add(elapsed);
                    }
                }
            });
            installed = true;
        }
    }

    static long renderedFrames() {
        return renderedFrames;
    }

    static void beginCapture() {
        install();
        synchronized (LOCK) {
            SAMPLES.clear();
            frameStartNanos = 0L;
            capture = true;
        }
    }

    static long[] endCapture() {
        capture = false;
        synchronized (LOCK) {
            long[] result = new long[SAMPLES.size()];
            for (int i = 0; i < SAMPLES.size(); i++) {
                result[i] = SAMPLES.get(i);
            }
            SAMPLES.clear();
            frameStartNanos = 0L;
            return result;
        }
    }
}

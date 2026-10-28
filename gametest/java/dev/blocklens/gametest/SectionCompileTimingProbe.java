package dev.blocklens.gametest;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Test-only section-compile cost probe: wall, thread CPU and allocated bytes of every completed
 * vanilla section compile, split into sections inside a declared scene box and all others.
 *
 * <p>The M8 rebuild metric measures a fixed settle window; this probe measures the compile work
 * itself, so pack and BlockLens meshing cost can be compared on the same scene.</p>
 */
public final class SectionCompileTimingProbe {
    private static final com.sun.management.ThreadMXBean BEAN =
            (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    private static final ThreadLocal<long[]> START = ThreadLocal.withInitial(() -> new long[4]);
    private static final LongAdder SCENE_COUNT = new LongAdder(), SCENE_WALL = new LongAdder(),
            SCENE_CPU = new LongAdder(), SCENE_ALLOCATED = new LongAdder(), OTHER_COUNT = new LongAdder(),
            OTHER_CPU = new LongAdder(), INCOMPLETE = new LongAdder();
    private static final AtomicLong LAST_RETURN = new AtomicLong();
    private static volatile boolean capture;
    private static volatile int minX, minY, minZ, maxX, maxY, maxZ;

    private SectionCompileTimingProbe() { }

    /** Inclusive section-coordinate box that holds the measured scene. */
    static void scene(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        minX = fromX; minY = fromY; minZ = fromZ; maxX = toX; maxY = toY; maxZ = toZ;
    }

    public static void head(int x, int y, int z) {
        if (!capture) return;
        long[] start = START.get();
        if (start[0] != 0L) INCOMPLETE.increment();
        boolean inScene = x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        start[3] = inScene ? 1L : 0L;
        start[2] = BEAN.getCurrentThreadAllocatedBytes();
        start[1] = BEAN.getCurrentThreadCpuTime();
        start[0] = System.nanoTime();
    }

    public static void returned() {
        long[] start = START.get();
        if (start[0] == 0L) return;
        long wall = System.nanoTime() - start[0];
        long cpu = BEAN.getCurrentThreadCpuTime() - start[1];
        long allocated = BEAN.getCurrentThreadAllocatedBytes() - start[2];
        boolean inScene = start[3] == 1L;
        start[0] = 0L;
        if (!capture) return;
        if (inScene) {
            SCENE_COUNT.increment();
            SCENE_WALL.add(wall);
            SCENE_CPU.add(cpu);
            SCENE_ALLOCATED.add(allocated);
        } else {
            OTHER_COUNT.increment();
            OTHER_CPU.add(cpu);
        }
        LAST_RETURN.accumulateAndGet(System.nanoTime(), Math::max);
    }

    static void begin() {
        if (!BEAN.isThreadCpuTimeEnabled()) BEAN.setThreadCpuTimeEnabled(true);
        if (!BEAN.isThreadAllocatedMemoryEnabled()) BEAN.setThreadAllocatedMemoryEnabled(true);
        for (LongAdder adder : new LongAdder[] {SCENE_COUNT, SCENE_WALL, SCENE_CPU, SCENE_ALLOCATED,
                OTHER_COUNT, OTHER_CPU, INCOMPLETE}) {
            adder.reset();
        }
        LAST_RETURN.set(0L);
        capture = true;
    }

    static long completed() {
        return SCENE_COUNT.sum() + OTHER_COUNT.sum();
    }

    static Snapshot end() {
        capture = false;
        return new Snapshot(SCENE_COUNT.sum(), SCENE_WALL.sum(), SCENE_CPU.sum(), SCENE_ALLOCATED.sum(),
                OTHER_COUNT.sum(), OTHER_CPU.sum(), INCOMPLETE.sum(), LAST_RETURN.get());
    }

    record Snapshot(long sceneCompiles, long sceneWallNanos, long sceneCpuNanos, long sceneAllocatedBytes,
            long otherCompiles, long otherCpuNanos, long incomplete, long lastReturnNanos) {
    }
}

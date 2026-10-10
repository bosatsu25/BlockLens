package dev.blocklens.gametest;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Test-only observation of actual, completed Sodium terrain model calls. */
public final class SodiumTerrainProbe {
    public static final BlockPos TARGET = new BlockPos(0, -57, 4);
    private static final int LIMIT = 4096;
    private static final ThreadLocal<Sample> IN_FLIGHT = new ThreadLocal<>();
    private static volatile Sample current;

    private SodiumTerrainProbe() { }

    static void arm(BlockState state, BlockStateModel expected) {
        current = new Sample(state, expected);
    }

    static Result result() {
        Sample sample = current;
        return sample == null ? new Result(0, 0, 0, 0, false) : sample.snapshot();
    }

    static void clear() {
        current = null;
    }

    public static void beforeModel(BlockState state, BlockPos position) {
        Sample sample = current;
        if (sample == null || state != sample.state || !TARGET.equals(position)) {
            IN_FLIGHT.remove();
            return;
        }
        IN_FLIGHT.set(sample);
        sample.visit();
    }

    public static void emitted(BlockStateModel model, BlockState state, BlockPos position) {
        Sample sample = IN_FLIGHT.get();
        if (sample != null && state == sample.state && TARGET.equals(position)) sample.emit(model);
    }

    public static void afterModel() {
        Sample sample = IN_FLIGHT.get();
        IN_FLIGHT.remove();
        if (sample != null) sample.complete();
    }

    record Result(int visited, int completed, int emitted, int expectedModel, boolean saturated) { }

    private static final class Sample {
        final BlockState state;
        final BlockStateModel expected;
        private int visited;
        private int completed;
        private int emitted;
        private int expectedModel;
        private boolean saturated;

        Sample(BlockState state, BlockStateModel expected) {
            this.state = state;
            this.expected = expected;
        }

        synchronized void visit() {
            visited = bounded(visited);
        }

        synchronized void complete() {
            completed = bounded(completed);
        }

        synchronized void emit(BlockStateModel model) {
            emitted = bounded(emitted);
            if (model == expected) expectedModel = bounded(expectedModel);
        }

        private int bounded(int value) {
            int next = Math.min(LIMIT, value + 1);
            if (next == LIMIT) saturated = true;
            return next;
        }

        synchronized Result snapshot() {
            return new Result(visited, completed, emitted, expectedModel, saturated);
        }
    }
}

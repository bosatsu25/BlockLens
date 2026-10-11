package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Test-only counters; retain no positions, worlds, entities or third-party objects. */
public final class BBEPathProbe {
    private static final AtomicInteger ENABLED = new AtomicInteger();
    private static final AtomicInteger DISABLED = new AtomicInteger();
    private BBEPathProbe() { }
    public static void reset() { ENABLED.set(0); DISABLED.set(0); }
    public static int enabled() { return ENABLED.get(); }
    public static int disabled() { return DISABLED.get(); }
    public static void emitted(BlockState state) {
        if (state.is(Blocks.CHEST)) {
            (BlockLensRuntime.config().isEnabled(CapabilityId.BRIGHT_CHEST) ? ENABLED : DISABLED).incrementAndGet();
        }
    }
}

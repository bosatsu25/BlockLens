package dev.blocklens.gametest;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

/** Real close/save/reopen regression for Fabric's late server-task shutdown race. */
final class WorldShutdownOracle {
    private static final AtomicBoolean DELAY_NEXT_TICK = new AtomicBoolean();
    private static final AtomicInteger DELAY_COUNT = new AtomicInteger();
    static {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (DELAY_NEXT_TICK.compareAndSet(true, false)) {
                DELAY_COUNT.incrementAndGet();
                try {
                    // Let the ordinary server tick exhaust its queue before the deferred halt.
                    Thread.sleep(250);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Controlled close delay interrupted", exception);
                }
            }
        });
    }

    private WorldShutdownOracle() { }

    static void verify(ClientGameTestContext context) {
        context.runOnClient(client -> PackagedRuntimeOracle.verify());
        int before = DELAY_COUNT.get();
        var created = TestWorlds.create(context);
        var save = created.world().getWorldSave();
        try (created) {
            created.world().getServer().runCommand("setblock 2 -60 5 minecraft:diamond_block");
            context.waitTicks(2);
            DELAY_NEXT_TICK.set(true);
        } finally {
            DELAY_NEXT_TICK.set(false);
        }
        requireOneDelay(before);
        for (int round = 0; round < 2; round++) {
            before = DELAY_COUNT.get();
            try (var reopened = TestWorlds.open(context, save)) {
                reopened.world().getServer().runOnServer(server -> {
                    if (!server.overworld().getBlockState(new BlockPos(2, -60, 5)).is(Blocks.DIAMOND_BLOCK))
                        throw new AssertionError("Saved shutdown fixture block was not retained");
                });
                DELAY_NEXT_TICK.set(true);
            } finally {
                DELAY_NEXT_TICK.set(false);
            }
            requireOneDelay(before);
        }
        System.out.println("BLOCKLENS_WORLD_SHUTDOWN round_trips=2 delayed_close=true retained_state=true timeout=false");
    }

    private static void requireOneDelay(int before) {
        if (DELAY_COUNT.get() != before + 1)
            throw new AssertionError("Controlled close delay did not run exactly once");
    }
}

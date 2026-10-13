package dev.blocklens.gametest;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave;
import net.minecraft.client.Minecraft;

/** Keeps Fabric's server task queue active during its ordinary singleplayer close. */
final class TestWorlds {
    private TestWorlds() { }

    static Opened create(ClientGameTestContext context) {
        return new Opened(context, context.worldBuilder().create());
    }

    static Opened open(ClientGameTestContext context, TestWorldSave save) {
        return new Opened(context, save.open());
    }

    record Opened(ClientGameTestContext context, TestSingleplayerContext world) implements AutoCloseable {
        @Override public void close() {
            var server = context.computeOnClient(Minecraft::getSingleplayerServer);
            if (server == null) throw new AssertionError("Shutdown requires the live integrated server");
            var guard = TestWorldShutdown.arm(server);
            boolean succeeded = false;
            try {
                // The server wait hook pumps directly; a queued TickTask can miss eligibility.
                world.close();
                guard.verifySucceeded();
                if (!server.isShutdown() || !context.computeOnClient(c -> c.level == null
                        && !c.isLocalServer())) throw new AssertionError("World close was incomplete");
                succeeded = true;
                System.out.println("BLOCKLENS_WORLD_CLOSE task=true client_halt=true timeout=false server_stopped=true level_unloaded=true");
            } finally {
                if (!succeeded) guard.cancel();
                TestWorldShutdown.clear(guard);
            }
        }
    }
}

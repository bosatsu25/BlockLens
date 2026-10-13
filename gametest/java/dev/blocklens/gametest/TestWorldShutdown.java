package dev.blocklens.gametest;

import dev.blocklens.testing.ShutdownHandshake;
import net.minecraft.server.MinecraftServer;

/** GameTest-only bridge; no session exists during ordinary ticks or product startup. */
public final class TestWorldShutdown {
    private static volatile ShutdownHandshake<MinecraftServer> active;

    private TestWorldShutdown() { }

    static synchronized ShutdownHandshake<MinecraftServer> arm(MinecraftServer server) {
        if (active != null) throw new AssertionError("Overlapping test world shutdowns");
        active = new ShutdownHandshake<>(server, System::nanoTime, server::managedBlock, 30_000_000_000L);
        return active;
    }

    static synchronized void clear(ShutdownHandshake<MinecraftServer> session) {
        if (active != session) throw new AssertionError("Shutdown session changed unexpectedly");
        active = null;
    }

    public static void beforeServerWait(MinecraftServer server) {
        var session = active;
        if (session != null && server.isSameThread()) session.pumpOnServer(server);
    }

    public static void afterHalt(MinecraftServer server, boolean clientThread) {
        var session = active;
        if (session != null) session.haltReturned(server, clientThread);
    }
}

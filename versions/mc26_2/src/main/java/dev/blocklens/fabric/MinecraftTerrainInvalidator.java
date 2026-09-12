package dev.blocklens.fabric;

import java.util.Objects;
import net.minecraft.client.Minecraft;

/** Minecraft 26.2 terrain-mesh invalidation boundary. */
public final class MinecraftTerrainInvalidator {
    private MinecraftTerrainInvalidator() {
    }

    /** Marks all retained terrain geometry dirty so config-dependent model output is recompiled. */
    public static void invalidateAll(Minecraft client) {
        Objects.requireNonNull(client, "client");
        client.levelExtractor.allChanged();
    }
}

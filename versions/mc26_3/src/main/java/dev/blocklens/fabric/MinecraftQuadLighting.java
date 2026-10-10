package dev.blocklens.fabric;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.core.Direction;

/** 26.3 replaces the diffuse flag with a shade direction; UP retains vanilla unshaded brightness. */
final class MinecraftQuadLighting {
    private MinecraftQuadLighting() {
    }

    static void disableDirectionalShade(MutableQuadView quad) {
        quad.shadeDirectionOverride(Direction.UP);
    }
}

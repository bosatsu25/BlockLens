package dev.blocklens.fabric;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;

/** Legacy renderer API boundary; product color and emission policies remain shared. */
final class MinecraftQuadLighting {
    private MinecraftQuadLighting() {
    }

    static void disableDirectionalShade(MutableQuadView quad) {
        quad.diffuseShade(false);
    }
}

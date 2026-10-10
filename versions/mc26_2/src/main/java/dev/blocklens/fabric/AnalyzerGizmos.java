package dev.blocklens.fabric;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.Gizmos;

public final class AnalyzerGizmos {
    private AnalyzerGizmos() { }
    public static Gizmos.TemporaryCollection collect(LevelRenderer renderer) {
        return renderer.collectPerFrameRenderThreadGizmos();
    }
}

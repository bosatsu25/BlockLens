package dev.blocklens.gametest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

/** Typed completed-native-frame target for static Shader captures. */
final class ExternalCaptureTarget {
    private ExternalCaptureTarget() { }

    static RenderTarget read(Minecraft client) {
        return client.gameRenderer.mainRenderTarget();
    }
}

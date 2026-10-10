package dev.blocklens.gametest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

/** Readback target for the 26.1.2 Sodium screenshot lifecycle exception. */
final class ExternalCaptureTarget {
    private ExternalCaptureTarget() { }

    static RenderTarget read(Minecraft client) {
        return client.getMainRenderTarget();
    }
}

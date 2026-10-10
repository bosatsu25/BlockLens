package dev.blocklens.gametest;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;

/** Modern clients retain the standard Fabric GameTest screenshot path. */
final class ExternalCaptureTarget {
    private ExternalCaptureTarget() { }

    static RenderTarget read(Minecraft client) {
        throw new AssertionError("Completed native frame capture is limited to 26.1.2 Sodium shader-off");
    }
}

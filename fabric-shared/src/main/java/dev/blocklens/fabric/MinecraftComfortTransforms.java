package dev.blocklens.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.ComfortRenderPolicy;

/** Bounded transforms confined to the requested first-person effect. */
public final class MinecraftComfortTransforms {
    private MinecraftComfortTransforms() { }
    public static boolean beginFire(PoseStack poses) {
        var config = BlockLensRuntime.config();
        if (!config.isEnabled(CapabilityId.LOW_FIRE)) return false;
        int preset = config.comfortOptions().fireSize();
        poses.pushPose();
        poses.translate(0f, ComfortRenderPolicy.fireOffsetY(preset), 0f);
        poses.scale(1f, ComfortRenderPolicy.fireScaleY(preset), 1f);
        return true;
    }
}

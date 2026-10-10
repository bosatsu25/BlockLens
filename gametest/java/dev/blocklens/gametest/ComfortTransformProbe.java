package dev.blocklens.gametest;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;

/** Test-only fixed primitive samples taken inside actual vanilla item/effect submissions. */
public final class ComfortTransformProbe {
    public static volatile boolean active;
    public static volatile float fireY = Float.NaN;
    public static volatile float fireOffsetY = Float.NaN;
    public static volatile float leftScale = Float.NaN;
    public static volatile float rightScale = Float.NaN;
    private ComfortTransformProbe() { }
    public static void reset() {
        fireY = fireOffsetY = leftScale = rightScale = Float.NaN;
        active = true;
    }
    public static void fire(PoseStack poses) {
        if (!active) return;
        var matrix = poses.last().pose();
        fireY = matrix.m11();
        fireOffsetY = matrix.m31();
    }
    public static void held(ItemDisplayContext context, PoseStack poses) {
        if (!active) return;
        var matrix = poses.last().pose();
        float scale = (float) Math.sqrt(matrix.m00() * matrix.m00()
                + matrix.m01() * matrix.m01() + matrix.m02() * matrix.m02());
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) leftScale = scale;
        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) rightScale = scale;
    }
}

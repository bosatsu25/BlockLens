package dev.blocklens.gametest;

import dev.blocklens.core.render.CompactModelTransforms.Transform;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3fc;

/** Test-only observations scoped to real native item submissions and display contexts. */
public final class CompactDisplayProbe {
    public static volatile boolean active, left, right;
    public static volatile Transform observedLeft, observedRight;
    private static volatile Transform expectedLeft, expectedRight;
    private static volatile boolean capture;
    private static ItemDisplayContext submitted;
    private static volatile ItemDisplayContext expectedContext;
    private CompactDisplayProbe() { }
    public static void begin(ItemDisplayContext context) { submitted = context; }
    public static void end() { submitted = null; }
    public static void reset(Transform a, Transform b) { resetFor(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, a, b); }
    public static void resetFor(ItemDisplayContext context, Transform a, Transform b) {
        expectedContext = context; expectedLeft = a; expectedRight = b;
        capture = false; observedLeft = observedRight = null; left = right = false; active = true;
    }
    public static void captureHands() { reset(null, null); capture = true; }
    private static boolean selected() {
        if (expectedContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return submitted == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || submitted == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        }
        if (expectedContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return submitted == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || submitted == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        }
        return submitted == expectedContext;
    }
    public static void record(boolean mirrored, Vector3fc rotation, Vector3fc translation, Vector3fc scale) {
        if (!active || !selected()) return;
        if (capture) {
            if (mirrored && !left) { observedLeft = value(rotation, translation, scale); left = true; }
            if (!mirrored && !right) { observedRight = value(rotation, translation, scale); right = true; }
            return;
        }
        Transform value = mirrored ? expectedLeft : expectedRight;
        if (value != null && close(rotation.x(), value.rx()) && close(rotation.y(), value.ry())
                && close(rotation.z(), value.rz()) && close(translation.x(), value.tx())
                && close(translation.y(), value.ty()) && close(translation.z(), value.tz())
                && close(scale.x(), value.sx()) && close(scale.y(), value.sy()) && close(scale.z(), value.sz())) {
            if (mirrored) left = true; else right = true;
        }
    }
    private static Transform value(Vector3fc r, Vector3fc t, Vector3fc s) {
        return new Transform(r.x(), r.y(), r.z(), t.x(), t.y(), t.z(), s.x(), s.y(), s.z());
    }
    private static boolean close(float a, float b) { return Math.abs(a - b) < .00001f; }
}

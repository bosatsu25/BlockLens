package dev.blocklens.core.render;

/** Pure contract for the inspected optional BBE/Sodium ABI; no external classes are linked here. */
public final class BBEChestCompatibility {
    private static final int BBE_VANILLA_LIGHTING = 2;

    private BBEChestCompatibility() { }

    public static boolean supported(String minecraft, String bbe, String sodium) {
        return "26.3".equals(minecraft) && "1.3.9+mc26.3".equals(bbe)
                && "0.9.2+mc26.3".equals(sodium);
    }

    public static boolean useBrightMaterial(boolean enabled, boolean regularChest, boolean christmas) {
        return enabled && regularChest && !christmas;
    }

    /** Keep unrelated BBE flags while selecting Sodium's emissive-aware lighting path. */
    public static int emissiveFlags(int original) {
        return original & ~BBE_VANILLA_LIGHTING;
    }
}

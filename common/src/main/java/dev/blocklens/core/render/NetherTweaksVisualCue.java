package dev.blocklens.core.render;

import java.util.Map;
import java.util.Objects;

/**
 * Procedural Nether palette used by BlockLens without redistributing source textures.
 *
 * <p>The pinned AMATERAS source uses mostly 16x16 textures with a 14x14 flat interior and a
 * one-pixel contrasting frame. P0 keeps those existing cues and extends the same grammar to the
 * ChiseTweaks-only polished-basalt target.</p>
 */
public record NetherTweaksVisualCue(
        int sideFillArgb,
        int upFillArgb,
        int downFillArgb,
        int frameArgb,
        int upperBandArgb) {

    public static final int NO_BAND = 0;

    private static final int NETHERRACK_FILL = argb(0x72, 0x32, 0x32);
    private static final int NETHERRACK_FRAME = argb(0x50, 0x1B, 0x1B);

    private static final Map<String, NetherTweaksVisualCue> BY_TARGET = Map.ofEntries(
            entry("basalt", cue(0x5C5C5C, 0x5C5C5C, 0x5C5C5C, 0x3A3B48)),
            entry("polished_basalt", cue(0x6A6A6A, 0x777777, 0x777777, 0x454653)),
            entry("blackstone", cue(0x312C36, 0x312C36, 0x312C36, 0x160F10)),
            entry("chiseled_nether_bricks", cue(0x44242A, 0x44242A, 0x44242A, 0x30181C)),
            entry("chiseled_polished_blackstone", cue(0x312C36, 0x312C36, 0x312C36, 0x160F10)),
            entry("cracked_nether_bricks", cue(0x44242A, 0x44242A, 0x44242A, 0x30181C)),
            entry("cracked_polished_blackstone_bricks", cue(0x312C36, 0x312C36, 0x312C36, 0x160F10)),
            entry("crimson_nylium", new NetherTweaksVisualCue(
                    argb(0x72, 0x32, 0x32), argb(0xAC, 0x20, 0x20), NETHERRACK_FILL,
                    NETHERRACK_FRAME, argb(0xAC, 0x20, 0x20))),
            entry("crimson_stem", cue(0x863E5A, 0x863E5A, 0x863E5A, 0xAC2020)),
            entry("crying_obsidian", cue(0x100C1C, 0x100C1C, 0x100C1C, 0x8308E4)),
            entry("gilded_blackstone", cue(0x312C36, 0x312C36, 0x312C36, 0xF8AF2B)),
            entry("glowstone", cue(0xCC8654, 0xCC8654, 0xCC8654, 0x6F4522)),
            entry("gravel", cue(0x817F7F, 0x817F7F, 0x817F7F, 0x645B5B)),
            entry("magma_block", cue(0x501B1B, 0x501B1B, 0x501B1B, 0xE66410)),
            entry("nether_bricks", cue(0x44242A, 0x44242A, 0x44242A, 0x30181C)),
            entry("nether_gold_ore", cue(0x723232, 0x723232, 0x723232, 0xF8AF2B)),
            entry("nether_quartz_ore", cue(0x723232, 0x723232, 0x723232, 0xEAE5DE)),
            entry("nether_wart_block", cue(0x7B0000, 0x7B0000, 0x7B0000, 0x5A0000)),
            entry("netherrack", cue(0x723232, 0x723232, 0x723232, 0x501B1B)),
            entry("obsidian", cue(0x100C1C, 0x100C1C, 0x100C1C, 0x3B2754)),
            entry("polished_blackstone", cue(0x312C36, 0x312C36, 0x312C36, 0x160F10)),
            entry("polished_blackstone_bricks", cue(0x312C36, 0x312C36, 0x312C36, 0x160F10)),
            entry("shroomlight", cue(0xFEAC6D, 0xFEAC6D, 0xFEAC6D, 0xE47205)),
            entry("soul_sand", cue(0x6A5244, 0x6A5244, 0x6A5244, 0x49372C)),
            entry("soul_soil", cue(0x49372C, 0x49372C, 0x49372C, 0x352922)),
            entry("warped_nylium", new NetherTweaksVisualCue(
                    argb(0x72, 0x32, 0x32), argb(0x18, 0x6F, 0x69), NETHERRACK_FILL,
                    NETHERRACK_FRAME, argb(0x18, 0x6F, 0x69))),
            entry("warped_stem", cue(0x398382, 0x398382, 0x398382, 0x452D5C)),
            entry("warped_wart_block", cue(0x167E86, 0x167E86, 0x167E86, 0x16615B)));

    public NetherTweaksVisualCue {
        requireOpaque(sideFillArgb, "sideFillArgb");
        requireOpaque(upFillArgb, "upFillArgb");
        requireOpaque(downFillArgb, "downFillArgb");
        requireOpaque(frameArgb, "frameArgb");
        if (upperBandArgb != NO_BAND) {
            requireOpaque(upperBandArgb, "upperBandArgb");
        }
    }

    public boolean hasUpperBand() {
        return upperBandArgb != NO_BAND;
    }

    public static NetherTweaksVisualCue forTarget(String minecraftPath) {
        return BY_TARGET.get(Objects.requireNonNull(minecraftPath, "minecraftPath"));
    }

    public static int targetCount() {
        return BY_TARGET.size();
    }

    private static Map.Entry<String, NetherTweaksVisualCue> entry(String path, NetherTweaksVisualCue cue) {
        return Map.entry(path, cue);
    }

    private static NetherTweaksVisualCue cue(int side, int up, int down, int frame) {
        return new NetherTweaksVisualCue(argb(side), argb(up), argb(down), argb(frame), NO_BAND);
    }

    private static int argb(int rgb) {
        return 0xFF000000 | rgb;
    }

    private static int argb(int red, int green, int blue) {
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static void requireOpaque(int argb, String name) {
        if ((argb >>> 24) != 0xFF) {
            throw new IllegalArgumentException(name + " must be opaque ARGB: 0x" + Integer.toHexString(argb));
        }
    }
}

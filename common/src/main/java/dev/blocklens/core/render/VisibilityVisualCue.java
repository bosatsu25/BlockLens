package dev.blocklens.core.render;

/** Source-derived procedural cue palette for M4 without redistributing source textures. */
public enum VisibilityVisualCue {
    BLUE_ICE_OUTLINE(0xFF0000FF, Mode.OUTLINE),
    DEAD_CORAL_OUTLINE(0xFFD962A3, Mode.OUTLINE),
    POWDER_SNOW_OUTLINE(0xFFA5F6FF, Mode.OUTLINE),
    SCULK_CATALYST_IDLE(0xFF29DFEB, Mode.OUTLINE),
    SCULK_CATALYST_BLOOM(0xFF16DEEC, Mode.OUTLINE),
    STRING_UNPOWERED(0xFF00FF00, Mode.FINE_LINE),
    STRING_POWERED(0xFFFF0000, Mode.FINE_LINE);

    public enum Mode {
        OUTLINE,
        FINE_LINE
    }

    private final int accentArgb;
    private final Mode mode;

    VisibilityVisualCue(int accentArgb, Mode mode) {
        this.accentArgb = accentArgb;
        this.mode = mode;
    }

    public int accentArgb() {
        return accentArgb;
    }

    public Mode mode() {
        return mode;
    }
}

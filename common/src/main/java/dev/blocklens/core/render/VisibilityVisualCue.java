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

    private final DecorationQuadInstruction instruction;
    private final Mode mode;

    VisibilityVisualCue(int accentArgb, Mode mode) {
        this.instruction = DecorationQuadInstruction.emissiveTint(accentArgb);
        this.mode = mode;
    }

    public int accentArgb() {
        return instruction.multiplyArgb();
    }

    public Mode mode() {
        return mode;
    }

    /**
     * Returns the single immutable instruction owned by this cue.
     *
     * <p>M4 rendering is quad-heavy, so retaining one bounded instruction per enum value avoids
     * constructing equivalent records for every emitted quad while keeping the palette and visual
     * semantics unchanged.</p>
     */
    public DecorationQuadInstruction instruction() {
        return instruction;
    }
}

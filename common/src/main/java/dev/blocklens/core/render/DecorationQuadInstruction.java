package dev.blocklens.core.render;

/**
 * Pure quad instruction applied by the Minecraft-version render adapter.
 *
 * <p>The historical class name is retained to avoid churn in the verified M3 pipeline, but the
 * instruction is now shared by M3-M7 renderer families.</p>
 */
public record DecorationQuadInstruction(int multiplyArgb, boolean forceSolid, boolean emissive) {
    public static final int IDENTITY_COLOR = 0xFFFFFFFF;
    public static final DecorationQuadInstruction IDENTITY =
            new DecorationQuadInstruction(IDENTITY_COLOR, false, false);
    public static final DecorationQuadInstruction OPAQUE =
            new DecorationQuadInstruction(IDENTITY_COLOR, true, false);

    /** Backward-compatible M3 constructor: pre-M4 instructions are non-emissive. */
    public DecorationQuadInstruction(int multiplyArgb, boolean forceSolid) {
        this(multiplyArgb, forceSolid, false);
    }

    public DecorationQuadInstruction {
        if ((multiplyArgb >>> 24) != 0xFF) {
            throw new IllegalArgumentException("quad multiplier must be fully opaque ARGB: 0x"
                    + Integer.toHexString(multiplyArgb));
        }
    }

    public boolean changesColor() {
        return multiplyArgb != IDENTITY_COLOR;
    }

    public static DecorationQuadInstruction tint(int multiplyArgb) {
        return new DecorationQuadInstruction(multiplyArgb, false, false);
    }

    public static DecorationQuadInstruction emissiveTint(int multiplyArgb) {
        return new DecorationQuadInstruction(multiplyArgb, false, true);
    }
}

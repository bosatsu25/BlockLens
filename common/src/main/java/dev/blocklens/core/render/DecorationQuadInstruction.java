package dev.blocklens.core.render;

/** Pure render instruction applied by the Minecraft-version quad adapter. */
public record DecorationQuadInstruction(int multiplyArgb, boolean forceSolid) {
    public static final int IDENTITY_COLOR = 0xFFFFFFFF;
    public static final DecorationQuadInstruction IDENTITY =
            new DecorationQuadInstruction(IDENTITY_COLOR, false);
    public static final DecorationQuadInstruction OPAQUE =
            new DecorationQuadInstruction(IDENTITY_COLOR, true);

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
        return new DecorationQuadInstruction(multiplyArgb, false);
    }
}

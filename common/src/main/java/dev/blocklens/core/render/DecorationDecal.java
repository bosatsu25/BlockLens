package dev.blocklens.core.render;

/**
 * One face decal: which glyph, how many clockwise quarter turns from glyph up, and its tint.
 *
 * <p>{@link #NONE} means the face carries no decal. Quarter turns are interpreted in the
 * face frame defined by {@link DecalProjection}, never in model UV space, so the result does
 * not depend on the active resource pack's UV rotations.</p>
 */
public record DecorationDecal(DecorationGlyph glyph, int quarterTurns, int argb) {
    public static final DecorationDecal NONE = new DecorationDecal(null, 0, 0);

    public DecorationDecal {
        if (glyph == null) {
            if (quarterTurns != 0 || argb != 0) {
                throw new IllegalArgumentException("an absent decal carries no rotation or color");
            }
        } else {
            if (quarterTurns < 0 || quarterTurns > 3) {
                throw new IllegalArgumentException("quarter turns must be 0..3: " + quarterTurns);
            }
            if ((argb >>> 24) != 0xFF) {
                throw new IllegalArgumentException("decal tint must be fully opaque ARGB: 0x"
                        + Integer.toHexString(argb));
            }
        }
    }

    public static DecorationDecal of(DecorationGlyph glyph, int quarterTurns, int argb) {
        if (glyph == null) {
            throw new NullPointerException("glyph");
        }
        return new DecorationDecal(glyph, quarterTurns, argb);
    }

    public boolean present() {
        return glyph != null;
    }
}

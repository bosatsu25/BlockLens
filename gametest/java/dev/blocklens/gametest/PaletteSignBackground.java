package dev.blocklens.gametest;
/** Test-only absent-board control; scoped to a blank synthetic sign editor. */
public final class PaletteSignBackground {
    private static volatile boolean hidden;
    private PaletteSignBackground() { }
    public static boolean hidden() { return hidden; }
    static void hide(boolean value) { hidden=value; }
}

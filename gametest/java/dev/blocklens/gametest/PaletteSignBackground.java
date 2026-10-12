package dev.blocklens.gametest;
/** Test-only absent-board control; scoped to a blank synthetic sign editor. */
public final class PaletteSignBackground {
    private static volatile boolean hidden;
    private static volatile boolean testing;
    private PaletteSignBackground() { }
    public static boolean hidden() { return hidden; }
    public static boolean testing() { return testing; }
    static void activate(boolean value) { testing=value; }
    static void hide(boolean value) { hidden=value; }
}

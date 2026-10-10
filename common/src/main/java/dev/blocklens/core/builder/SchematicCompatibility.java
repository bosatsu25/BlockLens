package dev.blocklens.core.builder;

/** Exact typed integration contracts; newer or missing optional mods require separate evidence. */
public final class SchematicCompatibility {
    private SchematicCompatibility() { }
    public static boolean supported(String minecraft, String litematica, String malilib) {
        if (minecraft == null) return false;
        return switch (minecraft) {
            case "26.1.2" -> "0.27.14".equals(litematica) && "0.28.12".equals(malilib);
            case "26.2" -> "0.28.8".equals(litematica) && "0.29.6".equals(malilib);
            case "26.3" -> "0.29.1".equals(litematica) && "0.30.2".equals(malilib);
            default -> false;
        };
    }
}

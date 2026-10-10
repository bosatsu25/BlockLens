package dev.blocklens.core.render;

/** Exact optional terrain adapter boundary; other builds need their own native evidence. */
public final class SodiumTerrainCompatibility {
    private SodiumTerrainCompatibility() { }

    public static boolean supported(String minecraft, String sodium) {
        if (minecraft == null) return false;
        return switch (minecraft) {
            case "26.1.2" -> "0.9.2+mc26.1.2".equals(sodium);
            case "26.2" -> "0.9.2+mc26.2".equals(sodium);
            case "26.3" -> "0.9.2+mc26.3".equals(sodium);
            default -> false;
        };
    }
}

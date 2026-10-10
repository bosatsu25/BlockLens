package dev.blocklens.core;

/** Immutable, bounded first-person visual options; no Minecraft dependencies. */
public record ComfortOptions(int fireSize, int blocksPercent, int itemsPercent, int toolsPercent) {
    public ComfortOptions {
        if (fireSize < 0 || fireSize > 2) {
            throw new IllegalArgumentException("fireSize must be 0..2");
        }
        requirePercent(blocksPercent);
        requirePercent(itemsPercent);
        requirePercent(toolsPercent);
    }

    public static ComfortOptions defaults() {
        return new ComfortOptions(1, 70, 60, 75);
    }

    private static void requirePercent(int value) {
        if (value < 40 || value > 100) {
            throw new IllegalArgumentException("handheld percent must be 40..100");
        }
    }
}

package dev.blocklens.core;

/** Immutable, bounded first-person visual options; no Minecraft dependencies. */
public record ComfortOptions(int fireSize, int blocksPercent, int itemsPercent, int toolsPercent) {
    public static final int MIN_FIRE_SIZE = 0;
    public static final int MAX_FIRE_SIZE = 2;
    public static final int MIN_PERCENT = 40;
    public static final int MAX_PERCENT = 100;

    public ComfortOptions {
        if (fireSize < MIN_FIRE_SIZE || fireSize > MAX_FIRE_SIZE) {
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
        if (value < MIN_PERCENT || value > MAX_PERCENT) {
            throw new IllegalArgumentException("handheld percent must be 40..100");
        }
    }
}

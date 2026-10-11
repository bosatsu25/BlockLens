package dev.blocklens.testing;

import java.awt.image.BufferedImage;

/** Interior wood regions, excluding animated sky, floor shadows and silhouette edges. */
public final class PaleOakFrameMetrics {
    private static final int[][] WOOD = {{243, 205, 290, 239}, {316, 205, 360, 239}};
    private PaleOakFrameMetrics() { }
    public static boolean visibleChange(BufferedImage before, BufferedImage after) {
        for (var area : WOOD) if (warmth(before, area) - warmth(after, area) < 2 || !structure(before, after, area)) return false;
        return true;
    }
    public static boolean stable(BufferedImage before, BufferedImage after) {
        for (var area : WOOD) if (Math.abs(warmth(before, area) - warmth(after, area)) >= .5 || !structure(before, after, area)) return false;
        return true;
    }

    // Three-pixel cells suppress temporal antialiasing noise while retaining projected wood grain.
    private static boolean structure(BufferedImage before, BufferedImage after, int[] area) {
        double sumA = 0, sumB = 0, squareA = 0, squareB = 0, product = 0;
        int count = 0;
        for (int y = area[1]; y + 2 < area[3]; y += 3) {
            for (int x = area[0]; x + 2 < area[2]; x += 3) {
                double a = sample(before, x, y), b = sample(after, x, y);
                sumA += a; sumB += b; squareA += a * a; squareB += b * b; product += a * b;
                count++;
            }
        }
        double varianceA = squareA - sumA * sumA / count;
        double varianceB = squareB - sumB * sumB / count;
        if (varianceA < 64 * count || varianceB < 64 * count
                || varianceB < varianceA * .5 || varianceB > varianceA * 2) return false;
        double correlation = (product - sumA * sumB / count) / Math.sqrt(varianceA * varianceB);
        return correlation >= .9;
    }
    private static double sample(BufferedImage image, int x, int y) {
        int sum = 0;
        for (int dy = 0; dy < 3; dy++) for (int dx = 0; dx < 3; dx++) {
            int pixel = image.getRGB(x + dx, y + dy);
            sum += (pixel >>> 16 & 255) + (pixel >>> 8 & 255) + (pixel & 255);
        }
        return sum / 9.0;
    }

    private static double warmth(BufferedImage image, int[] area) {
        if (image == null || image.getWidth() != 640 || image.getHeight() != 360)
            throw new IllegalArgumentException("Expected the bounded native palette framebuffer");
        long sum = 0;
        for (int y = area[1]; y < area[3]; y++) for (int x = area[0]; x < area[2]; x++) {
            int pixel = image.getRGB(x, y);
            sum += (pixel >>> 16 & 255) - (pixel & 255);
        }
        return (double) sum / ((area[2] - area[0]) * (area[3] - area[1]));
    }
}

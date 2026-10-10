package dev.blocklens.testing;

/** Static external M5 background at the existing fixed camera and unchanged image-space ROI. */
public record M5BackgroundFloor(int minX, int maxX, int minZ, int maxZ) {
    public static final int BLOCK_Y = -60;
    // Absolute integer X/Z tokens in the existing tp command are centered by Minecraft.
    public static final double CAMERA_X = 0.5;
    public static final double CAMERA_Z = 13.5;
    public static final M5BackgroundFloor EXTERNAL = new M5BackgroundFloor(-32, 32, -24, 9);

    public String fillCommand() {
        return "fill " + minX + " " + BLOCK_Y + " " + minZ + " " + maxX + " " + BLOCK_Y + " " + maxZ
                + " minecraft:smooth_quartz";
    }

    public boolean contains(Cell cell) {
        return cell.x >= minX && cell.x <= maxX && cell.z >= minZ && cell.z <= maxZ;
    }

    /** Intersect ROI corner rays at the unchanged centered teleport camera, yaw 180, pitch 18. */
    public static Cell[] viewportCorners(double eyeY, double verticalFov, int width, int height,
            int minPixelX, int minPixelY, int maxPixelX, int maxPixelY) {
        Cell[] corners = new Cell[4];
        double tangent = Math.tan(Math.toRadians(verticalFov) / 2.0);
        double sine = Math.sin(Math.toRadians(18));
        double cosine = Math.cos(Math.toRadians(18));
        int index = 0;
        for (int y : new int[] {minPixelY, maxPixelY - 1}) {
            for (int x : new int[] {minPixelX, maxPixelX - 1}) {
                double u = (2.0 * (x + 0.5) / width - 1.0) * width / height * tangent;
                double v = (1.0 - 2.0 * (y + 0.5) / height) * tangent;
                double dy = -sine + v * cosine;
                double distance = (BLOCK_Y + 1 - eyeY) / dy;
                if (dy >= 0 || !Double.isFinite(distance) || distance <= 0) {
                    throw new IllegalArgumentException("The M5 ROI does not intersect the background floor");
                }
                corners[index++] = new Cell((int) Math.floor(CAMERA_X + distance * u),
                        (int) Math.floor(CAMERA_Z + distance * (-cosine - v * sine)));
            }
        }
        return corners;
    }

    public record Cell(int x, int z) { }
}

package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Rendered proof that decoration decals carry the source pack's information, read from the face
 * a player actually sees: a top-down camera looks at one block at a time, so only its UP face is
 * visible. Arrow direction, rail orientation, axis color and slab-face semantics are decided from
 * the ON-vs-OFF pixel difference, which isolates the decal from the active texture.
 */
final class DecorationDecalOracle {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int HALF_WINDOW = 120;
    private static final int PIXEL_DELTA = 48;
    private static final int SETTLE_TICKS = 30;
    private static final int MIN_DECAL_PIXELS = 120;
    private static final int MAX_ABSENT_PIXELS = 20;
    // Away from the other oracle scenes so their blocks and thresholds stay untouched.
    private static final String TARGET = "40 -59 40";

    private DecorationDecalOracle() {
    }

    static void verify(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        BlockLensConfig original = BlockLensRuntime.config();
        Path outputDir = FabricLoader.getInstance().getGameDir().resolve("decoration-decals");
        StringBuilder manifest = new StringBuilder("minecraft=" + BlockLensRuntime.minecraftVersion() + "\n");
        try {
            Files.createDirectories(outputDir);
            prepareWorld(context, singleplayer);
            BlockLensConfig off = withoutDecorations(original);

            // Pack parity: the arrow points in the placer's look direction, opposite to facing.
            String[][] arrows = {{"north", "down"}, {"east", "left"}, {"south", "up"}, {"west", "right"}};
            for (String[] arrow : arrows) {
                Mask mask = decal(context, singleplayer, outputDir, off, CapabilityId.GLAZED_TERRACOTTA,
                        "light_gray_glazed_terracotta[facing=" + arrow[0] + "]", "arrow-" + arrow[0]);
                require(mask.count() >= MIN_DECAL_PIXELS, "arrow decal missing for facing=" + arrow[0]
                        + " pixels=" + mask.count());
                String pointing = mask.arrowDirection();
                manifest.append("arrow.").append(arrow[0]).append('=').append(pointing)
                        .append(" ratio=").append(mask.bestRatio()).append('\n');
                require(pointing.equals(arrow[1]), "glazed terracotta facing=" + arrow[0]
                        + " must point " + arrow[1] + " on screen but points " + pointing);
            }

            // Axis: Y is an end face (frame); X and Z show rails parallel to the axis.
            String[] axes = {"x", "y", "z"};
            for (String axis : axes) {
                Mask mask = decal(context, singleplayer, outputDir, off, CapabilityId.LOG,
                        "oak_log[axis=" + axis + "]", "axis-" + axis);
                require(mask.count() >= MIN_DECAL_PIXELS, "axis decal missing for axis=" + axis);
                int rows = mask.occupiedRows();
                int columns = mask.occupiedColumns();
                manifest.append("axis.").append(axis).append(" rows=").append(rows).append(" columns=")
                        .append(columns).append(" rgb=").append(mask.meanRgb()).append('\n');
                switch (axis) {
                    case "x" -> require(columns >= 3 * rows, "X rails must run left-right: rows=" + rows
                            + " columns=" + columns);
                    case "z" -> require(rows >= 3 * columns, "Z rails must run up-down: rows=" + rows
                            + " columns=" + columns);
                    default -> require(rows * 2 > columns && columns * 2 > rows,
                            "Y end face must show a frame: rows=" + rows + " columns=" + columns);
                }
                require(mask.dominantChannel().equals(axis.equals("x") ? "red" : axis.equals("y") ? "green" : "blue"),
                        "axis " + axis + " decal has the wrong legend color: " + mask.meanRgb());
            }

            // Slabs mark the face that meets the block boundary, as the source pack does.
            Mask bottom = decal(context, singleplayer, outputDir, off, CapabilityId.SLABS,
                    "smooth_stone_slab[type=bottom]", "slab-bottom");
            Mask top = decal(context, singleplayer, outputDir, off, CapabilityId.SLABS,
                    "smooth_stone_slab[type=top]", "slab-top");
            Mask doubled = decal(context, singleplayer, outputDir, off, CapabilityId.SLABS,
                    "smooth_stone_slab[type=double]", "slab-double");
            manifest.append("slab.bottom=").append(bottom.count()).append("\nslab.top=").append(top.count())
                    .append("\nslab.double=").append(doubled.count()).append('\n');
            require(bottom.count() <= MAX_ABSENT_PIXELS, "bottom slab must not mark its upper face");
            require(top.count() >= MIN_DECAL_PIXELS, "top slab must mark its upper face");
            require(doubled.count() * 10 >= top.count() * 13, "double slab must show the double frame");
            Files.writeString(outputDir.resolve("decoration-decals-manifest.txt"), manifest.toString(),
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new AssertionError("decoration decal evidence failed", exception);
        } finally {
            singleplayer.getServer().runCommand("setblock " + TARGET + " minecraft:air");
            install(context, original);
        }
    }

    private static void prepareWorld(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        var server = singleplayer.getServer();
        server.runCommand("gamerule minecraft:advance_time false");
        server.runCommand("gamerule minecraft:advance_weather false");
        server.runCommand("time set noon");
        server.runCommand("weather clear");
        server.runCommand("gamemode spectator @a");
        // Straight down while facing north: screen up is north and screen right is east.
        server.runCommand("tp @a 40.5 -57 40.5 180 90");
        context.waitTicks(SETTLE_TICKS);
        server.runCommand("fill 37 -59 37 43 -52 43 minecraft:air");
        server.runCommand("fill 37 -60 37 43 -60 43 minecraft:smooth_quartz");
    }

    private static Mask decal(ClientGameTestContext context, TestSingleplayerContext singleplayer, Path outputDir,
            BlockLensConfig off, CapabilityId capability, String block, String name) throws IOException {
        singleplayer.getServer().runCommand("setblock " + TARGET + " minecraft:" + block);
        install(context, off);
        BufferedImage offImage = capture(context, outputDir, name + "-off");
        install(context, off.withEnabled(capability, true));
        BufferedImage onImage = capture(context, outputDir, name + "-on");
        return Mask.of(offImage, onImage);
    }

    private static BlockLensConfig withoutDecorations(BlockLensConfig base) {
        BlockLensConfig result = base;
        for (CapabilityId capability : CapabilityId.values()) {
            result = result.withEnabled(capability, false);
        }
        return result;
    }

    private static void install(ClientGameTestContext context, BlockLensConfig config) {
        context.runOnClient(client -> BlockLensRuntime.installConfig(config));
        context.runOnClient(MinecraftTerrainInvalidator::invalidateAll);
        context.waitTicks(SETTLE_TICKS);
    }

    private static BufferedImage capture(ClientGameTestContext context, Path outputDir, String name)
            throws IOException {
        Path path = context.takeScreenshot(TestScreenshotOptions.of("decal-" + name)
                .withDestinationDir(outputDir).withSize(WIDTH, HEIGHT).disableCounterPrefix());
        BufferedImage image = ImageIO.read(path.toFile());
        require(image != null && image.getWidth() == WIDTH && image.getHeight() == HEIGHT,
                "invalid decal capture: " + path);
        return image;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /** Pixels inside the centered window whose color changed between OFF and ON. */
    private record Mask(boolean[] changed, long red, long green, long blue, int count) {
        static Mask of(BufferedImage off, BufferedImage on) {
            boolean[] changed = new boolean[4 * HALF_WINDOW * HALF_WINDOW];
            long red = 0, green = 0, blue = 0;
            int count = 0;
            for (int y = 0; y < 2 * HALF_WINDOW; y++) {
                for (int x = 0; x < 2 * HALF_WINDOW; x++) {
                    int a = off.getRGB(WIDTH / 2 - HALF_WINDOW + x, HEIGHT / 2 - HALF_WINDOW + y);
                    int b = on.getRGB(WIDTH / 2 - HALF_WINDOW + x, HEIGHT / 2 - HALF_WINDOW + y);
                    int delta = Math.abs(((a >>> 16) & 255) - ((b >>> 16) & 255))
                            + Math.abs(((a >>> 8) & 255) - ((b >>> 8) & 255)) + Math.abs((a & 255) - (b & 255));
                    if (delta >= PIXEL_DELTA) {
                        changed[y * 2 * HALF_WINDOW + x] = true;
                        red += (b >>> 16) & 255;
                        green += (b >>> 8) & 255;
                        blue += b & 255;
                        count++;
                    }
                }
            }
            return new Mask(changed, red, green, blue, count);
        }

        int occupiedRows() {
            int rows = 0;
            for (int y = 0; y < 2 * HALF_WINDOW; y++) {
                for (int x = 0; x < 2 * HALF_WINDOW; x++) {
                    if (changed[y * 2 * HALF_WINDOW + x]) {
                        rows++;
                        break;
                    }
                }
            }
            return rows;
        }

        int occupiedColumns() {
            int columns = 0;
            for (int x = 0; x < 2 * HALF_WINDOW; x++) {
                for (int y = 0; y < 2 * HALF_WINDOW; y++) {
                    if (changed[y * 2 * HALF_WINDOW + x]) {
                        columns++;
                        break;
                    }
                }
            }
            return columns;
        }

        String meanRgb() {
            return count == 0 ? "none" : (red / count) + "," + (green / count) + "," + (blue / count);
        }

        String dominantChannel() {
            if (red >= green && red >= blue) return "red";
            return green >= blue ? "green" : "blue";
        }

        /**
         * The arrow head is wider than its shaft, so the half of the glyph on the tip side has the
         * larger spread across the arrow axis. Returns the screen direction with the largest
         * tip-to-tail spread ratio.
         */
        String arrowDirection() {
            String best = "none";
            double bestRatio = 0;
            for (String direction : new String[] {"up", "right", "down", "left"}) {
                double ratio = spreadRatio(direction);
                if (ratio > bestRatio) {
                    bestRatio = ratio;
                    best = direction;
                }
            }
            return bestRatio >= 1.5 ? best : "ambiguous";
        }

        double bestRatio() {
            double best = 0;
            for (String direction : new String[] {"up", "right", "down", "left"}) {
                best = Math.max(best, spreadRatio(direction));
            }
            return Math.round(best * 100.0) / 100.0;
        }

        private double spreadRatio(String direction) {
            int minX = Integer.MAX_VALUE, maxX = -1, minY = Integer.MAX_VALUE, maxY = -1;
            int size = 2 * HALF_WINDOW;
            for (int i = 0; i < changed.length; i++) {
                if (!changed[i]) continue;
                minX = Math.min(minX, i % size);
                maxX = Math.max(maxX, i % size);
                minY = Math.min(minY, i / size);
                maxY = Math.max(maxY, i / size);
            }
            if (maxX < 0) return 0;
            double centerX = (minX + maxX) / 2.0, centerY = (minY + maxY) / 2.0;
            double tipSpread = 0, tailSpread = 0;
            int tip = 0, tail = 0;
            for (int i = 0; i < changed.length; i++) {
                if (!changed[i]) continue;
                double dx = i % size - centerX, dy = i / size - centerY;
                double along = switch (direction) {
                    case "up" -> -dy;
                    case "down" -> dy;
                    case "right" -> dx;
                    default -> -dx;
                };
                double across = direction.equals("up") || direction.equals("down") ? Math.abs(dx) : Math.abs(dy);
                if (along > 0) {
                    tipSpread += across;
                    tip++;
                } else if (along < 0) {
                    tailSpread += across;
                    tail++;
                }
            }
            if (tip == 0 || tail == 0 || tailSpread == 0) return 0;
            return (tipSpread / tip) / (tailSpread / tail);
        }
    }
}

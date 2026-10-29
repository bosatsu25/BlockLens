package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensConfig;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.DecorationDecals;
import dev.blocklens.fabric.MinecraftTerrainInvalidator;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

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
    private static final String GLYPH_STRIP = "/assets/blocklens/textures/block/cue/decal_glyphs.png";

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
            boolean[] arrowGlyph = arrowGlyph();

            // Pack parity: the arrow points in the placer's look direction, opposite to facing.
            String[][] arrows = {{"north", "down"}, {"east", "left"}, {"south", "up"}, {"west", "right"}};
            for (String[] arrow : arrows) {
                Mask mask = decal(context, singleplayer, outputDir, off, CapabilityId.GLAZED_TERRACOTTA,
                        "light_gray_glazed_terracotta[facing=" + arrow[0] + "]", "arrow-" + arrow[0]);
                require(mask.count() >= MIN_DECAL_PIXELS, "arrow decal missing for facing=" + arrow[0]
                        + " pixels=" + mask.count());
                String pointing = mask.arrowDirection(arrowGlyph);
                manifest.append("arrow.").append(arrow[0]).append('=').append(pointing).append(' ')
                        .append(mask.describe(arrowGlyph).lines().findFirst().orElse("")).append('\n');
                if (!pointing.equals(arrow[1])) {
                    System.out.println("BLOCKLENS_DECAL_ARROW facing=" + arrow[0] + " expected=" + arrow[1]
                            + " " + mask.describe(arrowGlyph));
                }
                require(pointing.equals(arrow[1]), "glazed terracotta facing=" + arrow[0]
                        + " must point " + arrow[1] + " on screen but points " + pointing);
            }

            // Anvil: the source pack swaps only the top texture, whose arrow points toward -u, so the
            // baked vanilla top quad's UVs decide where the pack's arrow points. The decal must agree.
            for (String facing : new String[] {"north", "east", "south", "west"}) {
                install(context, off);
                String pack = packAnvilArrow(context, facing);
                Mask mask = decal(context, singleplayer, outputDir, off, CapabilityId.ANVIL,
                        "anvil[facing=" + facing + "]", "anvil-" + facing);
                require(mask.count() >= MIN_DECAL_PIXELS, "anvil arrow missing for facing=" + facing
                        + " pixels=" + mask.count());
                String pointing = mask.arrowDirection(arrowGlyph);
                manifest.append("anvil.").append(facing).append(" pack=").append(pack).append(" decal=")
                        .append(pointing).append(' ')
                        .append(mask.describe(arrowGlyph).lines().findFirst().orElse("")).append('\n');
                if (!pointing.equals(pack)) {
                    System.out.println("BLOCKLENS_DECAL_ANVIL facing=" + facing + " pack=" + pack + " "
                            + mask.describe(arrowGlyph));
                }
                require(pointing.equals(pack), "anvil facing=" + facing + " decal points " + pointing
                        + " on screen but the source pack's arrow points " + pack);
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

    /**
     * Screen direction (top-down, north up) of the source pack's anvil arrow for {@code facing}. The
     * pack keeps template_anvil and draws its arrow toward -u of the top texture, so the arrow points
     * where u decreases across the baked vanilla top quad. Read with every decoration off, so the
     * wrapper emits the unmodified vanilla model.
     */
    private static String packAnvilArrow(ClientGameTestContext context, String facing) {
        return context.computeOnClient(client -> {
            Direction direction = switch (facing) {
                case "north" -> Direction.NORTH;
                case "east" -> Direction.EAST;
                case "south" -> Direction.SOUTH;
                default -> Direction.WEST;
            };
            BlockState state = Blocks.ANVIL.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, direction);
            BlockStateModel model = client.getModelManager().getBlockStateModelSet().get(state);
            double[] gradient = new double[2];
            int[] tops = new int[1];
            QuadEmitter emitter = Renderer.get().mutableMesh().emitter();
            emitter.pushTransform(quad -> {
                boolean top = quad.nominalFace() == Direction.UP;
                for (int vertex = 0; vertex < 4 && top; vertex++) top = quad.y(vertex) > 1 - 1.0E-4F;
                if (top) {
                    tops[0]++;
                    double x = 0, z = 0, u = 0;
                    for (int vertex = 0; vertex < 4; vertex++) {
                        x += quad.x(vertex) / 4;
                        z += quad.z(vertex) / 4;
                        u += quad.u(vertex) / 4;
                    }
                    for (int vertex = 0; vertex < 4; vertex++) {
                        gradient[0] += (quad.x(vertex) - x) * (quad.u(vertex) - u);
                        gradient[1] += (quad.z(vertex) - z) * (quad.u(vertex) - u);
                    }
                }
                return false;
            });
            model.emitQuads(emitter, BlockAndTintGetter.EMPTY, BlockPos.ZERO, state, RandomSource.create(0),
                    ignored -> false);
            require(tops[0] == 1, "vanilla anvil " + facing + " must have one top quad, found " + tops[0]);
            require(Math.abs(gradient[0]) != Math.abs(gradient[1]), "vanilla anvil top UVs have no single axis");
            // -u is toward -x (screen left) when u grows with x, toward -z (screen up) when u grows with z.
            return Math.abs(gradient[0]) > Math.abs(gradient[1]) ? gradient[0] > 0 ? "left" : "right"
                    : gradient[1] > 0 ? "up" : "down";
        });
    }

    /** The shipped arrow glyph (tip toward glyph up) as an occupancy mask: any non-clear texel. */
    private static boolean[] arrowGlyph() throws IOException {
        try (InputStream stream = DecorationDecalOracle.class.getResourceAsStream(GLYPH_STRIP)) {
            require(stream != null, "decal glyph strip missing: " + GLYPH_STRIP);
            BufferedImage strip = ImageIO.read(stream);
            int size = DecorationDecals.GLYPH_PIXELS;
            boolean[] glyph = new boolean[size * size];
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    glyph[y * size + x] = (strip.getRGB(x, DecorationDecals.ARROW * size + y) >>> 24) != 0;
                }
            }
            return glyph;
        }
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
        private static final String[] DIRECTIONS = {"up", "right", "down", "left"};
        private static final int GRID = DecorationDecals.GLYPH_PIXELS;
        private static final double MIN_ARROW_MATCH = 0.6;
        private static final double MIN_ARROW_MARGIN = 0.15;

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
         * Matches the changed pixels against the arrow glyph turned to each screen direction. Both
         * are scaled to the same grid over their bounding boxes, and the direction whose template
         * overlaps best wins. A silhouette match keeps the contour, mip filtering and the texture
         * underneath from deciding the result, which a single width or spread statistic cannot.
         */
        String arrowDirection(boolean[] glyph) {
            double[] scores = arrowScores(glyph);
            int best = 0, second = -1;
            for (int i = 1; i < scores.length; i++) {
                if (scores[i] > scores[best]) {
                    second = best;
                    best = i;
                } else if (second < 0 || scores[i] > scores[second]) {
                    second = i;
                }
            }
            return scores[best] >= MIN_ARROW_MATCH && scores[best] - scores[second] >= MIN_ARROW_MARGIN
                    ? DIRECTIONS[best] : "ambiguous";
        }

        String describe(boolean[] glyph) {
            double[] scores = arrowScores(glyph);
            StringBuilder result = new StringBuilder("count=").append(count);
            for (int i = 0; i < DIRECTIONS.length; i++) {
                result.append(' ').append(DIRECTIONS[i]).append('=').append(Math.round(scores[i] * 100.0) / 100.0);
            }
            int size = 2 * HALF_WINDOW;
            for (int cy = 0; cy < size; cy += 8) {
                result.append("\n  ");
                for (int cx = 0; cx < size; cx += 8) {
                    int hits = 0;
                    for (int y = cy; y < cy + 8; y++) {
                        for (int x = cx; x < cx + 8; x++) {
                            if (changed[y * size + x]) hits++;
                        }
                    }
                    result.append(hits == 0 ? '.' : hits < 32 ? '+' : '#');
                }
            }
            return result.toString();
        }

        /** Intersection-over-union per screen direction, in {@link #DIRECTIONS} order. */
        double[] arrowScores(boolean[] glyph) {
            boolean[] observed = normalized(largestComponent(changed, 2 * HALF_WINDOW), 2 * HALF_WINDOW);
            double[] scores = new double[DIRECTIONS.length];
            boolean[] template = glyph;
            for (int i = 0; i < DIRECTIONS.length; i++) {
                boolean[] expected = normalized(template, GRID);
                int both = 0, either = 0;
                for (int j = 0; j < expected.length; j++) {
                    if (expected[j] && observed[j]) both++;
                    if (expected[j] || observed[j]) either++;
                }
                scores[i] = either == 0 ? 0 : (double) both / either;
                template = rotateClockwise(template);
            }
            return scores;
        }

        /** Resamples the bounding box of {@code mask} (size x size) onto a GRID x GRID majority grid. */
        private static boolean[] normalized(boolean[] mask, int size) {
            int minX = size, maxX = -1, minY = size, maxY = -1;
            for (int i = 0; i < mask.length; i++) {
                if (!mask[i]) continue;
                minX = Math.min(minX, i % size);
                maxX = Math.max(maxX, i % size);
                minY = Math.min(minY, i / size);
                maxY = Math.max(maxY, i / size);
            }
            boolean[] grid = new boolean[GRID * GRID];
            if (maxX < 0) return grid;
            double width = maxX - minX + 1, height = maxY - minY + 1;
            for (int gy = 0; gy < GRID; gy++) {
                int y0 = minY + (int) (gy * height / GRID), y1 = Math.max(y0 + 1, minY + (int) ((gy + 1) * height / GRID));
                for (int gx = 0; gx < GRID; gx++) {
                    int x0 = minX + (int) (gx * width / GRID), x1 = Math.max(x0 + 1, minX + (int) ((gx + 1) * width / GRID));
                    int hits = 0, cells = 0;
                    for (int y = y0; y < y1; y++) {
                        for (int x = x0; x < x1; x++) {
                            cells++;
                            if (mask[y * size + x]) hits++;
                        }
                    }
                    grid[gy * GRID + gx] = hits * 2 >= cells;
                }
            }
            return grid;
        }

        /**
         * The 8-connected region with the most pixels. A stray changed pixel elsewhere in the window
         * (relighting, a neighbor's edge) would otherwise stretch the bounding box the match uses.
         */
        private static boolean[] largestComponent(boolean[] mask, int size) {
            int[] label = new int[mask.length];
            int[] queue = new int[mask.length];
            int bestLabel = 0, bestSize = 0, next = 0;
            for (int start = 0; start < mask.length; start++) {
                if (!mask[start] || label[start] != 0) continue;
                next++;
                int head = 0, tail = 0;
                queue[tail++] = start;
                label[start] = next;
                while (head < tail) {
                    int i = queue[head++];
                    int x = i % size, y = i / size;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = x + dx, ny = y + dy;
                            if (nx < 0 || ny < 0 || nx >= size || ny >= size) continue;
                            int j = ny * size + nx;
                            if (mask[j] && label[j] == 0) {
                                label[j] = next;
                                queue[tail++] = j;
                            }
                        }
                    }
                }
                if (tail > bestSize) {
                    bestSize = tail;
                    bestLabel = next;
                }
            }
            boolean[] result = new boolean[mask.length];
            for (int i = 0; i < mask.length; i++) {
                result[i] = bestLabel != 0 && label[i] == bestLabel;
            }
            return result;
        }

        private static boolean[] rotateClockwise(boolean[] square) {
            boolean[] result = new boolean[square.length];
            for (int y = 0; y < GRID; y++) {
                for (int x = 0; x < GRID; x++) {
                    result[x * GRID + (GRID - 1 - y)] = square[y * GRID + x];
                }
            }
            return result;
        }
    }
}

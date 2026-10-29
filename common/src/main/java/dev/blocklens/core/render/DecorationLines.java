package dev.blocklens.core.render;

import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.state.SemanticState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Line decals of the stairs and trapdoor cues: thin rectangles on the faces of the block's own
 * shape, derived from the semantic state alone and drawn with the {@link DecorationDecals#LINES}
 * glyph (see {@code knowledge/current/pack-information-parity.md}).
 *
 * <p>Stairs follow the source pack's drawing convention. The back face, on the facing side where
 * the step stands, is outlined in green. Every other face that is a full square is outlined in
 * amber with a green edge where it meets the back face. On full faces the step edges hidden behind
 * the face are dashed, so the shape reads from behind and below. Any other surface carries a green
 * line where it meets the back face. Trapdoors carry two hinge stubs on the edge they pivot on and
 * a handle on the opposite edge.</p>
 *
 * <p>Coordinates are sixteenths in the viewer frame of a face, the frame {@link DecorationDecals}
 * uses (screen-right x screen-up is the outward normal); depth is the distance of the face plane
 * inside the block boundary. Lines are rasterized per face plane, green over amber over dashes,
 * then merged into rectangles, so no two lines overlap and a dashed run is one quad.</p>
 */
public final class DecorationLines {
    /** Edges of the back face, where the step stands. */
    public static final int GREEN = 0xFF58D040;
    /** Outline of the other full faces and the hidden step edges. */
    public static final int AMBER = 0xFFF0B030;
    /** Trapdoor hinges and handle. */
    public static final int METAL = 0xFF464A52;
    public static final int UNITS = 16;

    private static final int UP = 4, DOWN = 5;
    // Raster classes, painted in priority order.
    private static final int DASHED = 1, OUTLINE = 2, BACK = 3, HARDWARE = 4;
    private static final int[] CLASS_ARGB = {0, AMBER, AMBER, GREEN, METAL};
    // Frame edges as numbered by DecorationDecals.quarterTurnsToward: top, right, bottom, left.
    private static final int[] EDGE_S = {0, 1, 0, -1};
    private static final int[] EDGE_T = {1, 0, -1, 0};
    private static final int HALF = UNITS / 2;
    private static final int FACES = 6;
    private static final Line[] EMPTY = {};
    private static final ConcurrentHashMap<Long, Line[]> INTERNED = new ConcurrentHashMap<>();

    /**
     * One rectangle {@code [left, right) x [bottom, top)} in sixteenths on the plane {@code depth}
     * sixteenths inside {@code face} (a {@link DecorationQuadFace} ordinal, NORTH..DOWN).
     */
    public record Line(int face, int depth, int left, int bottom, int right, int top, boolean dashed, int argb) {
        public Line {
            if (face < 0 || face >= FACES || depth < 0 || depth >= UNITS || left < 0 || bottom < 0
                    || right <= left || top <= bottom || right > UNITS || top > UNITS) {
                throw new IllegalArgumentException("invalid line " + face + "/" + depth + " [" + left + ","
                        + bottom + ")-[" + right + "," + top + ")");
            }
        }
    }

    private DecorationLines() {
    }

    /**
     * Lines for one (capability, state); empty for every cue that draws glyph decals instead.
     * Interned and shared across baked models: callers must never write to the returned array.
     */
    public static Line[] lines(DecorationRenderDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        CapabilityId capability = descriptor.capability();
        if (capability != CapabilityId.STAIRS && capability != CapabilityId.TRAPDOOR) {
            return EMPTY;
        }
        long key = (long) capability.ordinal() << 32 | descriptor.state().packed() & 0xFFFFFFFFL;
        return INTERNED.computeIfAbsent(key, ignored -> build(descriptor));
    }

    /** Uncached lines; {@link #lines} is the bake-time entry point. */
    static Line[] build(DecorationRenderDescriptor descriptor) {
        SemanticState state = descriptor.state();
        int front = state.facing().ordinal() - 1;
        if (front < 0 || state.half() == SemanticState.Half.NONE) {
            return EMPTY;
        }
        boolean top = state.half() == SemanticState.Half.TOP;
        int[][] planes = new int[FACES * UNITS][];
        if (descriptor.capability() == CapabilityId.STAIRS) {
            if (state.stairShape() == SemanticState.StairShape.NONE) {
                return EMPTY;
            }
            stairs(planes, front, solid(front, top, state.stairShape()));
        } else {
            trapdoor(planes, front, top, state.open());
        }
        List<Line> lines = new ArrayList<>();
        for (int plane = 0; plane < planes.length; plane++) {
            if (planes[plane] != null) {
                merge(lines, plane / UNITS, plane % UNITS, planes[plane]);
            }
        }
        return lines.toArray(EMPTY);
    }

    /** Octant occupancy, indexed x + 2y + 4z with each coordinate 0 or 1 (the lower or upper half). */
    static boolean[] solid(int back, boolean top, SemanticState.StairShape shape) {
        // Left of the facing as seen from above: east -> north, north -> west, west -> south, south -> east.
        int left = (back + 3) % 4;
        boolean[] solid = new boolean[8];
        for (int octant = 0; octant < 8; octant++) {
            int x = octant & 1, y = octant >> 1 & 1, z = octant >> 2 & 1;
            boolean f = toward(back, x, z), l = toward(left, x, z);
            boolean step = y == (top ? 0 : 1);
            solid[octant] = !step || switch (shape) {
                case STRAIGHT -> f;
                case INNER_LEFT -> f || l;
                case INNER_RIGHT -> f || !l;
                case OUTER_LEFT -> f && l;
                case OUTER_RIGHT -> f && !l;
                case NONE -> false;
            };
        }
        return solid;
    }

    /** Whether octant column (x, z) lies in the half of the block toward horizontal face {@code direction}. */
    private static boolean toward(int direction, int x, int z) {
        int nx = DecorationDecals.normalX(direction);
        return nx != 0 ? x == (nx > 0 ? 1 : 0) : z == (DecorationDecals.normalZ(direction) > 0 ? 1 : 0);
    }

    private static void stairs(int[][] planes, int back, boolean[] solid) {
        boolean[] exposed = new boolean[4];
        boolean[] behind = new boolean[4];
        for (int face = 0; face < FACES; face++) {
            int backEdge = DecorationDecals.quarterTurnsToward(face, back);
            for (int depth = 0; depth < UNITS; depth += HALF) {
                int count = 0;
                // Cell c covers sixteenths [8a, 8a + 8) x [8b, 8b + 8) with a = c & 1, b = c >> 1.
                for (int cell = 0; cell < 4; cell++) {
                    int s = (cell & 1) * HALF + HALF / 2, t = (cell >> 1) * HALF + HALF / 2;
                    boolean outer = solid[octant(face, s, t, HALF / 2)];
                    behind[cell] = solid[octant(face, s, t, HALF + HALF / 2)];
                    exposed[cell] = depth == 0 ? outer : behind[cell] && !outer;
                    if (exposed[cell]) count++;
                }
                if (count == 0) continue;
                int[] raster = plane(planes, face, depth);
                boolean backFace = face == back && depth == 0;
                boolean full = count == 4;
                if (backFace && count == 3) {
                    // Close the L silhouette's inner corner, in the cell diagonal to the missing one.
                    int corner = 0;
                    while (exposed[3 - corner]) corner++;
                    int s = (corner & 1) == 0 ? HALF - 1 : HALF, t = (corner >> 1) == 0 ? HALF - 1 : HALF;
                    paint(raster, BACK, s, t, s + 1, t + 1);
                }
                for (int cell = 0; cell < 4; cell++) {
                    if (!exposed[cell]) continue;
                    for (int edge = 0; edge < 4; edge++) {
                        int neighbor = neighbor(cell, edge);
                        boolean outside = neighbor < 0;
                        if (full && !outside && !behind[cell] && behind[neighbor]) {
                            // A hidden step edge, drawn inside the cell with nothing behind it.
                            edge(raster, DASHED, cell, edge);
                        }
                        if (!outside && exposed[neighbor]) continue;
                        if (backFace || edge == backEdge && outside) {
                            edge(raster, BACK, cell, edge);
                        } else if (full) {
                            edge(raster, OUTLINE, cell, edge);
                        }
                    }
                }
            }
        }
    }

    private static void trapdoor(int[][] planes, int front, boolean top, boolean open) {
        int hinge = DecorationDecals.opposite(front);
        if (!open) {
            // A 3/16 panel against the bottom or top boundary, pivoting on its hinge-side edge.
            panel(plane(planes, top ? UP : DOWN, 0), top ? UP : DOWN, hinge);
            panel(plane(planes, top ? DOWN : UP, UNITS - 3), top ? DOWN : UP, hinge);
            int bottom = top ? UNITS - 3 : 0;
            int[] hingeSide = plane(planes, hinge, 0);
            paint(hingeSide, HARDWARE, 3, bottom, 5, bottom + 3);
            paint(hingeSide, HARDWARE, 11, bottom, 13, bottom + 3);
            paint(plane(planes, front, 0), HARDWARE, 5, bottom, 11, bottom + 3);
        } else {
            // A 3/16 panel against the hinge side, pivoting on its bottom (or top) edge.
            int pivot = top ? UP : DOWN;
            int free = top ? DOWN : UP;
            panel(plane(planes, hinge, 0), hinge, pivot);
            panel(plane(planes, front, UNITS - 3), front, pivot);
            int[] pivotSide = plane(planes, pivot, 0);
            int edge = DecorationDecals.quarterTurnsToward(pivot, hinge);
            mark(pivotSide, edge, 3, 5, 3);
            mark(pivotSide, edge, 11, 13, 3);
            mark(plane(planes, free, 0), DecorationDecals.quarterTurnsToward(free, hinge), 5, 11, 3);
        }
    }

    /** Two hinge stubs along the panel edge toward {@code pivot} and a handle along the opposite edge. */
    private static void panel(int[] raster, int face, int pivot) {
        int edge = DecorationDecals.quarterTurnsToward(face, pivot);
        mark(raster, edge, 3, 5, 1);
        mark(raster, edge, 11, 13, 1);
        mark(raster, (edge + 2) % 4, 5, 11, 2);
    }

    /** Paints {@code [from, to)} along frame {@code edge}, {@code deep} sixteenths inward from it. */
    private static void mark(int[] raster, int edge, int from, int to, int deep) {
        switch (edge) {
            case 0 -> paint(raster, HARDWARE, from, UNITS - deep, to, UNITS);
            case 1 -> paint(raster, HARDWARE, UNITS - deep, from, UNITS, to);
            case 2 -> paint(raster, HARDWARE, from, 0, to, deep);
            default -> paint(raster, HARDWARE, 0, from, deep, to);
        }
    }

    /** The one-sixteenth strip of {@code cell} along its frame {@code edge}. */
    private static void edge(int[] raster, int cls, int cell, int edge) {
        int s = (cell & 1) * HALF, t = (cell >> 1) * HALF;
        switch (edge) {
            case 0 -> paint(raster, cls, s, t + HALF - 1, s + HALF, t + HALF);
            case 1 -> paint(raster, cls, s + HALF - 1, t, s + HALF, t + HALF);
            case 2 -> paint(raster, cls, s, t, s + HALF, t + 1);
            default -> paint(raster, cls, s, t, s + 1, t + HALF);
        }
    }

    /** The cell across frame {@code edge} of {@code cell}, or -1 outside the face. */
    private static int neighbor(int cell, int edge) {
        int a = (cell & 1) + EDGE_S[edge], b = (cell >> 1) + EDGE_T[edge];
        return a < 0 || a > 1 || b < 0 || b > 1 ? -1 : a + 2 * b;
    }

    private static int octant(int face, int s, int t, int depth) {
        int index = 0;
        for (int axis = 0; axis < 3; axis++) {
            index |= world(face, axis, s, t, depth) / HALF << axis;
        }
        return index;
    }

    /**
     * World coordinate in sixteenths on {@code axis} (0 x, 1 y, 2 z) of frame point {@code (s, t)}
     * on the plane {@code depth} sixteenths inside {@code face}.
     */
    static int world(int face, int axis, int s, int t, int depth) {
        return along(DecorationDecals.screenRight(face), axis, s) + along(DecorationDecals.screenUp(face), axis, t)
                + along(face, axis, UNITS - depth);
    }

    private static int along(int direction, int axis, int value) {
        int n = component(direction, axis);
        return n > 0 ? value : n < 0 ? UNITS - value : 0;
    }

    private static int component(int direction, int axis) {
        return axis == 0 ? DecorationDecals.normalX(direction)
                : axis == 1 ? DecorationDecals.normalY(direction) : DecorationDecals.normalZ(direction);
    }

    private static int[] plane(int[][] planes, int face, int depth) {
        int index = face * UNITS + depth;
        if (planes[index] == null) {
            planes[index] = new int[UNITS * UNITS];
        }
        return planes[index];
    }

    private static void paint(int[] raster, int cls, int s0, int t0, int s1, int t1) {
        for (int t = t0; t < t1; t++) {
            for (int s = s0; s < s1; s++) {
                raster[t * UNITS + s] = Math.max(raster[t * UNITS + s], cls);
            }
        }
    }

    /** Greedy rectangles: maximal runs along s, extended along t while the same run repeats. */
    private static void merge(List<Line> lines, int face, int depth, int[] raster) {
        boolean[] used = new boolean[raster.length];
        for (int t = 0; t < UNITS; t++) {
            for (int s = 0; s < UNITS; s++) {
                int cls = raster[t * UNITS + s];
                if (cls == 0 || used[t * UNITS + s]) continue;
                int right = s + 1;
                while (right < UNITS && raster[t * UNITS + right] == cls && !used[t * UNITS + right]) right++;
                int top = t + 1;
                while (top < UNITS && run(raster, used, cls, s, right, top)) top++;
                for (int row = t; row < top; row++) {
                    for (int column = s; column < right; column++) {
                        used[row * UNITS + column] = true;
                    }
                }
                lines.add(new Line(face, depth, s, t, right, top, cls == DASHED, CLASS_ARGB[cls]));
            }
        }
    }

    private static boolean run(int[] raster, boolean[] used, int cls, int from, int to, int row) {
        for (int s = from; s < to; s++) {
            if (raster[row * UNITS + s] != cls || used[row * UNITS + s]) return false;
        }
        return true;
    }

    /**
     * Whether every line lies on {@code surfaces}. Lines assume the vanilla shape; a resource pack
     * that reshapes the block (a thinner trapdoor, say) leaves some off its quads, and the cue then
     * keeps its tint rather than drawing marks in the air.
     */
    public static boolean fits(Line[] lines, Surfaces surfaces) {
        for (Line line : lines) {
            boolean[] covered = surfaces.planes[line.face() * UNITS + line.depth()];
            if (covered == null) return false;
            for (int t = line.bottom(); t < line.top(); t++) {
                for (int s = line.left(); s < line.right(); s++) {
                    if (!covered[t * UNITS + s]) return false;
                }
            }
        }
        return true;
    }

    /** The sixteenth cells of a block's face planes that its quads cover, fed one quad at a time. */
    public static final class Surfaces {
        private static final float GRID_TOLERANCE = 1.0E-3F;
        private final boolean[][] planes = new boolean[FACES * UNITS][];

        /**
         * Records one quad on {@code face} given as x, y, z (in blocks) of its four vertices. Only an
         * axis-aligned rectangle in a sixteenth plane covers anything, and only the cells wholly inside it.
         */
        public void add(int face, float[] positions) {
            if (face < 0 || face >= FACES) return;
            int normalAxis = axis(face);
            int rightAxis = axis(DecorationDecals.screenRight(face)), upAxis = axis(DecorationDecals.screenUp(face));
            int depth = -1;
            float minS = Float.POSITIVE_INFINITY, maxS = Float.NEGATIVE_INFINITY;
            float minT = Float.POSITIVE_INFINITY, maxT = Float.NEGATIVE_INFINITY;
            float[] s = new float[4], t = new float[4];
            for (int vertex = 0; vertex < 4; vertex++) {
                int plane = sixteenth(UNITS - unit(face, positions[3 * vertex + normalAxis] * UNITS));
                if (plane < 0 || plane >= UNITS || vertex > 0 && plane != depth) return;
                depth = plane;
                s[vertex] = unit(DecorationDecals.screenRight(face), positions[3 * vertex + rightAxis] * UNITS);
                t[vertex] = unit(DecorationDecals.screenUp(face), positions[3 * vertex + upAxis] * UNITS);
                minS = Math.min(minS, s[vertex]);
                maxS = Math.max(maxS, s[vertex]);
                minT = Math.min(minT, t[vertex]);
                maxT = Math.max(maxT, t[vertex]);
            }
            for (int vertex = 0; vertex < 4; vertex++) {
                boolean cornerS = Math.abs(s[vertex] - minS) < GRID_TOLERANCE || Math.abs(s[vertex] - maxS) < GRID_TOLERANCE;
                boolean cornerT = Math.abs(t[vertex] - minT) < GRID_TOLERANCE || Math.abs(t[vertex] - maxT) < GRID_TOLERANCE;
                if (!cornerS || !cornerT) return; // Not an axis-aligned rectangle in this frame.
            }
            int left = Math.max(0, (int) Math.ceil(minS - GRID_TOLERANCE));
            int right = Math.min(UNITS, (int) Math.floor(maxS + GRID_TOLERANCE));
            int bottom = Math.max(0, (int) Math.ceil(minT - GRID_TOLERANCE));
            int top = Math.min(UNITS, (int) Math.floor(maxT + GRID_TOLERANCE));
            boolean[] covered = planes[face * UNITS + depth];
            if (covered == null) {
                covered = planes[face * UNITS + depth] = new boolean[UNITS * UNITS];
            }
            for (int row = bottom; row < top; row++) {
                for (int column = left; column < right; column++) {
                    covered[row * UNITS + column] = true;
                }
            }
        }

        /** The frame coordinate, in sixteenths, of a world coordinate along {@code direction}'s axis. */
        private static float unit(int direction, float sixteenths) {
            int n = component(direction, axis(direction));
            return n > 0 ? sixteenths : UNITS - sixteenths;
        }

        private static int axis(int direction) {
            return DecorationDecals.normalX(direction) != 0 ? 0 : DecorationDecals.normalY(direction) != 0 ? 1 : 2;
        }

        /** The whole sixteenth {@code value} rounds to, or -1 when it is off the grid. */
        private static int sixteenth(float value) {
            int rounded = Math.round(value);
            return Math.abs(value - rounded) < GRID_TOLERANCE ? rounded : -1;
        }
    }

    /**
     * Fills {@code positions} with x, y, z (in blocks) of the line's corners in vanilla order: top-left,
     * bottom-left, bottom-right, top-right as seen from outside, lifted by
     * {@link DecorationDecals#SURFACE_OFFSET} along the face normal.
     */
    public static void vertices(Line line, float[] positions) {
        int face = line.face();
        for (int vertex = 0; vertex < 4; vertex++) {
            int s = vertex < 2 ? line.left() : line.right();
            int t = vertex == 1 || vertex == 2 ? line.bottom() : line.top();
            for (int axis = 0; axis < 3; axis++) {
                positions[3 * vertex + axis] = world(face, axis, s, t, line.depth()) / (float) UNITS
                        + component(face, axis) * DecorationDecals.SURFACE_OFFSET;
            }
        }
    }

    /**
     * Glyph-strip coordinates of the corners {@link #vertices} fills, for the sprite
     * {@code [u0, u1] x [v0, v1]}. Solid lines sample the opaque upper half of the
     * {@link DecorationDecals#LINES} cell; dashed lines sample its striped lower half with u running
     * along the line at one glyph width per block, so every dash of a block keeps one phase.
     */
    public static void uvs(Line line, float u0, float u1, float v0, float v1, float[] uvs) {
        float du = u1 - u0;
        float cell = (v1 - v0) / DecorationDecals.GLYPHS;
        float glyphTop = v0 + DecorationDecals.LINES * cell;
        boolean alongS = line.right() - line.left() >= line.top() - line.bottom();
        for (int vertex = 0; vertex < 4; vertex++) {
            boolean first = vertex < 2;
            boolean high = vertex == 0 || vertex == 3;
            float u, v;
            if (line.dashed()) {
                int along = alongS ? (first ? line.left() : line.right()) : (high ? line.top() : line.bottom());
                boolean across = alongS ? high : !first;
                u = u0 + du * Math.clamp(along / (float) UNITS, 1.0F / 1024.0F, 1023.0F / 1024.0F);
                v = glyphTop + cell * (across ? 0.875F : 0.625F);
            } else {
                u = u0 + du * (first ? 0.25F : 0.75F);
                v = glyphTop + cell * (high ? 0.125F : 0.375F);
            }
            uvs[2 * vertex] = u;
            uvs[2 * vertex + 1] = v;
        }
    }
}

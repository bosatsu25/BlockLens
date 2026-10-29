package dev.blocklens.core.analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Tick-thread bounded inspection; immutable snapshots are the only render-thread input. */
public final class AnalyzerEngine {
    /** Cell values a {@link WorldView} reports; {@link #HIDDEN} may be or-ed onto a physical value. */
    public static final int UNKNOWN = -1, OTHER = 0, LAVA = 2, BEACON = 3, ROD = 4, BASE = 5;
    public static final int HIDDEN = 256;
    public static final int CELL_LIMIT = 512, STEP_LIMIT = 4096;

    // Analyzer kinds: a marker's kind, an AnalyzerOptions row and bit 1 << kind of the enabled mask.
    private static final int LAVA_SCAN = 0, BEACON_SCAN = 1, ROD_SCAN = 2, VILLAGE = 3;
    private static final int BLOCK_SCANS = 3;
    // AnalyzerOptions fields of each kind.
    private static final int HORIZONTAL = 0, VERTICAL = 1, LIMIT = 2, INTERVAL = 3;
    private static final int VILLAGE_SECTIONS = 125, VILLAGE_ENTITIES = 64;
    private static final int BEACON_LAYERS = 4, ROD_RADIUS = 128;

    public record Point(double x, double y, double z) {
        public Point {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("non-finite analyzer position");
            }
        }

        public double distanceSquared(Point other) {
            double dx = x - other.x, dy = y - other.y, dz = z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }
    }

    public record Marker(int kind, Point from, Point to, int radius) { }

    public record Villager(Point from, Point site) { }

    public record VillageSample(List<Villager> villagers, int sections, int entities) {
        public VillageSample {
            villagers = List.copyOf(villagers);
        }
    }

    public record Work(int cells, int sections, int cursorSteps, int entitySections, int entities) { }

    public interface WorldView {
        /** Physical kind, optionally HIDDEN for presentation; UNKNOWN remains unmodified. */
        int cell(int x, int y, int z);

        VillageSample villagers(Point eye, int horizontal, int vertical, int sectionLimit, int entityLimit, int candidates);
    }

    private record Snapshot(List<Marker> markers, int mask, AnalyzerOptions options) { }

    private static final Snapshot EMPTY_SNAPSHOT = new Snapshot(List.of(), 0, null);

    private final Scan[] scans = new Scan[BLOCK_SCANS];
    private volatile Snapshot published = EMPTY_SNAPSHOT;
    private Object identity;
    private AnalyzerOptions options;
    private int enabled, tick, turn, nextVillage;
    private int cells, steps, entitySections, entities;
    private int anchorX, anchorY, anchorZ;

    public List<Marker> markers() {
        return published.markers;
    }

    /** The published markers when they were computed for this mask and these options, else none. */
    public List<Marker> markers(int mask, AnalyzerOptions settings) {
        Snapshot current = published;
        return current.mask == mask && settings.equals(current.options) ? current.markers : List.of();
    }

    /**
     * This tick's work. No current analyzer probes chunk sections (the removed debris scan did), so
     * {@code sections} is always 0.
     */
    public Work work() {
        return new Work(cells, 0, steps, entitySections, entities);
    }

    public void clear() {
        identity = null;
        options = null;
        enabled = 0;
        published = EMPTY_SNAPSHOT;
        cells = steps = entitySections = entities = 0;
        Arrays.fill(scans, null);
        nextVillage = 0;
        turn = 0;
        tick = 0;
    }

    public void tick(Object worldIdentity, WorldView view, Point eye, int mask, AnalyzerOptions settings) {
        cells = steps = entitySections = entities = 0;
        if (worldIdentity == null || view == null || eye == null || mask == 0) {
            clear();
            return;
        }
        Objects.requireNonNull(settings, "settings");
        int x = (int) Math.floor(eye.x), y = (int) Math.floor(eye.y), z = (int) Math.floor(eye.z);
        if (identity != worldIdentity || enabled != mask || !settings.equals(options)
                || anchorX != x || anchorY != y || anchorZ != z) {
            // Any change starts over, so published markers always belong to this world, mask,
            // options and anchor block: every one of them is enabled and inside its box.
            clear();
            identity = worldIdentity;
            enabled = mask;
            options = settings;
            anchorX = x;
            anchorY = y;
            anchorZ = z;
            for (int kind = 0; kind < BLOCK_SCANS; kind++) {
                if (isEnabled(mask, kind)) scans[kind] = new Scan(kind);
            }
        }
        tick++;
        ArrayList<Marker> retained = revalidate(view);
        if (isEnabled(mask, VILLAGE) && tick >= nextVillage && CELL_LIMIT - cells >= settings.value(VILLAGE, LIMIT)) {
            sampleVillage(view, eye, retained);
        }
        scanAndPublish(view, eye, retained);
        published = new Snapshot(List.copyOf(retained), mask, settings);
    }

    /** Rereads each published marker's own cell; a marker survives while its target is still there. */
    private ArrayList<Marker> revalidate(WorldView view) {
        ArrayList<Marker> retained = new ArrayList<>();
        for (Marker marker : published.markers) {
            int value = read(view, marker.to);
            if (visible(value) && (marker.kind == VILLAGE || physical(value) == target(marker.kind))) {
                retained.add(marker);
            }
        }
        return retained;
    }

    /** Replaces the village markers with one bounded sample; each job-site endpoint costs one cell. */
    private void sampleVillage(WorldView view, Point eye, List<Marker> retained) {
        int candidates = options.value(VILLAGE, LIMIT);
        VillageSample sample = view.villagers(eye, options.value(VILLAGE, HORIZONTAL),
                options.value(VILLAGE, VERTICAL), VILLAGE_SECTIONS, VILLAGE_ENTITIES, candidates);
        if (sample.sections < 0 || sample.sections > VILLAGE_SECTIONS || sample.entities < 0
                || sample.entities > VILLAGE_ENTITIES || sample.villagers.size() > candidates) {
            throw new IllegalStateException("unbounded village adapter");
        }
        entitySections = sample.sections;
        entities = sample.entities;
        retained.removeIf(marker -> marker.kind == VILLAGE);
        for (Villager villager : sample.villagers) {
            if (villager.site != null && inside(VILLAGE, villager.site) && visible(read(view, villager.site))) {
                retained.add(new Marker(VILLAGE, villager.from, villager.site, 0));
            }
        }
        nextVillage = tick + options.value(VILLAGE, INTERVAL);
    }

    /** Round-robin over the block scans until all of them stall in a row or the step budget ends. */
    private void scanAndPublish(WorldView view, Point eye, List<Marker> retained) {
        int stalled = 0;
        while (steps < STEP_LIMIT && stalled < BLOCK_SCANS) {
            Scan scan = scans[turn];
            turn = (turn + 1) % BLOCK_SCANS;
            if (scan == null || !scan.step(view, eye)) {
                stalled++;
                continue;
            }
            stalled = 0;
            if (scan.completed && scan.fits()) {
                scan.publishNext(view, retained);
            }
        }
    }

    /** The marker for a candidate cell, or null when the cell is not a complete target of {@code kind}. */
    private Marker describe(WorldView view, int kind, Point position) {
        int x = (int) Math.floor(position.x), y = (int) Math.floor(position.y), z = (int) Math.floor(position.z);
        int value = read(view, x, y, z);
        if (!visible(value) || physical(value) != target(kind)) return null;
        return switch (kind) {
            case LAVA_SCAN -> lavaBoundary(new int[] {
                    read(view, x + 1, y, z), read(view, x - 1, y, z), read(view, x, y + 1, z),
                    read(view, x, y - 1, z), read(view, x, y, z + 1), read(view, x, y, z - 1)})
                    ? new Marker(kind, position, position, 0) : null;
            case BEACON_SCAN -> {
                int layers = baseLayers(view, x, y, z);
                yield layers == 0 ? null : new Marker(kind, position, position, 10 + 10 * layers);
            }
            default -> new Marker(kind, position, position, ROD_RADIUS);
        };
    }

    /** Consecutive complete base layers under a beacon; layer n is the (2n + 1)^2 square n blocks down. */
    private int baseLayers(WorldView view, int x, int y, int z) {
        for (int layer = 1; layer <= BEACON_LAYERS; layer++) {
            for (int bx = x - layer; bx <= x + layer; bx++) {
                for (int bz = z - layer; bz <= z + layer; bz++) {
                    if (physical(read(view, bx, y - layer, bz)) != BASE) return layer - 1;
                }
            }
        }
        return BEACON_LAYERS;
    }

    /** Most cells one candidate check of {@code kind} reads, reserved before the check starts. */
    private static int reservation(int kind) {
        return switch (kind) {
            case LAVA_SCAN -> 1 + 6;
            case BEACON_SCAN -> 1 + 9 + 25 + 49 + 81;
            default -> 1;
        };
    }

    private int read(WorldView view, Point point) {
        return read(view, (int) Math.floor(point.x), (int) Math.floor(point.y), (int) Math.floor(point.z));
    }

    private int read(WorldView view, int x, int y, int z) {
        if (cells >= CELL_LIMIT) throw new IllegalStateException("cell budget exceeded");
        cells++;
        return view.cell(x, y, z);
    }

    /** Whether {@code point} lies in {@code kind}'s box around the anchor block. */
    private boolean inside(int kind, Point point) {
        int h = options.value(kind, HORIZONTAL), v = options.value(kind, VERTICAL);
        return Math.abs(Math.floor(point.x) - anchorX) <= h && Math.abs(Math.floor(point.y) - anchorY) <= v
                && Math.abs(Math.floor(point.z) - anchorZ) <= h;
    }

    private static boolean isEnabled(int mask, int kind) {
        return (mask & (1 << kind)) != 0;
    }

    private static int target(int kind) {
        return switch (kind) {
            case LAVA_SCAN -> LAVA;
            case BEACON_SCAN -> BEACON;
            case ROD_SCAN -> ROD;
            default -> throw new IllegalArgumentException("not a block analyzer");
        };
    }

    public static boolean lavaBoundary(int[] neighbors) {
        for (int neighbor : neighbors) {
            if (physical(neighbor) != UNKNOWN && physical(neighbor) != LAVA) return true;
        }
        return false;
    }

    private static int physical(int value) {
        return value == UNKNOWN ? UNKNOWN : value & ~HIDDEN;
    }

    private static boolean visible(int value) {
        return value != UNKNOWN && (value & HIDDEN) == 0;
    }

    /** One block analyzer's pass over its box around the anchor block, then its publication. */
    private final class Scan {
        final int kind, minX, minY, minZ, nx, ny;
        final long cellCount;
        final ArrayList<Marker> found = new ArrayList<>();
        long index;
        int ready, publishIndex;
        boolean completed, publicationStarted;
        Point referenceEye;

        Scan(int kind) {
            this.kind = kind;
            int h = options.value(kind, HORIZONTAL), v = options.value(kind, VERTICAL);
            minX = anchorX - h;
            minY = anchorY - v;
            minZ = anchorZ - h;
            nx = 2 * h + 1;
            ny = 2 * v + 1;
            cellCount = nx * (long) ny * nx;
        }

        boolean fits() {
            return CELL_LIMIT - cells >= reservation(kind);
        }

        /** Checks the next cell of the box; a completed pass only reports whether it can publish. */
        boolean step(WorldView view, Point eye) {
            if (completed) return fits();
            if (tick < ready || !fits()) return false;
            if (referenceEye == null) referenceEye = eye;
            int x = minX + (int) (index % nx), y = minY + (int) ((index / nx) % ny), z = minZ + (int) (index / (nx * (long) ny));
            steps++;
            if (++index >= cellCount) completed = true;
            Marker candidate = describe(view, kind, new Point(x + .5, y + .5, z + .5));
            if (candidate != null) {
                // Nearest to the pass's first eye position, at most the configured number of markers.
                found.add(candidate);
                found.sort(Comparator.comparingDouble(marker -> marker.from.distanceSquared(referenceEye)));
                if (found.size() > options.value(kind, LIMIT)) found.removeLast();
            }
            return true;
        }

        /** Publishes the next candidate after rechecking it; the first one removes the last pass's markers. */
        void publishNext(WorldView view, List<Marker> retained) {
            if (!publicationStarted) {
                retained.removeIf(marker -> marker.kind == kind);
                publicationStarted = true;
            }
            if (publishIndex < found.size()) {
                Marker fresh = describe(view, kind, found.get(publishIndex++).to);
                if (fresh != null) retained.add(fresh);
            }
            if (publishIndex == found.size()) {
                found.clear();
                restart(tick + options.value(kind, INTERVAL));
            }
        }

        void restart(int next) {
            index = 0;
            ready = next;
            completed = false;
            referenceEye = null;
            publishIndex = 0;
            publicationStarted = false;
        }
    }
}

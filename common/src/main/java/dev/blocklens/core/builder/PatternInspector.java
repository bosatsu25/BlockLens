package dev.blocklens.core.builder;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Fixed-domain incremental comparison of one explicitly selected, client-local reference. */
public final class PatternInspector {
    public static final int HORIZONTAL_RADIUS = 8, VERTICAL_RADIUS = 4;
    public static final int DOMAIN_SIZE = 2601, MAX_OPERATIONS = 256, RESCAN_TICKS = 20;

    @FunctionalInterface
    public interface WorldView {
        /** Returns null for an unavailable cell; implementations must never load a chunk. */
        BuilderState state(int x, int y, int z);
    }

    public record Snapshot(BuilderState reference, int scanned, int compared, int matches,
            int mismatches, int unknown, Map<String, Integer> propertyMismatches, boolean complete) { }

    private static final Snapshot EMPTY = new Snapshot(null, 0, 0, 0, 0, 0, Map.of(), false);
    private Object identity;
    private BuilderState reference;
    private int x, y, z, cursor, cooldown, compared, matches, mismatches, unknown;
    private final Map<String, Integer> counts = new LinkedHashMap<>();
    private Snapshot snapshot = EMPTY;

    public boolean select(Object world, WorldView view, int x, int y, int z) {
        clear();
        if (world == null || view == null || !fits(x, HORIZONTAL_RADIUS)
                || !fits(y, VERTICAL_RADIUS) || !fits(z, HORIZONTAL_RADIUS)) return false;
        BuilderState state = view.state(x, y, z);
        if (state == null) return false;
        identity = world;
        reference = state;
        this.x = x; this.y = y; this.z = z;
        for (String property : state.properties().keySet()) counts.put(property, 0);
        publish();
        return true;
    }

    /** Returns all cursor/read operations spent this tick, including reference validation. */
    public int tick(Object world, WorldView view, boolean enabled) {
        if (!enabled || world != identity || view == null) { clear(); return 0; }
        if (reference == null) return 0;
        if (!reference.equals(view.state(x, y, z))) { clear(); return 1; }
        if (cooldown > 0) { cooldown--; return 1; }
        if (cursor == DOMAIN_SIZE) {
            cursor = 0; compared = 0; matches = 0; mismatches = 0; unknown = 0;
            counts.replaceAll((property, count) -> 0);
        }
        int operations = 1;
        while (cursor < DOMAIN_SIZE && operations < MAX_OPERATIONS) {
            int index = cursor++;
            operations++;
            int dy = index % 9 - VERTICAL_RADIUS;
            int plane = index / 9;
            int dx = plane % 17 - HORIZONTAL_RADIUS;
            int dz = plane / 17 - HORIZONTAL_RADIUS;
            if (dx == 0 && dy == 0 && dz == 0) continue;
            BuilderState candidate = view.state(x + dx, y + dy, z + dz);
            if (candidate == null) { unknown++; continue; }
            if (!reference.id().equals(candidate.id())) continue;
            compared++;
            boolean mismatch = false;
            for (var property : reference.properties().entrySet()) {
                if (!Objects.equals(property.getValue(), candidate.properties().get(property.getKey()))) {
                    counts.merge(property.getKey(), 1, Integer::sum);
                    mismatch = true;
                }
            }
            if (mismatch) mismatches++; else matches++;
        }
        if (cursor == DOMAIN_SIZE) cooldown = RESCAN_TICKS;
        publish();
        return operations;
    }

    public void clear() {
        identity = null; reference = null;
        x = 0; y = 0; z = 0; cursor = 0; cooldown = 0;
        compared = 0; matches = 0; mismatches = 0; unknown = 0;
        counts.clear(); snapshot = EMPTY;
    }

    public Snapshot snapshot() { return snapshot; }

    private void publish() {
        snapshot = new Snapshot(reference, cursor, compared, matches, mismatches, unknown,
                Collections.unmodifiableMap(new LinkedHashMap<>(counts)), cursor == DOMAIN_SIZE);
    }

    private static boolean fits(int coordinate, int radius) {
        return coordinate >= Integer.MIN_VALUE + radius && coordinate <= Integer.MAX_VALUE - radius;
    }
}

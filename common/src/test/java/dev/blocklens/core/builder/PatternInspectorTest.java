package dev.blocklens.core.builder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class PatternInspectorTest {
    private static final BuilderState NORTH = new BuilderState("minecraft:oak_stairs", Map.of("facing", "north", "half", "bottom"));
    private static final BuilderState SOUTH = new BuilderState("minecraft:oak_stairs", Map.of("facing", "south", "half", "top"));
    private static final BuilderState OTHER = new BuilderState("minecraft:stone", Map.of());

    @Test void countsOnlyReferenceTypeAndExcludesReferenceInsideExactDomain() {
        var world = new World();
        world.put(0, 0, 0, NORTH); world.put(8, 4, 8, NORTH); world.put(-8, -4, -8, SOUTH);
        world.put(1, 0, 0, new BuilderState("minecraft:oak_stairs", Map.of("half", "bottom")));
        var engine = new PatternInspector();
        assertTrue(engine.select(world, world, 0, 0, 0));
        world.visited.clear();
        finish(engine, world);
        var result = engine.snapshot();
        assertEquals(2601, result.scanned());
        assertEquals(3, result.compared()); assertEquals(1, result.matches()); assertEquals(2, result.mismatches());
        assertEquals(Map.of("facing", 2, "half", 1), result.propertyMismatches());
        assertEquals(0, result.unknown());
        assertEquals(2601, world.visited.size());
        assertTrue(world.visited.contains("8,4,8")); assertTrue(world.visited.contains("-8,-4,-8"));
        for (var coordinate : world.visited) {
            var p = coordinate.split(",");
            assertTrue(Math.abs(Integer.parseInt(p[0])) <= 8);
            assertTrue(Math.abs(Integer.parseInt(p[1])) <= 4);
            assertTrue(Math.abs(Integer.parseInt(p[2])) <= 8);
        }
        assertThrows(UnsupportedOperationException.class, () -> result.propertyMismatches().put("facing", 0));
    }

    @Test void enforcesBudgetIncludingReferenceValidationAndPublishesProgress() {
        var world = new World(); world.put(0, 0, 0, NORTH);
        var engine = new PatternInspector(); assertTrue(engine.select(world, world, 0, 0, 0));
        world.calls = 0;
        assertEquals(256, engine.tick(world, world, true));
        assertTrue(world.calls <= 256); assertEquals(255, engine.snapshot().scanned());
        assertFalse(engine.snapshot().complete());
        finish(engine, world);
        var completed = engine.snapshot();
        for (int i = 0; i < 20; i++) {
            world.calls = 0;
            assertEquals(1, engine.tick(world, world, true)); assertEquals(1, world.calls);
            assertEquals(completed, engine.snapshot());
        }
        engine.tick(world, world, true);
        assertEquals(255, engine.snapshot().scanned()); assertFalse(engine.snapshot().complete());
    }

    @Test void unknownCellsAreCountedWithoutPretendingTheyMatch() {
        var world = new World(); world.put(0, 0, 0, NORTH); world.unknown.add("1,0,0");
        var engine = new PatternInspector(); assertTrue(engine.select(world, world, 0, 0, 0));
        finish(engine, world);
        assertEquals(1, engine.snapshot().unknown()); assertEquals(0, engine.snapshot().compared());
    }

    @Test void offAndWorldChangeClearWithoutReadingAnotherCell() {
        var world = new World(); world.put(0, 0, 0, NORTH);
        var engine = new PatternInspector(); assertTrue(engine.select(world, world, 0, 0, 0));
        world.calls = 0; assertEquals(0, engine.tick(world, world, false)); assertEquals(0, world.calls);
        assertNull(engine.snapshot().reference());
        assertEquals(0, engine.tick(world, world, true));
        assertTrue(engine.select(world, world, 0, 0, 0));
        world.calls = 0; assertEquals(0, engine.tick(new Object(), world, true)); assertEquals(0, world.calls);
        assertNull(engine.snapshot().reference());
    }

    @Test void changedOrMissingReferenceInvalidatesAllResultsImmediately() {
        var world = new World(); world.put(0, 0, 0, NORTH);
        var engine = new PatternInspector(); assertTrue(engine.select(world, world, 0, 0, 0));
        finish(engine, world); world.put(0, 0, 0, SOUTH);
        assertEquals(1, engine.tick(world, world, true)); assertNull(engine.snapshot().reference());
        assertTrue(engine.select(world, world, 0, 0, 0)); world.unknown.add("0,0,0");
        assertEquals(1, engine.tick(world, world, true)); assertNull(engine.snapshot().reference());
    }

    @Test void explicitReselectionAndClearDiscardPartialCounts() {
        var world = new World(); world.put(0, 0, 0, NORTH); world.put(1, 0, 0, SOUTH);
        var engine = new PatternInspector(); assertTrue(engine.select(world, world, 0, 0, 0));
        engine.tick(world, world, true);
        assertTrue(engine.select(world, world, 1, 0, 0));
        assertEquals(SOUTH, engine.snapshot().reference()); assertEquals(0, engine.snapshot().scanned());
        finish(engine, world); assertEquals(1, engine.snapshot().mismatches());
        engine.clear(); assertNull(engine.snapshot().reference()); assertEquals(0, engine.snapshot().compared());
    }

    @Test void invalidSelectionIsAbsentSafeAndNeverOverflowsCoordinateBounds() {
        var world = new World(); var engine = new PatternInspector();
        assertFalse(engine.select(null, world, 0, 0, 0));
        assertFalse(engine.select(world, null, 0, 0, 0));
        assertFalse(engine.select(world, world, Integer.MAX_VALUE, 0, 0));
        assertFalse(engine.select(world, world, 0, Integer.MIN_VALUE, 0));
        assertFalse(engine.select(world, world, 0, 0, Integer.MAX_VALUE));
        assertEquals(0, world.calls);
        world.unknown.add("0,0,0"); assertFalse(engine.select(world, world, 0, 0, 0));
        assertNull(engine.snapshot().reference());
    }

    @Test void boundaryCoordinatesAtExtremeLimitsAreValidAndRejectJustOutside() {
        var world = new World(); var engine = new PatternInspector();
        // Exact on-boundary points (valid): MIN_VALUE + radius, MAX_VALUE - radius
        world.put(Integer.MIN_VALUE + 8, 0, 0, NORTH);
        assertTrue(engine.select(world, world, Integer.MIN_VALUE + 8, 0, 0));
        world.put(Integer.MAX_VALUE - 8, 0, 0, NORTH);
        assertTrue(engine.select(world, world, Integer.MAX_VALUE - 8, 0, 0));
        world.put(0, Integer.MIN_VALUE + 4, 0, NORTH);
        assertTrue(engine.select(world, world, 0, Integer.MIN_VALUE + 4, 0));
        world.put(0, Integer.MAX_VALUE - 4, 0, NORTH);
        assertTrue(engine.select(world, world, 0, Integer.MAX_VALUE - 4, 0));

        // Just outside boundary points (invalid by 1 coordinate)
        assertFalse(engine.select(world, world, Integer.MIN_VALUE + 7, 0, 0));
        assertFalse(engine.select(world, world, Integer.MAX_VALUE - 7, 0, 0));
        assertFalse(engine.select(world, world, 0, Integer.MIN_VALUE + 3, 0));
        assertFalse(engine.select(world, world, 0, Integer.MAX_VALUE - 3, 0));
        assertFalse(engine.select(world, world, 0, 0, Integer.MIN_VALUE + 7));
        assertFalse(engine.select(world, world, 0, 0, Integer.MAX_VALUE - 7));
    }

    @Test void completeStateTransitionCycleFromSelectToCooldownAndRescan() {
        var world = new World();
        world.put(0, 0, 0, NORTH);
        world.put(1, 0, 0, SOUTH);
        var engine = new PatternInspector();

        // 1. Initial / Unselected State
        assertNull(engine.snapshot().reference());
        assertFalse(engine.snapshot().complete());
        assertEquals(0, engine.snapshot().scanned());

        // 2. Select State
        assertTrue(engine.select(world, world, 0, 0, 0));
        assertEquals(NORTH, engine.snapshot().reference());
        assertEquals(0, engine.snapshot().scanned());
        assertFalse(engine.snapshot().complete());

        // 3. Progressive scanning
        int ops = engine.tick(world, world, true);
        assertEquals(256, ops);
        assertTrue(engine.snapshot().scanned() > 0);
        assertFalse(engine.snapshot().complete());

        // 4. Scan completion
        finish(engine, world);
        assertTrue(engine.snapshot().complete());
        assertEquals(PatternInspector.DOMAIN_SIZE, engine.snapshot().scanned());
        assertEquals(1, engine.snapshot().compared());
        assertEquals(1, engine.snapshot().mismatches());

        // 5. Cooldown period (20 ticks)
        for (int i = 0; i < PatternInspector.RESCAN_TICKS; i++) {
            assertEquals(1, engine.tick(world, world, true));
            assertTrue(engine.snapshot().complete());
            assertEquals(PatternInspector.DOMAIN_SIZE, engine.snapshot().scanned());
        }

        // 6. Rescan trigger after cooldown expires
        int rescanOps = engine.tick(world, world, true);
        assertTrue(rescanOps > 1);
        assertFalse(engine.snapshot().complete());

        // 7. Clear transition
        engine.clear();
        assertNull(engine.snapshot().reference());
        assertEquals(0, engine.snapshot().scanned());
    }

    @Test void propertyMismatchMergeHandlesCandidatesWithDifferentPropertiesSafely() {
        var world = new World();
        world.put(0, 0, 0, NORTH); // properties: facing=north, half=bottom
        // Candidate with missing "half" property and extra "shape" property
        world.put(1, 0, 0, new BuilderState("minecraft:oak_stairs", Map.of("facing", "south", "shape", "straight")));
        var engine = new PatternInspector();
        assertTrue(engine.select(world, world, 0, 0, 0));
        finish(engine, world);
        var snapshot = engine.snapshot();
        assertEquals(1, snapshot.compared());
        assertEquals(1, snapshot.mismatches());
        // facing is mismatch (south != north) -> count=1
        assertEquals(1, snapshot.propertyMismatches().get("facing"));
        // half is mismatch (null != bottom) -> count=1
        assertEquals(1, snapshot.propertyMismatches().get("half"));
    }


    private static void finish(PatternInspector engine, World world) {
        for (int i = 0; i < 12 && !engine.snapshot().complete(); i++) {
            world.calls = 0;
            assertTrue(engine.tick(world, world, true) <= 256);
            assertTrue(world.calls <= 256);
        }
        assertTrue(engine.snapshot().complete(), "declared domain did not complete within bounded pass");
    }

    private static final class World implements PatternInspector.WorldView {
        final Map<String, BuilderState> states = new HashMap<>();
        final Set<String> visited = new HashSet<>(), unknown = new HashSet<>();
        int calls;
        void put(int x, int y, int z, BuilderState state) { states.put(x + "," + y + "," + z, state); }
        @Override public BuilderState state(int x, int y, int z) {
            calls++; String key = x + "," + y + "," + z; visited.add(key);
            return unknown.contains(key) ? null : states.getOrDefault(key, OTHER);
        }
    }
}

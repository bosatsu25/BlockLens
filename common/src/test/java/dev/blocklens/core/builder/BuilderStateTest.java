package dev.blocklens.core.builder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class BuilderStateTest {
    @Test void distinguishesMissingDifferentBlockAndChangedProperties() {
        var north = new BuilderState("minecraft:oak_stairs", Map.of("facing", "north", "half", "bottom"));
        assertEquals(BuilderState.UNAVAILABLE, BuilderState.compare(north, null));
        assertEquals(BuilderState.UNAVAILABLE, BuilderState.compare(null, north));
        assertEquals(BuilderState.MATCH, BuilderState.compare(north,
                new BuilderState("minecraft:oak_stairs", Map.of("half", "bottom", "facing", "north"))));
        assertEquals(BuilderState.ADJUSTED, BuilderState.compare(north,
                new BuilderState("minecraft:oak_stairs", Map.of("facing", "south", "half", "bottom"))));
        assertEquals(BuilderState.ADJUSTED, BuilderState.compare(north,
                new BuilderState("minecraft:oak_stairs", Map.of("facing", "north"))));
        assertEquals(BuilderState.DIFFERENT, BuilderState.compare(north,
                new BuilderState("minecraft:birch_stairs", north.properties())));
    }

    @Test void copiesPropertiesAndSortsTheirOrder() {
        var raw = new LinkedHashMap<String, String>();
        raw.put("waterlogged", "false"); raw.put("facing", "north");
        var state = new BuilderState("minecraft:oak_stairs", raw);
        raw.put("facing", "south");
        assertEquals("north", state.properties().get("facing"));
        assertEquals("facing", state.properties().keySet().iterator().next());
        assertThrows(UnsupportedOperationException.class, () -> state.properties().put("half", "top"));
    }

    @Test void rejectsOversizedOrInvalidInputInsteadOfComparingTruncatedState() {
        assertThrows(IllegalArgumentException.class, () -> new BuilderState(null, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuilderState("minecraft:stone", null));
        assertThrows(IllegalArgumentException.class, () -> new BuilderState("", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuilderState("minecraft:bad\nline", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuilderState("minecraft:" + "a".repeat(247), Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new BuilderState("minecraft:stone", Map.of("face", "a".repeat(65))));
        assertThrows(IllegalArgumentException.class, () -> new BuilderState("minecraft:stone", Map.of("", "north")));
        assertThrows(IllegalArgumentException.class, () -> new BuilderState("minecraft:stone", Map.of("face", "")));
        assertThrows(IllegalArgumentException.class, () -> new BuilderState("minecraft:stone", Map.of("a".repeat(65), "north")));
        assertThrows(IllegalArgumentException.class, () -> new BuilderState("minecraft:stone", Map.of("face", "UP")));
        var raw = new LinkedHashMap<String, String>();
        for (int i = 0; i < 32; i++) raw.put("p" + i, "value");
        assertEquals(32, new BuilderState("minecraft:stone", raw).properties().size());
        raw.put("p32", "value");
        assertThrows(IllegalArgumentException.class, () -> new BuilderState("minecraft:stone", raw));
        assertEquals(256, new BuilderState("minecraft:" + "a".repeat(246), Map.of()).id().length());
        assertEquals(64, new BuilderState("minecraft:stone", Map.of("a".repeat(64), "b".repeat(64)))
                .properties().keySet().iterator().next().length());
        raw.clear(); raw.put("face", null);
        assertThrows(IllegalArgumentException.class, () -> new BuilderState("minecraft:stone", raw));
    }
}

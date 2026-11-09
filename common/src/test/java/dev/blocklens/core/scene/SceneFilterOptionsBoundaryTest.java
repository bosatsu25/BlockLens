package dev.blocklens.core.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * CTFL 4.2.2 three-value BVA of the three bounds of {@link SceneFilterOptions} (IDs per list, chars
 * per ID, encoded chars over the four lists) and 4.2.1 partitions of a namespaced ID's path
 * segments; see {@code knowledge/current/test-design-ctfl.md} §7 (TD-F*). The aggregate bound is
 * reached through the record constructor, so the separator count is part of what is checked.
 */
final class SceneFilterOptionsBoundaryTest {
    private static List<String> ids(int count) {
        var ids = new ArrayList<String>();
        for (int i = 0; i < count; i++) ids.add("t:b" + i);
        return ids;
    }

    @Test
    void idCountBoundaryIsFiveHundredTwelveInclusive() {
        assertEquals(511, SceneFilterOptions.normalize(ids(511)).size());
        assertEquals(512, SceneFilterOptions.normalize(ids(512)).size());
        assertThrows(IllegalArgumentException.class, () -> SceneFilterOptions.normalize(ids(513)));
    }

    @Test
    void idLengthBoundaryIsTwoHundredFiftySixRawCharactersInclusive() {
        String base = "t:";
        assertEquals(255, SceneFilterOptions.normalize(List.of(base + "a".repeat(253))).get(0).length());
        assertEquals(256, SceneFilterOptions.normalize(List.of(base + "a".repeat(254))).get(0).length());
        assertThrows(IllegalArgumentException.class,
                () -> SceneFilterOptions.normalize(List.of(base + "a".repeat(255))));
        // The raw length counts before trimming: 256 raw characters pass, 257 do not, whatever the trimmed value.
        assertEquals("t:a", SceneFilterOptions.normalize(List.of(" ".repeat(253) + "t:a")).get(0));
        assertThrows(IllegalArgumentException.class,
                () -> SceneFilterOptions.normalize(List.of(" ".repeat(254) + "t:a")));
    }

    @Test
    void encodedTextBoundaryCountsSeparatorsAcrossAllFourLists() {
        // 191 IDs of 256 chars + 190 separators = 49,086; the entity whitelist adds "t:" + 63 = 65 chars
        // and, within its own list of two, one separator: 49,086 + 65 + 1 = 49,152 = MAX_TOTAL_CHARS.
        var longIds = new ArrayList<String>();
        for (int i = 0; i < 191; i++) {
            String prefix = "t:b" + i + "_";
            longIds.add(prefix + "a".repeat(256 - prefix.length()));
        }
        var blocks = new SceneFilterOptions.Rule(SceneFilterOptions.Mode.BLACKLIST, longIds, List.of());
        var twoEntities = new SceneFilterOptions.Rule(SceneFilterOptions.Mode.WHITELIST, List.of(),
                List.of("t:a", "t:" + "c".repeat(60)));
        assertEquals(SceneFilterOptions.MAX_TOTAL_CHARS,
                49086 + 3 + 62 + 1, "fixture arithmetic");
        var exact = new SceneFilterOptions(blocks, twoEntities);
        assertEquals(2, exact.entities().whitelist().size());
        assertEquals(exact.entities().whitelist(), exact.entities().list(true));
        assertEquals(longIds.size(), exact.blocks().list(false).size());
        assertEquals(List.of(), exact.blocks().list(true));
        var oneOver = new SceneFilterOptions.Rule(SceneFilterOptions.Mode.WHITELIST, List.of(),
                List.of("t:a", "t:" + "c".repeat(61)));
        assertThrows(IllegalArgumentException.class, () -> new SceneFilterOptions(blocks, oneOver));
        // Without the separator the same characters fit: one ID of 65 chars instead of two IDs of 64.
        var oneId = new SceneFilterOptions.Rule(SceneFilterOptions.Mode.WHITELIST, List.of(),
                List.of("t:" + "c".repeat(64)));
        assertEquals(1, new SceneFilterOptions(blocks, oneId).entities().whitelist().size());
    }

    @ParameterizedTest(name = "TD-F4 rejected segment form [{0}]")
    @ValueSource(strings = {"t:.", "t:..", "t:a/.", "t:./a", "t:a/./b", "t:a/../b", "t:a//b", "t:a/", "t:/a"})
    void pathSegmentsMustBeNonEmptyAndNotDots(String id) {
        assertThrows(IllegalArgumentException.class, () -> SceneFilterOptions.normalize(List.of(id)));
    }

    @ParameterizedTest(name = "TD-F5 accepted segment form [{0}]")
    @ValueSource(strings = {"t:a.b", "t:.a", "t:a.", "t:a/b", "t:a/b.c/d", "t:...a", "a.b:c"})
    void dotsInsideSegmentsAndNamespacesAreAllowed(String id) {
        assertEquals(List.of(id), SceneFilterOptions.normalize(List.of(id)));
    }
}

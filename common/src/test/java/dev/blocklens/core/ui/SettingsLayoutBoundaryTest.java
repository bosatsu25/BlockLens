package dev.blocklens.core.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * CTFL 4.2.2 three-value boundary value analysis and a 4.2.3 decision table for the shared settings
 * layout; see {@code knowledge/current/test-design-ctfl.md} §3 (TD-L*). The existing
 * {@link SettingsLayoutTest} drives representative window sizes; this class drives every boundary
 * the policy declares: accepted dimensions and item counts, the content width cap, the row-height
 * clamp, the scroll clamp, and the four conditions of {@link SettingsLayout#visible}.
 */
final class SettingsLayoutBoundaryTest {
    // 320 x 240 with 21 rows of 48 px: listTop 64, listBottom 190 (span 126), maxScroll 882.
    private static final int SPAN = 126;
    private static final int MAX_SCROLL = 21 * 48 - SPAN;

    @ParameterizedTest(name = "TD-L1 {0}x{1} count={2} accepted={3}")
    @CsvSource({
            // width boundary 160 (3-value), height and count nominal
            "159, 240, 5, false", "160, 240, 5, true", "161, 240, 5, true",
            // height boundary 140
            "320, 139, 5, false", "320, 140, 5, true", "320, 141, 5, true",
            // count lower boundary 0
            "320, 240, -1, false", "320, 240, 0, true", "320, 240, 1, true",
            // count upper boundary 64
            "320, 240, 63, true", "320, 240, 64, true", "320, 240, 65, false",
    })
    void acceptedDimensionsAndCountsAreInclusiveAtEveryDeclaredBoundary(int width, int height, int count,
            boolean accepted) {
        if (accepted) {
            var layout = SettingsLayout.create(width, height, count, 0);
            assertEquals(count, layout.count());
            assertTrue(layout.listTop() < layout.listBottom() || height == 140 || height == 141,
                    "a list must have height except at the very smallest accepted window");
        } else {
            assertThrows(IllegalArgumentException.class, () -> SettingsLayout.create(width, height, count, 0));
        }
    }

    @ParameterizedTest(name = "TD-L2 width {0} -> contentWidth {1}, left {2}")
    @CsvSource({"160, 136, 12", "643, 619, 12", "644, 620, 12", "645, 620, 12", "1920, 620, 650"})
    void contentWidthIsCappedAtSixHundredTwentyAndCentered(int width, int contentWidth, int left) {
        var layout = SettingsLayout.create(width, 480, 5, 0);
        assertEquals(contentWidth, layout.contentWidth());
        assertEquals(left, layout.left());
        assertEquals(contentWidth - 90, layout.nameWidth());
    }

    @ParameterizedTest(name = "TD-L3 requested row height {0} -> {1}")
    @CsvSource({
            // lower clamp 42
            "41, 42", "42, 42", "43, 43",
            // upper clamp: the list span (126 at 320 x 240)
            "125, 125", "126, 126", "127, 126",
    })
    void rowHeightIsClampedBetweenFortyTwoAndTheListSpan(int requested, int rowHeight) {
        var layout = SettingsLayout.create(320, 240, 3, 0, requested);
        assertEquals(SPAN, layout.listBottom() - layout.listTop(), "fixture assumes the 320 x 240 span");
        assertEquals(rowHeight, layout.rowHeight());
    }

    @Test
    void resourceActionsTakeTwentyFourPixelsOffTheListAndNothingElse() {
        var plain = SettingsLayout.create(320, 240, 22, 0, 48, false);
        var actions = SettingsLayout.create(320, 240, 22, 0, 48, true);
        assertEquals(64, plain.listTop());
        assertEquals(88, actions.listTop());
        assertEquals(plain.listBottom(), actions.listBottom());
        assertEquals(plain.footerY(), actions.footerY());
        assertEquals(plain.maxScroll() + 24, actions.maxScroll());
    }

    @ParameterizedTest(name = "TD-L4 requested scroll {0} -> {1}")
    @CsvSource({"-1, 0", "0, 0", "1, 1", "881, 881", "882, 882", "883, 882",
            "-2147483648, 0", "2147483647, 882"})
    void scrollIsClampedToZeroAndMaxScrollInclusive(int requested, int scroll) {
        var layout = SettingsLayout.create(320, 240, 21, requested);
        assertEquals(MAX_SCROLL, layout.maxScroll(), "fixture assumes 21 rows of 48 px at 320 x 240");
        assertEquals(scroll, layout.scroll());
    }

    @Test
    void maxScrollIsZeroWhileEveryRowFitsAndGrowsByOneRowPastTheFit() {
        // Two rows of 48 px fit in a 126 px span; the third pushes 18 px past it.
        assertEquals(0, SettingsLayout.create(320, 240, 2, 0).maxScroll());
        assertEquals(18, SettingsLayout.create(320, 240, 3, 0).maxScroll());
        assertEquals(0, SettingsLayout.create(320, 240, 0, 0).maxScroll());
    }

    /**
     * Decision table for {@code visible(index)} with conditions C1 index >= 0, C2 index < count,
     * C3 rowY >= listTop and C4 rowY + rowHeight <= listBottom. Rules: all true -> visible; each
     * condition false on its own -> hidden. C2 false cannot occur with C3 and C4 true (a row past the
     * last one is always below the list), so that rule is collapsed to C2 false, C4 false.
     */
    @ParameterizedTest(name = "TD-L5 scroll {0} index {1} -> visible {2} ({3})")
    @CsvSource({
            "0, 0, true, all conditions hold with rowY on listTop",
            "882, 20, true, all conditions hold with the row bottom on listBottom",
            "0, -1, false, C1 false",
            "882, 21, false, C2 false and the row is below the list",
            "1, 0, false, C3 false by one pixel",
            "881, 20, false, C4 false by one pixel",
            "882, 17, false, C3 false with C4 true",
            "0, 2, false, C4 false with C3 true",
    })
    void visibilityDecisionTable(int scroll, int index, boolean visible, String rule) {
        var layout = SettingsLayout.create(320, 240, 21, scroll);
        assertEquals(visible, layout.visible(index), rule);
    }

    @Test
    void rowYFollowsScrollLinearly() {
        var layout = SettingsLayout.create(320, 240, 21, 100);
        assertEquals(64 - 100, layout.rowY(0));
        assertEquals(64 - 100 + 48 * 20, layout.rowY(20));
    }

    @ParameterizedTest(name = "TD-L6 scroll {0}, reveal {1} -> {2}")
    @CsvSource({
            // Already visible: the scroll is unchanged.
            "0, 0, 0", "0, 1, 0", "882, 20, 882",
            // One pixel above the top: align the row with listTop.
            "49, 1, 48", "1, 0, 0",
            // One pixel below the bottom: align the row bottom with listBottom.
            "0, 2, 18", "881, 20, 882",
            // Far away: the same alignment, clamped to the scroll range.
            "882, 0, 0", "0, 20, 882",
    })
    void scrollToRevealMovesByTheSmallestAmountAndClamps(int scroll, int index, int expected) {
        var layout = SettingsLayout.create(320, 240, 21, scroll);
        assertEquals(expected, layout.scrollToReveal(index));
        assertTrue(SettingsLayout.create(320, 240, 21, expected).visible(index));
    }

    @Test
    void scrollToRevealRejectsIndexesOutsideTheCategoryAtBothEnds() {
        var layout = SettingsLayout.create(320, 240, 21, 0);
        assertThrows(IllegalArgumentException.class, () -> layout.scrollToReveal(-1));
        assertEquals(0, layout.scrollToReveal(0));
        assertEquals(MAX_SCROLL, layout.scrollToReveal(20));
        assertThrows(IllegalArgumentException.class, () -> layout.scrollToReveal(21));
        var empty = SettingsLayout.create(320, 240, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> empty.scrollToReveal(0));
        assertFalse(empty.visible(0));
    }
}

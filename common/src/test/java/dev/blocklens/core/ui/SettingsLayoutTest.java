package dev.blocklens.core.ui;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class SettingsLayoutTest {
    @Test
    void listAndFooterRemainSeparateAtAllSupportedSizes() {
        for (int[] size : new int[][] {{320,240},{640,360},{854,480},{1920,1080}}) {
            var layout = SettingsLayout.create(size[0], size[1], 21, 0);
            assertTrue(layout.left() >= 0);
            assertTrue(layout.left() + layout.contentWidth() <= size[0]);
            assertTrue(layout.listTop() > 54);
            assertTrue(layout.listBottom() <= layout.footerY() - 22);
            assertTrue(layout.footerY() + 20 <= size[1]);
            assertEquals(0, layout.scroll());
            assertTrue(layout.visible(0));
            assertTrue(layout.nameWidth() >= 160);
            assertTrue(layout.rowHeight() >= 42);
        }
    }

    @Test
    void scrollingClampsAndCanRevealEveryRow() {
        var top = SettingsLayout.create(320, 240, 21, -50);
        assertEquals(0, top.scroll());
        var bottom = SettingsLayout.create(320, 240, 21, Integer.MAX_VALUE);
        assertEquals(bottom.maxScroll(), bottom.scroll());
        assertTrue(bottom.visible(20));
        assertFalse(bottom.visible(0));
        for (int index = 0; index < 21; index++) {
            int scroll = top.scrollToReveal(index);
            assertTrue(SettingsLayout.create(320,240,21,scroll).visible(index));
        }
        assertEquals(0, SettingsLayout.create(854,480,0,500).maxScroll());
        assertThrows(IllegalArgumentException.class, () -> SettingsLayout.create(320,240,-1,0));
    }
}

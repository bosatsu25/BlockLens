package dev.blocklens.core.render;

import dev.blocklens.testing.PaleOakFrameMetrics;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class PaleOakFrameMetricsTest {
    private static BufferedImage frame(int rgb) {
        var image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 360; y++) for (int x = 0; x < 640; x++) {
            int grain = ((x / 4 + y / 5) % 4) * 9;
            int r = (rgb >>> 16 & 255) + grain, g = (rgb >>> 8 & 255) + grain, b = (rgb & 255) + grain;
            image.setRGB(x, y, r << 16 | g << 8 | b);
        }
        return image;
    }
    @Test void neutralBrightnessDriftDoesNotFalselyBreakRestoration() {
        assertTrue(PaleOakFrameMetrics.stable(frame(0xADA2A1), frame(0xB2A7A6)));
        assertTrue(PaleOakFrameMetrics.stable(frame(0xB6B8B6), frame(0xBBBDBB)));
    }
    @Test void paletteLeftEnabledOrMissingChangeStillFails() {
        var off = frame(0xADA2A1); var on = frame(0xA5A6A6);
        assertTrue(PaleOakFrameMetrics.visibleChange(off, on));
        assertFalse(PaleOakFrameMetrics.stable(off, on));
        assertFalse(PaleOakFrameMetrics.visibleChange(off, off));
        assertFalse(PaleOakFrameMetrics.visibleChange(on, off));
    }
    @Test void onlyOneChangedWoodRoleDoesNotEstablishBothRoles() {
        var before = frame(0xADA2A1); var after = frame(0xADA2A1);
        for (int y = 205; y < 239; y++) for (int x = 243; x < 290; x++) after.setRGB(x, y, 0xA5A6A6);
        assertFalse(PaleOakFrameMetrics.visibleChange(before, after));
    }
    @Test void unboundedOrMissingFrameCannotSupplyEvidence() {
        assertThrows(IllegalArgumentException.class, () -> PaleOakFrameMetrics.visibleChange(null, frame(0)));
        assertThrows(IllegalArgumentException.class, () -> PaleOakFrameMetrics.visibleChange(frame(0), new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB)));
    }
    @Test void constantChromaDoesNotHideBlankOrReshuffledWood() {
        var before = frame(0xA6A6A6);
        var black = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        assertFalse(PaleOakFrameMetrics.stable(before, black));
        var solid = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 360; y++) for (int x = 0; x < 640; x++) solid.setRGB(x, y, 0xA6A6A6);
        assertFalse(PaleOakFrameMetrics.stable(before, solid));
        assertFalse(PaleOakFrameMetrics.visibleChange(frame(0xADA2A1), solid));
        var shuffled = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 360; y++) for (int x = 0; x < 640; x++) shuffled.setRGB(x, y, before.getRGB(639 - x, y));
        assertFalse(PaleOakFrameMetrics.stable(before, shuffled));
    }
    @Test void disappearanceInOnlyOneWoodRegionIsRejected() {
        var before = frame(0xA6A6A6); var after = frame(0xA6A6A6);
        for (int y = 205; y < 239; y++) for (int x = 243; x < 290; x++) after.setRGB(x, y, 0xA6A6A6);
        assertFalse(PaleOakFrameMetrics.stable(before, after));
    }
}

package dev.blocklens.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.blocklens.testing.M5BackgroundFloor;
import org.junit.jupiter.api.Test;

final class ExternalM5BackgroundTest {
    private static M5BackgroundFloor.Cell[] corners() {
        return M5BackgroundFloor.viewportCorners(-54 + 1.62, 70, 640, 360, 105, 175, 535, 315);
    }

    @Test
    void originalBoardLeavesUnchangedGrassInsideTheGeometryMask() {
        var original = new M5BackgroundFloor(-8, 8, -3, 5);
        for (var corner : corners()) {
            assertFalse(original.contains(corner));
        }
        assertTrue(corners()[2].z() > original.maxZ(), "The bottom of the ROI sees grass below the old board");
    }

    @Test
    void externalFloorCoversTheFullOriginalRoiWithoutChangingItsBounds() {
        for (var corner : corners()) {
            assertTrue(M5BackgroundFloor.EXTERNAL.contains(corner), () -> "Uncovered ROI corner: " + corner);
        }
    }

    @Test
    void projectionUsesTheCenteredIntegerTeleportCoordinates() {
        // The pinned Minecraft Vec3Argument parser maps "0 -54 13" to (0.5, -54, 13.5).
        assertArrayEquals(new M5BackgroundFloor.Cell[] {
                new M5BackgroundFloor.Cell(-19, -9), new M5BackgroundFloor.Cell(19, -9),
                new M5BackgroundFloor.Cell(-7, 7), new M5BackgroundFloor.Cell(7, 7)}, corners());
    }
}

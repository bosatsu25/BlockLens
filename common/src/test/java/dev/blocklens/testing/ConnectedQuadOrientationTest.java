package dev.blocklens.testing;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
final class ConnectedQuadOrientationTest {
    @Test void allSixFacesKeepVertexUvAndRejectSameBoundsMirrorsAndRotations() {
        // Independently enumerated (position, UV) corners for down/up/north/south/west/east.
        float[][][] faces={
            {{0,0,1,0,0},{1,0,1,1,0},{1,0,0,1,1},{0,0,0,0,1}},
            {{0,1,0,0,0},{1,1,0,1,0},{1,1,1,1,1},{0,1,1,0,1}},
            {{1,1,0,0,0},{0,1,0,1,0},{0,0,0,1,1},{1,0,0,0,1}},
            {{0,1,1,0,0},{1,1,1,1,0},{1,0,1,1,1},{0,0,1,0,1}},
            {{0,1,0,0,0},{0,1,1,1,0},{0,0,1,1,1},{0,0,0,0,1}},
            {{1,1,1,0,0},{1,1,0,1,0},{1,0,0,1,1},{1,0,1,0,1}}};
        for(int face=0;face<6;face++) {
            int mirrored=0,rotated=0;
            for(var point:faces[face]) {
                assertTrue(ConnectedQuadOrientation.matches(face,point[0],point[1],point[2],point[3],point[4]));
                if(!ConnectedQuadOrientation.matches(face,point[0],point[1],point[2],1-point[3],point[4]))mirrored++;
                if(!ConnectedQuadOrientation.matches(face,point[0],point[1],point[2],point[4],1-point[3]))rotated++;
            }
            assertEquals(4,mirrored);assertEquals(4,rotated);
        }
        assertFalse(ConnectedQuadOrientation.matches(6,0,0,0,0,0));
        assertFalse(ConnectedQuadOrientation.matches(3,0,0,0,Float.NaN,0));
    }
}

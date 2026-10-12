package dev.blocklens.testing;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
final class SpatialFrameDifferenceTest {
    private static BufferedImage scene(int kind) {
        var image=new BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<360;y++) for(int x=0;x<640;x++) {
            int r=170,g=170,b=170;
            if(kind!=0 && x>=300 && x<340 && y>=180 && y<225) {
                int grain=75+((x*13+y*7)%125);
                r=grain+20;g=grain+7;b=grain;
                if(kind==2) r=g=b=40;
                if(kind==3) {grain=75+((x*7+y*13)%125);r=grain+20;g=grain+7;b=grain;}
            }
            image.setRGB(x,y,r<<16|g<<8|b);
        }
        return image;
    }
    private static int changed(BufferedImage a,BufferedImage b) {
        return SpatialFrameDifference.changedCells(a,b,280,140,360,270);
    }
    @Test void highFrequencyScreenSpaceNoisePreservesRestoration() {
        var original=scene(1);var noisy=scene(1);
        for(int y=140;y<270;y++) for(int x=280;x<360;x++) {
            int noise=switch(x%3){case 0->12;case 1->-12;default->0;};
            int p=original.getRGB(x,y),r=(p>>>16&255)+noise,g=(p>>>8&255)+noise,b=(p&255)+noise;
            noisy.setRGB(x,y,r<<16|g<<8|b);
        }
        assertTrue(SpatialFrameDifference.matches(original,noisy,280,140,360,270,20));
    }
    @Test void absentSimplifiedAndWrongGrainFramesCannotRestoreBaseline() {
        for(int kind:new int[]{0,2,3}) assertTrue(changed(scene(1),scene(kind))>=20);
        assertEquals(0,changed(scene(1),scene(1)));
    }
    @Test void identicalCellMeansCannotHideFlattenedOrShuffledGrain() {
        var original=scene(1);var flattened=scene(1);var shuffled=scene(1);
        for(int y=140;y+2<270;y+=3) for(int x=280;x+2<360;x+=3) {
            int r=0,g=0,b=0;
            for(int dy=0;dy<3;dy++) for(int dx=0;dx<3;dx++) {
                int p=original.getRGB(x+dx,y+dy);r+=p>>>16&255;g+=p>>>8&255;b+=p&255;
            }
            for(int dy=0;dy<3;dy++) for(int dx=0;dx<3;dx++) {
                flattened.setRGB(x+dx,y+dy,(r/9)<<16|(g/9)<<8|b/9);
                shuffled.setRGB(x+dx,y+dy,original.getRGB(x+2-dx,y+2-dy));
            }
        }
        assertEquals(0,changed(original,flattened));assertEquals(0,changed(original,shuffled));
        assertFalse(SpatialFrameDifference.matches(original,flattened,280,140,360,270,20));
        assertFalse(SpatialFrameDifference.matches(original,shuffled,280,140,360,270,20));
    }
    @Test void flatOriginalCannotAcquireGrayOrEqualLuminanceColorStripes() {
        for(boolean chromatic:new boolean[]{false,true}) {
            var original=scene(0);var striped=scene(0);
            for(int y=140;y<270;y++) for(int x=280;x<360;x++) {
                int shift=switch(x%3){case 0->40;case 1->-40;default->0;};
                int r=170+shift,g=170+(chromatic ? -shift : shift),b=170+(chromatic ? 0 : shift);
                striped.setRGB(x,y,r<<16|g<<8|b);
            }
            assertEquals(0,changed(original,striped));
            assertFalse(SpatialFrameDifference.matches(original,striped,280,140,360,270,20));
        }
    }
    @Test void invalidFramesOrRegionsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->changed(null,scene(1)));
        assertThrows(IllegalArgumentException.class,()->changed(new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB),scene(1)));
        assertThrows(IllegalArgumentException.class,()->SpatialFrameDifference.changedCells(scene(1),scene(1),-1,0,20,20));
        assertThrows(IllegalArgumentException.class,()->SpatialFrameDifference.changedCells(scene(1),scene(1),0,0,641,360));
        assertThrows(IllegalArgumentException.class,()->SpatialFrameDifference.changedCells(scene(1),scene(1),20,20,21,21));
    }
}

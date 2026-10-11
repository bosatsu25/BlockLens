package dev.blocklens.testing;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
final class PaleOakObjectFramesTest {
    private static final PaleOakObjectFrames.Region REGION=new PaleOakObjectFrames.Region(280,140,360,270);
    private static BufferedImage image(int kind) {
        var image=new BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<360;y++) for(int x=0;x<640;x++) image.setRGB(x,y,0xFFCCCCCC);
        if(kind==0) return image;
        for(int y=160;y<220;y++) for(int x=295;x<345;x++) {
            int grain=90+((x*13+y*7)%70);
            int color=kind==1 ? (grain+25)<<16 | (grain+12)<<8 | grain : grain<<16 | grain<<8 | grain;
            if(kind==3) color=0;
            if(kind==4) color=0xFF777777;
            if(kind==5) {grain=90+((x*7+y*13)%70);color=grain<<16|grain<<8|grain;}
            image.setRGB(x,y,color);
        }
        return image;
    }
    @Test void realChromaChangeKeepsForegroundAndGrain() {
        assertTrue(PaleOakObjectFrames.changed(image(1),image(2),image(0),REGION));
        assertTrue(PaleOakObjectFrames.restored(image(1),image(1),image(0),REGION));
    }
    @Test void missingBlackFlatAndReorderedWoodCannotPass() {
        for(int kind:new int[]{0,1,3,4,5}) assertFalse(PaleOakObjectFrames.changed(image(1),image(kind),image(0),REGION));
        assertFalse(PaleOakObjectFrames.changed(image(0),image(2),image(0),REGION));
        assertFalse(PaleOakObjectFrames.restored(image(1),image(2),image(0),REGION));
    }
    @Test void uniformLightingDimmingCannotCertifyPaletteApplication() {
        var off=image(1);var dimmed=new BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<360;y++) for(int x=0;x<640;x++) {
            int p=off.getRGB(x,y),r=(int)((p>>>16&255)*.9),g=(int)((p>>>8&255)*.9),b=(int)((p&255)*.9);
            dimmed.setRGB(x,y,r<<16|g<<8|b);
        }
        assertFalse(PaleOakObjectFrames.changed(off,dimmed,image(0),REGION));
    }
    @Test void boundedRegionsAndFramesAreRequired() {
        assertThrows(IllegalArgumentException.class,()->new PaleOakObjectFrames.Region(-1,0,10,10));
        assertThrows(IllegalArgumentException.class,()->new PaleOakObjectFrames.Region(0,0,641,360));
        assertFalse(PaleOakObjectFrames.changed(new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB),image(2),image(0),REGION));
    }
}

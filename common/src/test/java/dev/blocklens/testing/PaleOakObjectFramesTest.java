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
    @Test void coolShaderIlluminationDoesNotExcludeVisibleWood() {
        var off=new java.awt.image.BufferedImage(640,360,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var on=new java.awt.image.BufferedImage(640,360,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var air=new java.awt.image.BufferedImage(640,360,java.awt.image.BufferedImage.TYPE_INT_RGB);
        for(int y=150;y<190;y++) for(int x=280;x<350;x++) {
            int grain=(x*13+y*7)%40;
            off.setRGB(x,y,(120+grain)<<16|(135+grain)<<8|150+grain);
            on.setRGB(x,y,(110+grain)<<16|(135+grain)<<8|159+grain);
        }
        var region=new PaleOakObjectFrames.Region(220,130,420,250);
        assertTrue(PaleOakObjectFrames.changed(off,on,air,region));
        assertTrue(PaleOakObjectFrames.restored(off,off,air,region));
        assertFalse(PaleOakObjectFrames.changed(off,air,air,region));
    }

    @Test void recoloredSupportCannotCertifyUnchangedWood() {
        var off=image(1);var air=image(0);var on=image(1);
        for(int y=140;y<160;y++) for(int x=280;x<360;x++) {
            int grain=90+(x*13+y*7)%70;
            int support=(grain+25)<<16|(grain+12)<<8|grain;
            off.setRGB(x,y,support);air.setRGB(x,y,support);
            on.setRGB(x,y,grain<<16|grain<<8|grain);
        }
        assertFalse(PaleOakObjectFrames.changed(off,on,air,REGION));
        for(int y=160;y<220;y++) for(int x=295;x<345;x++) {
            int grain=90+(x*13+y*7)%70;
            on.setRGB(x,y,grain<<16|grain<<8|grain);
        }
        assertTrue(PaleOakObjectFrames.changed(off,on,air,REGION));
    }

    @Test void independentSupportWithObjectShadowsCannotContaminateFixedWoodRegion() {
        var off=image(1);var on=image(2);var air=image(0);
        for(int y=140;y<160;y++) for(int x=280;x<360;x++) {
            // The object casts a small support shadow only in the populated scene.
            off.setRGB(x,y,0xFFAAAABB);air.setRGB(x,y,0xFFBBBBCC);
            on.setRGB(x,y,0xFFFFEEEE);
        }
        assertFalse(PaleOakObjectFrames.changed(off,on,air,REGION));
        var wood=new PaleOakObjectFrames.Region(295,160,345,220);
        assertTrue(PaleOakObjectFrames.changed(off,on,air,wood));
        for(int kind:new int[]{0,1,3,4,5})
            assertFalse(PaleOakObjectFrames.changed(off,image(kind),air,wood));
    }
    @Test void blinkingBlankEditorCaretMustNotBeAcceptedAsTextureRestoration() {
        var off=image(1);var blinking=image(1);
        for(int y=170;y<172;y++)for(int x=310;x<320;x++)blinking.setRGB(x,y,0xFF000000);
        assertFalse(PaleOakObjectFrames.restored(off,blinking,image(0),REGION));
        assertTrue(PaleOakObjectFrames.restored(off,off,image(0),REGION));
    }
}

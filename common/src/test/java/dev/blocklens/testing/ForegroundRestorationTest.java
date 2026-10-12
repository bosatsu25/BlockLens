package dev.blocklens.testing;
import java.awt.image.BufferedImage;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
final class ForegroundRestorationTest {
 private static BufferedImage image(boolean object,boolean particle) {
  var image=new BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);
  if(object)for(int y=200;y<235;y++)for(int x=305;x<335;x++)image.setRGB(x,y,0xFF00CCBB);
  if(particle)for(int y=300;y<310;y++)for(int x=285;x<295;x++)image.setRGB(x,y,0xFF33DD55);
  return image;
 }
 @Test void outsideParticlesCannotInvalidateFixedTargetRestoration() {
  var result=ForegroundRestoration.measure(image(true,false),image(true,true),image(false,false),24);
  assertEquals(1050,result.expected());assertEquals(1050,result.restored());assertEquals(1050,result.shared());
 }
 @Test void outsideParticlesCannotCertifyMissingOrRepositionedTarget() {
  var absent=ForegroundRestoration.measure(image(true,false),image(false,true),image(false,false),24);
  assertEquals(0,absent.restored());assertEquals(0,absent.shared());
  var particles=ForegroundRestoration.measure(image(false,true),image(false,true),image(false,false),24);
  assertEquals(0,particles.expected());assertEquals(0,particles.restored());assertEquals(0,particles.shared());
  var moved=image(false,false);for(int y=200;y<235;y++)for(int x=320;x<350;x++)moved.setRGB(x,y,0xFF00CCBB);
  var partial=ForegroundRestoration.measure(image(true,false),moved,image(false,false),24);
  assertEquals(525,partial.shared());assertTrue(partial.shared()*1000<partial.expected()*990);
 }
 @Test void invalidFramesAndDeltaAreRejected() {
  assertThrows(IllegalArgumentException.class,()->ForegroundRestoration.measure(null,image(true,false),image(false,false),24));
  assertThrows(IllegalArgumentException.class,()->ForegroundRestoration.measure(image(true,false),image(true,false),image(false,false),0));
 }
}

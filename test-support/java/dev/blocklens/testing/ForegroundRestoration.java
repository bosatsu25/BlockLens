package dev.blocklens.testing;
import java.awt.image.BufferedImage;
/** Test-only fixed native target silhouette; transient background particles cannot certify a model. */
public final class ForegroundRestoration {
 public record Overlap(int expected,int restored,int shared) { }
 private ForegroundRestoration() { }
 public static Overlap measure(BufferedImage expected,BufferedImage restored,BufferedImage empty,int delta) {
  if(!valid(expected)||!valid(restored)||!valid(empty)||delta<1||delta>765)
   throw new IllegalArgumentException("Invalid native foreground comparison");
  int beforeCount=0,afterCount=0,shared=0;
  for(int y=170;y<270;y++)for(int x=280;x<360;x++) {
   int background=empty.getRGB(x,y);
   boolean before=difference(expected.getRGB(x,y),background)>=delta;
   boolean after=difference(restored.getRGB(x,y),background)>=delta;
   if(before)beforeCount++;if(after)afterCount++;if(before&&after)shared++;
  }
  return new Overlap(beforeCount,afterCount,shared);
 }
 private static boolean valid(BufferedImage image) {return image!=null&&image.getWidth()==640&&image.getHeight()==360;}
 private static int difference(int a,int b) {
  return Math.abs((a>>>16&255)-(b>>>16&255))+Math.abs((a>>>8&255)-(b>>>8&255))+Math.abs((a&255)-(b&255));
 }
}

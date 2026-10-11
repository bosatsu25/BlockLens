package dev.blocklens.testing;
import java.awt.image.BufferedImage;

/** Test-only foreground, chroma and grain proof for fixed synthetic native scenes. */
public final class PaleOakObjectFrames {
    public record Region(int x0,int y0,int x1,int y1) {
        public Region {
            if(x0<0 || y0<0 || x1>640 || y1>360 || x0>=x1 || y0>=y1)
                throw new IllegalArgumentException("Invalid native object region");
        }
    }
    private PaleOakObjectFrames() { }
    public static boolean changed(BufferedImage off,BufferedImage on,BufferedImage air,Region region) {
        var stats=measure(off,on,air,region);
        return stats!=null && stats.count>=80 && stats.retained*100>=stats.count*95
                && (stats.warmA-stats.warmB)/stats.count>=2
                && (stats.chromaA-stats.chromaB)/stats.count>=4 && stats.pattern(.9)
                && Math.abs(stats.sumA-stats.sumB)/stats.count<=48;
    }
    public static boolean restored(BufferedImage off,BufferedImage restored,BufferedImage air,Region region) {
        var stats=measure(off,restored,air,region);
        return stats!=null && stats.count>=80 && stats.retained*100>=stats.count*99
                && Math.abs(stats.warmA-stats.warmB)/stats.count<=.5 && stats.pattern(.99)
                && stats.absoluteDifference/stats.count<=4;
    }
    private static Stats measure(BufferedImage a,BufferedImage b,BufferedImage air,Region region) {
        if(!valid(a) || !valid(b) || !valid(air) || region==null) return null;
        var stats=new Stats();
        for(int y=region.y0;y<region.y1;y++) for(int x=region.x0;x<region.x1;x++) {
            int p=a.getRGB(x,y),q=b.getRGB(x,y),empty=air.getRGB(x,y);
            int r=p>>>16&255,g=p>>>8&255,blue=p&255;
            if(blue<=20 || r<g || g<blue || r-blue<8 || r-blue>70 || difference(p,empty)<24) continue;
            int s=q>>>16&255,t=q>>>8&255,u=q&255;
            double first=r+g+blue,second=s+t+u;
            stats.count++;if(difference(q,empty)>=24) stats.retained++;
            stats.sumA+=first;stats.sumB+=second;stats.squareA+=first*first;stats.squareB+=second*second;
            stats.product+=first*second;stats.warmA+=r-blue;stats.warmB+=s-u;
            // Normalizing by brightness rejects uniform lighting-only darkening.
            stats.chromaA+=1000.0*(r-blue)/first;
            stats.chromaB+=second==0 ? 0 : 1000.0*(s-u)/second;
            stats.absoluteDifference+=difference(p,q);
        }
        return stats;
    }
    private static boolean valid(BufferedImage image) { return image!=null && image.getWidth()==640 && image.getHeight()==360; }
    private static int difference(int a,int b) {
        return Math.abs((a>>>16&255)-(b>>>16&255))+Math.abs((a>>>8&255)-(b>>>8&255))+Math.abs((a&255)-(b&255));
    }
    private static final class Stats {
        int count,retained;
        double sumA,sumB,squareA,squareB,product,warmA,warmB,chromaA,chromaB,absoluteDifference;
        boolean pattern(double minimumCorrelation) {
            if(count==0) return false;
            double first=squareA-sumA*sumA/count,second=squareB-sumB*sumB/count;
            if(first<64*count || second<64*count || second<first*.5 || second>first*2) return false;
            return (product-sumA*sumB/count)/Math.sqrt(first*second)>=minimumCorrelation;
        }
    }
}

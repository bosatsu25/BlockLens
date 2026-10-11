package dev.blocklens.testing;
import java.awt.image.BufferedImage;

/** Test-only temporal-noise comparison; raw within-cell detail must also survive. */
public final class SpatialFrameDifference {
    private SpatialFrameDifference() { }
    public static int changedCells(BufferedImage a,BufferedImage b,int x0,int y0,int x1,int y1) {
        return measure(a,b,x0,y0,x1,y1).changed;
    }
    public static boolean matches(BufferedImage a,BufferedImage b,int x0,int y0,int x1,int y1,int maximumChanged) {
        if(maximumChanged<=0) throw new IllegalArgumentException("Positive cell bound required");
        var stats=measure(a,b,x0,y0,x1,y1);
        if(stats.unstable>=maximumChanged) return false;
        if(stats.detailA==0) return true; // A flat original has no within-cell grain to preserve.
        return stats.detailB>=stats.detailA*.5 && stats.detailB<=stats.detailA*2
                && stats.product/Math.sqrt(stats.detailA*stats.detailB)>=.85;
    }
    private static Stats measure(BufferedImage a,BufferedImage b,int x0,int y0,int x1,int y1) {
        if(!valid(a) || !valid(b) || x0<0 || y0<0 || x1>640 || y1>360 || x0>=x1 || y0>=y1 || x1-x0<3 || y1-y0<3)
            throw new IllegalArgumentException("Expected bounded native frames and region");
        var stats=new Stats();
        for(int y=y0;y+2<y1;y+=3) for(int x=x0;x+2<x1;x+=3) {
            int redA=0,greenA=0,blueA=0,redB=0,greenB=0,blueB=0;
            double squareA=0,squareB=0,product=0;
            for(int dy=0;dy<3;dy++) for(int dx=0;dx<3;dx++) {
                int p=a.getRGB(x+dx,y+dy),q=b.getRGB(x+dx,y+dy);
                int r=p>>>16&255,g=p>>>8&255,u=p&255,s=q>>>16&255,t=q>>>8&255,v=q&255;
                redA+=r;greenA+=g;blueA+=u;redB+=s;greenB+=t;blueB+=v;
                squareA+=r*r+g*g+u*u;squareB+=s*s+t*t+v*v;product+=r*s+g*t+u*v;
            }
            boolean changed=Math.abs(redA-redB)+Math.abs(greenA-greenB)+Math.abs(blueA-blueB)>=24*9;
            if(changed) stats.changed++;
            double varianceA=squareA-(redA*redA+greenA*greenA+blueA*blueA)/9.0;
            double varianceB=squareB-(redB*redB+greenB*greenB+blueB*blueB)/9.0;
            double covariance=product-(redA*redB+greenA*greenB+blueA*blueB)/9.0;
            // Bound per-channel residual RMS to 16, including originally flat cells.
            // Equal-luminance stripes must not disappear in a summed-brightness comparison.
            if(changed || varianceA+varianceB-2*covariance>=256*3*9) stats.unstable++;
            if(varianceA>=64*9) {
                stats.detailA+=varianceA;stats.detailB+=varianceB;stats.product+=covariance;
            }
        }
        return stats;
    }
    private static boolean valid(BufferedImage a) {return a!=null && a.getWidth()==640 && a.getHeight()==360;}
    private static final class Stats {int changed,unstable;double detailA,detailB,product;}
}

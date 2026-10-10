package dev.blocklens.testing;

/** Brightness-normalized hue match for alpha/antialias coverage on a dark controlled framebuffer. */
public final class CueColorEvidence {
    private CueColorEvidence() { }
    public static boolean matches(int actual,int cue) {
        int r=actual>>16&255,g=actual>>8&255,b=actual&255;
        int intensity=Math.max(r,Math.max(g,b));
        if(intensity<80) return false;
        return Math.abs(r*255.0/intensity-(cue>>16&255))<=18
                && Math.abs(g*255.0/intensity-(cue>>8&255))<=18
                && Math.abs(b*255.0/intensity-(cue&255))<=18;
    }
}
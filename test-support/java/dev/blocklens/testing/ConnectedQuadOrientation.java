package dev.blocklens.testing;
/** Independent test oracle for unrotated Minecraft cube face UV coordinates. */
public final class ConnectedQuadOrientation {
    private ConnectedQuadOrientation() { }
    public static boolean matches(int face,float x,float y,float z,float u,float v) {
        if(face<0 || face>5 || !Float.isFinite(x+y+z+u+v))return false;
        float a=x<.5f ? 0 : 1,b=y<.5f ? 0 : 1,c=z<.5f ? 0 : 1;
        float expectedU=switch(face){case 2->1-a;case 4->c;case 5->1-c;default->a;};
        float expectedV=switch(face){case 0->1-c;case 1->c;default->1-b;};
        return Math.abs(u-expectedU)<.001f && Math.abs(v-expectedV)<.001f;
    }
}

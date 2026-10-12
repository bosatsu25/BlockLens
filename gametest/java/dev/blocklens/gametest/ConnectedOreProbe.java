package dev.blocklens.gametest;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Test-only observations from both sides of the actual connected border emission path. */
public final class ConnectedOreProbe {
    private static final Map<Integer,Set<Integer>> MASKS=new ConcurrentHashMap<>();
    private static volatile boolean active;
    private static volatile int anchor;
    private ConnectedOreProbe() { }
    public static void reset(){reset(0);}
    public static void reset(int x){active=false;MASKS.clear();anchor=x;active=true;}
    public static void stop(){active=false;MASKS.clear();}
    public static void record(int x,int y,int z,int face,int mask){
        if(active && (x==anchor || x==anchor+1) && y==-59 && z==4 && face==3)
            MASKS.computeIfAbsent(x-anchor,key->ConcurrentHashMap.newKeySet()).add(mask);
    }
    public static boolean only(int expected){return onlyAt(0,expected);}
    public static boolean onlyAt(int offset,int expected){var seen=MASKS.get(offset);return seen!=null && seen.size()==1 && seen.contains(expected);}
    public static boolean absentAt(int offset){return !MASKS.containsKey(offset);}
    public static String observed(){return MASKS.toString();}
}

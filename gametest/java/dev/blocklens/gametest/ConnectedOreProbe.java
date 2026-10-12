package dev.blocklens.gametest;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Test-only observations from both sides of the actual connected border emission path. */
public final class ConnectedOreProbe {
    private static final Map<Integer,Set<Integer>> MASKS=new ConcurrentHashMap<>();
    private static final Map<Integer,Set<Integer>> CELLS=new ConcurrentHashMap<>();
    private static volatile boolean validUv;
    private static volatile boolean active;
    private static volatile int anchor;
    private ConnectedOreProbe() { }
    public static void reset(){reset(0);}
    public static void reset(int x){active=false;MASKS.clear();CELLS.clear();validUv=true;anchor=x;active=true;}
    public static void stop(){active=false;MASKS.clear();CELLS.clear();}
    public static void record(int x,int y,int z,int face,int mask){
        if(active && (x==anchor || x==anchor+1) && y==-59 && z==4 && face==3)
            MASKS.computeIfAbsent(x-anchor,key->ConcurrentHashMap.newKeySet()).add(mask);
    }
    public static boolean active(){return active;}
    public static void recordCell(int x,int y,int z,int face,int mask,boolean valid){
        if(active && (x==anchor || x==anchor+1) && y==-59 && z==4 && face==3){
            if(!valid)validUv=false;
            CELLS.computeIfAbsent(x-anchor,key->ConcurrentHashMap.newKeySet()).add(mask);
        }
    }
    public static boolean cellAt(int offset,int mask){var cells=CELLS.get(offset);return validUv && cells!=null && cells.size()==1 && cells.contains(mask);}
    public static boolean only(int expected){return onlyAt(0,expected);}
    public static boolean onlyAt(int offset,int expected){var seen=MASKS.get(offset);return seen!=null && seen.size()==1 && seen.contains(expected);}
    public static boolean absentAt(int offset){return !MASKS.containsKey(offset);}
    public static String observed(){return MASKS.toString();}
}

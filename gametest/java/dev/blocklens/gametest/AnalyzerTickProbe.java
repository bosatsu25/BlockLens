package dev.blocklens.gametest;

import dev.blocklens.core.analyzer.AnalyzerEngine;
import dev.blocklens.fabric.AnalyzerClient;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

/** Test-only bounded samples of the actual analyzer tick span, including sampler overhead. */
public final class AnalyzerTickProbe {
    private static final com.sun.management.ThreadMXBean BEAN=ManagementFactory.getThreadMXBean()
            instanceof com.sun.management.ThreadMXBean value?value:null;
    private static final long[] NANOS=new long[256],BYTES=new long[256];
    private static boolean active;
    private static long started,allocated;
    private static int count,maxCells,maxSections,maxSteps,maxEntitySections,maxEntities;
    private AnalyzerTickProbe() { }
    public static void startTick() {
        if(!active) return;
        allocated=BEAN.getCurrentThreadAllocatedBytes();started=System.nanoTime();
    }
    public static void endTick() {
        if(!active) return;
        long nanos=System.nanoTime()-started,bytes=BEAN.getCurrentThreadAllocatedBytes()-allocated;
        if(count>=NANOS.length || nanos<0 || bytes<0) throw new AssertionError("invalid bounded analyzer sample");
        NANOS[count]=nanos;BYTES[count++]=bytes;
        var work=AnalyzerClient.engine().work();
        maxCells=Math.max(maxCells,work.cells());maxSections=Math.max(maxSections,work.sections());
        maxSteps=Math.max(maxSteps,work.cursorSteps());maxEntitySections=Math.max(maxEntitySections,work.entitySections());
        maxEntities=Math.max(maxEntities,work.entities());
        if(work.cells()>512 || work.sections()>64 || work.cursorSteps()>4096 || work.entitySections()>125 || work.entities()>64)
            throw new AssertionError("actual analyzer tick exceeded its work budget");
    }
    static void begin() {
        if(BEAN==null || !BEAN.isThreadAllocatedMemorySupported()) throw new AssertionError("analyzer allocation counter unavailable");
        if(!BEAN.isThreadAllocatedMemoryEnabled()) BEAN.setThreadAllocatedMemoryEnabled(true);
        count=maxCells=maxSections=maxSteps=maxEntitySections=maxEntities=0;active=true;
    }
    static String end(String prefix,boolean off) {
        active=false;
        if(count==0 || off && maxCells+maxSections+maxSteps+maxEntitySections+maxEntities!=0)
            throw new AssertionError("OFF analyzer did work or analyzer samples were empty");
        return prefix+"Ticks="+count+'\n'+prefix+"MedianNanos="+median(NANOS)+'\n'
                +prefix+"MedianAllocatedBytes="+median(BYTES)+'\n'+prefix+"MaxCells="+maxCells+'\n'
                +prefix+"MaxPaletteSections="+maxSections+'\n'+prefix+"MaxCursorSteps="+maxSteps+'\n'
                +prefix+"MaxEntitySections="+maxEntitySections+'\n'+prefix+"MaxEntities="+maxEntities+'\n';
    }
    static void stop() { active=false; }
    private static long median(long[] source) {
        long[] values=Arrays.copyOf(source,count);Arrays.sort(values);return values[count/2];
    }
}
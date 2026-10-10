package dev.blocklens.gametest;

import dev.blocklens.testing.AllocationDeltas;
import dev.blocklens.testing.StageAllocationCounters;
import java.lang.management.ManagementFactory;

/** Test-only normal-return compiler attribution; no thread identity, coordinates or raw recording. */
public final class SectionCompileAllocationProbe {
    private static final Object LOCK=new Object();
    private static final StageAllocationCounters COUNTERS=new StageAllocationCounters();
    private static final Call[] CALLS=new Call[StageAllocationCounters.MAX_SLOTS];
    private static int claimed;
    private static volatile boolean capture;
    private static com.sun.management.ThreadMXBean bean;
    private static final ThreadLocal<Call> LOCAL=ThreadLocal.withInitial(SectionCompileAllocationProbe::claim);
    static {
        for(int i=0;i<CALLS.length;i++) CALLS[i]=new Call(COUNTERS.claimSlot());
    }
    private static final class Call {
        final StageAllocationCounters.Slot slot;
        long token;
        Call(StageAllocationCounters.Slot slot) {this.slot=slot;}
    }
    private SectionCompileAllocationProbe() { }
    private static Call claim() {
        synchronized(LOCK) {return claimed<CALLS.length?CALLS[claimed++]:null;}
    }
    // The verified vanilla compile method is non-reentrant. Overlap is rejected and appears
    // in dropped/incomplete counters rather than being presented as a complete measurement.
    public static void head() {
        synchronized(LOCK) {
            if(!capture) return;
            long before=bean.getCurrentThreadAllocatedBytes();
            var call=LOCAL.get();
            int role=AllocationDeltas.role(Thread.currentThread().getName());
            long token=COUNTERS.head(call==null?null:call.slot,role,before);
            if(call!=null) call.token=token;
        }
    }
    public static void returned() {
        synchronized(LOCK) {
            if(!capture) return;
            long after=bean.getCurrentThreadAllocatedBytes();
            var call=LOCAL.get();
            if(call!=null) COUNTERS.returned(call.slot,call.token,after);
        }
    }
    static void begin() {
        synchronized(LOCK) {
            bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
            COUNTERS.begin();capture=true;
        }
    }
    static StageAllocationCounters.Snapshot end() {
        synchronized(LOCK) {capture=false;return COUNTERS.end();}
    }
}

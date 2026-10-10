package dev.blocklens.testing;

/** Test-only fixed-role totals; thread names and IDs never leave this calculation. */
public final class AllocationDeltas {
    public static final int MAX_THREADS=512;
    private AllocationDeltas() { }
    public static long[] byRole(long[] beforeIds,long[] beforeBytes,long[] afterIds,long[] afterBytes,String[] names) {
        if(beforeIds.length!=beforeBytes.length || afterIds.length!=afterBytes.length
                || afterIds.length!=names.length || beforeIds.length>MAX_THREADS || afterIds.length>MAX_THREADS) {
            throw new IllegalArgumentException("invalid or oversized allocation snapshot");
        }
        long[] totals=new long[5];
        for(int i=0;i<afterIds.length;i++) {
            long after=afterBytes[i];
            if(after<0) continue;
            long before=0;
            for(int j=0;j<beforeIds.length;j++) if(beforeIds[j]==afterIds[i]) { before=beforeBytes[j];break; }
            if(before<0 || after<before) continue;
            long delta=after-before;
            int role=role(names[i]);
            totals[role]=totals[role]>Long.MAX_VALUE-delta?Long.MAX_VALUE:totals[role]+delta;
        }
        return totals;
    }
    private static int role(String name) {
        if(name==null) return 4;
        if(name.contains("Render")) return 0;
        if(name.contains("Chunk")) return 1;
        if(name.startsWith("Worker-Main-") || name.startsWith("ForkJoinPool-")) return 2;
        return name.contains("Test thread")?3:4;
    }
}

package dev.blocklens.testing;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class AllocationDeltasTest {
    @Test void groupsOnlyFixedRolesAndNeverExportsNamesOrIds() {
        long[] result=AllocationDeltas.byRole(new long[]{1,2},new long[]{100,200},
                new long[]{1,2,3,4,5},new long[]{150,230,40,20,10},
                new String[]{"Render thread","Chunk builder","Worker-Main-1","Test thread","private synthetic account"});
        assertArrayEquals(new long[]{50,30,40,20,10},result);
    }
    @Test void terminatedAndUnsupportedCountersAreExcludedAndNewThreadsStartAtZero() {
        long[] result=AllocationDeltas.byRole(new long[]{1,2,3},new long[]{100,-1,300},
                new long[]{1,2,4,5},new long[]{90,200,50,-1},
                new String[]{"Render thread","Render thread","ForkJoinPool-1-worker-1","Chunk builder"});
        assertArrayEquals(new long[]{0,0,50,0,0},result);
    }
    @Test void invalidInputsFailInsteadOfSilentlyTruncatingThreadCoverage() {
        assertThrows(IllegalArgumentException.class,()->AllocationDeltas.byRole(new long[513],new long[513],
                new long[0],new long[0],new String[0]));
        assertThrows(IllegalArgumentException.class,()->AllocationDeltas.byRole(new long[0],new long[1],
                new long[0],new long[0],new String[0]));
        assertThrows(IllegalArgumentException.class,()->AllocationDeltas.byRole(new long[0],new long[0],
                new long[1],new long[0],new String[1]));
    }
    @Test void totalsSaturateAndUnknownNamesUseOtherRole() {
        assertArrayEquals(new long[]{0,0,0,0,Long.MAX_VALUE},AllocationDeltas.byRole(new long[0],new long[0],
                new long[]{1,2},new long[]{Long.MAX_VALUE,10},new String[]{null,"unknown"}));
    }
}

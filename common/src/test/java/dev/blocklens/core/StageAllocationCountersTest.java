package dev.blocklens.core;

import dev.blocklens.testing.StageAllocationCounters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class StageAllocationCountersTest {
    @Test void completedAllocationsRemainSeparatedAcrossAllFiveRoles() {
        var counters = new StageAllocationCounters();
        long epoch = counters.begin();
        for (int role = 0; role < 5; role++) {
            var slot = counters.claimSlot();
            long token = counters.head(slot, role, 100);
            assertEquals(epoch, token);
            counters.returned(slot, token, 110 + 10L * role);
        }
        var worker = counters.claimSlot();
        counters.returned(worker, counters.head(worker, 2, 400), 445);
        var snapshot = counters.end();
        assertArrayEquals(new long[] {10, 20, 75, 40, 50}, snapshot.bytesByRole());
        assertArrayEquals(new long[] {1, 1, 2, 1, 1}, snapshot.completedByRole());
        assertEquals(0, snapshot.incomplete());
        assertEquals(0, snapshot.dropped());
        assertEquals(0, snapshot.invalid());
        assertEquals(0, snapshot.saturations());
    }

    @Test void lateReturnCannotCompleteOrClearTheNextWindowsSample() {
        var counters = new StageAllocationCounters();
        var slot = counters.claimSlot();
        long firstEpoch = counters.begin();
        long oldToken = counters.head(slot, 2, 100);
        var first = counters.end();
        assertEquals(1, first.incomplete());
        long secondEpoch = counters.begin();
        assertNotEquals(firstEpoch, secondEpoch);
        long newToken = counters.head(slot, 2, 300);
        counters.returned(slot, oldToken, 310);
        counters.returned(slot, newToken, 325);
        var second = counters.end();
        assertArrayEquals(new long[] {0, 0, 25, 0, 0}, second.bytesByRole());
        assertArrayEquals(new long[] {0, 0, 1, 0, 0}, second.completedByRole());
        assertEquals(0, second.incomplete());
        assertArrayEquals(new long[5], first.bytesByRole());
        assertEquals(1, first.incomplete());
    }

    @Test void callbacksOutsideAWindowDoNotCreateSamplesOrDiagnostics() {
        var counters = new StageAllocationCounters();
        var slot = counters.claimSlot();
        assertEquals(0, counters.head(slot, -1, -1));
        counters.begin();
        counters.returned(slot, 0, 200);
        var first = counters.end();
        assertEquals(0, counters.head(null, -1, -1));
        counters.returned(slot, first.epoch(), -1);
        var repeated = counters.end();
        assertArrayEquals(new long[5], repeated.bytesByRole());
        assertArrayEquals(new long[5], repeated.completedByRole());
        assertEquals(0, repeated.incomplete());
        assertEquals(0, repeated.invalid());
        assertEquals(0, repeated.dropped());
    }

    @Test void exactly512SlotsCanBeClaimedAndAreNeverReissued() {
        var counters = new StageAllocationCounters();
        var slots = new java.util.HashSet<StageAllocationCounters.Slot>();
        StageAllocationCounters.Slot last = null;
        for (int i = 0; i < 512; i++) {
            last = counters.claimSlot();
            assertNotNull(last);
            assertTrue(slots.add(last));
        }
        assertNull(counters.claimSlot());
        counters.begin();
        assertEquals(0, counters.head(null, 2, 100));
        counters.returned(last, counters.head(last, 2, 100), 107);
        var first = counters.end();
        assertEquals(1, first.dropped());
        assertEquals(7, first.bytesByRole()[2]);
        counters.begin();
        assertNull(counters.claimSlot());
        counters.returned(last, counters.head(last, 2, 200), 209);
        assertEquals(9, counters.end().bytesByRole()[2]);
    }

    @Test void unavailableAndBackwardsCountersAreRejectedAndReleaseTheSlot() {
        var counters = new StageAllocationCounters();
        var slot = counters.claimSlot();
        counters.begin();
        assertEquals(0, counters.head(slot, 2, -1));
        counters.returned(slot, counters.head(slot, 2, 20), -1);
        counters.returned(slot, counters.head(slot, 2, 30), 29);
        counters.returned(slot, counters.head(slot, 2, 40), 40);
        var snapshot = counters.end();
        assertEquals(3, snapshot.invalid());
        assertEquals(0, snapshot.incomplete());
        assertEquals(0, snapshot.dropped());
        assertEquals(1, snapshot.completedByRole()[2]);
        assertEquals(0, snapshot.bytesByRole()[2]);
    }

    @Test void invalidRolesAndForeignSlotsCannotCorruptAnAcceptedSample() {
        var counters = new StageAllocationCounters();
        var own = counters.claimSlot();
        var foreign = new StageAllocationCounters().claimSlot();
        counters.begin();
        assertEquals(0, counters.head(own, -1, 10));
        assertEquals(0, counters.head(own, 5, 10));
        assertEquals(0, counters.head(foreign, 2, 10));
        counters.returned(own, counters.head(own, 2, 10), 12);
        var snapshot = counters.end();
        assertEquals(3, snapshot.invalid());
        assertEquals(2, snapshot.bytesByRole()[2]);
        assertEquals(1, snapshot.completedByRole()[2]);
    }

    @Test void allocationOverflowSaturatesWithoutWrappingOrLosingCompletionCounts() {
        var counters = new StageAllocationCounters();
        var slot = counters.claimSlot();
        counters.begin();
        counters.returned(slot, counters.head(slot, 2, 0), Long.MAX_VALUE);
        counters.returned(slot, counters.head(slot, 2, 0), 5);
        var snapshot = counters.end();
        assertEquals(Long.MAX_VALUE, snapshot.bytesByRole()[2]);
        assertEquals(2, snapshot.completedByRole()[2]);
        assertEquals(1, snapshot.saturations());
    }

    @Test void endingAWindowClearsUnfinishedSamplesAndFreezesIndependentArrays() {
        var counters = new StageAllocationCounters();
        var unfinished = counters.claimSlot();
        var completed = counters.claimSlot();
        counters.begin();
        counters.head(unfinished, 2, 1);
        counters.returned(completed, counters.head(completed, 0, 10), 16);
        var first = counters.end();
        first.bytesByRole()[0] = 900;
        first.completedByRole()[0] = 900;
        assertEquals(6, first.bytesByRole()[0]);
        assertEquals(1, first.completedByRole()[0]);
        counters.begin();
        counters.returned(unfinished, counters.head(unfinished, 1, 100), 120);
        var second = counters.end();
        assertEquals(20, second.bytesByRole()[1]);
        assertEquals(0, second.incomplete());
        assertEquals(1, first.incomplete());
        assertEquals(6, first.bytesByRole()[0]);
    }

    @Test void overlappingHeadIsDroppedWithoutDoubleCountingTheOuterSample() {
        var counters = new StageAllocationCounters();
        var slot = counters.claimSlot();
        counters.begin();
        long outer = counters.head(slot, 2, 100);
        long nested = counters.head(slot, 2, 110);
        assertEquals(0, nested);
        counters.returned(slot, nested, 115);
        counters.returned(slot, outer, 130);
        var snapshot = counters.end();
        assertEquals(30, snapshot.bytesByRole()[2]);
        assertEquals(1, snapshot.completedByRole()[2]);
        assertEquals(1, snapshot.dropped());
    }

    @Test void aSecondBeginCannotSilentlyDiscardAnActiveWindowsEvidence() {
        var counters = new StageAllocationCounters();
        counters.begin();
        assertThrows(IllegalStateException.class, counters::begin);
        assertEquals(0, counters.end().incomplete());
    }
}

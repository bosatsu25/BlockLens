package dev.blocklens.testing;

import java.util.Arrays;

/** Test-only counters; no thread identity, names, world state or per-callback allocation. */
public final class StageAllocationCounters {
    public static final int MAX_SLOTS = 512;
    public static final int ROLE_COUNT = 5;

    /** Opaque lifetime lease. Every field is primitive; leases are never reassigned. */
    public static final class Slot {
        private final int index;
        private long epoch;
        private long start;
        private int role;

        private Slot(int index) { this.index = index; }
        private void clear() { epoch = 0; start = 0; role = 0; }
    }

    /** Frozen window totals. Returned arrays cannot mutate this evidence or later windows. */
    public record Snapshot(long epoch, long[] bytesByRole, long[] completedByRole,
                           long incomplete, long dropped, long invalid, long saturations) {
        public Snapshot {
            bytesByRole = bytesByRole.clone();
            completedByRole = completedByRole.clone();
        }
        @Override public long[] bytesByRole() { return bytesByRole.clone(); }
        @Override public long[] completedByRole() { return completedByRole.clone(); }
    }

    private final Slot[] slots = new Slot[MAX_SLOTS];
    private final long[] bytes = new long[ROLE_COUNT];
    private final long[] completed = new long[ROLE_COUNT];
    private int claimed;
    private long epoch;
    private boolean active;
    private long incomplete, dropped, invalid, saturations;
    private Snapshot frozen = new Snapshot(0, bytes, completed, 0, 0, 0, 0);

    public StageAllocationCounters() {
        for (int i = 0; i < slots.length; i++) slots[i] = new Slot(i);
    }

    /** Claim once per caller lifetime, including outside windows; null means capacity exhausted. */
    public synchronized Slot claimSlot() {
        return claimed < MAX_SLOTS ? slots[claimed++] : null;
    }

    /** A window must be ended before another begins; epoch zero is never a valid token. */
    public synchronized long begin() {
        if (active) throw new IllegalStateException("allocation window already active");
        if (epoch == Long.MAX_VALUE) throw new IllegalStateException("allocation epoch exhausted");
        epoch++;
        Arrays.fill(bytes, 0);
        Arrays.fill(completed, 0);
        incomplete = dropped = invalid = saturations = 0;
        active = true;
        return epoch;
    }

    /** Returns an epoch token, or zero when inactive/rejected. Keep the token until RETURN. */
    public synchronized long head(Slot slot, int role, long allocatedBytes) {
        if (!active) return 0;
        if (slot == null) {
            dropped = add(dropped, 1);
            return 0;
        }
        if (!owned(slot) || role < 0 || role >= ROLE_COUNT || allocatedBytes < 0) {
            invalid = add(invalid, 1);
            return 0;
        }
        if (slot.epoch != 0) {
            // An overlapping call may be included in its outer sample, but never counted twice.
            dropped = add(dropped, 1);
            return 0;
        }
        slot.epoch = epoch;
        slot.start = allocatedBytes;
        slot.role = role;
        return epoch;
    }

    /** Only a matching, normally completed call inside the current window contributes bytes. */
    public synchronized void returned(Slot slot, long token, long allocatedBytes) {
        if (!active || token == 0 || token != epoch) return;
        if (slot == null || !owned(slot)) {
            invalid = add(invalid, 1);
            return;
        }
        if (slot.epoch != token) return;
        if (allocatedBytes < 0 || allocatedBytes < slot.start) {
            invalid = add(invalid, 1);
        } else {
            bytes[slot.role] = add(bytes[slot.role], allocatedBytes - slot.start);
            completed[slot.role] = add(completed[slot.role], 1);
        }
        slot.clear();
    }

    /** Freeze once, count unfinished matching calls and clear their leases' sample state. */
    public synchronized Snapshot end() {
        if (!active) return frozen;
        active = false;
        for (int i = 0; i < claimed; i++) {
            if (slots[i].epoch == epoch) {
                incomplete = add(incomplete, 1);
                slots[i].clear();
            }
        }
        frozen = new Snapshot(epoch, bytes, completed, incomplete, dropped, invalid, saturations);
        return frozen;
    }

    private boolean owned(Slot slot) {
        return slot.index < claimed && slots[slot.index] == slot;
    }

    private long add(long value, long increment) {
        if (value > Long.MAX_VALUE - increment) {
            if (saturations != Long.MAX_VALUE) saturations++;
            return Long.MAX_VALUE;
        }
        return value + increment;
    }
}

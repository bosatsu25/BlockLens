package dev.blocklens.core.state;

import dev.blocklens.core.CapabilityId;
import java.util.Objects;

/**
 * Fixed-size O(1) capability lookup keyed by a version adapter's session-local raw target id.
 *
 * <p>Each target stores a capability bit-set rather than a single capability because BlockLens
 * capabilities are intentionally composable. For example, obsidian can participate in both the
 * dedicated obsidian highlight and Nether Tweaks, while crimson/warped stems can participate in
 * both log orientation and Nether Tweaks. The index never scans Minecraft state at lookup time,
 * cannot grow after build(), and performs no hot-path collection allocation.</p>
 */
public final class TargetCapabilityIndex {
    private final long[] masksByRawId;

    private TargetCapabilityIndex(long[] masksByRawId) {
        this.masksByRawId = masksByRawId;
    }

    public static Builder builder(int targetCount) {
        return new Builder(targetCount);
    }

    /** Returns the immutable capability mask for a target, or zero for an invalid/unmapped id. */
    public long mask(int rawId) {
        return rawId >= 0 && rawId < masksByRawId.length ? masksByRawId[rawId] : 0L;
    }

    /** O(1) membership check without allocating a capability collection. */
    public boolean contains(int rawId, CapabilityId capability) {
        Objects.requireNonNull(capability, "capability");
        return (mask(rawId) & bit(capability)) != 0L;
    }

    public int capacity() {
        return masksByRawId.length;
    }

    /** Number of raw targets with at least one capability binding. */
    public int mappedCount() {
        int count = 0;
        for (long mask : masksByRawId) {
            if (mask != 0L) {
                count++;
            }
        }
        return count;
    }

    /** Number of target-to-capability bindings, including overlaps on the same target. */
    public int bindingCount() {
        int count = 0;
        for (long mask : masksByRawId) {
            count += Long.bitCount(mask);
        }
        return count;
    }

    private static long bit(CapabilityId capability) {
        int ordinal = capability.ordinal();
        if (ordinal >= Long.SIZE) {
            throw new IllegalStateException("CapabilityId count exceeds 64-bit target index capacity");
        }
        return 1L << ordinal;
    }

    public static final class Builder {
        private long[] masksByRawId;

        private Builder(int targetCount) {
            if (targetCount < 0) {
                throw new IllegalArgumentException("targetCount must be >= 0");
            }
            masksByRawId = new long[targetCount];
        }

        /** Adds a composable capability binding. Rebinding the same pair is idempotent. */
        public Builder bind(int rawId, CapabilityId capability) {
            long[] entries = requireMutable();
            if (rawId < 0 || rawId >= entries.length) {
                throw new IndexOutOfBoundsException("rawId=" + rawId + ", capacity=" + entries.length);
            }
            Objects.requireNonNull(capability, "capability");
            entries[rawId] |= bit(capability);
            return this;
        }

        public TargetCapabilityIndex build() {
            long[] entries = requireMutable();
            masksByRawId = null;
            return new TargetCapabilityIndex(entries.clone());
        }

        private long[] requireMutable() {
            if (masksByRawId == null) {
                throw new IllegalStateException("builder already consumed");
            }
            return masksByRawId;
        }
    }
}

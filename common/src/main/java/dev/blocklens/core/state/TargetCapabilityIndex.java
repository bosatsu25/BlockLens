package dev.blocklens.core.state;

import dev.blocklens.core.CapabilityId;
import java.util.Arrays;
import java.util.Objects;

/**
 * Fixed-size O(1) capability lookup keyed by a version adapter's session-local raw target id.
 *
 * <p>The index never scans Minecraft state at lookup time and cannot grow after build().
 * Version modules may populate it lazily from their explicit target catalog.</p>
 */
public final class TargetCapabilityIndex {
    private final CapabilityId[] byRawId;

    private TargetCapabilityIndex(CapabilityId[] byRawId) {
        this.byRawId = byRawId;
    }

    public static Builder builder(int targetCount) {
        return new Builder(targetCount);
    }

    public CapabilityId lookup(int rawId) {
        return rawId >= 0 && rawId < byRawId.length ? byRawId[rawId] : null;
    }

    public int capacity() {
        return byRawId.length;
    }

    public int mappedCount() {
        int count = 0;
        for (CapabilityId capability : byRawId) {
            if (capability != null) {
                count++;
            }
        }
        return count;
    }

    public static final class Builder {
        private CapabilityId[] byRawId;

        private Builder(int targetCount) {
            if (targetCount < 0) {
                throw new IllegalArgumentException("targetCount must be >= 0");
            }
            byRawId = new CapabilityId[targetCount];
        }

        public Builder bind(int rawId, CapabilityId capability) {
            CapabilityId[] entries = requireMutable();
            if (rawId < 0 || rawId >= entries.length) {
                throw new IndexOutOfBoundsException("rawId=" + rawId + ", capacity=" + entries.length);
            }
            Objects.requireNonNull(capability, "capability");
            CapabilityId previous = entries[rawId];
            if (previous != null && previous != capability) {
                throw new IllegalStateException(
                        "raw target " + rawId + " already mapped to " + previous + ", cannot map " + capability);
            }
            entries[rawId] = capability;
            return this;
        }

        public TargetCapabilityIndex build() {
            CapabilityId[] entries = requireMutable();
            byRawId = null;
            return new TargetCapabilityIndex(Arrays.copyOf(entries, entries.length));
        }

        private CapabilityId[] requireMutable() {
            if (byRawId == null) {
                throw new IllegalStateException("builder already consumed");
            }
            return byRawId;
        }
    }
}

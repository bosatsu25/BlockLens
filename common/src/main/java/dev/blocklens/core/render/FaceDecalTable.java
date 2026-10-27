package dev.blocklens.core.render;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-face decals compiled once per (capability, semantic state) and shared by every baked model
 * with that pair. The key space is bounded by the compiled decoration catalog (a few hundred
 * pairs), so baked models retain one reference instead of their own table.
 */
public final class FaceDecalTable {
    private static final DecorationQuadFace[] FACES = DecorationQuadFace.values();
    private static final ConcurrentHashMap<Long, FaceDecalTable> INTERNED = new ConcurrentHashMap<>();

    private final DecorationDecal[] decals;
    private final boolean empty;

    private FaceDecalTable(DecorationDecal[] decals) {
        this.decals = decals;
        boolean anyPresent = false;
        for (DecorationDecal decal : decals) {
            anyPresent |= decal.present();
        }
        this.empty = !anyPresent;
    }

    public static FaceDecalTable compile(DecorationRenderDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");
        long key = ((long) descriptor.capability().ordinal() << 32) | (descriptor.state().packed() & 0xFFFFFFFFL);
        // Building first keeps bake-time interning lock-free; a losing duplicate is garbage.
        FaceDecalTable built = build(descriptor);
        FaceDecalTable previous = INTERNED.putIfAbsent(key, built);
        return previous == null ? built : previous;
    }

    /** Uncached table; {@link #compile} is the bake-time entry point. */
    static FaceDecalTable build(DecorationRenderDescriptor descriptor) {
        var decals = new DecorationDecal[FACES.length];
        for (DecorationQuadFace face : FACES) {
            decals[face.ordinal()] = DecorationDecalPolicy.decal(descriptor, face);
        }
        return new FaceDecalTable(decals);
    }

    public DecorationDecal decal(DecorationQuadFace face) {
        return decals[Objects.requireNonNull(face, "face").ordinal()];
    }

    /** True when no face of this state carries a decal (for example stained glass). */
    public boolean isEmpty() {
        return empty;
    }

    static int internedCount() {
        return INTERNED.size();
    }
}

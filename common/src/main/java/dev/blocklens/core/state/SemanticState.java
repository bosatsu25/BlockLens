package dev.blocklens.core.state;

import java.util.Objects;

/**
 * Compact, Minecraft-independent representation of BlockState semantics needed by BlockLens.
 *
 * <p>The value is intentionally packed into one int so later render/model paths can retain
 * semantic state without carrying mapped Minecraft objects or allocating collections.</p>
 */
public final class SemanticState {
    public enum Facing { NONE, NORTH, EAST, SOUTH, WEST }
    public enum Axis { NONE, X, Y, Z }
    public enum Half { NONE, BOTTOM, TOP }
    public enum StairShape { NONE, STRAIGHT, INNER_LEFT, INNER_RIGHT, OUTER_LEFT, OUTER_RIGHT }
    public enum MountFace { NONE, FLOOR, WALL, CEILING }
    public enum SlabType { NONE, BOTTOM, TOP, DOUBLE }

    public static final int CONNECT_NORTH = 1;
    public static final int CONNECT_EAST = 1 << 1;
    public static final int CONNECT_SOUTH = 1 << 2;
    public static final int CONNECT_WEST = 1 << 3;
    public static final int CONNECTION_MASK = CONNECT_NORTH | CONNECT_EAST | CONNECT_SOUTH | CONNECT_WEST;

    private static final int FACING_SHIFT = 0;
    private static final int AXIS_SHIFT = 3;
    private static final int HALF_SHIFT = 5;
    private static final int SHAPE_SHIFT = 7;
    private static final int MOUNT_SHIFT = 10;
    private static final int SLAB_SHIFT = 12;
    private static final int CONNECTION_SHIFT = 14;
    private static final int LEVEL_SHIFT = 18;
    private static final int OPEN_BIT = 1 << 21;
    private static final int LIT_BIT = 1 << 22;
    private static final int IN_WALL_BIT = 1 << 23;
    private static final int POWERED_BIT = 1 << 24;
    private static final int ATTACHED_BIT = 1 << 25;

    private static final int FACING_MASK = 0b111 << FACING_SHIFT;
    private static final int AXIS_MASK = 0b11 << AXIS_SHIFT;
    private static final int HALF_MASK = 0b11 << HALF_SHIFT;
    private static final int SHAPE_MASK = 0b111 << SHAPE_SHIFT;
    private static final int MOUNT_MASK = 0b11 << MOUNT_SHIFT;
    private static final int SLAB_MASK = 0b11 << SLAB_SHIFT;
    private static final int LEVEL_MASK = 0b111 << LEVEL_SHIFT;

    private static final SemanticState EMPTY = new SemanticState(0);

    private final int packed;

    private SemanticState(int packed) {
        this.packed = packed;
    }

    public static SemanticState empty() {
        return EMPTY;
    }

    public static SemanticState facing(Facing facing) {
        return create(facing, Axis.NONE, Half.NONE, StairShape.NONE, MountFace.NONE, SlabType.NONE,
                0, -1, false, false, false, false, false);
    }

    public static SemanticState axis(Axis axis) {
        return create(Facing.NONE, axis, Half.NONE, StairShape.NONE, MountFace.NONE, SlabType.NONE,
                0, -1, false, false, false, false, false);
    }

    public static SemanticState beehive(Facing facing, int honeyLevel) {
        if (honeyLevel < 0 || honeyLevel > 5) {
            throw new IllegalArgumentException("honeyLevel must be 0..5: " + honeyLevel);
        }
        return create(facing, Axis.NONE, Half.NONE, StairShape.NONE, MountFace.NONE, SlabType.NONE,
                0, honeyLevel, false, false, false, false, false);
    }

    public static SemanticState campfire(Facing facing, boolean lit) {
        return create(facing, Axis.NONE, Half.NONE, StairShape.NONE, MountFace.NONE, SlabType.NONE,
                0, -1, false, lit, false, false, false);
    }

    public static SemanticState grindstone(MountFace mountFace, Facing facing) {
        return create(facing, Axis.NONE, Half.NONE, StairShape.NONE, mountFace, SlabType.NONE,
                0, -1, false, false, false, false, false);
    }

    public static SemanticState fenceGate(Facing facing, boolean open, boolean inWall) {
        return create(facing, Axis.NONE, Half.NONE, StairShape.NONE, MountFace.NONE, SlabType.NONE,
                0, -1, open, false, inWall, false, false);
    }

    public static SemanticState slab(SlabType slabType) {
        return create(Facing.NONE, Axis.NONE, Half.NONE, StairShape.NONE, MountFace.NONE, slabType,
                0, -1, false, false, false, false, false);
    }

    public static SemanticState connections(boolean north, boolean east, boolean south, boolean west) {
        int mask = (north ? CONNECT_NORTH : 0)
                | (east ? CONNECT_EAST : 0)
                | (south ? CONNECT_SOUTH : 0)
                | (west ? CONNECT_WEST : 0);
        return create(Facing.NONE, Axis.NONE, Half.NONE, StairShape.NONE, MountFace.NONE, SlabType.NONE,
                mask, -1, false, false, false, false, false);
    }

    public static SemanticState stairs(Facing facing, Half half, StairShape shape) {
        return create(facing, Axis.NONE, half, shape, MountFace.NONE, SlabType.NONE,
                0, -1, false, false, false, false, false);
    }

    public static SemanticState trapdoor(Facing facing, Half half, boolean open) {
        return create(facing, Axis.NONE, half, StairShape.NONE, MountFace.NONE, SlabType.NONE,
                0, -1, open, false, false, false, false);
    }

    public static SemanticState poweredAttached(boolean powered, boolean attached) {
        return create(Facing.NONE, Axis.NONE, Half.NONE, StairShape.NONE, MountFace.NONE, SlabType.NONE,
                0, -1, false, false, false, powered, attached);
    }

    static SemanticState create(
            Facing facing,
            Axis axis,
            Half half,
            StairShape shape,
            MountFace mountFace,
            SlabType slabType,
            int connections,
            int level,
            boolean open,
            boolean lit,
            boolean inWall,
            boolean powered,
            boolean attached) {
        Objects.requireNonNull(facing, "facing");
        Objects.requireNonNull(axis, "axis");
        Objects.requireNonNull(half, "half");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(mountFace, "mountFace");
        Objects.requireNonNull(slabType, "slabType");
        if ((connections & ~CONNECTION_MASK) != 0) {
            throw new IllegalArgumentException("connections contains unsupported bits: " + connections);
        }
        if (level < -1 || level > 5) {
            throw new IllegalArgumentException("level must be -1 (absent) or 0..5: " + level);
        }

        int bits = facing.ordinal() << FACING_SHIFT;
        bits |= axis.ordinal() << AXIS_SHIFT;
        bits |= half.ordinal() << HALF_SHIFT;
        bits |= shape.ordinal() << SHAPE_SHIFT;
        bits |= mountFace.ordinal() << MOUNT_SHIFT;
        bits |= slabType.ordinal() << SLAB_SHIFT;
        bits |= connections << CONNECTION_SHIFT;
        bits |= (level + 1) << LEVEL_SHIFT;
        if (open) bits |= OPEN_BIT;
        if (lit) bits |= LIT_BIT;
        if (inWall) bits |= IN_WALL_BIT;
        if (powered) bits |= POWERED_BIT;
        if (attached) bits |= ATTACHED_BIT;
        return bits == 0 ? EMPTY : new SemanticState(bits);
    }

    public Facing facing() {
        return Facing.values()[(packed & FACING_MASK) >>> FACING_SHIFT];
    }

    public Axis axis() {
        return Axis.values()[(packed & AXIS_MASK) >>> AXIS_SHIFT];
    }

    public Half half() {
        return Half.values()[(packed & HALF_MASK) >>> HALF_SHIFT];
    }

    public StairShape stairShape() {
        return StairShape.values()[(packed & SHAPE_MASK) >>> SHAPE_SHIFT];
    }

    public MountFace mountFace() {
        return MountFace.values()[(packed & MOUNT_MASK) >>> MOUNT_SHIFT];
    }

    public SlabType slabType() {
        return SlabType.values()[(packed & SLAB_MASK) >>> SLAB_SHIFT];
    }

    public int connectionMask() {
        return (packed >>> CONNECTION_SHIFT) & CONNECTION_MASK;
    }

    public boolean connected(Facing facing) {
        return switch (Objects.requireNonNull(facing, "facing")) {
            case NORTH -> (connectionMask() & CONNECT_NORTH) != 0;
            case EAST -> (connectionMask() & CONNECT_EAST) != 0;
            case SOUTH -> (connectionMask() & CONNECT_SOUTH) != 0;
            case WEST -> (connectionMask() & CONNECT_WEST) != 0;
            case NONE -> false;
        };
    }

    public int level() {
        return ((packed & LEVEL_MASK) >>> LEVEL_SHIFT) - 1;
    }

    public boolean open() {
        return (packed & OPEN_BIT) != 0;
    }

    public boolean lit() {
        return (packed & LIT_BIT) != 0;
    }

    public boolean inWall() {
        return (packed & IN_WALL_BIT) != 0;
    }

    public boolean powered() {
        return (packed & POWERED_BIT) != 0;
    }

    public boolean attached() {
        return (packed & ATTACHED_BIT) != 0;
    }

    public int packed() {
        return packed;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof SemanticState state && packed == state.packed;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(packed);
    }

    @Override
    public String toString() {
        return "SemanticState[packed=" + Integer.toUnsignedString(packed) + ']';
    }
}

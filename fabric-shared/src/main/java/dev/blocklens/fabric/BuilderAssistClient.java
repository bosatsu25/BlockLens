package dev.blocklens.fabric;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.builder.BuilderState;
import dev.blocklens.core.builder.PatternInspector;
import dev.blocklens.core.builder.SchematicCompatibility;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Client-thread read-model owner; no renderer, interaction callback or world-write dependency. */
public final class BuilderAssistClient {
    public record Snapshot(int kind, BuilderState target, BuilderState prediction,
            String face, boolean upper, BuilderState expected) { }
    public record Placement(BlockPos position, BlockState state) { }
    private static final Snapshot EMPTY = new Snapshot(0, null, null, "", false, null);
    private static final BuilderState OTHER = new BuilderState("minecraft:air", Map.of());
    private static final PatternInspector PATTERN = new PatternInspector();
    private static final Map<BlockState, BuilderState> STATES = new IdentityHashMap<>();
    private static final BlockPos.MutableBlockPos QUERY = new BlockPos.MutableBlockPos();
    private static final PatternInspector.WorldView VIEW = BuilderAssistClient::patternState;
    private static ClientLevel level;
    private static Block referenceBlock;
    private static Snapshot snapshot = EMPTY;
    private static int patternOperations;
    private static volatile boolean invalidated;
    private static Boolean schematicSupported;

    private BuilderAssistClient() { }

    public static void register() { ClientTickEvents.END_CLIENT_TICK.register(BuilderAssistClient::tick); }

    /** May be requested by a resource-reload thread; actual retained-state disposal stays on the client. */
    public static void invalidate() { invalidated = true; }

    public static void clear() {
        PATTERN.clear(); STATES.clear(); referenceBlock = null; level = null;
        snapshot = EMPTY; patternOperations = 0; invalidated = false;
    }

    public static Snapshot snapshot() { if (invalidated) clear(); return snapshot; }
    public static PatternInspector.Snapshot pattern() { if (invalidated) clear(); return PATTERN.snapshot(); }
    public static int patternOperations() { return invalidated ? 0 : patternOperations; }
    public static void clearReference() { PATTERN.clear(); referenceBlock = null; patternOperations = 0; }

    public static void tick(Minecraft client) {
        if (invalidated) clear();
        var config = BlockLensRuntime.config();
        boolean inspector = config.isEnabled(CapabilityId.BLOCK_INSPECTOR);
        boolean preview = config.isEnabled(CapabilityId.PLACEMENT_PREVIEW);
        boolean pattern = config.isEnabled(CapabilityId.PATTERN_CONSISTENCY);
        boolean schematic = config.isEnabled(CapabilityId.SCHEMATIC_COMPARISON);
        if ((!inspector && !preview && !pattern && !schematic) || client.level == null || client.player == null) {
            clear(); return;
        }
        if (level != client.level) { clear(); level = client.level; }
        try {
            patternOperations = PATTERN.tick(level, VIEW, pattern);
            if (PATTERN.snapshot().reference() == null) referenceBlock = null;
            Snapshot next = EMPTY;
            if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                var state = loaded(level, hit.getBlockPos());
                if (state != null && !state.isAir()) {
                    var placement = preview || schematic ? predict(client, hit) : null;
                    var predicted = placement == null ? null : describe(placement.state());
                    var expected = schematic && predicted != null ? schematicState(placement.position()) : null;
                    next = new Snapshot(1, inspector ? describe(state) : null, predicted,
                            hit.getDirection().getName(), hit.getLocation().y - hit.getBlockPos().getY() > .5, expected);
                }
            } else if (inspector && client.hitResult instanceof EntityHitResult hit && hit.getEntity().isAlive()) {
                next = new Snapshot(2, new BuilderState(BuiltInRegistries.ENTITY_TYPE.getKey(hit.getEntity().getType()).toString(),
                        Map.of()), null, "", false, null);
            }
            if (!next.equals(snapshot)) snapshot = next;
        } catch (RuntimeException | LinkageError unavailable) { clear(); }
    }

    public static boolean selectReference(Minecraft client) {
        if (!BlockLensRuntime.config().isEnabled(CapabilityId.PATTERN_CONSISTENCY)
                || client.level == null || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) return false;
        if (invalidated || level != client.level) { clear(); level = client.level; }
        var state = loaded(level, hit.getBlockPos());
        if (state == null || state.isAir() || describe(state) == null) return false;
        referenceBlock = state.getBlock();
        var position = hit.getBlockPos();
        return PATTERN.select(level, VIEW, position.getX(), position.getY(), position.getZ());
    }

    /** Native state prediction only: this method never calls placement, item use or entity enumeration. */
    public static Placement predict(Minecraft client, BlockHitResult hit) {
        var config = BlockLensRuntime.config();
        if ((!config.isEnabled(CapabilityId.PLACEMENT_PREVIEW) && !config.isEnabled(CapabilityId.SCHEMATIC_COMPARISON))
                || client.level == null || client.player == null || hit == null || hit.getType() != HitResult.Type.BLOCK) return null;
        var stack = client.player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item) || !supported(item.getBlock())
                || !neighborhoodLoaded(client.level, hit.getBlockPos())) return null;
        try {
            var context = new BlockPlaceContext(client.player, InteractionHand.MAIN_HAND, stack, hit);
            var position = context.getClickedPos().immutable();
            BlockState state = context.canPlace() ? item.getBlock().getStateForPlacement(context) : null;
            if (state != null) state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(state);
            return new Placement(position, state);
        } catch (RuntimeException | LinkageError unavailable) { return null; }
    }

    private static boolean supported(Block block) {
        var id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !id.getNamespace().equals("minecraft")
                || !block.getClass().getName().startsWith("net.minecraft.world.level.block.")) return false;
        if (block instanceof TrapDoorBlock || block instanceof SlabBlock || block instanceof StairBlock
                || block instanceof GlazedTerracottaBlock || block instanceof FenceGateBlock || block instanceof GrindstoneBlock
                || block instanceof BeehiveBlock || block instanceof CampfireBlock) return true;
        var state = block.defaultBlockState();
        return block instanceof RotatedPillarBlock && state.hasProperty(BlockStateProperties.AXIS)
                && (state.is(BlockTags.LOGS) || id.getPath().endsWith("_froglight"));
    }

    private static boolean neighborhoodLoaded(ClientLevel world, BlockPos position) {
        // Vanilla stairs and redstone state calculations inspect at most two cells around the
        // adjacent placement cell: three from the existing hit. Unknown data is not treated as air.
        if (position.getY() < world.getMinY() + 3 || position.getY() >= world.getMaxY() - 3
                || position.getX() < Integer.MIN_VALUE + 3 || position.getX() > Integer.MAX_VALUE - 3
                || position.getZ() < Integer.MIN_VALUE + 3 || position.getZ() > Integer.MAX_VALUE - 3) return false;
        for (int x = Math.floorDiv(position.getX() - 3, 16); x <= Math.floorDiv(position.getX() + 3, 16); x++) {
            for (int z = Math.floorDiv(position.getZ() - 3, 16); z <= Math.floorDiv(position.getZ() + 3, 16); z++) {
                if (world.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false) == null) return false;
            }
        }
        return true;
    }

    private static BlockState loaded(ClientLevel world, BlockPos position) {
        if (world == null || position.getY() < world.getMinY() || position.getY() >= world.getMaxY()) return null;
        var chunk = world.getChunkSource().getChunk(Math.floorDiv(position.getX(), 16), Math.floorDiv(position.getZ(), 16),
                ChunkStatus.FULL, false);
        return chunk == null ? null : chunk.getBlockState(position);
    }

    private static BuilderState patternState(int x, int y, int z) {
        var state = loaded(level, QUERY.set(x, y, z));
        return state == null ? null : state.getBlock() == referenceBlock ? describe(state) : OTHER;
    }

    /** At most 64 immutable native states are retained; unrelated pattern blocks are never decoded. */
    public static BuilderState describe(BlockState state) {
        if (state == null) return null;
        var cached = STATES.get(state);
        if (cached != null) return cached;
        if (state.getProperties().size() > BuilderState.MAX_PROPERTIES) return null;
        try {
            var values = new HashMap<String, String>();
            for (Property<?> property : state.getProperties()) addProperty(state, property, values);
            var result = new BuilderState(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(), values);
            if (STATES.size() >= 64) STATES.clear();
            STATES.put(state, result);
            return result;
        } catch (IllegalArgumentException invalidState) { return null; }
    }

    private static <T extends Comparable<T>> void addProperty(BlockState state, Property<T> property, Map<String, String> values) {
        values.put(property.getName(), property.getName(state.getValue(property)));
    }

    public static boolean schematicSupported() {
        if (schematicSupported == null) schematicSupported = SchematicCompatibility.supported(
                BlockLensRuntime.minecraftVersion(), version("litematica"), version("malilib"));
        return schematicSupported;
    }

    private static String version(String id) {
        return FabricLoader.getInstance().getModContainer(id).map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("");
    }

    public static BuilderState schematicState(BlockPos position) {
        if (!BlockLensRuntime.config().isEnabled(CapabilityId.SCHEMATIC_COMPARISON) || position == null
                || !schematicSupported() || loaded(Minecraft.getInstance().level, position) == null) return null;
        try { return describe(LitematicaStateReader.read(position)); }
        catch (RuntimeException | LinkageError unavailable) { return null; }
    }
}

package dev.blocklens.fabric;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.scene.SceneFilterOptions;
import dev.blocklens.core.scene.SceneFilterPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

/** Bounded configured-ID compilation on the client tick; immutable membership queries in renderers. */
public final class SceneFilterClient {
    private record CompiledRule<T>(SceneFilterOptions.Mode mode, Set<T> blacklist, Set<T> whitelist) {
        boolean hidden(T value) {
            return SceneFilterPolicy.hidden(true, mode, blacklist.contains(value), whitelist.contains(value), false);
        }
    }
    private record Snapshot(SceneFilterOptions options, int mask, CompiledRule<Block> blocks,
            CompiledRule<EntityType<?>> entities, int lookups) { }
    private static final Snapshot EMPTY = new Snapshot(null,0,
            new CompiledRule<>(SceneFilterOptions.Mode.NONE,Set.of(),Set.of()),
            new CompiledRule<>(SceneFilterOptions.Mode.NONE,Set.of(),Set.of()),0);
    private static volatile Snapshot snapshot = EMPTY;
    private static Object worldIdentity;
    private static final class InvisibleType {
        private static final EntityType<?> VALUE = BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft","interaction"));
    }
    private SceneFilterClient() { }

    public static void register() { ClientTickEvents.END_CLIENT_TICK.register(SceneFilterClient::tick); }
    public static void clear() { snapshot = EMPTY; worldIdentity = null; }
    public static int compiledLookupCount() { return snapshot.lookups(); }
    /** Tiny compiler guard: an inactive generation does not need per-block config validation. */
    public static boolean blockFilterActive() { return snapshot.blocks().mode()!=SceneFilterOptions.Mode.NONE; }

    private static int mask() {
        var config = BlockLensRuntime.config();
        return (config.isEnabled(CapabilityId.BLOCK_FILTER) ? 1 : 0)
                | (config.isEnabled(CapabilityId.ENTITY_FILTER) ? 2 : 0);
    }
    public static boolean readyForCurrentConfig() {
        int current = mask();
        return current == 0 || (snapshot.mask() == current
                && snapshot.options() == BlockLensRuntime.config().sceneFilterOptions());
    }
    private static void tick(Minecraft client) {
        int current = mask();
        if (client.level == null || client.player == null || current == 0) {
            boolean previouslyFiltering = snapshot.mask() != 0;
            boolean previouslyFilteringBlocks = (snapshot.mask() & 1) != 0;
            clear();
            if (previouslyFiltering) AnalyzerClient.clear();
            if (previouslyFilteringBlocks && client.level != null) {
                MinecraftTerrainInvalidator.invalidateAll(client);
            }
            return;
        }
        var options = BlockLensRuntime.config().sceneFilterOptions();
        Snapshot previous = snapshot;
        if (worldIdentity == client.level && previous.mask() == current && previous.options() == options) return;
        if (worldIdentity == client.level && previous.mask() == current && options.equals(previous.options())) {
            snapshot = new Snapshot(options,current,previous.blocks(),previous.entities(),previous.lookups());
            return;
        }
        boolean blocksChanged = worldIdentity != client.level || ((current ^ previous.mask()) & 1) != 0
                || previous.options() == null || !options.blocks().equals(previous.options().blocks());
        var blockRule = options.blocks();
        var entityRule = options.entities();
        boolean blocksActive = (current & 1) != 0 && blockRule.mode() != SceneFilterOptions.Mode.NONE;
        boolean entitiesActive = (current & 2) != 0 && entityRule.mode() != SceneFilterOptions.Mode.NONE;
        CompiledRule<Block> blocks = blocksActive ? compile(blockRule,BuiltInRegistries.BLOCK) : EMPTY.blocks();
        CompiledRule<EntityType<?>> entities = entitiesActive ? compile(entityRule,BuiltInRegistries.ENTITY_TYPE) : EMPTY.entities();
        int lookups = (blocksActive ? blockRule.blacklist().size()+blockRule.whitelist().size() : 0)
                + (entitiesActive ? entityRule.blacklist().size()+entityRule.whitelist().size() : 0);
        snapshot = new Snapshot(options,current,blocks,entities,lookups);
        worldIdentity = client.level;
        AnalyzerClient.clear();
        if (blocksChanged && ((current | previous.mask()) & 1) != 0) MinecraftTerrainInvalidator.invalidateAll(client);
    }
    private static <T> CompiledRule<T> compile(SceneFilterOptions.Rule rule, Registry<T> registry) {
        return new CompiledRule<>(rule.mode(),resolve(rule.blacklist(),registry),resolve(rule.whitelist(),registry));
    }
    private static <T> Set<T> resolve(List<String> ids, Registry<T> registry) {
        HashSet<T> result = new HashSet<>();
        for (String raw : ids) {
            Identifier id = Identifier.tryParse(raw);
            if (id != null && registry.containsKey(id)) result.add(registry.getValue(id));
        }
        return Set.copyOf(result);
    }
    public static boolean registered(int kind, String raw) {
        Identifier id = Identifier.tryParse(raw);
        if (id == null) return false;
        return kind == 0 ? BuiltInRegistries.BLOCK.containsKey(id)
                : kind == 1 && BuiltInRegistries.ENTITY_TYPE.containsKey(id);
    }
    public static boolean hiddenBlock(Block block) {
        // Keep the last published generation until tick synchronization invalidates changed block rules.
        // Entity-only edits and equivalent decoded options must not drop already-filtered terrain.
        if (!BlockLensRuntime.config().isEnabled(CapabilityId.BLOCK_FILTER)) return false;
        return snapshot.blocks().hidden(block);
    }
    public static boolean hiddenEntity(Entity entity) {
        if (entity == Minecraft.getInstance().player || !BlockLensRuntime.config().isEnabled(CapabilityId.ENTITY_FILTER)) return false;
        return snapshot.entities().hidden(entity.getType());
    }
    public static EntityRenderState invisibleState(Entity entity, float delta) {
        EntityRenderState state = new EntityRenderState();
        var position = entity.getPosition(delta);
        state.entityType = InvisibleType.VALUE;
        state.x = position.x;
        state.y = position.y;
        state.z = position.z;
        state.isInvisible = true;
        return state;
    }
}

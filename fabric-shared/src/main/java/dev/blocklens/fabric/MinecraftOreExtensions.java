package dev.blocklens.fabric;

import dev.blocklens.api.OreHighlightExtensions;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.extension.OreExtensionCatalog;
import dev.blocklens.core.state.SemanticState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Exact bounded ore bindings; synchronized tags are compiled into retained terrain-only models. */
public final class MinecraftOreExtensions {
    public static final int MAX_TAG_MEMBERS = 512;
    private static final List<TagKey<Block>> MATERIAL_TAGS = List.of(
            tag("ores/coal"), tag("ores/copper"), tag("ores/diamond"), tag("ores/emerald"),
            tag("ores/gold"), tag("ores/iron"), tag("ores/lapis"), tag("ores/netherite_scrap"),
            tag("ores/quartz"), tag("ores/redstone"));
    private static final TagKey<Block> DEEPSLATE = tag("ores_in_ground/deepslate");
    private static final TagKey<Block> NETHERRACK = tag("ores_in_ground/netherrack");
    private static volatile Map<BlockState, MinecraftDecorationModel> models = Map.of();
    private static volatile Map<String, CapabilityId> tagged = Map.of();
    private static Map<String, CapabilityId> appliedTags = Map.of();
    private static Map<String, CapabilityId> bindings = Map.of();
    private static BlockStateModelSet appliedModels;
    private static int rebuilds;
    private static int tagUpdates;

    private MinecraftOreExtensions() { }

    public static void register() {
        CommonLifecycleEvents.TAGS_LOADED.register((registries, client) -> {
            if (client) {
                Map<String, CapabilityId> next = readTags(registries);
                if (!next.equals(tagged)) tagged = next;
                tagUpdates++;
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(MinecraftOreExtensions::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear(client));
    }

    public static int compiledModelCount() { return models.size(); }
    public static int tagCandidateCount() { return tagged.size(); }
    public static int rebuildCount() { return rebuilds; }
    public static int tagUpdateCount() { return tagUpdates; }
    public static boolean ready() { return appliedTags == tagged; }

    /** One immutable-map lookup at section compilation; never enumerates registries or reads tags. */
    public static BlockStateModel model(BlockStateModel original, BlockState state) {
        MinecraftDecorationModel candidate = models.get(state);
        return candidate != null && candidate.wraps(original) ? candidate : original;
    }

    private static Map<String, CapabilityId> readTags(RegistryAccess registries) {
        var registry = registries.lookup(Registries.BLOCK);
        if (registry.isEmpty()) return Map.of();
        OreExtensionCatalog candidates = new OreExtensionCatalog();
        Map<String, CapabilityId> explicit = OreHighlightExtensions.freeze();
        for (int material = 0; material < MATERIAL_TAGS.size(); material++) {
            var members = registry.get().get(MATERIAL_TAGS.get(material));
            if (members.isEmpty()) continue;
            if (members.get().size() > MAX_TAG_MEMBERS) return Map.of();
            for (var holder : members.get()) {
                var key = holder.unwrapKey();
                if (key.isEmpty()) continue;
                Identifier id = key.get().identifier();
                if (id.getNamespace().equals("minecraft") || id.getNamespace().length() + id.getPath().length() + 1
                        > OreExtensionCatalog.MAX_ID_LENGTH) continue;
                String blockId = id.toString();
                if (explicit.containsKey(blockId)) continue;
                candidates.add(blockId, OreExtensionCatalog.taggedCapability(material,
                        holder.is(DEEPSLATE), holder.is(NETHERRACK)));
                if (candidates.overflowed()) return Map.of();
            }
        }
        return candidates.freeze();
    }

    private static void tick(Minecraft client) {
        if (client.level == null) {
            models = Map.of();
            appliedTags = Map.of();
            bindings = Map.of();
            appliedModels = null;
            return;
        }
        Map<String, CapabilityId> currentTags = tagged;
        BlockStateModelSet currentModels = client.getModelManager().getBlockStateModelSet();
        if (currentTags == appliedTags && currentModels == appliedModels) return;
        Map<String, CapabilityId> resolved = resolve(currentTags);
        appliedTags = currentTags;
        if (resolved.equals(bindings) && currentModels == appliedModels) return;

        Map<BlockState, MinecraftDecorationModel> next = new HashMap<>();
        for (var entry : resolved.entrySet()) {
            Block block = supportedBlock(entry.getKey());
            if (block == null) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                BlockStateModel base = currentModels.get(state);
                next.put(state, new MinecraftDecorationModel(base,
                        new CapabilityId[]{entry.getValue()}, new SemanticState[]{SemanticState.empty()}, null));
            }
        }
        boolean changed = !models.isEmpty() || !next.isEmpty();
        models = Map.copyOf(next);
        bindings = resolved;
        appliedModels = currentModels;
        if (changed) {
            rebuilds++;
            MinecraftTerrainInvalidator.invalidateAll(client);
        }
    }

    private static Map<String, CapabilityId> resolve(Map<String, CapabilityId> candidates) {
        Map<String, CapabilityId> result = new TreeMap<>();
        int states = 0;
        for (var source : List.of(OreHighlightExtensions.freeze(), candidates)) {
            // A combined target overflow discards only this conventional generation.
            Map<String, CapabilityId> previous = new TreeMap<>(result);
            for (var entry : source.entrySet()) {
                if (entry.getValue() == null) continue;
                Block block = supportedBlock(entry.getKey());
                if (block == null) continue;
                int count = block.getStateDefinition().getPossibleStates().size();
                if (states + count > OreExtensionCatalog.MAX_STATE_MODELS) continue;
                if (result.size() >= OreExtensionCatalog.MAX_TARGETS) return previous;
                result.put(entry.getKey(), entry.getValue());
                states += count;
            }
        }
        return result;
    }

    private static Block supportedBlock(String rawId) {
        Identifier id = Identifier.tryParse(rawId);
        if (id == null || !BuiltInRegistries.BLOCK.containsKey(id)) return null;
        Block block = BuiltInRegistries.BLOCK.getValue(id);
        return block != null && block.getStateDefinition().getPossibleStates().size()
                <= OreExtensionCatalog.MAX_STATES_PER_TARGET ? block : null;
    }

    private static TagKey<Block> tag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("c", path));
    }

    private static void clear(Minecraft client) {
        boolean changed = !models.isEmpty();
        models = Map.of();
        tagged = Map.of();
        appliedTags = Map.of();
        bindings = Map.of();
        appliedModels = null;
        if (changed && client.level != null) MinecraftTerrainInvalidator.invalidateAll(client);
    }
}

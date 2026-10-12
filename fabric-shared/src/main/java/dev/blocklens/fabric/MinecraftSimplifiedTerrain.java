package dev.blocklens.fabric;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.VisualGeometryKey;
import dev.blocklens.core.render.RetainedValue;
import dev.blocklens.core.render.SimplifiedTerrainPolicy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

/** Opt-in bounded preparation and retained state geometry; external definitions remain authoritative. */
public final class MinecraftSimplifiedTerrain {
    private static final Identifier CUBE_ID = id("block/simplified/cube");
    private static final ExtraModelKey<BlockStateModel> CUBE = ExtraModelKey.create(CUBE_ID::toString);
    private static final String[] MASKS = {"short_grass", "tall_grass", "bush"};
    private MinecraftSimplifiedTerrain() { }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("blocklens",path); }
    public static void register() {
        PreparableModelLoadingPlugin.register((state,executor) -> CompletableFuture.supplyAsync(
                () -> prepare(state.resourceManager()), executor), (targets,context) -> {
            if (targets.isEmpty()) return;
            context.addModel(CUBE,SimpleUnbakedExtraModel.blockStateModel(CUBE_ID));
            for (String mask : MASKS) {
                var modelId=id("block/simplified/"+mask);
                context.addModel(ExtraModelKey.create(modelId::toString),SimpleUnbakedExtraModel.blockStateModel(modelId));
            }
            context.modifyBlockModelAfterBake().register(ModelModifier.OVERRIDE_PHASE, (model,bake) -> {
                var kind = targets.get(bake.state().getBlock());
                return kind == null ? model : new SimplifiedModel(model,kind);
            });
        });
    }
    private static Map<Block,SimplifiedTerrainPolicy.Kind> prepare(ResourceManager resources) {
        if (!BlockLensRuntime.config().isEnabled(CapabilityId.SIMPLIFIED_TERRAIN)) return Map.of();
        var result = new HashMap<Block,SimplifiedTerrainPolicy.Kind>();
        var ownership = new Ownership(resources);
        for (String path : SimplifiedTerrainPolicy.sourcePaths()) {
            var blockId = Identifier.fromNamespaceAndPath("minecraft",path);
            if (!BuiltInRegistries.BLOCK.containsKey(blockId)) continue;
            if (!ownership.owned(Identifier.fromNamespaceAndPath("minecraft","blockstates/"+path+".json"),0)) continue;
            result.put(BuiltInRegistries.BLOCK.getValue(blockId),SimplifiedTerrainPolicy.kind("minecraft",path));
        }
        return Map.copyOf(result);
    }
    /** Traverses exact referenced resources only, with shared cache and fail-closed bounds. */
    private static final class Ownership {
        private final ResourceManager resources;
        private final Map<Identifier,Boolean> cache = new HashMap<>();
        private final Set<Identifier> visiting = new HashSet<>();
        private int reads;
        Ownership(ResourceManager resources) { this.resources=resources; }
        boolean owned(Identifier resourceId, int depth) {
            Boolean previous=cache.get(resourceId); if(previous!=null) return previous;
            if(depth>24 || reads>=1024 || !visiting.add(resourceId)) return false;
            reads++;
            boolean owned=false;
            try {
                var resource=resources.getResource(resourceId);
                if(resource.isPresent() && resource.get().sourcePackId().equals("vanilla")) {
                    if(!resourceId.getPath().endsWith(".json")) owned=true;
                    else try(var stream=resource.get().open()) {
                        byte[] bytes=stream.readNBytes(65537);
                        owned=bytes.length<=65536 && references(JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)),depth,0);
                    }
                }
            } catch(IOException | IllegalArgumentException failure) { owned=false; }
            visiting.remove(resourceId); cache.put(resourceId,owned); return owned;
        }
        private boolean references(JsonElement value,int resourceDepth,int jsonDepth) {
            if(jsonDepth>32) return false;
            if(value.isJsonArray()) {
                if(value.getAsJsonArray().size()>256) return false;
                for(var child:value.getAsJsonArray()) if(!references(child,resourceDepth,jsonDepth+1)) return false;
            } else if(value.isJsonObject()) {
                if(value.getAsJsonObject().size()>256) return false;
                for(var entry:value.getAsJsonObject().entrySet()) {
                    String key=entry.getKey(); var child=entry.getValue();
                    if(key.equals("model") || key.equals("parent")) {
                        if(!child.isJsonPrimitive() || !reference(child.getAsString(),"models/",".json",resourceDepth)) return false;
                    } else if(key.equals("textures") && child.isJsonObject()) {
                        for(var texture:child.getAsJsonObject().entrySet()) {
                            if(!texture.getValue().isJsonPrimitive()) return false;
                            String raw=texture.getValue().getAsString();
                            if(!raw.startsWith("#") && !reference(raw,"textures/",".png",resourceDepth)) return false;
                        }
                    } else if(!references(child,resourceDepth,jsonDepth+1)) return false;
                }
            }
            return true;
        }
        private boolean reference(String raw,String prefix,String suffix,int depth) {
            var parsed=Identifier.tryParse(raw);
            if(parsed==null || !parsed.getNamespace().equals("minecraft")) return false;
            return owned(Identifier.fromNamespaceAndPath(parsed.getNamespace(),prefix+parsed.getPath()+suffix),depth+1);
        }
    }
    private static final class SimplifiedModel extends WrapperBlockStateModel {
        private final SimplifiedTerrainPolicy.Kind kind;
        private final RetainedValue<BlockStateModel> cube = new RetainedValue<>();
        SimplifiedModel(BlockStateModel wrapped,SimplifiedTerrainPolicy.Kind kind) { super(wrapped);this.kind=kind; }
        @Override public void emitQuads(QuadEmitter emitter,BlockAndTintGetter level,BlockPos pos,
                BlockState state,RandomSource random,Predicate<@Nullable Direction> cullTest) {
            if(!BlockLensRuntime.config().isEnabled(CapabilityId.SIMPLIFIED_TERRAIN)) {
                super.emitQuads(emitter,level,pos,state,random,cullTest);return;
            }
            if(kind==SimplifiedTerrainPolicy.Kind.CUBE) {
                var model=cube.get(CUBE,key -> Minecraft.getInstance().getModelManager().getModel(key));
                if(model!=null) { model.emitQuads(emitter,level,pos,state,random,cullTest);return; }
                super.emitQuads(emitter,level,pos,state,random,cullTest);return;
            }
            var effective=kind==SimplifiedTerrainPolicy.Kind.TALL_GRASS
                    && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)==DoubleBlockHalf.UPPER
                    ? SimplifiedTerrainPolicy.Kind.SHORT_GRASS : kind;
            String texture="grain";
            if(effective==SimplifiedTerrainPolicy.Kind.SHORT_GRASS) texture="short_grass";
            else if(effective==SimplifiedTerrainPolicy.Kind.TALL_GRASS) texture="tall_grass";
            else if(effective==SimplifiedTerrainPolicy.Kind.BUSH) texture="bush";
            var atlas=Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
            var replacement=atlas.getSprite(id("block/simplified/"+texture));
            var finder=atlas.spriteFinder();
            emitter.pushTransform(quad -> {
                var original=finder.find(quad);
                float width=original.getU1()-original.getU0(),height=original.getV1()-original.getV0();
                if(width<=0 || height<=0) return true;
                for(int vertex=0;vertex<4;vertex++) {
                    float u=(quad.u(vertex)-original.getU0())/width,v=(quad.v(vertex)-original.getV0())/height;
                    quad.uv(vertex,replacement.getU(u),replacement.getV(v));
                }
                quad.tintIndex(-1);quad.color(-1,-1,-1,-1);
                if(effective==SimplifiedTerrainPolicy.Kind.SURFACE) {
                    quad.chunkLayer(ChunkSectionLayer.SOLID);
                    MinecraftQuadLighting.disableDirectionalShade(quad);
                }
                quad.animated(false);return true;
            });
            try { super.emitQuads(emitter,level,pos,state,random,cullTest); }
            finally { emitter.popTransform(); }
        }
        @Override public @Nullable Object createGeometryKey(BlockAndTintGetter level,BlockPos pos,
                BlockState state,RandomSource random) {
            Object key=wrapped.createGeometryKey(level,pos,state,random);
            return key==null || !BlockLensRuntime.config().isEnabled(CapabilityId.SIMPLIFIED_TERRAIN)
                    ? key : new VisualGeometryKey(key,1L << CapabilityId.SIMPLIFIED_TERRAIN.bitPosition());
        }
    }
}
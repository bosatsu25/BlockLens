package dev.blocklens.fabric;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.CapabilityId;
import dev.blocklens.core.render.VisualGeometryKey;
import dev.blocklens.core.render.DecorationDecals;
import dev.blocklens.core.render.DecorationQuadFace;
import dev.blocklens.core.render.DecorationQuadInstruction;
import dev.blocklens.core.render.DecorationRenderDescriptor;
import dev.blocklens.core.render.NetherTweaksVisualCue;
import dev.blocklens.core.render.ResourceHighlightCue;
import dev.blocklens.core.render.ResourceBorderPolicy;
import dev.blocklens.core.render.FaceInstructionTable;
import dev.blocklens.core.render.RetainedValue;
import dev.blocklens.core.render.VisibilityRenderPolicy;
import dev.blocklens.core.state.DecorationStateKind;
import dev.blocklens.core.state.SemanticState;
import dev.blocklens.core.state.VisibilityStateKind;
import java.util.Objects;
import java.util.ArrayList;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.IronBarsBlock;
import org.jspecify.annotations.Nullable;

/** Shared baked-model wrapper shared by the M3-M7 rendering families. */
final class MinecraftDecorationModel extends WrapperBlockStateModel {
    private static final long NETHER_TWEAKS_BIT = CapabilityId.NETHER_TWEAKS.renderBit();

    private static final long LIGHTWEIGHT_OVERLAY_MASK = CapabilityId.GLASS_HIGHLIGHT.renderBit()
            | CapabilityId.KELP_HIGHLIGHT.renderBit();
    private final @Nullable RetainedValue<BlockStateModel> lightweightOverlay;
    private final @Nullable RetainedValue<BlockStateModel> oreBorderOverlay;
    private final @Nullable RetainedValue<BlockStateModel> connectedOreBorderOverlay;
    private final @Nullable RetainedValue<BlockStateModelPart> connectedOrePart;
    private final long oreBorderMask;
    private final int oreBorderTint;

    private static final Identifier DECAL_SPRITE = Identifier.fromNamespaceAndPath("blocklens", DecorationDecals.SPRITE);
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int UNCULLED = DIRECTIONS.length;

    private final Object[] descriptors;
    private final long[] descriptorBits;
    // Interned per (capability, state); present only when the wrapped model is one fixed variant.
    private final long @Nullable [][] decalTables;
    private final long decalMask;
    private final long representedRenderableMask;
    private final @Nullable NetherTweaksVisualCue netherCue;
    private final SemanticState netherState;

    // Decal quads depend only on this fixed geometry, the enabled decal cues and the glyph sprite, so
    // they are encoded once and replayed per block instead of re-emitting and re-mapping the model.
    private volatile @Nullable DecalMeshes decalMeshes;
    private volatile @Nullable BlockStateModel netherInteriorOverlay;
    private volatile @Nullable BlockStateModel netherBandOverlay;
    private volatile boolean interiorLookupAttempted;
    private volatile boolean bandLookupAttempted;

    MinecraftDecorationModel(
            BlockStateModel wrapped,
            CapabilityId[] capabilities,
            SemanticState[] semanticStates,
            @Nullable NetherTweaksVisualCue netherCue) {
        super(Objects.requireNonNull(wrapped, "wrapped"));
        Objects.requireNonNull(capabilities, "capabilities");
        Objects.requireNonNull(semanticStates, "semanticStates");
        if (capabilities.length == 0 || capabilities.length != semanticStates.length) {
            throw new IllegalArgumentException("capability/state arrays must be non-empty and have equal length");
        }

        int count = capabilities.length;
        this.descriptors = new Object[count];
        this.descriptorBits = new long[count];
        long represented = 0L;
        long borders = 0L;
        int borderTint = 0;
        boolean foundNether = false;
        SemanticState capturedNetherState = SemanticState.empty();
        // Decals re-emit the wrapped geometry; only a single fixed variant guarantees the second
        // emission matches the first. Weighted, multipart and other wrappers keep the tint cue.
        boolean decalCapable = wrapped.getClass() == SingleVariant.class;
        long[][] decals = null;
        long decalBits = 0L;

        for (int i = 0; i < count; i++) {
            CapabilityId capability = Objects.requireNonNull(capabilities[i], "capability");
            SemanticState state = Objects.requireNonNull(semanticStates[i], "semanticState");
            long bit = capability.renderBit();

            if (capability == CapabilityId.NETHER_TWEAKS) {
                if (netherCue == null) {
                    throw new IllegalArgumentException("Nether Tweaks capability requires a visual cue");
                }
                capturedNetherState = state;
                foundNether = true;
                descriptorBits[i] = bit;
                represented |= bit;
                continue;
            }
            if (capability == CapabilityId.GLASS_HIGHLIGHT || capability == CapabilityId.KELP_HIGHLIGHT) {
                descriptors[i] = DecorationQuadInstruction.IDENTITY;
            } else if (capability == CapabilityId.BRIGHT_CONCRETE) {
                descriptors[i] = DecorationQuadInstruction.emissiveTint(0xFFFFFFFF);
            } else if (DecorationStateKind.isDecorationCapability(capability)) {
                descriptors[i] = FaceInstructionTable.compile(DecorationRenderDescriptor.of(capability, state));
                long[] decalTable = decalCapable
                        ? DecorationDecals.table(DecorationRenderDescriptor.of(capability, state)) : null;
                if (decalTable != null && !DecorationDecals.isEmpty(decalTable)) {
                    if (decals == null) {
                        decals = new long[count][];
                    }
                    decals[i] = decalTable;
                    decalBits |= bit;
                }
            } else if (VisibilityStateKind.isVisibilityCapability(capability)) {
                descriptors[i] = VisibilityRenderPolicy.describeEnabled(capability, state).cue().instruction();
            } else if (capability.isResourceHighlight()) {
                var cue = ResourceHighlightCue.forCapability(capability);
                descriptors[i] = cue.instruction();
                if (ResourceBorderPolicy.supports(capability)) {
                    borders |= bit;
                    borderTint = cue.accentArgb();
                }
            } else {
                continue;
            }
            descriptorBits[i] = bit;
            represented |= bit;
        }
        if (netherCue != null && !foundNether) {
            throw new IllegalArgumentException("Nether Tweaks cue supplied without target capability");
        }
        this.netherCue = netherCue;
        this.netherState = capturedNetherState;
        this.representedRenderableMask = represented;
        this.lightweightOverlay = (represented & LIGHTWEIGHT_OVERLAY_MASK) == 0L ? null : new RetainedValue<>();
        this.oreBorderOverlay = borders == 0L ? null : new RetainedValue<>();
        this.connectedOreBorderOverlay = borders == 0L ? null : new RetainedValue<>();
        this.connectedOrePart=borders==0L || wrapped.getClass()!=SingleVariant.class ? null : new RetainedValue<>();
        this.oreBorderMask = borders;
        this.oreBorderTint = borderTint;
        this.decalTables = decals;
        this.decalMask = decalBits;
    }

    boolean wraps(BlockStateModel model) {
        return wrapped == model;
    }

    @Override
    public void emitQuads(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest) {
        long enabledCapabilities = BlockLensRuntime.config().enabledMask() & representedRenderableMask;
        if (enabledCapabilities == 0L) {
            super.emitQuads(emitter, level, pos, state, random, cullTest);
            return;
        }

        boolean connected = connectedOrePart!=null && (enabledCapabilities & oreBorderMask) != 0L
                && BlockLensRuntime.config().isEnabled(CapabilityId.CONNECTED_ORE_EDGES);
        int[] cubeFaces = connected ? new int[2] : null;
        boolean netherEnabled = netherCue != null && (enabledCapabilities & NETHER_TWEAKS_BIT) != 0L;
        // A decoration drawn as a decal keeps the active texture's colors instead of tinting it.
        long decalCapabilities = enabledCapabilities & decalMask;
        long tintCapabilities = enabledCapabilities & ~decalCapabilities;
        if (cubeFaces == null && !netherEnabled && tintCapabilities == 0L) {
            // Decal-only cues leave the base geometry untouched: no per-quad transform at all.
            super.emitQuads(emitter, level, pos, state, random, cullTest);
        } else {
            emitter.pushTransform(quad -> {
                if(cubeFaces != null) {
                    Direction direction=quad.cullFace();
                    if(direction==null) cubeFaces[1]=63;
                    else if(unitCubeFace(quad,direction)) cubeFaces[0]|=1<<direction.ordinal();
                    else cubeFaces[1]|=1<<direction.ordinal();
                }
                if (netherEnabled) {
                    quad.multiplyColor(netherCue.frameArgb());
                }
                applyNonNetherInstructions(quad, tintCapabilities);
                return true;
            });
            try {
                super.emitQuads(emitter, level, pos, state, random, cullTest);
            } finally {
                emitter.popTransform();
            }
        }

        if (netherEnabled) {
            emitNetherOverlay(emitter, level, pos, state, random, cullTest, tintCapabilities);
        }
        if ((enabledCapabilities & LIGHTWEIGHT_OVERLAY_MASK) != 0L) {
            emitLightweightOverlay(emitter, level, pos, state, random, cullTest, enabledCapabilities);
        }
        if ((enabledCapabilities & oreBorderMask) != 0L) {
            emitOreBorder(emitter, level, pos, state, random, cullTest,
                    cubeFaces==null ? -1 : cubeFaces[0]&~cubeFaces[1]);
        }
        if (decalCapabilities != 0L) {
            emitDecals(emitter, level, pos, state, random, cullTest, decalCapabilities);
        }
    }

    /**
     * Draws the decal quads of the wrapped single-variant geometry: each quad gets the face's
     * clean-room glyph mapped onto its own bounds, lifted just above the surface. The quads are
     * encoded once per (enabled decal cues, glyph sprite) and grouped by cull face, so culled faces
     * stay culled exactly as when the model itself is emitted with the same cull test.
     */
    private void emitDecals(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
            BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest, long decalCapabilities) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(DECAL_SPRITE);
        if (!DECAL_SPRITE.equals(sprite.contents().name())) {
            return; // Never draw the missing-texture checkerboard over a block.
        }
        DecalMeshes meshes = decalMeshes;
        // The sprite identity check rebuilds after any atlas change instead of keeping stale UVs.
        if (meshes == null || meshes.capabilities() != decalCapabilities || meshes.sprite() != sprite) {
            meshes = encodeDecals(level, pos, state, random, sprite, decalCapabilities);
            decalMeshes = meshes;
        }
        Mesh[] byCullFace = meshes.byCullFace();
        for (int i = 0; i < byCullFace.length; i++) {
            Mesh mesh = byCullFace[i];
            if (mesh != null && !cullTest.test(i == UNCULLED ? null : DIRECTIONS[i])) {
                mesh.outputTo(emitter);
            }
        }
    }

    private DecalMeshes encodeDecals(BlockAndTintGetter level, BlockPos pos, BlockState state,
            RandomSource random, TextureAtlasSprite sprite, long decalCapabilities) {
        MutableMesh[] groups = new MutableMesh[UNCULLED + 1];
        QuadEmitter[] emitters = new QuadEmitter[UNCULLED + 1];
        QuadEmitter source = Renderer.get().mutableMesh().emitter();
        float[] positions = new float[12];
        float[] uvs = new float[8];
        source.pushTransform(quad -> {
            if (applyDecal(quad, sprite, decalCapabilities, positions, uvs)) {
                Direction cullFace = quad.cullFace();
                int group = cullFace == null ? UNCULLED : cullFace.ordinal();
                if (groups[group] == null) {
                    groups[group] = Renderer.get().mutableMesh();
                    emitters[group] = groups[group].emitter();
                }
                emitters[group].copyFrom(quad).emit();
            }
            return false; // Only the grouped copies are kept.
        });
        try {
            super.emitQuads(source, level, pos, state, random, direction -> false);
        } finally {
            source.popTransform();
        }
        Mesh[] byCullFace = new Mesh[UNCULLED + 1];
        for (int i = 0; i < groups.length; i++) {
            byCullFace[i] = groups[i] == null ? null : groups[i].immutableCopy();
        }
        return new DecalMeshes(decalCapabilities, sprite, byCullFace);
    }

    private record DecalMeshes(long capabilities, TextureAtlasSprite sprite, @Nullable Mesh[] byCullFace) {
    }

    private boolean applyDecal(MutableQuadView quad, TextureAtlasSprite sprite, long decalCapabilities,
            float[] positions, float[] uvs) {
        DecorationQuadFace face = face(quad.nominalFace());
        if (face == DecorationQuadFace.NONE || decalTables == null) {
            return false;
        }
        int f = face.ordinal();
        long decal = DecorationDecals.NONE;
        for (int i = 0; i < decalTables.length && decal == DecorationDecals.NONE; i++) {
            if (decalTables[i] != null && (decalCapabilities & descriptorBits[i]) != 0L) {
                decal = decalTables[i][f];
            }
        }
        if (decal == DecorationDecals.NONE) {
            return false;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            positions[3 * vertex] = quad.x(vertex);
            positions[3 * vertex + 1] = quad.y(vertex);
            positions[3 * vertex + 2] = quad.z(vertex);
        }
        if (!DecorationDecals.layout(f, decal, positions, sprite.getU0(), sprite.getU1(), sprite.getV0(),
                sprite.getV1(), uvs)) {
            return false;
        }
        for (int vertex = 0; vertex < 4; vertex++) {
            quad.uv(vertex, uvs[2 * vertex], uvs[2 * vertex + 1]);
            quad.pos(vertex, positions[3 * vertex], positions[3 * vertex + 1], positions[3 * vertex + 2]);
        }
        int argb = DecorationDecals.argb(decal);
        quad.color(argb, argb, argb, argb);
        quad.tintIndex(-1);
        quad.animated(false);
        quad.chunkLayer(ChunkSectionLayer.CUTOUT);
        // Keep the base face's lighting (block/sky light, directional shade, AO), as a texture in the
        // source pack would be lit: the decal darkens in darkness instead of glowing.
        return true;
    }

    private void emitNetherOverlay(
            QuadEmitter emitter,
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            Predicate<@Nullable Direction> cullTest,
            long enabledCapabilities) {
        BlockStateModel interior = lookupInteriorOverlay();
        if (interior != null) {
            emitter.pushTransform(quad -> {
                quad.multiplyColor(netherCue.fillArgb(netherState.axis(), face(quad.nominalFace())));
                applyNonNetherInstructions(quad, enabledCapabilities);
                return true;
            });
            try {
                interior.emitQuads(emitter, level, pos, state, random, cullTest);
            } finally {
                emitter.popTransform();
            }
        }

        if (netherCue.hasUpperBand()) {
            BlockStateModel band = lookupBandOverlay();
            if (band != null) {
                emitter.pushTransform(quad -> {
                    quad.multiplyColor(netherCue.upperBandArgb());
                    applyNonNetherInstructions(quad, enabledCapabilities);
                    return true;
                });
                try {
                    band.emitQuads(emitter, level, pos, state, random, cullTest);
                } finally {
                    emitter.popTransform();
                }
            }
        }
    }

    private void emitLightweightOverlay(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
            BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest, long enabled) {
        ExtraModelKey<BlockStateModel> key = (enabled & CapabilityId.KELP_HIGHLIGHT.renderBit()) != 0L
                ? LightweightOverlayModels.KELP : state.getBlock() instanceof IronBarsBlock
                ? LightweightOverlayModels.PANE : LightweightOverlayModels.GLASS;
        BlockStateModel overlay = lookupLightweightOverlay(key);
        if (overlay == null) return;
        emitter.pushTransform(quad -> {
            quad.chunkLayer(ChunkSectionLayer.CUTOUT);
            quad.emissive(true);
            MinecraftQuadLighting.disableDirectionalShade(quad);
            quad.ambientOcclusion(TriState.FALSE);
            return true;
        });
        try {
            overlay.emitQuads(emitter, level, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    /** Clean-room emissive edge model; baked once, emitted only for enabled ore highlights. */
    private void emitOreBorder(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
            BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest,int connectedFaces) {
        boolean connected=connectedFaces>=0;
        var retained=connected ? connectedOreBorderOverlay : oreBorderOverlay;
        BlockStateModel overlay = retained == null ? null
                : retained.get(connected ? LightweightOverlayModels.CONNECTED_ORE_BORDER
                    : LightweightOverlayModels.ORE_BORDER, MinecraftDecorationModel::lookup);
        if (overlay == null) return;
        var sprite=connected ? Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(Identifier.fromNamespaceAndPath("blocklens","block/lightweight/connected_ore_border")) : null;
        var part=connected ? fixedOrePart() : null;
        emitter.pushTransform(quad -> {
            Direction direction=quad.cullFace();
            if(sprite!=null && direction!=null && (connectedFaces&(1<<direction.ordinal()))!=0) {
                int mask=connectedNeighbors(level,pos,state,direction,part);
                float uOffset=(sprite.getU1()-sprite.getU0())*ResourceBorderPolicy.sheetColumn(mask)
                        /ResourceBorderPolicy.SHEET_TILES;
                float vOffset=(sprite.getV1()-sprite.getV0())*ResourceBorderPolicy.sheetRow(mask)
                        /ResourceBorderPolicy.SHEET_TILES;
                for(int vertex=0;vertex<4;vertex++)quad.uv(vertex,quad.u(vertex)+uOffset,quad.v(vertex)+vOffset);
            }
            quad.chunkLayer(ChunkSectionLayer.CUTOUT);
            quad.multiplyColor(oreBorderTint);
            quad.emissive(true);
            MinecraftQuadLighting.disableDirectionalShade(quad);
            quad.ambientOcclusion(TriState.FALSE);
            return true;
        });
        try {
            overlay.emitQuads(emitter, level, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    private static boolean unitCubeFace(MutableQuadView quad, Direction direction) {
        int side = face(direction).ordinal();
        int corners = 0;
        for (int vertex = 0; vertex < 4; vertex++) {
            int corner = ResourceBorderPolicy.unitCubeCorner(side, quad.x(vertex), quad.y(vertex), quad.z(vertex));
            if (corner < 0) {
                return false;
            }
            corners |= 1 << corner;
        }
        return corners == 15;
    }
    private @Nullable BlockStateModelPart fixedOrePart() {
        return connectedOrePart==null ? null : connectedOrePart.get(wrapped,model->{
            var parts=new ArrayList<BlockStateModelPart>(1);
            model.collectParts(RandomSource.create(0),parts);
            // Exact SingleVariant class above guarantees one immutable baked part.
            return parts.getFirst();
        });
    }
    private static int connectedNeighbors(BlockAndTintGetter level,BlockPos pos,BlockState state,Direction face,
            @Nullable BlockStateModelPart part) {
        int side = face(face).ordinal();
        var neighbor=new BlockPos.MutableBlockPos();var front=new BlockPos.MutableBlockPos();int mask=0;
        for(int bit=0;bit<8;bit++) {
            neighbor.set(pos.getX() + ResourceBorderPolicy.neighborOffset(side, bit, 0),
                    pos.getY() + ResourceBorderPolicy.neighborOffset(side, bit, 1),
                    pos.getZ() + ResourceBorderPolicy.neighborOffset(side, bit, 2));
            var adjacent=level.getBlockState(neighbor);
            front.set(neighbor).move(face);
            if(part!=null && adjacent.getBlock()==state.getBlock()
                    && !level.getBlockState(front).isSolidRender()) {
                var models=Minecraft.getInstance().getModelManager().getBlockStateModelSet();
                var candidate=MinecraftOreExtensions.model(models.get(adjacent),adjacent);
                if(candidate instanceof MinecraftDecorationModel decoration && decoration.fixedOrePart()==part)mask|=1<<bit;
            }
        }
        return mask;
    }

    private @Nullable BlockStateModel lookupLightweightOverlay(ExtraModelKey<BlockStateModel> key) {
        return lightweightOverlay == null ? null : lightweightOverlay.get(key, MinecraftDecorationModel::lookup);
    }

    private void applyNonNetherInstructions(MutableQuadView quad, long enabledCapabilities) {
        DecorationQuadFace face = face(quad.nominalFace());
        for (int i = 0; i < descriptorBits.length; i++) {
            long bit = descriptorBits[i];
            if (bit == 0L || bit == NETHER_TWEAKS_BIT || (enabledCapabilities & bit) == 0L) {
                continue;
            }

            DecorationQuadInstruction instruction;
            Object descriptor = descriptors[i];
            if (descriptor instanceof FaceInstructionTable table) {
                instruction = table.instruction(face);
            } else {
                instruction = (DecorationQuadInstruction) descriptor;
            }
            applyInstruction(quad, instruction);
        }
    }

    private static void applyInstruction(MutableQuadView quad, DecorationQuadInstruction instruction) {
        if (instruction.forceSolid()) {
            quad.chunkLayer(ChunkSectionLayer.SOLID);
        }
        if (instruction.changesColor()) {
            quad.multiplyColor(instruction.multiplyArgb());
        }
        if (instruction.emissive()) {
            quad.emissive(true);
            MinecraftQuadLighting.disableDirectionalShade(quad);
            quad.ambientOcclusion(TriState.FALSE);
        }
    }

    private @Nullable BlockStateModel lookupInteriorOverlay() {
        if (interiorLookupAttempted) return netherInteriorOverlay;
        BlockStateModel cached;
        synchronized (this) {
            cached = netherInteriorOverlay;
            if (cached != null || interiorLookupAttempted) {
                return cached;
            }
            cached = lookup(NetherTweaksOverlayModels.INTERIOR);
            netherInteriorOverlay = cached;
            interiorLookupAttempted = true;
            return cached;
        }
    }

    private @Nullable BlockStateModel lookupBandOverlay() {
        if (bandLookupAttempted) return netherBandOverlay;
        BlockStateModel cached;
        synchronized (this) {
            cached = netherBandOverlay;
            if (cached != null || bandLookupAttempted) {
                return cached;
            }
            cached = lookup(NetherTweaksOverlayModels.UPPER_BAND);
            netherBandOverlay = cached;
            bandLookupAttempted = true;
            return cached;
        }
    }

    private static @Nullable BlockStateModel lookup(ExtraModelKey<BlockStateModel> key) {
        try {
            return Minecraft.getInstance().getModelManager().getModel(key);
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    @Override
    @Nullable
    public Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        if((BlockLensRuntime.config().enabledMask()&oreBorderMask)!=0L
                && BlockLensRuntime.config().isEnabled(CapabilityId.CONNECTED_ORE_EDGES))return null;
        Object wrappedKey = wrapped.createGeometryKey(level, pos, state, random);
        if (wrappedKey == null) {
            return null;
        }
        long enabledCapabilities = BlockLensRuntime.config().enabledMask() & representedRenderableMask;
        if (enabledCapabilities == 0L) {
            return wrappedKey;
        }
        return new VisualGeometryKey(wrappedKey, enabledCapabilities);
    }

    private static DecorationQuadFace face(@Nullable Direction direction) {
        if (direction == null) {
            return DecorationQuadFace.NONE;
        }
        return switch (direction) {
            case NORTH -> DecorationQuadFace.NORTH;
            case EAST -> DecorationQuadFace.EAST;
            case SOUTH -> DecorationQuadFace.SOUTH;
            case WEST -> DecorationQuadFace.WEST;
            case UP -> DecorationQuadFace.UP;
            case DOWN -> DecorationQuadFace.DOWN;
        };
    }

}

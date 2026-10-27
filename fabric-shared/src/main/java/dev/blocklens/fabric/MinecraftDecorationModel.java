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
    private static final long NETHER_TWEAKS_BIT = 1L << CapabilityId.NETHER_TWEAKS.bitPosition();

    private static final long LIGHTWEIGHT_OVERLAY_MASK = (1L << CapabilityId.GLASS_HIGHLIGHT.bitPosition())
            | (1L << CapabilityId.KELP_HIGHLIGHT.bitPosition());
    private final @Nullable RetainedValue<BlockStateModel> lightweightOverlay;
    private final @Nullable RetainedValue<BlockStateModel> oreBorderOverlay;
    private final @Nullable RetainedValue<BlockStateModel> connectedOreBorderOverlay;
    private static final int[] NEIGHBOR_U={0,1,0,-1,1,1,-1,-1};
    private static final int[] NEIGHBOR_V={-1,0,1,0,-1,1,1,-1};
    private final @Nullable RetainedValue<BlockStateModelPart> connectedOrePart;
    private final long oreBorderMask;
    private final int oreBorderTint;

    private static final Identifier DECAL_SPRITE = Identifier.fromNamespaceAndPath("blocklens", DecorationDecals.SPRITE);

    private final Object[] descriptors;
    private final long[] descriptorBits;
    // Interned per (capability, state); present only when the wrapped model is one fixed variant.
    private final long @Nullable [][] decalTables;
    private final long decalMask;
    private final long representedRenderableMask;
    private final @Nullable NetherTweaksVisualCue netherCue;
    private final SemanticState netherState;

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
            long bit = 1L << capability.bitPosition();

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
     * Re-emits the wrapped single-variant geometry once more as decal quads: each kept quad gets
     * the face's clean-room glyph mapped onto its own bounds, lifted just above the surface.
     * Culled faces stay culled because the same cull test is applied.
     */
    private void emitDecals(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos,
            BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest, long decalCapabilities) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
                .getSprite(DECAL_SPRITE);
        if (!DECAL_SPRITE.equals(sprite.contents().name())) {
            return; // Never draw the missing-texture checkerboard over a block.
        }
        emitter.pushTransform(quad -> applyDecal(quad, sprite, decalCapabilities));
        try {
            super.emitQuads(emitter, level, pos, state, random, cullTest);
        } finally {
            emitter.popTransform();
        }
    }

    private boolean applyDecal(MutableQuadView quad, TextureAtlasSprite sprite, long decalCapabilities) {
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
        float minRight = Float.POSITIVE_INFINITY, maxRight = Float.NEGATIVE_INFINITY;
        float minDown = Float.POSITIVE_INFINITY, maxDown = Float.NEGATIVE_INFINITY;
        for (int vertex = 0; vertex < 4; vertex++) {
            float right = DecorationDecals.right(f, quad.x(vertex), quad.y(vertex), quad.z(vertex));
            float down = DecorationDecals.down(f, quad.x(vertex), quad.y(vertex), quad.z(vertex));
            minRight = Math.min(minRight, right);
            maxRight = Math.max(maxRight, right);
            minDown = Math.min(minDown, down);
            maxDown = Math.max(maxDown, down);
        }
        float width = maxRight - minRight, height = maxDown - minDown;
        if (width < 1.0E-4F || height < 1.0E-4F) {
            return false;
        }
        // One vertical strip holds every glyph; stay a hair inside the cell to avoid sampling a neighbour.
        int turns = DecorationDecals.quarterTurns(decal);
        float u0 = sprite.getU0(), du = sprite.getU1() - u0;
        float cell = (sprite.getV1() - sprite.getV0()) / DecorationDecals.GLYPHS;
        float v0 = sprite.getV0() + DecorationDecals.glyph(decal) * cell + cell / 1024.0F;
        float dv = cell - cell / 512.0F;
        float offset = DecorationDecals.SURFACE_OFFSET;
        for (int vertex = 0; vertex < 4; vertex++) {
            float x = quad.x(vertex), y = quad.y(vertex), z = quad.z(vertex);
            float s = (DecorationDecals.right(f, x, y, z) - minRight) / width;
            float t = (DecorationDecals.down(f, x, y, z) - minDown) / height;
            quad.uv(vertex, u0 + DecorationDecals.glyphU(turns, s, t) * du, v0 + DecorationDecals.glyphV(turns, s, t) * dv);
            quad.pos(vertex, x + DecorationDecals.normalX(f) * offset, y + DecorationDecals.normalY(f) * offset,
                    z + DecorationDecals.normalZ(f) * offset);
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
                DecorationQuadFace face = face(quad.nominalFace());
                quad.multiplyColor(fillColor(face));
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
        ExtraModelKey<BlockStateModel> key = (enabled & (1L << CapabilityId.KELP_HIGHLIGHT.bitPosition())) != 0L
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
                float uOffset=(sprite.getU1()-sprite.getU0())*(mask%16)/16;
                float vOffset=(sprite.getV1()-sprite.getV0())*(mask/16)/16;
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

    private static boolean unitCubeFace(MutableQuadView quad,Direction face) {
        int corners=0;
        for(int vertex=0;vertex<4;vertex++) {
            float x=quad.x(vertex),y=quad.y(vertex),z=quad.z(vertex);
            if((x!=0&&x!=1)||(y!=0&&y!=1)||(z!=0&&z!=1))return false;
            float plane=switch(face.getAxis()){case X->x;case Y->y;case Z->z;};
            if(plane!=(face.getAxisDirection()==Direction.AxisDirection.POSITIVE ? 1 : 0))return false;
            int corner=switch(face.getAxis()){case X->(int)y+2*(int)z;case Y->(int)x+2*(int)z;case Z->(int)x+2*(int)y;};
            corners|=1<<corner;
        }
        return corners==15;
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
        Direction u=switch(face){case NORTH->Direction.WEST;case EAST->Direction.NORTH;case WEST->Direction.SOUTH;default->Direction.EAST;};
        Direction v=switch(face){case UP->Direction.SOUTH;case DOWN->Direction.NORTH;default->Direction.DOWN;};
        var neighbor=new BlockPos.MutableBlockPos();var front=new BlockPos.MutableBlockPos();int mask=0;
        for(int bit=0;bit<8;bit++) {
            neighbor.set(pos.getX()+u.getStepX()*NEIGHBOR_U[bit]+v.getStepX()*NEIGHBOR_V[bit],
                    pos.getY()+u.getStepY()*NEIGHBOR_U[bit]+v.getStepY()*NEIGHBOR_V[bit],
                    pos.getZ()+u.getStepZ()*NEIGHBOR_U[bit]+v.getStepZ()*NEIGHBOR_V[bit]);
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

    private int fillColor(DecorationQuadFace face) {
        SemanticState.Axis axis = netherState.axis();
        if (axis != SemanticState.Axis.NONE) {
            boolean endFace = switch (axis) {
                case X -> face == DecorationQuadFace.EAST || face == DecorationQuadFace.WEST;
                case Y -> face == DecorationQuadFace.UP || face == DecorationQuadFace.DOWN;
                case Z -> face == DecorationQuadFace.NORTH || face == DecorationQuadFace.SOUTH;
                case NONE -> false;
            };
            return endFace ? netherCue.upFillArgb() : netherCue.sideFillArgb();
        }
        return switch (face) {
            case UP -> netherCue.upFillArgb();
            case DOWN -> netherCue.downFillArgb();
            default -> netherCue.sideFillArgb();
        };
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

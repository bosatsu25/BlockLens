package dev.blocklens.gametest;

import dev.blocklens.core.BlockLensRuntime;
import dev.blocklens.core.analyzer.AnalyzerCatalog;
import dev.blocklens.fabric.AnalyzerClient;
import dev.blocklens.fabric.SceneFilterClient;
import dev.blocklens.fabric.mixin.ClientEntityStorageAccess;
import dev.blocklens.fabric.mixin.EntitySectionStorageAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;

/** Test-only, fixed masks/booleans. Never emits entity identifiers, coordinates, or memory contents. */
final class AnalyzerReturnProbe {
    enum Stage { SEEDED, TIMEOUT }
    record Seed(ClientLevel level, Villager villager) { }
    private static final BlockPos SITE = new BlockPos(2, -59, 6);
    private static final BlockPos[] FIXTURES = {
            new BlockPos(-3, -59, 4), new BlockPos(0, -59, 4),
            new BlockPos(3, -59, 4), new BlockPos(-6, -59, 4), SITE
    };

    private AnalyzerReturnProbe() { }

    /** Call on the client thread immediately after assigning the controlled JOB_SITE memory. */
    static void report(Minecraft client, Seed seed, Stage stage) {
        int markerMask = 0;
        for (var marker : AnalyzerClient.engine().markers()) {
            if (marker.kind() >= 0 && marker.kind() < 5) markerMask |= 1 << marker.kind();
        }
        ClientLevel level = client.level;
        boolean context = level != null && client.player != null;
        int loadedMask = 0, physicalMask = 0, baseMask = 0;
        if (level != null) {
            for (int kind = 0; kind < FIXTURES.length; kind++) {
                BlockState state = loaded(level, FIXTURES[kind]);
                if (state == null) continue;
                loadedMask |= 1 << kind;
                boolean expected = switch (kind) {
                    case 0 -> state.is(Blocks.ANCIENT_DEBRIS);
                    case 1 -> state.is(Blocks.LAVA) && state.getFluidState().isSource();
                    case 2 -> state.is(Blocks.BEACON);
                    case 3 -> state.getBlock() instanceof net.minecraft.world.level.block.LightningRodBlock;
                    case 4 -> state.is(Blocks.BARREL);
                    default -> false;
                };
                if (expected && !SceneFilterClient.hiddenBlock(state.getBlock())) physicalMask |= 1 << kind;
            }
            for (int x = 2; x <= 4; x++) for (int z = 3; z <= 5; z++) {
                BlockState state = loaded(level, new BlockPos(x, -60, z));
                if (state != null && state.is(Blocks.IRON_BLOCK)) baseMask |= 1 << ((x - 2) * 3 + z - 3);
            }
        }
        // Entity ID is used only for an in-memory identity check; it is never included in output.
        var tracked = level == null ? null : level.getEntity(seed.villager().getId());
        Villager current = tracked instanceof Villager villager ? villager : null;
        var memory = current == null ? java.util.Optional.<net.minecraft.core.GlobalPos>empty()
                : current.getBrain().getMemory(MemoryModuleType.JOB_SITE);
        boolean accessible = false, sectionContains = false, villagerInRange = false, siteInRange = false;
        boolean eyeAnchor = false;
        if (context) {
            var eye = client.player.getEyePosition();
            eyeAnchor = Math.floor(eye.x) == 0 && Math.floor(eye.y) == -55 && Math.floor(eye.z) == 12;
            var options = BlockLensRuntime.config().analyzerOptions();
            int h = options.value(4, 0), v = options.value(4, 1);
            siteInRange = Math.abs(SITE.getX() - Math.floor(eye.x)) <= h
                    && Math.abs(SITE.getY() - Math.floor(eye.y)) <= v
                    && Math.abs(SITE.getZ() - Math.floor(eye.z)) <= h;
            if (current != null) {
                villagerInRange = Math.abs(current.getX() - eye.x) <= h
                        && Math.abs(current.getY() - eye.y) <= v && Math.abs(current.getZ() - eye.z) <= h;
                var manager = ((ClientEntityStorageAccess) level).blocklens$entityStorage();
                var storage = ((EntitySectionStorageAccess) manager).blocklens$sections();
                var section = storage.getSection(SectionPos.asLong(current.blockPosition()));
                accessible = section != null && section.getStatus().isAccessible();
                if (section != null) {
                    try (var entities = section.getEntities()) {
                        sectionContains = entities.limit(64).anyMatch(entity -> entity == current);
                    }
                }
            }
        }
        var work = AnalyzerClient.engine().work();
        System.out.println("BLOCKLENS_ANALYZER_RETURN stage=" + stage.name()
                + " markerMask=" + markerMask + " configMask=" + AnalyzerCatalog.mask(BlockLensRuntime.config())
                + " loadedMask=" + loadedMask + " physicalMask=" + physicalMask + " beaconBaseMask=" + baseMask
                + " overworld=" + (level != null && level.dimension().equals(Level.OVERWORLD))
                + " sameLevel=" + (level == seed.level()) + " sameVillager=" + (current == seed.villager())
                + " trackedVillager=" + (current != null) + " alive=" + (current != null && current.isAlive())
                + " entityVisible=" + (current != null && !SceneFilterClient.hiddenEntity(current))
                + " memoryPresent=" + memory.isPresent()
                + " memoryDimension=" + (memory.isPresent() && level != null && memory.get().dimension().equals(level.dimension()))
                + " memoryExpectedSite=" + (memory.isPresent() && memory.get().pos().equals(SITE))
                + " eyeAnchor=" + eyeAnchor + " villagerInRange=" + villagerInRange + " siteInRange=" + siteInRange
                + " entitySectionAccessible=" + accessible + " entityInSection=" + sectionContains
                + " flying=" + (context && client.player.getAbilities().flying)
                + " cells=" + work.cells() + " sections=" + work.sections() + " steps=" + work.cursorSteps()
                + " entitySections=" + work.entitySections() + " entities=" + work.entities());
    }

    private static BlockState loaded(ClientLevel level, BlockPos position) {
        var chunk = level.getChunkSource().getChunk(position.getX() >> 4, position.getZ() >> 4, ChunkStatus.FULL, false);
        return chunk == null ? null : chunk.getBlockState(position);
    }
}

package dev.blocklens.fabric;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Loaded only after the exact optional-version guard; no reflection or schematic mutation. */
final class LitematicaStateReader {
    private LitematicaStateReader() { }
    static BlockState read(BlockPos position) {
        var world = SchematicWorldHandler.getSchematicWorld();
        if (world == null || position.getY() < world.getMinY() || position.getY() >= world.getMaxY()) return null;
        var chunk = world.getChunkSource().getChunkIfExists(Math.floorDiv(position.getX(), 16), Math.floorDiv(position.getZ(), 16));
        if (chunk == null) return null;
        var state = chunk.getBlockState(position);
        return state.isAir() ? null : state;
    }
}

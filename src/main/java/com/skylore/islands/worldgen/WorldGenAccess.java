package com.skylore.islands.worldgen;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class WorldGenAccess {
    private WorldGenAccess() {}

    /** True when block queries are safe during structure generation. */
    public static boolean canAccess(LevelAccessor level, int blockX, int blockZ) {
        int chunkX = blockX >> 4;
        int chunkZ = blockZ >> 4;
        try {
            if (level instanceof WorldGenLevel worldGen) {
                // getChunk loads/creates during worldgen; hasChunk falsely rejects multi-chunk structures.
                return worldGen.getChunk(chunkX, chunkZ) != null;
            }
            return level.getChunk(chunkX, chunkZ, ChunkStatus.STRUCTURE_STARTS, false) != null
                    || level.getChunk(chunkX, chunkZ, ChunkStatus.EMPTY, false) != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}

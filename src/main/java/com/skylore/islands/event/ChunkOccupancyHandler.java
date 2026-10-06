package com.skylore.islands.event;

import com.skylore.islands.worldgen.ChunkOccupancy;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

/**
 * Persist occupancy on disk. Old chunks with no NBT infer VOID / ISLAND / PLAYER on first load.
 * 1.0.49 VOID light skip left dark layers; relight those once.
 */
public final class ChunkOccupancyHandler {
    private ChunkOccupancyHandler() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        LevelAccessor level = event.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        if (level instanceof net.minecraft.world.level.Level world) {
            CellularIslandDensityFunction.bindFromLevel(world);
        }
        ChunkAccess chunk = event.getChunk();
        if (chunk == null) {
            return;
        }
        ChunkOccupancy.Ticket ticket = ChunkOccupancy.get(chunk);
        if (!(level instanceof ServerLevel serverLevel) || !ChunkOccupancy.takeLightRepair(chunk)) {
            return;
        }
        Runnable relight = () -> relightIfLoaded(serverLevel, chunk.getPos());
        if (ticket.kind == ChunkOccupancy.Kind.ISLAND || ticket.kind == ChunkOccupancy.Kind.PLAYER) {
            serverLevel.getServer().tell(new TickTask(serverLevel.getServer().getTickCount() + 4, relight));
        } else {
            relight.run();
        }
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event) {
        if (event.getLevel() != null && event.getLevel().isClientSide()) {
            return;
        }
        ChunkOccupancy.writeToChunkNbt(event.getChunk(), event.getData());
    }

    @SubscribeEvent
    public static void onChunkDataLoad(ChunkDataEvent.Load event) {
        if (event.getLevel() != null && event.getLevel().isClientSide()) {
            return;
        }
        ChunkOccupancy.readFromChunkNbt(event.getChunk(), event.getData());
    }

    private static void relightIfLoaded(ServerLevel level, ChunkPos pos) {
        if (!level.getChunkSource().hasChunk(pos.x, pos.z)) {
            return;
        }
        ChunkAccess chunk = level.getChunk(pos.x, pos.z);
        LevelLightEngine engine = level.getChunkSource().getLightEngine();
        if (engine instanceof ThreadedLevelLightEngine threaded) {
            threaded.lightChunk(chunk, false).thenRun(() -> ChunkOccupancy.markLightOk(chunk));
        }
    }
}

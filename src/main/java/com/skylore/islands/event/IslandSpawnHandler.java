package com.skylore.islands.event;

import com.skylore.islands.config.SkyloreConfig;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

/**
 * Vanilla spawn search walks thousands of blocks looking for land in the void.
 * Pin world spawn to the guaranteed origin island without calling getHeight during ServerLevel init.
 */
public class IslandSpawnHandler {

    @SubscribeEvent
    public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }
        CellularIslandDensityFunction.IslandLayout origin = CellularIslandDensityFunction.layoutForCell(
                0, 0,
                SkyloreConfig.cellSize(),
                SkyloreConfig.minIslandAltitude(),
                SkyloreConfig.maxIslandAltitude(),
                SkyloreConfig.minIslandRadius(),
                SkyloreConfig.maxIslandRadius());
        if (origin == null) {
            return;
        }
        event.getSettings().setSpawn(new BlockPos(origin.centerX, origin.altitude + 8, origin.centerZ), 0.0f);
        event.setCanceled(true);
    }
}

package com.skylore.islands.worldgen;

import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Overworld vs Ignis layout bind. Implemented by the noise generator mixin;
 * static helpers are the one place other code should call.
 */
public interface IslandLayoutBinder {
    void skylore$bindIslandLayout();

    static void bind(ChunkGenerator generator) {
        if (generator instanceof IslandLayoutBinder binder) {
            binder.skylore$bindIslandLayout();
        }
    }

    static void bindFromLevel(Level level) {
        CellularIslandDensityFunction.setLayoutSalt(
                Level.NETHER.equals(level.dimension())
                        ? CellularIslandDensityFunction.NETHER_SALT
                        : CellularIslandDensityFunction.OVERWORLD_SALT);
    }

    static void bindFromHeightAccessor(LevelHeightAccessor height) {
        if (height instanceof ServerLevelAccessor sla) {
            bindFromLevel(sla.getLevel());
        }
    }
}

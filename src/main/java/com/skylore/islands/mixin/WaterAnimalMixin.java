package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows water animals to spawn in sky island ocean basins in 1.20.1 Forge.
 */
@Mixin(WaterAnimal.class)
public abstract class WaterAnimalMixin {

    @Inject(method = "checkSurfaceWaterAnimalSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void skylore$allowSkyOceanSpawn(
            EntityType<? extends WaterAnimal> type,
            LevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CellularIslandDensityFunction.isInsideWaterBasin(pos.getX(), pos.getY(), pos.getZ())) {
            boolean isWater = level.getBlockState(pos).is(Blocks.WATER);
            boolean isWaterBelow = level.getBlockState(pos.below()).is(Blocks.WATER);
            if (isWater && isWaterBelow) {
                cir.setReturnValue(true);
            }
        }
    }
}

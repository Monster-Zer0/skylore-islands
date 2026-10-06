package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows sea turtles to spawn along sky island beaches in 1.20.1 Forge.
 */
@Mixin(Turtle.class)
public abstract class TurtleMixin {

    @Inject(method = "checkTurtleSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void skylore$allowSkyTurtleSpawn(
            EntityType<Turtle> type,
            LevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CellularIslandDensityFunction.isInsideWaterBasin(pos.getX(), pos.getY(), pos.getZ())
                || CellularIslandDensityFunction.isInsideWaterBasin(pos.getX(), pos.getY() - 4, pos.getZ())) {
            boolean isSand = level.getBlockState(pos.below()).is(Blocks.SAND);
            if (isSand && level.getRawBrightness(pos, 0) > 8) {
                cir.setReturnValue(true);
            }
        }
    }
}

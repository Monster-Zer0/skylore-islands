package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

/**
 * Cave/canyon carvers replace water with air because sea level is -64.
 * That punches square ravine holes in ocean-island bowls.
 */
@Mixin(WorldCarver.class)
public abstract class WorldCarverMixin {

    @Inject(method = "carveBlock", at = @At("HEAD"), cancellable = true)
    private void skylore$protectSkyWater(
            CarvingContext context,
            CarverConfiguration config,
            ChunkAccess chunk,
            Function<BlockPos, Holder<Biome>> biomeGetter,
            CarvingMask carvingMask,
            BlockPos.MutableBlockPos pos,
            BlockPos.MutableBlockPos checkPos,
            Aquifer aquifer,
            MutableBoolean reachedSurface,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CellularIslandDensityFunction.shouldSkipCarver(pos.getX(), pos.getY(), pos.getZ())) {
            cir.setReturnValue(false);
        }
    }
}

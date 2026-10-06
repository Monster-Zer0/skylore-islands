package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.ChunkOccupancy;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Generation-only. Does not strip player blocks. Structures do not go through this method.
 */
@Mixin(PlacedFeature.class)
public abstract class PlacedFeatureMixin {

    @Inject(method = "placeWithContext", at = @At("HEAD"), cancellable = true)
    private void skylore$skipVoidBiomeFeatures(
            PlacementContext context,
            RandomSource random,
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (ChunkOccupancy.skipBiomeFeatures()) {
            cir.setReturnValue(false);
        }
    }
}

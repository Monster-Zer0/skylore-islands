package com.skylore.islands.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla: shouldSkipAirCheck == true means SKIP the air test and place anyway.
 * Returning true here made surface ores worse. Always return false so canPlaceOre
 * requires !isAdjacentToAir (same as discardChanceOnAirExposure = 1.0).
 */
@Mixin(OreFeature.class)
public abstract class OreFeatureMixin {

    @Inject(method = "shouldSkipAirCheck", at = @At("HEAD"), cancellable = true)
    private static void skylore$neverSkipAirExposure(RandomSource random, float discardChanceOnAirExposure, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}

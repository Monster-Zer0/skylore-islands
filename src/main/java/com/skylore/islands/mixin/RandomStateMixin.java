package com.skylore.islands.mixin;

import com.skylore.islands.SkyloreIslands;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.HolderGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Island hashes and shape noise used a fixed table, so every world looked the same.
 * Capture the world seed when RandomState is built.
 */
@Mixin(RandomState.class)
public abstract class RandomStateMixin {

    @Inject(
            method = "create(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)Lnet/minecraft/world/level/levelgen/RandomState;",
            at = @At("HEAD")
    )
    private static void skylore$captureWorldSeed(
            NoiseGeneratorSettings settings,
            HolderGetter<?> noises,
            long seed,
            CallbackInfoReturnable<RandomState> cir
    ) {
        long current = CellularIslandDensityFunction.worldSeed();
        if (current != seed) {
            if (current != 0L) {
                SkyloreIslands.LOGGER.warn("RandomState seed {} differs from active world seed {}; using it for island layout", seed, current);
            }
            CellularIslandDensityFunction.setWorldSeed(seed);
        }
        boolean nether = settings.defaultBlock().is(Blocks.NETHERRACK);
        CellularIslandDensityFunction.setLayoutSalt(
                nether ? CellularIslandDensityFunction.NETHER_SALT : CellularIslandDensityFunction.OVERWORLD_SALT);
    }
}

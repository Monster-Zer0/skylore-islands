package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.ChunkOccupancy;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Keep structure placement; skip biome features on VOID / VOID_STRUCTURE via PlacedFeatureMixin.
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {

    @Inject(method = "applyBiomeDecoration", at = @At("HEAD"))
    private void skylore$beginVoidDecoration(
            WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structures,
            CallbackInfo ci
    ) {
        ChunkOccupancy.beginDecoration(chunk);
    }

    @Inject(method = "applyBiomeDecoration", at = @At("RETURN"))
    private void skylore$endVoidDecoration(
            WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structures,
            CallbackInfo ci
    ) {
        ChunkOccupancy.endDecoration();
    }
}

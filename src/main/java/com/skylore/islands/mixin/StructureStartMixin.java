package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.StructureIslandPolicy;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks the structure being placed so ProtoChunkMixin can strip support legs that
 * processors write straight into the chunk below the structure.
 */
@Mixin(StructureStart.class)
public abstract class StructureStartMixin {

    @Inject(method = "placeInChunk", at = @At("HEAD"))
    private void skylore$beginPlacement(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
            RandomSource random, BoundingBox box, ChunkPos chunkPos, CallbackInfo ci) {
        CellularIslandDensityFunction.bindFromLevel(level.getLevel());
        StructureIslandPolicy.beginStructurePlacement(((StructureStart) (Object) this).getBoundingBox());
    }

    @Inject(method = "placeInChunk", at = @At("RETURN"))
    private void skylore$endPlacement(WorldGenLevel level, StructureManager structures, ChunkGenerator generator,
            RandomSource random, BoundingBox box, ChunkPos chunkPos, CallbackInfo ci) {
        StructureIslandPolicy.endStructurePlacement();
    }
}

package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.ChunkGenColumnCache;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fills sky ocean basins and crater lakes with water source blocks with per-block precision in 1.20.1 Forge.
 */
@Mixin(NoiseChunk.class)
public abstract class NoiseChunkMixin {

    /** final_density is interpolated, so the stone floor can sit a little below the exact seabed. */
    private static final int FLOOR_PATCH_DEPTH = 4;

    @Shadow public abstract int blockX();
    @Shadow public abstract int blockY();
    @Shadow public abstract int blockZ();

    @Inject(method = "getInterpolatedState", at = @At("RETURN"), cancellable = true)
    private void skylore$fillWaterBasins(CallbackInfoReturnable<BlockState> cir) {
        BlockState original = cir.getReturnValue();
        if (original == null || original.isAir()) {
            int y = this.blockY();
            if (patchBasinFloor(this.blockX(), y, this.blockZ(), cir)) {
                return;
            }
            if (!CellularIslandDensityFunction.couldHaveWaterAtY(y)) {
                return;
            }
            int x = this.blockX();
            int z = this.blockZ();
            if (ChunkGenColumnCache.isActiveFor(x, z)
                    && (!ChunkGenColumnCache.waterInteriorAt(x, z) || ChunkGenColumnCache.layoutAt(x, z) == null)) {
                return;
            }
            if (CellularIslandDensityFunction.isInsideInteriorWaterBasin(x, y, z)) {
                cir.setReturnValue(CellularIslandDensityFunction.basinFluid());
            }
        }
    }

    private static boolean patchBasinFloor(int x, int y, int z, CallbackInfoReturnable<BlockState> cir) {
        if (!ChunkGenColumnCache.isActiveFor(x, z) || !ChunkGenColumnCache.waterInteriorAt(x, z)) {
            return false;
        }
        CellularIslandDensityFunction.WaterColumn water = ChunkGenColumnCache.waterAt(x, z);
        if (water == null || y > water.seabedY || y <= water.seabedY - FLOOR_PATCH_DEPTH) {
            return false;
        }
        cir.setReturnValue(CellularIslandDensityFunction.usingNetherLayout()
                ? Blocks.NETHERRACK.defaultBlockState()
                : Blocks.STONE.defaultBlockState());
        return true;
    }
}

package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.StructureIslandPolicy;
import com.skylore.islands.worldgen.WorldGenAccess;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents procedural structure pieces from carving air into water basins and prevents
 * endless pillar legs from generating downward into the open sky void in 1.20.1 Forge.
 */
@Mixin(StructurePiece.class)
public abstract class StructurePieceMixin {

    @Shadow protected abstract int getWorldX(int x, int z);
    @Shadow protected abstract int getWorldY(int y);
    @Shadow protected abstract int getWorldZ(int x, int z);

    @Inject(method = "placeBlock", at = @At("HEAD"), cancellable = true)
    private void skylore$filterVoidPillarsAndPreserveWater(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box, CallbackInfo ci) {
        int blockX = this.getWorldX(x, z);
        int blockY = this.getWorldY(y);
        int blockZ = this.getWorldZ(x, z);
        BlockPos worldPos = new BlockPos(blockX, blockY, blockZ);
        if (!box.isInside(worldPos)) {
            return;
        }
        if (!WorldGenAccess.canAccess(level, blockX, blockZ)) {
            return;
        }
        CellularIslandDensityFunction.bindFromLevel(level.getLevel());

        if (state.isAir() && CellularIslandDensityFunction.couldHaveWaterAtY(blockY)) {
            if (CellularIslandDensityFunction.isInsideWaterBasin(blockX, blockY, blockZ)) {
                level.setBlock(new BlockPos(blockX, blockY, blockZ), CellularIslandDensityFunction.basinFluid(), 2);
                ci.cancel();
            }
            return;
        }
        if (!state.isAir() && !StructureIslandPolicy.allowsPlacement(blockX, blockY, blockZ)) {
            ci.cancel();
        }
    }

    @Inject(method = "fillColumnDown", at = @At("HEAD"), cancellable = true)
    private void skylore$clampPillarsToGround(WorldGenLevel level, BlockState state, int x, int y, int z, BoundingBox box, CallbackInfo ci) {
        int blockX = this.getWorldX(x, z);
        int startY = this.getWorldY(y);
        int blockZ = this.getWorldZ(x, z);

        if (!WorldGenAccess.canAccess(level, blockX, blockZ)) {
            ci.cancel();
            return;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(blockX, startY, blockZ);

        int placed = 0;
        while (placed < 3 && pos.getY() >= 35) {
            if (!WorldGenAccess.canAccess(level, pos.getX(), pos.getZ())) {
                break;
            }
            if (!StructureIslandPolicy.allowsPlacement(pos.getX(), pos.getY(), pos.getZ())) {
                break;
            }
            BlockState current = level.getBlockState(pos);
            if (current.isSolid()) {
                break;
            }
            level.setBlock(pos, state, 2);
            pos.move(0, -1, 0);
            placed++;
        }
        ci.cancel();
    }
}

package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.ChunkOccupancy;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Water must stay on islands. Flow into empty sky and long cliff sheets
 * cause huge fluid-tick lag. Short on-island waterfalls are still allowed.
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void skylore$containSkyWater(
            LevelAccessor level,
            BlockPos pos,
            BlockState blockState,
            Direction direction,
            FluidState fluidState,
            CallbackInfo ci
    ) {
        if (!fluidState.is(FluidTags.WATER) && !fluidState.is(FluidTags.LAVA)) {
            return;
        }
        if (isVoidOccupancy(level, pos)) {
            ci.cancel();
            return;
        }
        bindLayout(level);
        if (!CellularIslandDensityFunction.hasIslandColumn(pos.getX(), pos.getZ())) {
            ci.cancel();
            return;
        }
        if (direction == Direction.DOWN && blockState.isAir()
                && !CellularIslandDensityFunction.isInsideWaterBasin(pos.getX(), pos.getY(), pos.getZ())
                && !hasSupportWithin(level, pos, 3, fluidState)) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void skylore$clearVoidWater(Level level, BlockPos pos, FluidState state, CallbackInfo ci) {
        if (!state.is(FluidTags.WATER) && !state.is(FluidTags.LAVA)) {
            return;
        }
        if (isVoidOccupancy(level, pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            ci.cancel();
            return;
        }
        CellularIslandDensityFunction.bindFromLevel(level);
        if (!CellularIslandDensityFunction.hasIslandColumn(pos.getX(), pos.getZ())) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            ci.cancel();
        }
    }

    private static boolean isVoidOccupancy(LevelAccessor level, BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        if (!level.hasChunk(cx, cz)) {
            return false;
        }
        return ChunkOccupancy.get(level.getChunk(cx, cz)).skipAllRandomTicks();
    }

    private static void bindLayout(LevelAccessor level) {
        if (level instanceof Level lvl) {
            CellularIslandDensityFunction.bindFromLevel(lvl);
        } else if (level instanceof ServerLevelAccessor sla) {
            CellularIslandDensityFunction.bindFromLevel(sla.getLevel());
        }
    }

    private static boolean hasSupportWithin(LevelAccessor level, BlockPos pos, int maxDrop, FluidState fluidState) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 1; i <= maxDrop; i++) {
            cursor.set(pos.getX(), pos.getY() - i, pos.getZ());
            BlockState below = level.getBlockState(cursor);
            if (below.isSolid()) {
                return true;
            }
            if (!below.isAir() && below.getFluidState().is(fluidState.getType())) {
                return true;
            }
        }
        return false;
    }
}

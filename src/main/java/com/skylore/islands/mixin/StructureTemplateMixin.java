package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.StructureIslandPolicy;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Prevents structure templates from placing blocks into the open void floor below Y=35
 * and preserves water basins.
 */
@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {

    @Redirect(
            method = "placeInWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ServerLevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            )
    )
    private boolean skylore$filterVoidBlocksAndPreserveWater(ServerLevelAccessor level, BlockPos pos, BlockState state, int flags) {
        if (!com.skylore.islands.worldgen.WorldGenAccess.canAccess(level, pos.getX(), pos.getZ())) {
            return false;
        }
        CellularIslandDensityFunction.bindFromLevel(level.getLevel());

        if (CellularIslandDensityFunction.couldHaveWaterAtY(pos.getY())
                && CellularIslandDensityFunction.isInsideWaterBasin(pos.getX(), pos.getY(), pos.getZ())) {
            if (state.isAir() || state.is(Blocks.BARRIER)) {
                return level.setBlock(pos, CellularIslandDensityFunction.basinFluid(), flags);
            }
            if (!CellularIslandDensityFunction.usingNetherLayout()
                    && state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                state = state.setValue(BlockStateProperties.WATERLOGGED, true);
            }
        }
        if (!state.isAir() && !StructureIslandPolicy.allowsPlacement(pos.getX(), pos.getY(), pos.getZ())) {
            return false;
        }
        return level.setBlock(pos, state, flags);
    }
}

package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.StructureIslandPolicy;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Leg processors (YUNG's Better Strongholds / Dungeons and similar) fill air downward to the
 * build floor via chunk.setBlockState, skipping every placement filter. Below the structure's
 * own box, only allow those blocks inside island rock.
 */
@Mixin(ProtoChunk.class)
public abstract class ProtoChunkMixin {

    @Inject(method = "setBlockState", at = @At("HEAD"), cancellable = true)
    private void skylore$stripVoidLegs(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        BoundingBox box = StructureIslandPolicy.activePlacementBox();
        if (box == null || state.isAir() || pos.getY() >= box.minY()) {
            return;
        }
        if (!CellularIslandDensityFunction.isInsideIslandStone(pos.getX(), pos.getY(), pos.getZ())) {
            cir.setReturnValue(null);
        }
    }
}

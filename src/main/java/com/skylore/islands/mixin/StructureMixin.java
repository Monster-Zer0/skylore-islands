package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.StructureIslandPolicy;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionStructure;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Void / under-island filter only. Specific structure mixins own vertical placement; this must not
 * snap buried mineshafts/strongholds/jigsaws back to the surface.
 */
@Mixin(Structure.class)
public abstract class StructureMixin {

    @Inject(method = "findValidGenerationPoint", at = @At("HEAD"))
    private void skylore$bindIslandLayout(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        CellularIslandDensityFunction.bindFromGenerator(context.chunkGenerator());
    }

    @Inject(method = "findValidGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void skylore$filterVoidStructures(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        try {
            CellularIslandDensityFunction.bindFromGenerator(context.chunkGenerator());
            Optional<Structure.GenerationStub> opt = cir.getReturnValue();
            if (opt == null || opt.isEmpty()) {
                return;
            }

            Structure.GenerationStub stub = opt.get();
            BlockPos pos = stub.position();

            int actualSurfaceY = context.chunkGenerator().getBaseHeight(
                    pos.getX(), pos.getZ(),
                    Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(),
                    context.randomState()
            );

            if (!StructureIslandPolicy.allowsGenerationStub(
                    pos.getX(), pos.getY(), pos.getZ(), actualSurfaceY,
                    (Structure) (Object) this, context.registryAccess())) {
                cir.setReturnValue(Optional.empty());
                return;
            }
            if ((Object) this instanceof WoodlandMansionStructure) {
                ChunkPos chunk = context.chunkPos();
                if (!StructureIslandPolicy.isMegaIslandAt(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ())) {
                    cir.setReturnValue(Optional.empty());
                }
            }
        } finally {
            StructureIslandPolicy.clearVoidCraftStub();
        }
    }

    @Inject(method = "terrainAdaptation", at = @At("RETURN"), cancellable = true)
    private void skylore$disableBeardLegs(CallbackInfoReturnable<TerrainAdjustment> cir) {
        TerrainAdjustment adjustment = cir.getReturnValue();
        if (adjustment == TerrainAdjustment.BEARD_THIN || adjustment == TerrainAdjustment.BEARD_BOX) {
            cir.setReturnValue(TerrainAdjustment.NONE);
        }
    }
}

package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.StructureStubMover;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.OceanRuinStructure;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin({OceanRuinStructure.class, ShipwreckStructure.class})
public abstract class SkyWaterStructureMixin {

    @Inject(method = "findGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void skylore$placeInSkyWater(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        Optional<Structure.GenerationStub> opt = cir.getReturnValue();
        if (opt == null || opt.isEmpty()) {
            return;
        }
        ChunkPos chunk = context.chunkPos();
        CellularIslandDensityFunction.WaterColumn column = CellularIslandDensityFunction.waterColumnAt(
                chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());
        if (column == null) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        int sub = (Object) this instanceof ShipwreckStructure ? 3 : 4;
        int targetY = column.oceanBowl ? Math.max(column.seabedY + 1, column.waterLevel - sub) : column.waterLevel;
        cir.setReturnValue(StructureStubMover.atY(opt.get(), targetY));
    }
}

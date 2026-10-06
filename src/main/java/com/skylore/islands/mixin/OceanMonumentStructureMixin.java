package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.StructureStubMover;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.OceanMonumentStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(OceanMonumentStructure.class)
public abstract class OceanMonumentStructureMixin {

    @Inject(method = "findGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void skylore$placeInSkyBowl(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        Optional<Structure.GenerationStub> opt = cir.getReturnValue();
        if (opt == null || opt.isEmpty()) {
            return;
        }
        ChunkPos chunk = context.chunkPos();
        CellularIslandDensityFunction.IslandLayout layout = CellularIslandDensityFunction.layoutContaining(
                chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());
        if (layout == null || layout.archetype != 4) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        CellularIslandDensityFunction.WaterColumn column = CellularIslandDensityFunction.waterColumnAt(
                layout.centerX, layout.centerZ);
        if (column == null || !column.oceanBowl) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        // Monument is 58x23x58 with pieces hardcoded at Y=39. Sit it in the bowl, not under the island.
        int destMinY = Math.max(column.seabedY + 4, column.waterLevel - 22);
        cir.setReturnValue(StructureStubMover.alignMinCorner(
                opt.get(),
                layout.centerX - 29,
                destMinY,
                layout.centerZ - 29));
    }
}

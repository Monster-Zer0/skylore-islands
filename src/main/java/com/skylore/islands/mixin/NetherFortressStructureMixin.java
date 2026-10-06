package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.StructureStubMover;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.NetherFortressStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Snaps nether fortresses onto Ignis island stone instead of vanilla Y in the void.
 */
@Mixin(NetherFortressStructure.class)
public abstract class NetherFortressStructureMixin {

    @Inject(method = "findGenerationPoint", at = @At("RETURN"), cancellable = true)
    private void skylore$elevateFortress(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        CellularIslandDensityFunction.bindFromGenerator(context.chunkGenerator());
        CellularIslandDensityFunction.bindFromHeightAccessor(context.heightAccessor());
        Optional<Structure.GenerationStub> opt = cir.getReturnValue();
        if (opt == null || opt.isEmpty()) {
            return;
        }
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        if (!CellularIslandDensityFunction.hasIslandColumn(x, z)) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        int surfaceY = context.chunkGenerator().getBaseHeight(
                x, z,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );
        if (surfaceY <= 35) {
            cir.setReturnValue(Optional.empty());
            return;
        }
        cir.setReturnValue(StructureStubMover.atY(opt.get(), Math.max(40, surfaceY - 8)));
    }
}

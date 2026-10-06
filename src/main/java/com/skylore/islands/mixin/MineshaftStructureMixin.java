package com.skylore.islands.mixin;

import com.mojang.datafixers.util.Either;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftStructure;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Elevates procedural Mineshafts into the solid stone core of floating sky islands in 1.20.1 Forge.
 */
@Mixin(MineshaftStructure.class)
public abstract class MineshaftStructureMixin extends Structure {

    @Shadow @Final private MineshaftStructure.Type type;

    protected MineshaftStructureMixin(StructureSettings settings) {
        super(settings);
    }

    @Inject(method = "findGenerationPoint", at = @At("HEAD"), cancellable = true)
    private void skylore$elevateMineshaft(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        ChunkPos chunkpos = context.chunkPos();
        int blockX = chunkpos.getMiddleBlockX();
        int blockZ = chunkpos.getMinBlockZ();

        if (!CellularIslandDensityFunction.hasIslandColumn(blockX, blockZ)) {
            cir.setReturnValue(Optional.empty());
            return;
        }

        int surfaceY = context.chunkGenerator().getBaseHeight(
                blockX, blockZ,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );

        if (surfaceY > 30) {
            StructurePiecesBuilder builder = new StructurePiecesBuilder();
            MineshaftPieces.MineShaftRoom room = new MineshaftPieces.MineShaftRoom(0, context.random(), chunkpos.getBlockX(2), chunkpos.getBlockZ(2), this.type);
            builder.addPiece(room);
            room.addChildren(room, builder, context.random());

            int centerCurrentY = builder.getBoundingBox().getCenter().getY();
            int targetY = Math.max(40, surfaceY - 22);
            int deltaY = targetY - centerCurrentY;

            builder.offsetPiecesVertically(deltaY);

            BlockPos startPos = new BlockPos(blockX, targetY, blockZ);
            cir.setReturnValue(Optional.of(new Structure.GenerationStub(startPos, Either.right(builder))));
        } else {
            cir.setReturnValue(Optional.empty());
        }
    }
}

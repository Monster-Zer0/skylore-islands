package com.skylore.islands.mixin;

import com.mojang.datafixers.util.Either;
import com.skylore.islands.worldgen.StructureStubMover;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdStructure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * Places strongholds only on mega floating continents, fully inside the stone volume.
 */
@Mixin(StrongholdStructure.class)
public abstract class StrongholdStructureMixin extends Structure {

    private static final int BURY_BELOW_SURFACE = 12;
    private static final int MIN_FLOOR_Y = 40;
    private static final double FOOTPRINT_MARGIN = 0.58;
    private static final double DENSITY_STONE_THRESHOLD = 0.12;

    protected StrongholdStructureMixin(StructureSettings settings) {
        super(settings);
    }

    @Inject(method = "findGenerationPoint", at = @At("HEAD"), cancellable = true)
    private void skylore$elevateStronghold(Structure.GenerationContext context, CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir) {
        ChunkPos chunkpos = context.chunkPos();
        int blockX = chunkpos.getBlockX(2);
        int blockZ = chunkpos.getBlockZ(2);

        CellularIslandDensityFunction.IslandLayout layout = CellularIslandDensityFunction.findNearestMegaIsland(blockX, blockZ, 10);
        if (layout == null) {
            cir.setReturnValue(Optional.empty());
            return;
        }

        int surfaceY = highestSurfaceY(context, layout.centerX, layout.centerZ);
        if (surfaceY <= 35) {
            cir.setReturnValue(Optional.empty());
            return;
        }

        StructurePiecesBuilder builder = new StructurePiecesBuilder();
        int attempt = 0;

        do {
            builder.clear();
            context.random().setLargeFeatureSeed(context.seed() + attempt++, chunkpos.x, chunkpos.z);
            StrongholdPieces.resetPieces();

            StrongholdPieces.StartPiece startPiece = new StrongholdPieces.StartPiece(context.random(), layout.centerX, layout.centerZ);
            builder.addPiece(startPiece);
            startPiece.addChildren(startPiece, builder, context.random());

            List<StructurePiece> pending = startPiece.pendingChildren;
            while (!pending.isEmpty()) {
                int idx = context.random().nextInt(pending.size());
                StructurePiece piece = pending.remove(idx);
                piece.addChildren(startPiece, builder, context.random());
            }

            if (!builder.isEmpty() && startPiece.portalRoomPiece != null && fitStrongholdToMegaIsland(builder, layout, surfaceY)) {
                BoundingBox box = builder.getBoundingBox();
                BlockPos startPos = new BlockPos(box.getCenter().getX(), box.getCenter().getY(), box.getCenter().getZ());
                cir.setReturnValue(Optional.of(new Structure.GenerationStub(startPos, Either.right(builder))));
                return;
            }
        } while (attempt < 4);

        cir.setReturnValue(Optional.empty());
    }

    private static int highestSurfaceY(Structure.GenerationContext context, int blockX, int blockZ) {
        int best = sampleSurface(context, blockX, blockZ);
        best = Math.max(best, sampleSurface(context, blockX + 8, blockZ));
        best = Math.max(best, sampleSurface(context, blockX - 8, blockZ));
        best = Math.max(best, sampleSurface(context, blockX, blockZ + 8));
        best = Math.max(best, sampleSurface(context, blockX, blockZ - 8));
        return best;
    }

    private static int sampleSurface(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(
                x, z,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );
    }

    /**
     * Center on the mega island, bury under the surface, and reject layouts that still hang outside stone.
     */
    private static boolean fitStrongholdToMegaIsland(
            StructurePiecesBuilder builder,
            CellularIslandDensityFunction.IslandLayout layout,
            int surfaceY
    ) {
        BoundingBox box = builder.getBoundingBox();
        StructureStubMover.offsetPieces(
                builder,
                layout.centerX - box.getCenter().getX(),
                0,
                layout.centerZ - box.getCenter().getZ());

        box = builder.getBoundingBox();
        if (!horizontalFootprintFits(box, layout)) {
            return false;
        }

        int targetCenterY = Math.max(MIN_FLOOR_Y + 16, surfaceY - BURY_BELOW_SURFACE);
        builder.offsetPiecesVertically(targetCenterY - box.getCenter().getY());

        for (int lift = 0; lift < 24; lift++) {
            box = builder.getBoundingBox();
            if (stoneSupportsBase(box) && box.maxY() <= surfaceY + 2) {
                return true;
            }
            if (box.maxY() > surfaceY + 2) {
                builder.offsetPiecesVertically(surfaceY - 2 - box.maxY());
            } else {
                builder.offsetPiecesVertically(4);
            }
        }

        box = builder.getBoundingBox();
        return stoneSupportsBase(box) && horizontalFootprintFits(box, layout);
    }

    private static boolean horizontalFootprintFits(BoundingBox box, CellularIslandDensityFunction.IslandLayout layout) {
        int margin = (int) (layout.baseRadius * FOOTPRINT_MARGIN);
        int dx = Math.max(Math.abs(box.minX() - layout.centerX), Math.abs(box.maxX() - layout.centerX));
        int dz = Math.max(Math.abs(box.minZ() - layout.centerZ), Math.abs(box.maxZ() - layout.centerZ));
        return dx <= margin && dz <= margin;
    }

    private static boolean stoneSupportsBase(BoundingBox box) {
        int y = box.minY();
        if (y < MIN_FLOOR_Y) {
            return false;
        }
        return densityAt(box.minX(), y, box.minZ()) > DENSITY_STONE_THRESHOLD
                && densityAt(box.maxX(), y, box.minZ()) > DENSITY_STONE_THRESHOLD
                && densityAt(box.minX(), y, box.maxZ()) > DENSITY_STONE_THRESHOLD
                && densityAt(box.maxX(), y, box.maxZ()) > DENSITY_STONE_THRESHOLD;
    }

    private static double densityAt(int x, int y, int z) {
        return CellularIslandDensityFunction.calculateIslandDensity(
                x, y, z,
                com.skylore.islands.config.SkyloreConfig.cellSize(),
                com.skylore.islands.config.SkyloreConfig.minIslandAltitude(),
                com.skylore.islands.config.SkyloreConfig.maxIslandAltitude(),
                com.skylore.islands.config.SkyloreConfig.minIslandRadius(),
                com.skylore.islands.config.SkyloreConfig.maxIslandRadius());
    }
}

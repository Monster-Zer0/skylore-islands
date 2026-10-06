package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Post-processes template structure pieces to heal and seal water basins in 1.20.1 Forge.
 */
@Mixin(TemplateStructurePiece.class)
public abstract class TemplateStructurePieceMixin extends StructurePiece {

    protected TemplateStructurePieceMixin(StructurePieceType type, int genDepth, BoundingBox boundingBox) {
        super(type, genDepth, boundingBox);
    }

    @Inject(method = "postProcess", at = @At("RETURN"))
    private void skylore$healWaterBasinAirPockets(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox chunkBox,
            ChunkPos chunkPos,
            BlockPos pivot,
            CallbackInfo ci
    ) {
        BoundingBox pieceBox = this.getBoundingBox();
        if (pieceBox == null) {
            return;
        }

        int minX = Math.max(pieceBox.minX(), chunkBox.minX());
        int maxX = Math.min(pieceBox.maxX(), chunkBox.maxX());
        int minY = Math.max(pieceBox.minY(), chunkBox.minY());
        int maxY = Math.min(pieceBox.maxY(), chunkBox.maxY());
        int minZ = Math.max(pieceBox.minZ(), chunkBox.minZ());
        int maxZ = Math.min(pieceBox.maxZ(), chunkBox.maxZ());

        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return;
        }

        CellularIslandDensityFunction.bindFromLevel(level.getLevel());
        BlockState basin = CellularIslandDensityFunction.basinFluid();
        boolean nether = CellularIslandDensityFunction.usingNetherLayout();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                CellularIslandDensityFunction.WaterColumn column = CellularIslandDensityFunction.waterColumnAt(x, z);
                if (column == null) {
                    continue;
                }
                int y0 = Math.max(minY, column.seabedY + 1);
                int y1 = Math.min(maxY, column.waterLevel);
                for (int y = y0; y <= y1; y++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.is(Blocks.BARRIER)) {
                        level.setBlock(pos, basin, 2);
                    } else if (!nether && state.hasProperty(BlockStateProperties.WATERLOGGED) && !state.getValue(BlockStateProperties.WATERLOGGED)) {
                        level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, true), 2);
                    }
                }
            }
        }
    }
}

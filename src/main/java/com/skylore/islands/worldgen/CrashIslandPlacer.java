package com.skylore.islands.worldgen;

import com.skylore.islands.SkyloreIslands;
import com.skylore.islands.config.SkyloreConfig;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Pastes the crash-island schematic into empty void. It is the island; it must not sit on generated land.
 */
public final class CrashIslandPlacer {
    private static final int[][] STAND_OFFSETS = {
            {0, 0}, {2, 0}, {-2, 0}, {0, 2}, {0, -2}, {3, 3}, {-3, 3}, {3, -3}, {-3, -3}
    };
    private static final int SAMPLE_STEP = 8;

    private CrashIslandPlacer() {}

    public static final class Site {
        public final int cellX;
        public final int cellZ;
        public final int centerX;
        public final int centerY;
        public final int centerZ;

        public Site(int cellX, int cellZ, int centerX, int centerY, int centerZ) {
            this.cellX = cellX;
            this.cellZ = cellZ;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
        }
    }

    public static Site voidSite(int cellX, int cellZ) {
        int cellSize = SkyloreConfig.cellSize();
        int centerX = cellX * cellSize + cellSize / 2;
        int centerZ = cellZ * cellSize + cellSize / 2;
        int centerY = (SkyloreConfig.minIslandAltitude() + SkyloreConfig.maxIslandAltitude()) / 2;
        return new Site(cellX, cellZ, centerX, centerY, centerZ);
    }

    public static Vec3i templateSize(ServerLevel level) {
        StructureTemplate template = loadTemplate(level);
        return template == null ? null : template.getSize();
    }

    /**
     * True when this cell has no generated island and the schematic bbox (plus clearance)
     * does not overlap any island column.
     */
    public static boolean isClearVoid(int cellX, int cellZ, Vec3i size) {
        if (cellX == 0 && cellZ == 0 && SkyloreConfig.crashIslandSkipOriginCell()) {
            return false;
        }
        if (CellularIslandDensityFunction.layoutForCell(
                cellX, cellZ,
                SkyloreConfig.cellSize(),
                SkyloreConfig.minIslandAltitude(),
                SkyloreConfig.maxIslandAltitude(),
                SkyloreConfig.minIslandRadius(),
                SkyloreConfig.maxIslandRadius()) != null) {
            return false;
        }
        Site site = voidSite(cellX, cellZ);
        int pad = Math.max(16, SkyloreConfig.crashIslandMinBaseRadius());
        int minX = site.centerX - size.getX() / 2 - pad;
        int maxX = site.centerX + size.getX() - size.getX() / 2 + pad;
        int minZ = site.centerZ - size.getZ() / 2 - pad;
        int maxZ = site.centerZ + size.getZ() - size.getZ() / 2 + pad;
        for (int x = minX; x <= maxX; x += SAMPLE_STEP) {
            for (int z = minZ; z <= maxZ; z += SAMPLE_STEP) {
                if (CellularIslandDensityFunction.hasIslandColumn(x, z)) {
                    return false;
                }
            }
        }
        return !CellularIslandDensityFunction.hasIslandColumn(site.centerX, site.centerZ);
    }

    public static boolean paste(ServerLevel level, Site site) {
        StructureTemplate template = loadTemplate(level);
        if (template == null) {
            return false;
        }
        Vec3i size = template.getSize();
        if (size.getX() < 1 || size.getZ() < 1) {
            SkyloreIslands.LOGGER.error("Crash island structure has empty size");
            return false;
        }
        int anchorX = site.centerX - size.getX() / 2;
        int anchorY = site.centerY;
        int anchorZ = site.centerZ - size.getZ() / 2;
        int minCx = (anchorX >> 4) - 1;
        int maxCx = ((anchorX + size.getX()) >> 4) + 1;
        int minCz = (anchorZ >> 4) - 1;
        int maxCz = ((anchorZ + size.getZ()) >> 4) + 1;

        Set<Long> forced = new HashSet<>();
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                long packed = ChunkPos.asLong(cx, cz);
                if (!level.getForcedChunks().contains(packed)) {
                    level.setChunkForced(cx, cz, true);
                    forced.add(packed);
                }
                level.getChunk(cx, cz);
            }
        }

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false);
        BlockPos origin = new BlockPos(anchorX, anchorY, anchorZ);
        StructureIslandPolicy.allowVoidPlacement(true);
        boolean ok;
        try {
            ok = template.placeInWorld(level, origin, origin, settings, level.random, 3);
        } finally {
            StructureIslandPolicy.allowVoidPlacement(false);
        }
        for (long packed : forced) {
            level.setChunkForced(ChunkPos.getX(packed), ChunkPos.getZ(packed), false);
        }
        if (!ok) {
            SkyloreIslands.LOGGER.error("Failed to paste crash island at {}, {}, {}", anchorX, anchorY, anchorZ);
        }
        return ok;
    }

    public static Vec3 findStand(ServerLevel level, Site site) {
        for (int[] offset : STAND_OFFSETS) {
            int x = site.centerX + offset[0];
            int z = site.centerZ + offset[1];
            Integer y = standOnColumn(level, x, z, site.centerY);
            if (y != null) {
                return new Vec3(x + 0.5, y, z + 0.5);
            }
        }
        return new Vec3(
                site.centerX + 0.5,
                site.centerY + SkyloreConfig.crashIslandSpawnYOffset(),
                site.centerZ + 0.5);
    }

    private static StructureTemplate loadTemplate(ServerLevel level) {
        ResourceLocation id = ResourceLocation.tryParse(SkyloreConfig.crashIslandStructureId());
        if (id == null) {
            SkyloreIslands.LOGGER.error("Invalid crash island structureId '{}'", SkyloreConfig.crashIslandStructureId());
            return null;
        }
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> opt = manager.get(id);
        if (opt.isEmpty()) {
            SkyloreIslands.LOGGER.error("Missing crash island structure {}", id);
            return null;
        }
        return opt.get();
    }

    private static Integer standOnColumn(ServerLevel level, int x, int z, int guessY) {
        int y = Math.min(320, guessY + 64);
        int bottom = Math.max(-16, guessY - 80);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        while (y > bottom) {
            pos.set(x, y, z);
            BlockState feet = level.getBlockState(pos);
            BlockState below = level.getBlockState(pos.below());
            BlockState head = level.getBlockState(pos.above());
            if (isSolidFloor(below) && isOpen(feet) && isOpen(head)) {
                return y;
            }
            y--;
        }
        return null;
    }

    private static boolean isSolidFloor(BlockState state) {
        if (isOpen(state) || !state.getFluidState().isEmpty() || state.is(BlockTags.LEAVES)) {
            return false;
        }
        return state.isSolid();
    }

    private static boolean isOpen(BlockState state) {
        return state.isAir();
    }
}

package com.skylore.islands.worldgen;

import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Dry island surface for structures that must not sit in sky water (Ad Astra oil wells).
 * Crater lakes and ocean bowls are tagged as ocean biomes, so ocean-only structures
 * pick those chunks; Y must not be sampled from the rim while XZ stays over water.
 */
public final class LandSurfacePlacement {
    private LandSurfacePlacement() {}

    public static boolean isAdAstraOilPool(String poolName) {
        String lower = poolName.toLowerCase();
        return lower.contains("ad_astra") && lower.contains("oil");
    }

    public static boolean isAdAstraOilId(ResourceLocation id) {
        return id != null
                && "ad_astra".equals(id.getNamespace())
                && id.getPath().contains("oil");
    }

    /**
     * Closest dry land column near (x, z), or null if none.
     * If the start is a water basin, jump to that island's shore instead of lifting Y over the lake.
     */
    public static BlockPos findDryLandStart(Structure.GenerationContext context, int x, int z) {
        if (isDryLandColumn(context, x, z)) {
            return new BlockPos(x, sampleHeight(context, x, z), z);
        }

        CellularIslandDensityFunction.IslandLayout layout = CellularIslandDensityFunction.layoutContaining(x, z);
        if (layout != null) {
            BlockPos shore = shoreSpot(context, layout, x, z);
            if (shore != null) {
                return shore;
            }
        }

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int[] step = {8, -8, 16, -16, 24, -24, 32, -32, 40, -40, 48, -48, 64, -64, 80, -80};
        for (int dx : step) {
            for (int dz : step) {
                int sx = x + dx;
                int sz = z + dz;
                if (!isDryLandColumn(context, sx, sz)) {
                    continue;
                }
                double dist = (double) dx * dx + (double) dz * dz;
                if (dist < bestDist) {
                    bestDist = dist;
                    best = new BlockPos(sx, sampleHeight(context, sx, sz), sz);
                }
            }
        }
        return best;
    }

    private static BlockPos shoreSpot(
            Structure.GenerationContext context,
            CellularIslandDensityFunction.IslandLayout layout,
            int fromX,
            int fromZ
    ) {
        double dx = fromX - layout.centerX;
        double dz = fromZ - layout.centerZ;
        if (dx == 0 && dz == 0) {
            dx = 1;
        }
        double len = Math.sqrt(dx * dx + dz * dz);
        double nx = dx / len;
        double nz = dz / len;
        double shore = layout.baseRadius * shoreNorm(layout.archetype);
        int sx = layout.centerX + (int) Math.round(nx * shore);
        int sz = layout.centerZ + (int) Math.round(nz * shore);
        if (isDryLandColumn(context, sx, sz)) {
            return new BlockPos(sx, sampleHeight(context, sx, sz), sz);
        }
        for (int extra = 8; extra <= 40; extra += 8) {
            int ex = layout.centerX + (int) Math.round(nx * (shore + extra));
            int ez = layout.centerZ + (int) Math.round(nz * (shore + extra));
            if (isDryLandColumn(context, ex, ez)) {
                return new BlockPos(ex, sampleHeight(context, ex, ez), ez);
            }
        }
        return null;
    }

    private static double shoreNorm(int archetype) {
        if (archetype == 4) {
            return 0.78;
        }
        if (archetype == 1) {
            return 0.55;
        }
        if (archetype == 5) {
            return 0.70;
        }
        return 0.85;
    }

    private static boolean isDryLandColumn(Structure.GenerationContext context, int x, int z) {
        if (!CellularIslandDensityFunction.hasIslandColumn(x, z)) {
            return false;
        }
        int surfaceY = sampleHeight(context, x, z);
        if (surfaceY <= 35) {
            return false;
        }
        return !CellularIslandDensityFunction.isInsideWaterBasin(x, surfaceY, z);
    }

    public static int sampleHeight(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(
                x, z,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState()
        );
    }
}

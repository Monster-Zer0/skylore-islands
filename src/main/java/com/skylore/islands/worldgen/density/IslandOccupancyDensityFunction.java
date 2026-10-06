package com.skylore.islands.worldgen.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skylore.islands.config.SkyloreConfig;
import com.skylore.islands.worldgen.IslandLayout;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Cheap 2D occupancy for {@code initial_density_without_jaggedness}.
 * Full 3D island volume stays on {@link CellularIslandDensityFunction}.
 */
public class IslandOccupancyDensityFunction implements DensityFunction.SimpleFunction {

    public static final MapCodec<IslandOccupancyDensityFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("cell_size", SkyloreConfig.DEFAULT_CELL_SIZE).forGetter(d -> d.cellSize),
                    Codec.INT.optionalFieldOf("min_altitude", SkyloreConfig.DEFAULT_MIN_ALTITUDE).forGetter(d -> d.minAltitude),
                    Codec.INT.optionalFieldOf("max_altitude", SkyloreConfig.DEFAULT_MAX_ALTITUDE).forGetter(d -> d.maxAltitude),
                    Codec.INT.optionalFieldOf("min_radius", SkyloreConfig.DEFAULT_MIN_RADIUS).forGetter(d -> d.minRadius),
                    Codec.INT.optionalFieldOf("max_radius", SkyloreConfig.DEFAULT_MAX_RADIUS).forGetter(d -> d.maxRadius),
                    Codec.LONG.optionalFieldOf("layout_salt", CellularIslandDensityFunction.OVERWORLD_SALT).forGetter(d -> d.layoutSalt)
            ).apply(instance, IslandOccupancyDensityFunction::new)
    );

    public static final KeyDispatchDataCodec<IslandOccupancyDensityFunction> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private final int cellSize;
    private final int minAltitude;
    private final int maxAltitude;
    private final int minRadius;
    private final int maxRadius;
    private final long layoutSalt;

    public IslandOccupancyDensityFunction(int cellSize, int minAltitude, int maxAltitude, int minRadius, int maxRadius, long layoutSalt) {
        this.cellSize = cellSize;
        this.minAltitude = minAltitude;
        this.maxAltitude = maxAltitude;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.layoutSalt = layoutSalt;
    }

    @Override
    public double compute(DensityFunction.FunctionContext context) {
        CellularIslandDensityFunction.setLayoutSalt(this.layoutSalt);
        return IslandLayout.hasIslandColumn(context.blockX(), context.blockZ()) ? 1.0 : -1.0;
    }

    @Override
    public double minValue() {
        return -1.0;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}

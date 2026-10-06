package com.skylore.islands.worldgen.density;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.skylore.islands.config.SkyloreConfig;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public class OceanBowlContinentalnessDensityFunction implements DensityFunction.SimpleFunction {

    public static final MapCodec<OceanBowlContinentalnessDensityFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Codec.INT.optionalFieldOf("cell_size", SkyloreConfig.DEFAULT_CELL_SIZE).forGetter(d -> d.cellSize),
                    Codec.INT.optionalFieldOf("min_altitude", SkyloreConfig.DEFAULT_MIN_ALTITUDE).forGetter(d -> d.minAltitude),
                    Codec.INT.optionalFieldOf("max_altitude", SkyloreConfig.DEFAULT_MAX_ALTITUDE).forGetter(d -> d.maxAltitude),
                    Codec.INT.optionalFieldOf("min_radius", SkyloreConfig.DEFAULT_MIN_RADIUS).forGetter(d -> d.minRadius),
                    Codec.INT.optionalFieldOf("max_radius", SkyloreConfig.DEFAULT_MAX_RADIUS).forGetter(d -> d.maxRadius),
                    Codec.LONG.optionalFieldOf("layout_salt", CellularIslandDensityFunction.OVERWORLD_SALT).forGetter(d -> d.layoutSalt),
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("base_continents").forGetter(d -> d.baseContinents)
            ).apply(instance, (cellSize, minAltitude, maxAltitude, minRadius, maxRadius, layoutSalt, baseContinents) ->
                    new OceanBowlContinentalnessDensityFunction(cellSize, minAltitude, maxAltitude, minRadius, maxRadius, layoutSalt, baseContinents))
    );

    public static final KeyDispatchDataCodec<OceanBowlContinentalnessDensityFunction> CODEC =
            KeyDispatchDataCodec.of(MAP_CODEC);

    private final int cellSize;
    private final int minAltitude;
    private final int maxAltitude;
    private final int minRadius;
    private final int maxRadius;
    private final long layoutSalt;
    private final DensityFunction baseContinents;

    public OceanBowlContinentalnessDensityFunction(int cellSize, int minAltitude, int maxAltitude, int minRadius, int maxRadius, DensityFunction baseContinents) {
        this(cellSize, minAltitude, maxAltitude, minRadius, maxRadius, CellularIslandDensityFunction.OVERWORLD_SALT, baseContinents);
    }

    public OceanBowlContinentalnessDensityFunction(int cellSize, int minAltitude, int maxAltitude, int minRadius, int maxRadius, long layoutSalt, DensityFunction baseContinents) {
        this.cellSize = cellSize;
        this.minAltitude = minAltitude;
        this.maxAltitude = maxAltitude;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.layoutSalt = layoutSalt;
        this.baseContinents = baseContinents;
    }

    @Override
    public double compute(FunctionContext context) {
        CellularIslandDensityFunction.setLayoutSalt(this.layoutSalt);
        int x = context.blockX();
        int z = context.blockZ();

        int cellSize = SkyloreConfig.cellSize(this.cellSize);

        // Biome climate (FTB Chunks / Nature's Compass / Explorer's Compass) reads continents.
        // Terrain is islands via final_density, but continents used to pass vanilla noise through
        // for non-bowl columns — maps looked like a normal Overworld. Void must read as deep ocean.
        CellularIslandDensityFunction.IslandLayout layout = CellularIslandDensityFunction.layoutContaining(x, z);
        if (layout == null) {
            return -1.0;
        }
        if (CellularIslandDensityFunction.isOceanBiomeFootprintOnLayout(x, z, layout, cellSize)) {
            return -0.55;
        }

        return this.baseContinents.compute(context);
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

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new OceanBowlContinentalnessDensityFunction(
                cellSize, minAltitude, maxAltitude, minRadius, maxRadius, layoutSalt, this.baseContinents.mapAll(visitor)
        ));
    }
}

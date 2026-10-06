package com.skylore.islands.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Vanilla and most mod ores (Mekanism, AE2, etc.) use minecraft:ore + height_range.
 * Deep height bands miss high islands. Extra Y samples in -64..256 let those ores
 * generate in island stone without listing every mod ore.
 */
@Mixin(HeightRangePlacement.class)
public abstract class HeightRangePlacementMixin {

    @Inject(method = "getPositions", at = @At("RETURN"), cancellable = true)
    private void skylore$spreadOresThroughIslandStone(
            PlacementContext context,
            RandomSource random,
            BlockPos pos,
            CallbackInfoReturnable<Stream<BlockPos>> cir
    ) {
        if (!isOreFeature(context)) {
            return;
        }
        Stream<BlockPos> original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        List<BlockPos> extra = new ArrayList<>(1);
        extra.add(new BlockPos(pos.getX(), random.nextInt(321) - 64, pos.getZ()));
        cir.setReturnValue(Stream.concat(original, extra.stream()));
    }

    private static boolean isOreFeature(PlacementContext context) {
        return context.topFeature().map(placed ->
                placed.feature().value().config() instanceof OreConfiguration
        ).orElse(false);
    }
}

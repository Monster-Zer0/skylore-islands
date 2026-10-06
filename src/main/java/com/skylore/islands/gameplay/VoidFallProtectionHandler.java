package com.skylore.islands.gameplay;

import com.skylore.islands.config.SkyloreConfig;
import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * If a player falls below the configured Y in the Overworld, search for the closest
 * island surface and teleport them onto it with slow falling and a short resistance buff.
 */
public class VoidFallProtectionHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!SkyloreConfig.voidFallProtection()) {
            return;
        }
        if (player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        if (player.isSpectator()) {
            return;
        }
        if (player.getY() < SkyloreConfig.voidFallRescueY()) {
            rescuePlayerToNearestIsland(player);
        }
    }

    private static void rescuePlayerToNearestIsland(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int px = player.getBlockX();
        int pz = player.getBlockZ();

        CellularIslandDensityFunction.IslandLayout nearest =
                CellularIslandDensityFunction.findNearestIsland(px, pz, 2, 0);

        BlockPos bestSurfacePos = null;
        if (nearest != null) {
            int[] sampleOffsets = {0, -16, 16, -32, 32};
            double bestDistSq = Double.MAX_VALUE;
            for (int sox : sampleOffsets) {
                for (int soz : sampleOffsets) {
                    int testX = nearest.centerX + sox;
                    int testZ = nearest.centerZ + soz;
                    int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, testX, testZ);
                    if (surfaceY > 20) {
                        BlockPos candidate = new BlockPos(testX, surfaceY, testZ);
                        if (!level.getBlockState(candidate.below()).isAir()
                                && !level.getBlockState(candidate.below()).is(Blocks.VOID_AIR)) {
                            double dSq = (double) (px - testX) * (px - testX) + (double) (pz - testZ) * (pz - testZ);
                            if (dSq < bestDistSq) {
                                bestDistSq = dSq;
                                bestSurfacePos = candidate;
                            }
                        }
                    }
                }
            }
            if (bestSurfacePos == null) {
                bestSurfacePos = new BlockPos(nearest.centerX, nearest.altitude + 8, nearest.centerZ);
            }
        }

        if (bestSurfacePos == null) {
            bestSurfacePos = new BlockPos(px, 140, pz);
        }

        player.teleportTo(bestSurfacePos.getX() + 0.5, bestSurfacePos.getY() + 0.5, bestSurfacePos.getZ() + 0.5);
        player.setDeltaMovement(new Vec3(0, 0.1, 0));
        player.resetFallDistance();

        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 300, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 4, false, false, true));

        level.sendParticles(ParticleTypes.CLOUD, bestSurfacePos.getX() + 0.5, bestSurfacePos.getY(), bestSurfacePos.getZ() + 0.5, 45, 1.5, 0.5, 1.5, 0.1);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, bestSurfacePos.getX() + 0.5, bestSurfacePos.getY() + 1, bestSurfacePos.getZ() + 0.5, 30, 0.8, 1.2, 0.8, 0.2);

        level.playSound(null, bestSurfacePos, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
    }
}

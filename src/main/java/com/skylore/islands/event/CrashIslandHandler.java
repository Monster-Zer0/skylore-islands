package com.skylore.islands.event;

import com.skylore.islands.SkyloreIslands;
import com.skylore.islands.config.SkyloreConfig;
import com.skylore.islands.worldgen.CrashIslandCells;
import com.skylore.islands.worldgen.CrashIslandPlacer;
import com.skylore.islands.worldgen.CrashIslandSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.UUID;

/**
 * Assigns each player a unique empty Voronoi cell, pastes islandspawn once in the void, and sends them there.
 */
public class CrashIslandHandler {
    private static final int MAX_ALLOC_TRIES = 512;
    private static final int LOGIN_DELAY_TICKS = 20;
    private static final int RESPAWN_DELAY_TICKS = 5;

    @SubscribeEvent
    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!SkyloreConfig.crashIslandsEnabled() || player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        boolean firstTime = CrashIslandSavedData.get(overworld(player)).assignedCell(player.getUUID()) == null;
        schedule(player, LOGIN_DELAY_TICKS, firstTime);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!SkyloreConfig.crashIslandsEnabled() || player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        schedule(player, RESPAWN_DELAY_TICKS, false);
    }

    private static void schedule(ServerPlayer player, int delay, boolean announce) {
        MinecraftServer server = player.server;
        UUID uuid = player.getUUID();
        server.tell(new TickTask(server.getTickCount() + delay, () -> {
            ServerPlayer live = server.getPlayerList().getPlayer(uuid);
            if (live != null) {
                deliver(live, announce);
            }
        }));
    }

    private static void deliver(ServerPlayer player, boolean announce) {
        if (player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Assignment assignment = ensureAssignment(player, level);
        if (assignment == null) {
            return;
        }
        if (!assignment.built) {
            if (!CrashIslandPlacer.paste(level, assignment.site)) {
                player.sendSystemMessage(Component.literal("Could not build your crash island."));
                return;
            }
            CrashIslandSavedData.get(overworld(player)).markBuilt(assignment.site.cellX, assignment.site.cellZ);
            assignment.built = true;
        }
        Vec3 stand = CrashIslandPlacer.findStand(level, assignment.site);
        player.teleportTo(stand.x, stand.y, stand.z);
        BlockPos spawn = BlockPos.containing(stand);
        player.setRespawnPosition(Level.OVERWORLD, spawn, player.getYRot(), true, false);
        if (announce) {
            SkyloreIslands.LOGGER.info("Crash island for {} at cell {},{} -> {}",
                    player.getGameProfile().getName(), assignment.site.cellX, assignment.site.cellZ, spawn);
        }
    }

    private static Assignment ensureAssignment(ServerPlayer player, ServerLevel level) {
        CrashIslandSavedData data = CrashIslandSavedData.get(overworld(player));
        int[] assigned = data.assignedCell(player.getUUID());
        if (assigned != null) {
            return new Assignment(CrashIslandPlacer.voidSite(assigned[0], assigned[1]), data.isBuilt(assigned[0], assigned[1]));
        }
        if (CrashIslandPlacer.templateSize(level) == null) {
            player.sendSystemMessage(Component.literal("Could not build your crash island."));
            return null;
        }
        Assignment fresh = allocate(level, data);
        if (fresh == null) {
            player.sendSystemMessage(Component.literal("No open void cell in the spiral. The sky is out of leases."));
            return null;
        }
        data.assign(player.getUUID(), fresh.site.cellX, fresh.site.cellZ);
        return fresh;
    }

    private static Assignment allocate(ServerLevel level, CrashIslandSavedData data) {
        Vec3i size = CrashIslandPlacer.templateSize(level);
        if (size == null) {
            return null;
        }
        boolean skipOrigin = SkyloreConfig.crashIslandSkipOriginCell();
        for (int tries = 0; tries < MAX_ALLOC_TRIES; tries++) {
            int seq = data.takeNextSeq();
            int[] cell = CrashIslandCells.cellAtSpawnIndex(seq, skipOrigin);
            int cellX = cell[0];
            int cellZ = cell[1];
            if (skipOrigin && cellX == 0 && cellZ == 0) {
                continue;
            }
            if (data.isClaimed(cellX, cellZ)) {
                continue;
            }
            if (!CrashIslandPlacer.isClearVoid(cellX, cellZ, size)) {
                continue;
            }
            return new Assignment(CrashIslandPlacer.voidSite(cellX, cellZ), data.isBuilt(cellX, cellZ));
        }
        return null;
    }

    private static ServerLevel overworld(ServerPlayer player) {
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        return overworld != null ? overworld : player.serverLevel();
    }

    private static final class Assignment {
        final CrashIslandPlacer.Site site;
        boolean built;

        Assignment(CrashIslandPlacer.Site site, boolean built) {
            this.site = site;
            this.built = built;
        }
    }
}

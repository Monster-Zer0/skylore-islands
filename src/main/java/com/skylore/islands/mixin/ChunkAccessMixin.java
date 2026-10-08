package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.ChunkOccupancy;
import com.skylore.islands.worldgen.OccupancyHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vanilla pendingBlockEntities / blockEntities are HashMaps. VS and DH workers
 * read them while the server thread runs postProcessGeneration, which does
 * ImmutableList.copyOf(pendingBlockEntities.keySet()) and crashes with
 * NegativeArraySizeException when HashMap.size races to -1.
 */
@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin implements OccupancyHolder {

    @Unique
    private volatile ChunkOccupancy.Ticket skylore$ticket;

    @Unique
    private volatile boolean skylore$lightRepair;

    @Override
    public ChunkOccupancy.Ticket skylore$getTicket() {
        return skylore$ticket;
    }

    @Override
    public void skylore$setTicket(ChunkOccupancy.Ticket ticket) {
        skylore$ticket = ticket;
    }

    @Override
    public boolean skylore$needsLightRepair() {
        return skylore$lightRepair;
    }

    @Override
    public void skylore$setNeedsLightRepair(boolean value) {
        skylore$lightRepair = value;
    }

    @Shadow
    @Final
    @Mutable
    protected Map<BlockPos, CompoundTag> pendingBlockEntities;

    @Shadow
    @Final
    @Mutable
    protected Map<BlockPos, BlockEntity> blockEntities;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void skylore$threadSafeBlockEntityMaps(CallbackInfo ci) {
        if (!(this.pendingBlockEntities instanceof ConcurrentHashMap)) {
            this.pendingBlockEntities = new ConcurrentHashMap<>(this.pendingBlockEntities);
        }
        if (!(this.blockEntities instanceof ConcurrentHashMap)) {
            this.blockEntities = new ConcurrentHashMap<>(this.blockEntities);
        }
    }
}

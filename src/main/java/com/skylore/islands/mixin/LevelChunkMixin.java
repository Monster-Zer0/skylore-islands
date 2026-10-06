package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.ChunkOccupancy;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Player / piston / Create deploy of a non-air block wakes a VOID chunk. Never cancels the place.
 * Copies the gen-time ticket when a proto chunk is promoted.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at = @At("RETURN"))
    private void skylore$copyOccupancyFromProto(ServerLevel level, ProtoChunk proto, LevelChunk.PostLoadProcessor postLoad, CallbackInfo ci) {
        ChunkOccupancy.copy(proto, (LevelChunk) (Object) this);
    }

    @Inject(method = "setBlockState", at = @At("HEAD"))
    private void skylore$promoteVoidOnPlace(BlockPos pos, BlockState state, boolean moved, CallbackInfoReturnable<BlockState> cir) {
        if (!state.isAir()) {
            ChunkOccupancy.notePlacedBlock((LevelChunk) (Object) this, pos.getY());
        }
    }
}

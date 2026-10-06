package com.skylore.islands.mixin;

import com.skylore.islands.worldgen.ChunkOccupancy;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * VOID: skip the section walk. ISLAND: only tick sections in the stored Y-band.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    private static final ThreadLocal<ChunkOccupancy.Ticket> TICKING = new ThreadLocal<>();
    private static final ThreadLocal<int[]> SECTION_INDEX = ThreadLocal.withInitial(() -> new int[1]);

    @Inject(method = "tickChunk", at = @At("HEAD"), cancellable = true)
    private void skylore$skipVoidChunkTicks(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ChunkOccupancy.Ticket ticket = ChunkOccupancy.get(chunk);
        if (ticket.skipAllRandomTicks()) {
            ci.cancel();
            return;
        }
        TICKING.set(ticket.restrictRandomTicks() ? ticket : null);
        SECTION_INDEX.get()[0] = 0;
    }

    @Inject(method = "tickChunk", at = @At("RETURN"))
    private void skylore$clearTickState(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        TICKING.remove();
    }

    @Redirect(
            method = "tickChunk",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;isRandomlyTicking()Z")
    )
    private boolean skylore$restrictIslandSectionTicks(LevelChunkSection section, LevelChunk chunk, int randomTickSpeed) {
        int index = SECTION_INDEX.get()[0]++;
        if (!section.isRandomlyTicking()) {
            return false;
        }
        ChunkOccupancy.Ticket ticket = TICKING.get();
        if (ticket == null) {
            return true;
        }
        int sectionY = chunk.getSectionYFromSectionIndex(index);
        return sectionY >= ticket.minSection && sectionY <= ticket.maxSection;
    }
}

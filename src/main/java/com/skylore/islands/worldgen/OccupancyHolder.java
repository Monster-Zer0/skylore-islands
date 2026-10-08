package com.skylore.islands.worldgen;

/**
 * Per-chunk occupancy state stored directly on ChunkAccess (see ChunkAccessMixin),
 * so tick-time lookups need no shared map or lock.
 */
public interface OccupancyHolder {
    ChunkOccupancy.Ticket skylore$getTicket();

    void skylore$setTicket(ChunkOccupancy.Ticket ticket);

    boolean skylore$needsLightRepair();

    void skylore$setNeedsLightRepair(boolean value);
}

package com.skylore.islands.worldgen;

import com.skylore.islands.worldgen.density.CellularIslandDensityFunction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;


/**
 * Per-chunk occupancy ticket. VOID skips biome features, initial light, and random ticks.
 * Player-placed blocks promote VOID to PLAYER so builds stay and tick/light normally.
 */
public final class ChunkOccupancy {
    public static final String NBT_ROOT = "skylore_islands";
    public static final String NBT_KIND = "occupancy";
    public static final String NBT_MIN = "min_sec";
    public static final String NBT_MAX = "max_sec";
    public static final String NBT_LIGHT_OK = "light_ok";

    public enum Kind {
        UNKNOWN,
        VOID,
        VOID_STRUCTURE,
        ISLAND,
        PLAYER
    }

    public static final class Ticket {
        public Kind kind;
        public int minSection;
        public int maxSection;

        public Ticket(Kind kind, int minSection, int maxSection) {
            this.kind = kind;
            this.minSection = minSection;
            this.maxSection = maxSection;
        }

        public boolean skipBiomeFeatures() {
            return kind == Kind.VOID || kind == Kind.VOID_STRUCTURE;
        }

        public boolean skipAllRandomTicks() {
            return kind == Kind.VOID;
        }

        public boolean restrictRandomTicks() {
            return kind == Kind.ISLAND;
        }

        /** 256-bit mask of columns with island stone under them; built lazily, never persisted. */
        private volatile long[] islandColumns;
    }

    private static final ThreadLocal<Boolean> SKIP_BIOME_FEATURES = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private ChunkOccupancy() {}

    public static Ticket get(ChunkAccess chunk) {
        if (chunk == null) {
            return new Ticket(Kind.UNKNOWN, 0, 0);
        }
        Ticket attached = attached(chunk);
        if (attached != null && attached.kind != Kind.UNKNOWN) {
            return attached;
        }
        Ticket inferred = infer(chunk);
        put(chunk, inferred);
        return inferred;
    }

    public static void stampVoid(ChunkAccess chunk) {
        Kind kind = hasStructureStart(chunk) ? Kind.VOID_STRUCTURE : Kind.VOID;
        put(chunk, new Ticket(kind, 0, 0));
    }

    public static void stampIsland(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        int[] band = CellularIslandDensityFunction.sampleIslandYBand(pos.x, pos.z);
        if (band == null) {
            stampVoid(chunk);
            return;
        }
        int minSec = SectionPos.blockToSectionCoord(band[0]);
        int maxSec = SectionPos.blockToSectionCoord(band[1]);
        put(chunk, new Ticket(Kind.ISLAND, minSec, maxSec));
    }

    public static void promoteToPlayer(ChunkAccess chunk) {
        Ticket ticket = get(chunk);
        if (ticket.kind == Kind.VOID) {
            put(chunk, new Ticket(Kind.PLAYER, ticket.minSection, ticket.maxSection));
        }
    }

    public static void notePlacedBlock(ChunkAccess chunk, int blockY) {
        Ticket ticket = attached(chunk);
        if (ticket == null || ticket.kind == Kind.UNKNOWN) {
            return;
        }
        if (ticket.kind == Kind.VOID) {
            put(chunk, new Ticket(Kind.PLAYER, ticket.minSection, ticket.maxSection));
            return;
        }
        if (ticket.kind == Kind.ISLAND) {
            int sec = SectionPos.blockToSectionCoord(blockY);
            if (sec < ticket.minSection || sec > ticket.maxSection) {
                put(chunk, new Ticket(Kind.ISLAND, Math.min(ticket.minSection, sec), Math.max(ticket.maxSection, sec)));
            }
        }
    }

    public static void beginDecoration(ChunkAccess chunk) {
        SKIP_BIOME_FEATURES.set(get(chunk).skipBiomeFeatures());
    }

    public static void endDecoration() {
        SKIP_BIOME_FEATURES.set(Boolean.FALSE);
    }

    public static boolean skipBiomeFeatures() {
        return Boolean.TRUE.equals(SKIP_BIOME_FEATURES.get());
    }

    public static boolean sectionInIslandBand(ChunkAccess chunk, int sectionY) {
        Ticket ticket = get(chunk);
        if (!ticket.restrictRandomTicks()) {
            return true;
        }
        return sectionY >= ticket.minSection && sectionY <= ticket.maxSection;
    }

    public static void copy(ChunkAccess from, ChunkAccess to) {
        Ticket ticket = attached(from);
        if (ticket != null && ticket.kind != Kind.UNKNOWN) {
            put(to, ticket);
        }
        if (takeLightRepair(from)) {
            ((OccupancyHolder) to).skylore$setNeedsLightRepair(true);
        }
    }

    public static void writeToChunkNbt(ChunkAccess chunk, CompoundTag data) {
        Ticket ticket = attached(chunk);
        if (ticket == null || ticket.kind == Kind.UNKNOWN) {
            return;
        }
        data.put(NBT_ROOT, encode(ticket));
    }

    public static void readFromChunkNbt(ChunkAccess chunk, CompoundTag data) {
        if (data == null || !data.contains(NBT_ROOT)) {
            return;
        }
        CompoundTag root = data.getCompound(NBT_ROOT);
        Ticket ticket = decode(root);
        if (ticket != null) {
            put(chunk, ticket);
            if (!root.getBoolean(NBT_LIGHT_OK)) {
                ((OccupancyHolder) chunk).skylore$setNeedsLightRepair(true);
            }
        }
    }

    public static boolean takeLightRepair(ChunkAccess chunk) {
        OccupancyHolder holder = (OccupancyHolder) chunk;
        boolean pending = holder.skylore$needsLightRepair();
        if (pending) {
            holder.skylore$setNeedsLightRepair(false);
        }
        return pending;
    }

    public static void markLightOk(ChunkAccess chunk) {
        ((OccupancyHolder) chunk).skylore$setNeedsLightRepair(false);
    }

    private static Ticket attached(ChunkAccess chunk) {
        return ((OccupancyHolder) chunk).skylore$getTicket();
    }

    private static void put(ChunkAccess chunk, Ticket ticket) {
        ((OccupancyHolder) chunk).skylore$setTicket(ticket);
    }

    /**
     * Island column test from the chunk's cached mask. Caller must have bound the layout salt
     * for the chunk's dimension.
     */
    public static boolean hasIslandColumn(ChunkAccess chunk, int blockX, int blockZ) {
        Ticket ticket = get(chunk);
        if (ticket.skipAllRandomTicks()) {
            return false;
        }
        long[] mask = ticket.islandColumns;
        if (mask == null) {
            mask = buildColumnMask(chunk.getPos());
            ticket.islandColumns = mask;
        }
        int bit = (blockX & 15) | ((blockZ & 15) << 4);
        return (mask[bit >>> 6] & (1L << (bit & 63))) != 0;
    }

    private static long[] buildColumnMask(ChunkPos pos) {
        long[] mask = new long[4];
        int x0 = pos.getMinBlockX();
        int z0 = pos.getMinBlockZ();
        for (int bit = 0; bit < 256; bit++) {
            if (CellularIslandDensityFunction.hasIslandColumn(x0 + (bit & 15), z0 + (bit >>> 4))) {
                mask[bit >>> 6] |= 1L << (bit & 63);
            }
        }
        return mask;
    }

    private static boolean hasStructureStart(ChunkAccess chunk) {
        for (StructureStart start : chunk.getAllStarts().values()) {
            if (start != null && start.isValid()) {
                return true;
            }
        }
        return false;
    }

    private static Ticket infer(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        boolean aabb = CellularIslandDensityFunction.chunkAabbMightContainIsland(pos.x, pos.z);
        if (aabb) {
            int[] band = CellularIslandDensityFunction.sampleIslandYBand(pos.x, pos.z);
            if (band != null) {
                return new Ticket(Kind.ISLAND,
                        SectionPos.blockToSectionCoord(band[0]),
                        SectionPos.blockToSectionCoord(band[1]));
            }
        }
        boolean starts = hasStructureStart(chunk);
        if (starts) {
            return new Ticket(Kind.VOID_STRUCTURE, 0, 0);
        }
        if (!aabb) {
            return new Ticket(Kind.VOID, 0, 0);
        }
        if (hasMotionBlocking(chunk)) {
            return new Ticket(Kind.PLAYER, 0, 0);
        }
        return new Ticket(Kind.VOID, 0, 0);
    }

    private static boolean hasMotionBlocking(ChunkAccess chunk) {
        int min = chunk.getMinBuildHeight();
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        return chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x0, z0) > min
                || chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x0 + 15, z0) > min
                || chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x0, z0 + 15) > min
                || chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x0 + 15, z0 + 15) > min
                || chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x0 + 8, z0 + 8) > min;
    }

    private static CompoundTag encode(Ticket ticket) {
        CompoundTag root = new CompoundTag();
        root.putByte(NBT_KIND, (byte) ticket.kind.ordinal());
        root.putInt(NBT_MIN, ticket.minSection);
        root.putInt(NBT_MAX, ticket.maxSection);
        root.putBoolean(NBT_LIGHT_OK, true);
        return root;
    }

    private static Ticket decode(CompoundTag root) {
        if (root == null || !root.contains(NBT_KIND)) {
            return null;
        }
        int ordinal = root.getByte(NBT_KIND) & 0xFF;
        Kind[] values = Kind.values();
        if (ordinal >= values.length) {
            return null;
        }
        Kind kind = values[ordinal];
        if (kind == Kind.UNKNOWN) {
            return null;
        }
        return new Ticket(kind, root.getInt(NBT_MIN), root.getInt(NBT_MAX));
    }
}

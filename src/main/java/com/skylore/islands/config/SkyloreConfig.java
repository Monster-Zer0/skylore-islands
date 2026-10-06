package com.skylore.islands.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class SkyloreConfig {
    public static class Server {
        public final ModConfigSpec.IntValue cellSize;
        public final ModConfigSpec.IntValue islandDensity;
        public final ModConfigSpec.IntValue minIslandAltitude;
        public final ModConfigSpec.IntValue maxIslandAltitude;
        public final ModConfigSpec.IntValue minIslandRadius;
        public final ModConfigSpec.IntValue maxIslandRadius;

        public final ModConfigSpec.BooleanValue crashIslandsEnabled;
        public final ModConfigSpec.ConfigValue<String> crashIslandStructureId;
        public final ModConfigSpec.IntValue crashIslandMinBaseRadius;
        public final ModConfigSpec.BooleanValue crashIslandSkipOriginCell;
        public final ModConfigSpec.IntValue crashIslandSpawnYOffset;

        public final ModConfigSpec.BooleanValue voidFallProtection;
        public final ModConfigSpec.IntValue voidFallRescueY;

        public Server(ModConfigSpec.Builder builder) {
            builder.push("general");

            cellSize = builder
                    .comment("Island spacing in blocks (cell grid). Larger = more void between islands. Default 384 (~150-700 blocks between islands).")
                    .defineInRange("cellSize", 384, 256, 16384);

            islandDensity = builder
                    .comment("How often cells get islands, 0-100. 0 = spawn island only. 50 = isolated islands. 70 = denser with some clumps (default).")
                    .defineInRange("islandDensity", 70, 0, 100);

            minIslandAltitude = builder
                    .comment("Lowest island surface Y. Default is 20 so undersides can reach vanilla deep ores.")
                    .defineInRange("minIslandAltitude", 20, -16, 200);

            maxIslandAltitude = builder
                    .comment("Highest island elevation Y level. Default is 260.")
                    .defineInRange("maxIslandAltitude", 260, 100, 320);

            minIslandRadius = builder
                    .comment("Minimum island base radius in blocks. Default is 45.")
                    .defineInRange("minIslandRadius", 45, 20, 512);

            maxIslandRadius = builder
                    .comment("Maximum standard island base radius in blocks. Default is 170.")
                    .defineInRange("maxIslandRadius", 170, 50, 1024);

            builder.pop();

            builder.push("crash_islands");
            crashIslandsEnabled = builder
                    .comment("Give each player a unique crash island on first Overworld join.")
                    .define("enabled", true);
            crashIslandStructureId = builder
                    .comment("Structure resource location pasted on the assigned cell (data/<ns>/structures/<path>.nbt).")
                    .define("structureId", "skylore:islandspawn");
            crashIslandMinBaseRadius = builder
                    .comment("Void clearance around the crash schematic, in blocks. Cells that already have generated islands are skipped.")
                    .defineInRange("minBaseRadius", 40, 10, 512);
            crashIslandSkipOriginCell = builder
                    .comment("Never assign cell (0,0); that stays the world hub.")
                    .define("skipOriginCell", true);
            crashIslandSpawnYOffset = builder
                    .comment("Fallback player Y above island altitude when no stand block is found.")
                    .defineInRange("spawnYOffset", 1, 0, 16);
            builder.pop();

            builder.push("gameplay");
            voidFallProtection = builder
                    .comment("Whether falling into the void below islands automatically rescues the player.")
                    .define("voidFallProtection", true);
            voidFallRescueY = builder
                    .comment("The Y level threshold in the Overworld where void fall rescue triggers.")
                    .defineInRange("voidFallRescueY", -40, -64, 0);
            builder.pop();
        }
    }

    public static final ModConfigSpec SERVER_SPEC;
    public static final Server SERVER;

    public static final int DEFAULT_CELL_SIZE = 384;
    public static final int DEFAULT_ISLAND_DENSITY = 70;
    public static final int DEFAULT_MIN_ALTITUDE = 20;
    public static final int DEFAULT_MAX_ALTITUDE = 260;
    public static final int DEFAULT_MIN_RADIUS = 45;
    public static final int DEFAULT_MAX_RADIUS = 170;
    public static final boolean DEFAULT_CRASH_ISLANDS_ENABLED = true;
    public static final String DEFAULT_CRASH_ISLAND_STRUCTURE = "skylore:islandspawn";
    public static final int DEFAULT_CRASH_ISLAND_MIN_RADIUS = 40;
    public static final boolean DEFAULT_CRASH_ISLAND_SKIP_ORIGIN = true;
    public static final int DEFAULT_CRASH_ISLAND_SPAWN_Y_OFFSET = 1;

    static {
        final Pair<Server, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    private static boolean loaded() {
        return SERVER_SPEC != null && SERVER_SPEC.isLoaded();
    }

    public static int cellSize() {
        return loaded() ? SERVER.cellSize.get() : DEFAULT_CELL_SIZE;
    }

    public static int islandDensity() {
        return loaded() ? SERVER.islandDensity.get() : DEFAULT_ISLAND_DENSITY;
    }

    public static int minIslandAltitude() {
        return loaded() ? SERVER.minIslandAltitude.get() : DEFAULT_MIN_ALTITUDE;
    }

    public static int maxIslandAltitude() {
        return loaded() ? SERVER.maxIslandAltitude.get() : DEFAULT_MAX_ALTITUDE;
    }

    public static int minIslandRadius() {
        return loaded() ? SERVER.minIslandRadius.get() : DEFAULT_MIN_RADIUS;
    }

    public static int maxIslandRadius() {
        return loaded() ? SERVER.maxIslandRadius.get() : DEFAULT_MAX_RADIUS;
    }

    public static int cellSize(int fallback) {
        return loaded() ? SERVER.cellSize.get() : fallback;
    }

    public static int minIslandAltitude(int fallback) {
        return loaded() ? SERVER.minIslandAltitude.get() : fallback;
    }

    public static int maxIslandAltitude(int fallback) {
        return loaded() ? SERVER.maxIslandAltitude.get() : fallback;
    }

    public static int minIslandRadius(int fallback) {
        return loaded() ? SERVER.minIslandRadius.get() : fallback;
    }

    public static int maxIslandRadius(int fallback) {
        return loaded() ? SERVER.maxIslandRadius.get() : fallback;
    }

    public static boolean crashIslandsEnabled() {
        return loaded() ? SERVER.crashIslandsEnabled.get() : DEFAULT_CRASH_ISLANDS_ENABLED;
    }

    public static String crashIslandStructureId() {
        return loaded() ? SERVER.crashIslandStructureId.get() : DEFAULT_CRASH_ISLAND_STRUCTURE;
    }

    public static int crashIslandMinBaseRadius() {
        return loaded() ? SERVER.crashIslandMinBaseRadius.get() : DEFAULT_CRASH_ISLAND_MIN_RADIUS;
    }

    public static boolean crashIslandSkipOriginCell() {
        return loaded() ? SERVER.crashIslandSkipOriginCell.get() : DEFAULT_CRASH_ISLAND_SKIP_ORIGIN;
    }

    public static int crashIslandSpawnYOffset() {
        return loaded() ? SERVER.crashIslandSpawnYOffset.get() : DEFAULT_CRASH_ISLAND_SPAWN_Y_OFFSET;
    }

    public static boolean voidFallProtection() {
        return loaded() ? SERVER.voidFallProtection.get() : true;
    }

    public static int voidFallRescueY() {
        return loaded() ? SERVER.voidFallRescueY.get() : -40;
    }
}

package com.skylore.islands.worldgen;

/**
 * Square-ring spiral used by the pack prototype: ring 0 is (1,0), later rings skip (0,0).
 */
public final class CrashIslandCells {
    private CrashIslandCells() {}

    public static int[] cellAtSpawnIndex(int index, boolean skipOrigin) {
        int ring = 0;
        int count = 0;
        while (ring < 128) {
            int[][] cells = ringCells(ring, skipOrigin);
            if (index < count + cells.length) {
                return cells[index - count];
            }
            count += cells.length;
            ring++;
        }
        return new int[]{1, 0};
    }

    private static int[][] ringCells(int ring, boolean skipOrigin) {
        if (ring == 0) {
            return new int[][]{{1, 0}};
        }
        java.util.ArrayList<int[]> cells = new java.util.ArrayList<>();
        for (int x = -ring; x <= ring; x++) {
            cells.add(new int[]{x, -ring});
            cells.add(new int[]{x, ring});
        }
        for (int z = -ring + 1; z < ring; z++) {
            cells.add(new int[]{-ring, z});
            cells.add(new int[]{ring, z});
        }
        if (!skipOrigin) {
            return cells.toArray(new int[0][]);
        }
        cells.removeIf(c -> c[0] == 0 && c[1] == 0);
        return cells.toArray(new int[0][]);
    }
}

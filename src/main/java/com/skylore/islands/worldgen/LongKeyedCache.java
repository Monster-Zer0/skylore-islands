package com.skylore.islands.worldgen;

/**
 * Direct-mapped, thread-confined cache keyed by a primitive long. A miss overwrites one slot,
 * so there is no boxing and no wholesale clear. The whole cache is invalidated when the
 * caller's stamp (seed, salt, config) changes.
 */
public final class LongKeyedCache<V> {
    private final long[] keys;
    private final Object[] values;
    private final int mask;
    private long stamp;
    private boolean stamped;

    public LongKeyedCache(int capacityPow2) {
        if (Integer.bitCount(capacityPow2) != 1) {
            throw new IllegalArgumentException("capacity must be a power of two");
        }
        keys = new long[capacityPow2];
        values = new Object[capacityPow2];
        mask = capacityPow2 - 1;
    }

    /** Drops every entry if the stamp differs from the last one seen. */
    public void validate(long stamp) {
        if (!stamped || this.stamp != stamp) {
            java.util.Arrays.fill(values, null);
            this.stamp = stamp;
            stamped = true;
        }
    }

    @SuppressWarnings("unchecked")
    public V get(long key) {
        int slot = slot(key);
        Object value = values[slot];
        return value != null && keys[slot] == key ? (V) value : null;
    }

    public void put(long key, V value) {
        int slot = slot(key);
        keys[slot] = key;
        values[slot] = value;
    }

    private int slot(long key) {
        long h = key * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        return (int) h & mask;
    }

    /** Combines cache-invalidating inputs into one stamp. */
    public static long stamp(long worldSeed, long layoutSalt, int cellSize, int minAlt, int maxAlt, int minRad, int maxRad, int density) {
        long h = worldSeed * 0x9E3779B97F4A7C15L ^ layoutSalt;
        h = h * 31 + cellSize;
        h = h * 31 + minAlt;
        h = h * 31 + maxAlt;
        h = h * 31 + minRad;
        h = h * 31 + maxRad;
        h = h * 31 + density;
        return h;
    }
}

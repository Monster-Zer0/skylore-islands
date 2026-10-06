# Skylore Islands

Skylore Islands is a worldgen mod for **Minecraft 1.21.1** (NeoForge 21.1.x).
It turns the Overworld (and the Nether) into floating sky islands with empty void inbetween, mixed biomes, and most vanilla / modded structures still working.

Needs a **new world**. Existing terrain wont regenerate.

---

## Features

- Floating overworld with real void between islands. No ocean floor, no bedrock slab.
- Default spacing is 384 blocks (cell grid). Gaps are usually a few hundred blocks, sometimes more.
- Island height goes from around Y 20 up to about 260 so the underside can still hit deep ores.
- Size varies alot. Small islands, big plateaus, rare mega continents, the occasional ocean bowl or caldera lake.
- Nether uses it's own island map (not a copy of the overworld) with lava basins instead of water.
- Biomes still use vanilla climate. Modded overworld biomes should show up to.
- Structures try to sit on land or in bowls. Huge ones (mansions, Cataclysm stuff, etc) only on mega islands. Airships can spawn in open void.
- Water stays in bowls / lakes / rivers. It shouldnt sheet off the rim into the sky.
- Each player gets there own crash island on first join (schematic `skylore:islandspawn`). Cell (0,0) stays the world hub.
- Falling into the void gets you an updraft back toward land (slow falling).

---

## Config

Server config is `config/skylore_islands-server.toml` (world `serverconfig` after first load):

```toml
[general]
    cellSize = 384
    islandDensity = 70
    minIslandAltitude = 20
    maxIslandAltitude = 260
    minIslandRadius = 45
    maxIslandRadius = 170

[crash_islands]
    enabled = true
    structureId = "skylore:islandspawn"
    minBaseRadius = 40
    skipOriginCell = true
    spawnYOffset = 1
```

`islandDensity` 0 = hub island only. 50 is more isolated. 70 (default) is a bit denser and can clump. Changing cell size or density on an old save wont move islands you already explored.

---

## Notes

- Target is NeoForge 1.21.1, Java 21.
- Sea level is set to -64 and default fluid is air so aquifers dont flood the void.
- I recomend leaving aquifers off (the noise settings already do this).
- Distant Horizons is compatable, it just reads the same density as the server.

---

## Building

Java 21:

```bash
./gradlew jar
```

Jar lands in `libs/skylore_islands-<version>.jar`.
On Windows you can use `gradlew.bat` instead.

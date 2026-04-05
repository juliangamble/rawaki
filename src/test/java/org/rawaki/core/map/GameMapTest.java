package org.rawaki.core.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.Constants;
import org.rawaki.core.TerrainType;
import static org.junit.jupiter.api.Assertions.*;

class GameMapTest {

    private GameMap map;

    @BeforeEach
    void setUp() {
        map = new GameMap();
    }

    // ── constructor ───────────────────────────────────────────────────────

    @Test
    void creates256x256Grid() {
        assertEquals(256, map.cells().length);
        assertEquals(256, map.cells()[0].length);
    }

    @Test
    void initializesEmptyObjectLists() {
        assertTrue(map.pills().isEmpty());
        assertTrue(map.bases().isEmpty());
        assertTrue(map.starts().isEmpty());
    }

    // ── cellAtTile ────────────────────────────────────────────────────────

    @Test
    void returnsCellAtValidCoordinates() {
        MapCell cell = map.cellAtTile(100, 100);
        assertEquals(100, cell.x());
        assertEquals(100, cell.y());
    }

    @Test
    void returnsDummyCellForNegativeCoordinates() {
        MapCell cell = map.cellAtTile(-1, -1);
        assertEquals(TerrainType.DEEP_SEA, cell.type());
    }

    @Test
    void returnsDummyCellForOutOfBounds() {
        MapCell cell = map.cellAtTile(300, 300);
        assertEquals(TerrainType.DEEP_SEA, cell.type());
    }

    // ── each ──────────────────────────────────────────────────────────────

    @Test
    void iteratesOverAllCellsByDefault() {
        int[] count = {0};
        map.each(cell -> count[0]++);
        assertEquals(256 * 256, count[0]);
    }

    @Test
    void iteratesOverSpecifiedArea() {
        int[] count = {0};
        map.each(cell -> count[0]++, 0, 0, 9, 9);
        assertEquals(10 * 10, count[0]);
    }

    // ── clear ─────────────────────────────────────────────────────────────

    @Test
    void setsAllCellsToDeepSea() {
        map.cellAtTile(50, 50).setType(TerrainType.GRASS, null, -1);
        map.clear();
        assertEquals(TerrainType.DEEP_SEA, map.cellAtTile(50, 50).type());
    }

    @Test
    void clearsSpecifiedAreaOnly() {
        map.cellAtTile(50, 50).setType(TerrainType.GRASS, null, -1);
        map.cellAtTile(100, 100).setType(TerrainType.GRASS, null, -1);
        map.clear(40, 40, 60, 60);
        assertEquals(TerrainType.DEEP_SEA, map.cellAtTile(50, 50).type());
        assertEquals(TerrainType.GRASS, map.cellAtTile(100, 100).type());
    }

    // ── dump header ───────────────────────────────────────────────────────

    @Test
    void dumpCreatesBmapHeader() {
        byte[] data = map.dump();
        assertEquals('B', data[0]);
        assertEquals('M', data[1]);
        assertEquals('A', data[2]);
        assertEquals('P', data[3]);
        assertEquals('B', data[4]);
        assertEquals('O', data[5]);
        assertEquals('L', data[6]);
        assertEquals('O', data[7]);
    }

    @Test
    void dumpIncludesVersionNumber() {
        byte[] data = map.dump();
        assertEquals(1, data[8]);
    }

    @Test
    void dumpSerializesEmptyMap() {
        byte[] data = map.dump();
        assertNotNull(data);
        assertTrue(data.length > 12);
    }

    @Test
    void dumpExcludesPillsWhenFlagged() {
        map.pills().add(new Pillbox(50, 50, map.cellAtTile(50, 50), 255, 15, 50));
        byte[] with = map.dump(false, false, false);
        byte[] without = map.dump(true, false, false);
        assertTrue(without.length < with.length);
        assertEquals(0, without[9] & 0xFF); // numPills = 0
    }

    @Test
    void dumpExcludesBasesWhenFlagged() {
        map.bases().add(new Base(50, 50, map.cellAtTile(50, 50), 255, 90, 90, 90));
        byte[] with = map.dump(false, false, false);
        byte[] without = map.dump(false, true, false);
        assertTrue(without.length < with.length);
        assertEquals(0, without[10] & 0xFF); // numBases = 0
    }

    @Test
    void dumpExcludesStartsWhenFlagged() {
        map.starts().add(new Start(50, 50, map.cellAtTile(50, 50), 0));
        byte[] with = map.dump(false, false, false);
        byte[] without = map.dump(false, false, true);
        assertTrue(without.length < with.length);
        assertEquals(0, without[11] & 0xFF); // numStarts = 0
    }

    // ── load ──────────────────────────────────────────────────────────────

    @Test
    void loadEmptyMap() {
        byte[] data = map.dump();
        GameMap loaded = GameMap.load(data);
        assertNotNull(loaded);
        assertEquals(TerrainType.DEEP_SEA, loaded.cellAtTile(128, 128).type());
    }

    @Test
    void loadPreservesTerrainTypes() {
        map.cellAtTile(50, 50).setType(TerrainType.GRASS, null, -1);
        map.cellAtTile(51, 50).setType(TerrainType.FOREST, null, -1);
        map.cellAtTile(52, 50).setType(TerrainType.ROAD, null, -1);
        byte[] data = map.dump();
        GameMap loaded = GameMap.load(data);
        assertEquals(TerrainType.GRASS, loaded.cellAtTile(50, 50).type());
        assertEquals(TerrainType.FOREST, loaded.cellAtTile(51, 50).type());
        assertEquals(TerrainType.ROAD, loaded.cellAtTile(52, 50).type());
    }

    @Test
    void loadThrowsForInvalidMagic() {
        byte[] bad = "NOTBOLOx\1\0\0\0\4\u00FF\u00FF\u00FF".getBytes();
        assertThrows(IllegalArgumentException.class, () -> GameMap.load(bad));
    }

    @Test
    void loadThrowsForUnsupportedVersion() {
        byte[] data = map.dump();
        data[8] = 2; // corrupt version
        assertThrows(IllegalArgumentException.class, () -> GameMap.load(data));
    }

    // ── round-trip ────────────────────────────────────────────────────────

    @Test
    void preservesMapThroughDumpLoadCycle() {
        map.cellAtTile(50, 50).setType(TerrainType.GRASS, null, -1);
        map.cellAtTile(51, 50).setType(TerrainType.BUILDING, null, -1);
        map.cellAtTile(100, 100).setType(TerrainType.ROAD, null, -1);
        map.cellAtTile(100, 101).setType(TerrainType.ROAD, null, -1);
        map.cellAtTile(100, 102).setType(TerrainType.ROAD, null, -1);
        map.cellAtTile(100, 103).setType(TerrainType.ROAD, null, -1);

        byte[] data = map.dump();
        GameMap loaded = GameMap.load(data);

        assertEquals(TerrainType.GRASS, loaded.cellAtTile(50, 50).type());
        assertEquals(TerrainType.BUILDING, loaded.cellAtTile(51, 50).type());
        assertEquals(TerrainType.ROAD, loaded.cellAtTile(100, 100).type());
        assertEquals(TerrainType.ROAD, loaded.cellAtTile(100, 103).type());
        assertEquals(TerrainType.DEEP_SEA, loaded.cellAtTile(0, 0).type());
    }

    @Test
    void roundTripPreservesMapObjects() {
        map.pills().add(new Pillbox(50, 50, map.cellAtTile(50, 50), 2, 15, 50));
        map.bases().add(new Base(60, 60, map.cellAtTile(60, 60), 1, 90, 80, 70));
        map.starts().add(new Start(70, 70, map.cellAtTile(70, 70), 4));

        byte[] data = map.dump();
        GameMap loaded = GameMap.load(data);

        assertEquals(1, loaded.pills().size());
        assertEquals(50, loaded.pills().get(0).x());
        assertEquals(2, loaded.pills().get(0).ownerIdx());
        assertEquals(15, loaded.pills().get(0).armour());

        assertEquals(1, loaded.bases().size());
        assertEquals(60, loaded.bases().get(0).x());
        assertEquals(90, loaded.bases().get(0).armour());
        assertEquals(80, loaded.bases().get(0).shells());
        assertEquals(70, loaded.bases().get(0).mines());

        assertEquals(1, loaded.starts().size());
        assertEquals(70, loaded.starts().get(0).x());
        assertEquals(4, loaded.starts().get(0).direction());
    }

    @Test
    void roundTripPreservesLongRunsOfSameType() {
        // 20 consecutive road tiles — tests long sequence encoding
        for (int x = 50; x < 70; x++) {
            map.cellAtTile(x, 50).setType(TerrainType.ROAD, null, -1);
        }
        byte[] data = map.dump();
        GameMap loaded = GameMap.load(data);
        for (int x = 50; x < 70; x++) {
            assertEquals(TerrainType.ROAD, loaded.cellAtTile(x, 50).type(),
                "Cell at (" + x + ", 50) should be ROAD");
        }
    }

    @Test
    void roundTripPreservesMixedTerrain() {
        map.cellAtTile(50, 50).setType(TerrainType.GRASS, null, -1);
        map.cellAtTile(51, 50).setType(TerrainType.FOREST, null, -1);
        map.cellAtTile(52, 50).setType(TerrainType.SWAMP, null, -1);
        map.cellAtTile(53, 50).setType(TerrainType.CRATER, null, -1);
        map.cellAtTile(54, 50).setType(TerrainType.RUBBLE, null, -1);

        byte[] data = map.dump();
        GameMap loaded = GameMap.load(data);

        assertEquals(TerrainType.GRASS, loaded.cellAtTile(50, 50).type());
        assertEquals(TerrainType.FOREST, loaded.cellAtTile(51, 50).type());
        assertEquals(TerrainType.SWAMP, loaded.cellAtTile(52, 50).type());
        assertEquals(TerrainType.CRATER, loaded.cellAtTile(53, 50).type());
        assertEquals(TerrainType.RUBBLE, loaded.cellAtTile(54, 50).type());
    }
}

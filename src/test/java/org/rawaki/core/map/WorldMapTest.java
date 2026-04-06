package org.rawaki.core.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.Constants;
import org.rawaki.core.TerrainType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorldMapTest {

    private WorldMap map;

    @BeforeEach
    void setUp() {
        map = new WorldMap();
    }

    // ── cell type ─────────────────────────────────────────────────────────

    @Test
    void cellsAreWorldMapCells() {
        assertInstanceOf(WorldMapCell.class, map.cellAtTile(50, 50));
    }

    // ── cellAtPixel ───────────────────────────────────────────────────────

    @Test
    void cellAtPixelConvertsToCellCoordinates() {
        WorldMapCell cell = map.cellAtPixel(50 * Constants.TILE_SIZE_PIXELS + 1, 60 * Constants.TILE_SIZE_PIXELS + 1);
        assertEquals(50, cell.x());
        assertEquals(60, cell.y());
    }

    @Test
    void cellAtPixelHandlesEdgeCoordinates() {
        WorldMapCell cell = map.cellAtPixel(0, 0);
        assertEquals(0, cell.x());
        assertEquals(0, cell.y());
    }

    // ── cellAtWorld ───────────────────────────────────────────────────────

    @Test
    void cellAtWorldConvertsToCellCoordinates() {
        WorldMapCell cell = map.cellAtWorld(50 * Constants.TILE_SIZE_WORLD + 1, 60 * Constants.TILE_SIZE_WORLD + 1);
        assertEquals(50, cell.x());
        assertEquals(60, cell.y());
    }

    @Test
    void cellAtWorldHandlesFractionalCoordinates() {
        WorldMapCell cell = map.cellAtWorld(Constants.TILE_SIZE_WORLD / 2, Constants.TILE_SIZE_WORLD / 2);
        assertEquals(0, cell.x());
        assertEquals(0, cell.y());
    }

    // ── getRandomStart ────────────────────────────────────────────────────

    @Test
    void getRandomStartReturnsNullForEmptyStarts() {
        assertNull(map.getRandomStart());
    }

    @Test
    void getRandomStartReturnsAStartWhenAvailable() {
        Start start = new Start(50, 50, map.cellAtTile(50, 50), 4);
        map.starts().add(start);
        Start result = map.getRandomStart();
        assertNotNull(result);
        assertEquals(50, result.x());
    }

    @Test
    void getRandomStartReturnsFromMultipleStarts() {
        map.starts().add(new Start(10, 10, map.cellAtTile(10, 10), 0));
        map.starts().add(new Start(20, 20, map.cellAtTile(20, 20), 4));
        map.starts().add(new Start(30, 30, map.cellAtTile(30, 30), 8));
        Start result = map.getRandomStart();
        assertNotNull(result);
        assertTrue(List.of(10, 20, 30).contains(result.x()));
    }

    // ── world reference ───────────────────────────────────────────────────

    @Test
    void worldDefaultsToNull() {
        assertNull(map.world());
    }

    @Test
    void canSetAndGetWorld() {
        Object world = new Object();
        map.setWorld(world);
        assertSame(world, map.world());
    }
}

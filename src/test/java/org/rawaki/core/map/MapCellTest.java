package org.rawaki.core.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.TerrainType;
import static org.junit.jupiter.api.Assertions.*;

class MapCellTest {

    private GameMap map;
    private MapCell cell;

    @BeforeEach
    void setUp() {
        map = new GameMap();
        cell = map.cellAtTile(50, 50);
    }

    // ── constructor ───────────────────────────────────────────────────────

    @Test
    void initializesWithCorrectCoordinates() {
        assertEquals(50, cell.x());
        assertEquals(50, cell.y());
    }

    @Test
    void initializesWithDeepSeaType() {
        assertEquals(TerrainType.DEEP_SEA, cell.type());
    }

    @Test
    void calculatesCorrectIndex() {
        assertEquals(50 * 256 + 50, cell.idx());
    }

    @Test
    void marksEdgeCellsAsMined() {
        MapCell edgeCell = map.cellAtTile(10, 10);
        assertTrue(edgeCell.mine());
    }

    @Test
    void doesNotMarkCenterCellsAsMined() {
        assertFalse(cell.mine());
    }

    // ── neigh ─────────────────────────────────────────────────────────────

    @Test
    void returnsNeighboringCell() {
        MapCell neighbor = cell.neigh(1, 0);
        assertEquals(51, neighbor.x());
        assertEquals(50, neighbor.y());
    }

    @Test
    void returnsDummyCellForOffMapCoordinates() {
        MapCell neighbor = cell.neigh(-100, -100);
        assertEquals(TerrainType.DEEP_SEA, neighbor.type());
    }

    // ── isType ────────────────────────────────────────────────────────────

    @Test
    void returnsTrueForMatchingTerrainType() {
        cell.setType(TerrainType.GRASS);
        assertTrue(cell.isType(TerrainType.GRASS));
    }

    @Test
    void returnsTrueForMatchingAscii() {
        cell.setType(TerrainType.GRASS);
        assertTrue(cell.isType('.'));
    }

    @Test
    void returnsFalseForNonMatchingType() {
        cell.setType(TerrainType.GRASS);
        assertFalse(cell.isType(TerrainType.FOREST));
    }

    @Test
    void handlesMultipleTerrainTypes() {
        cell.setType(TerrainType.GRASS);
        assertTrue(cell.isType(TerrainType.GRASS, TerrainType.FOREST, TerrainType.ROAD));
        assertFalse(cell.isType(TerrainType.FOREST, TerrainType.ROAD, TerrainType.BUILDING));
    }

    @Test
    void handlesMultipleAsciiTypes() {
        cell.setType(TerrainType.GRASS);
        assertTrue(cell.isType('.', '#', '='));
        assertFalse(cell.isType('#', '=', '|'));
    }

    // ── isEdgeCell ────────────────────────────────────────────────────────

    @Test
    void returnsTrueForLeftEdge() {
        assertTrue(map.cellAtTile(10, 100).isEdgeCell());
    }

    @Test
    void returnsTrueForRightEdge() {
        assertTrue(map.cellAtTile(240, 100).isEdgeCell());
    }

    @Test
    void returnsTrueForTopEdge() {
        assertTrue(map.cellAtTile(100, 10).isEdgeCell());
    }

    @Test
    void returnsTrueForBottomEdge() {
        assertTrue(map.cellAtTile(100, 240).isEdgeCell());
    }

    @Test
    void returnsFalseForCenterCells() {
        assertFalse(map.cellAtTile(128, 128).isEdgeCell());
    }

    // ── getNumericType ────────────────────────────────────────────────────

    @Test
    void returnsNegativeOneForDeepSea() {
        cell.setType(TerrainType.DEEP_SEA);
        assertEquals(-1, cell.getNumericType());
    }

    @Test
    void returnsCorrectIndexForTerrainTypes() {
        cell.setType(TerrainType.GRASS);
        assertEquals(7, cell.getNumericType());
    }

    @Test
    void addsEightForMinedCells() {
        cell.setType(TerrainType.GRASS, true, -1);
        assertEquals(15, cell.getNumericType());
    }

    // ── setType ───────────────────────────────────────────────────────────

    @Test
    void setsByTerrainType() {
        cell.setType(TerrainType.GRASS);
        assertEquals(TerrainType.GRASS, cell.type());
    }

    @Test
    void setsByAsciiCharacter() {
        cell.setType('.');
        assertEquals(TerrainType.GRASS, cell.type());
    }

    @Test
    void setsByNumericValue() {
        cell.setType(7);
        assertEquals(TerrainType.GRASS, cell.type());
    }

    @Test
    void setsMineFlag() {
        cell.setType('.', true);
        assertTrue(cell.mine());
    }

    @Test
    void handlesNumericTypeWithMine() {
        cell.setType(15); // 7 + 8
        assertEquals(TerrainType.GRASS, cell.type());
        assertTrue(cell.mine());
    }

    @Test
    void throwsForInvalidAscii() {
        assertThrows(IllegalArgumentException.class, () -> cell.setType('?'));
    }

    @Test
    void throwsForInvalidNumericType() {
        assertThrows(IllegalArgumentException.class, () -> cell.setType(99));
    }

    @Test
    void forcesMineOnEdgeCells() {
        MapCell edgeCell = map.cellAtTile(10, 10);
        edgeCell.setType(TerrainType.GRASS, false, -1);
        assertTrue(edgeCell.mine());
    }

    // ── pill and base references ──────────────────────────────────────────

    @Test
    void pillDefaultsToNull() {
        assertNull(cell.pill());
    }

    @Test
    void baseDefaultsToNull() {
        assertNull(cell.base());
    }

    @Test
    void canSetAndGetPill() {
        Object pill = new Object();
        cell.setPill(pill);
        assertSame(pill, cell.pill());
    }

    @Test
    void canSetAndGetBase() {
        Object base = new Object();
        cell.setBase(base);
        assertSame(base, cell.base());
    }
}

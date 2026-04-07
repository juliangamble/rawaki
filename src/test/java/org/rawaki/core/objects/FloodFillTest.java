package org.rawaki.core.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.TerrainType;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

import static org.junit.jupiter.api.Assertions.*;

class FloodFillTest {

    private World world;
    private WorldMap map;
    private boolean destroyed;

    @BeforeEach
    void setUp() {
        map = new WorldMap();
        destroyed = false;
        world = new World() {
            public WorldMap map() { return map; }
            public void soundEffect(SoundEffect sfx, int x, int y, BoloObject owner) {}
            public <T extends BoloObject> T spawn(Class<T> type, Object... args) { return null; }
            public void destroy(BoloObject obj) { destroyed = true; }
            public void insert(BoloObject obj) {}
            public boolean authority() { return true; }
        };
    }

    private FloodFill createFloodFill(int tileX, int tileY) {
        var cell = (WorldMapCell) map.cellAtTile(tileX, tileY);
        var ff = new FloodFill(world);
        ff.spawn(cell);
        ff.anySpawn();
        return ff;
    }

    @Test
    void spawnSetsLifespanTo16() {
        var ff = createFloodFill(50, 50);
        assertEquals(16, ff.lifespan());
    }

    @Test
    void spawnSetsPositionFromCell() {
        var ff = createFloodFill(50, 50);
        assertNotNull(ff.x());
        assertNotNull(ff.y());
    }

    @Test
    void styledIsNull() {
        var ff = new FloodFill(world);
        assertNull(ff.styled());
    }

    @Test
    void anySpawnSetsCellAndNeighbours() {
        var ff = createFloodFill(50, 50);
        assertNotNull(ff.cell());
        assertNotNull(ff.neighbours());
        assertEquals(4, ff.neighbours().length);
    }

    @Test
    void updateDecrementsLifespan() {
        var ff = createFloodFill(50, 50);
        int before = ff.lifespan();
        ff.update();
        assertEquals(before - 1, ff.lifespan());
    }

    @Test
    void destroysAtLifespanZero() {
        var ff = createFloodFill(50, 50);
        ff.setLifespan(0);
        ff.update();
        assertTrue(destroyed);
    }

    @Test
    void canGetWetWhenNeighbourIsWater() {
        // Set cell to crater, one neighbour to river
        var cell = (WorldMapCell) map.cellAtTile(50, 50);
        cell.setType(TerrainType.CRATER, null, -1);
        ((WorldMapCell) map.cellAtTile(51, 50)).setType(TerrainType.RIVER, null, -1);
        var ff = createFloodFill(50, 50);
        assertTrue(ff.canGetWet());
    }

    @Test
    void canGetWetWhenNeighbourIsDeepSea() {
        var cell = (WorldMapCell) map.cellAtTile(50, 50);
        cell.setType(TerrainType.CRATER, null, -1);
        // Default is deep sea, so neighbours are already '^'
        var ff = createFloodFill(50, 50);
        assertTrue(ff.canGetWet());
    }

    @Test
    void cannotGetWetWhenSurroundedByLand() {
        // Set cell and all neighbours to grass
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                ((WorldMapCell) map.cellAtTile(50 + dx, 50 + dy)).setType(TerrainType.GRASS, null, -1);
        var ff = createFloodFill(50, 50);
        assertFalse(ff.canGetWet());
    }

    @Test
    void floodsWhenCanGetWet() {
        var cell = (WorldMapCell) map.cellAtTile(50, 50);
        cell.setType(TerrainType.CRATER, null, -1);
        ((WorldMapCell) map.cellAtTile(51, 50)).setType(TerrainType.RIVER, null, -1);
        var ff = createFloodFill(50, 50);
        ff.setLifespan(0);
        ff.update();
        assertEquals(TerrainType.RIVER, cell.type());
    }

    @Test
    void doesNotFloodWhenCannotGetWet() {
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                ((WorldMapCell) map.cellAtTile(50 + dx, 50 + dy)).setType(TerrainType.GRASS, null, -1);
        var cell = (WorldMapCell) map.cellAtTile(50, 50);
        cell.setType(TerrainType.CRATER, null, -1);
        var ff = createFloodFill(50, 50);
        ff.setLifespan(0);
        ff.update();
        assertEquals(TerrainType.CRATER, cell.type()); // unchanged
    }
}

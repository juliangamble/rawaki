package org.rawaki.core.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.TerrainType;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

import static org.junit.jupiter.api.Assertions.*;

class MineExplosionTest {

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
            public java.util.List<BoloObject> tanks() { return java.util.List.of(); }
        };
    }

    private MineExplosion createMineExplosion(int tileX, int tileY) {
        var cell = (WorldMapCell) map.cellAtTile(tileX, tileY);
        var me = new MineExplosion(world);
        me.spawn(cell);
        me.anySpawn();
        return me;
    }

    @Test
    void spawnSetsLifespanTo10() {
        var me = createMineExplosion(50, 50);
        assertEquals(10, me.lifespan());
    }

    @Test
    void spawnSetsPosition() {
        var me = createMineExplosion(50, 50);
        assertNotNull(me.x());
        assertNotNull(me.y());
    }

    @Test
    void styledIsNull() {
        var me = new MineExplosion(world);
        assertNull(me.styled());
    }

    @Test
    void anySpawnSetsCell() {
        var me = createMineExplosion(50, 50);
        assertNotNull(me.cell());
    }

    @Test
    void updateDecrementsLifespan() {
        var me = createMineExplosion(50, 50);
        int before = me.lifespan();
        me.update();
        assertEquals(before - 1, me.lifespan());
    }

    @Test
    void destroysAtLifespanZero() {
        var me = createMineExplosion(50, 50);
        me.setLifespan(0);
        me.update();
        assertTrue(destroyed);
    }

    @Test
    void doesNotExplodeIfNoMine() {
        var cell = (WorldMapCell) map.cellAtTile(50, 50);
        cell.setType(TerrainType.GRASS, false, -1);
        var me = createMineExplosion(50, 50);
        me.setLifespan(0);
        me.update();
        // Cell should still be grass (no explosion damage)
        assertEquals(TerrainType.GRASS, cell.type());
        assertTrue(destroyed);
    }

    @Test
    void explodesIfMinePresent() {
        var cell = (WorldMapCell) map.cellAtTile(50, 50);
        cell.setType(TerrainType.GRASS, true, -1);
        var me = createMineExplosion(50, 50);
        me.setLifespan(0);
        me.update();
        // Mine should be cleared
        assertFalse(cell.mine());
        // Terrain should be damaged (grass -> crater from takeExplosionHit)
        assertEquals(TerrainType.CRATER, cell.type());
    }

    @Test
    void doesNotDestroyBeforeZero() {
        var me = createMineExplosion(50, 50);
        me.setLifespan(5);
        me.update();
        assertFalse(destroyed);
    }
}

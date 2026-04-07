package org.rawaki.core.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.TerrainType;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

import static org.junit.jupiter.api.Assertions.*;

class FireballTest {

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

    private Fireball createFireball(int x, int y, int direction, boolean large) {
        var fb = new Fireball(world);
        fb.spawn(x, y, direction, large);
        return fb;
    }

    @Test
    void spawnSetsLifespanTo80() {
        var fb = createFireball(5120, 5120, 64, false);
        assertEquals(80, fb.lifespan());
    }

    @Test
    void spawnSetsPosition() {
        var fb = createFireball(100, 200, 64, false);
        assertEquals(100, fb.x());
        assertEquals(200, fb.y());
    }

    @Test
    void spawnSetsDirection() {
        var fb = createFireball(5120, 5120, 128, false);
        assertEquals(128, fb.direction());
    }

    @Test
    void spawnSetsLargeExplosion() {
        var fb = createFireball(5120, 5120, 64, true);
        assertTrue(fb.largeExplosion());
    }

    @Test
    void styledIsNull() {
        var fb = new Fireball(world);
        assertNull(fb.styled());
    }

    @Test
    void updateDecrementsLifespan() {
        // Place fireball on grass so wreck doesn't destroy it
        int cx = 50 * 256 + 128, cy = 50 * 256 + 128;
        ((WorldMapCell) map.cellAtTile(50, 50)).setType(TerrainType.GRASS, null, -1);
        var fb = createFireball(cx, cy, 64, false);
        int before = fb.lifespan();
        fb.update();
        assertEquals(before - 1, fb.lifespan());
    }

    @Test
    void destroysAtLifespanZero() {
        int cx = 50 * 256 + 128, cy = 50 * 256 + 128;
        ((WorldMapCell) map.cellAtTile(50, 50)).setType(TerrainType.GRASS, null, -1);
        // Set up surrounding cells as grass too for explosion
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                ((WorldMapCell) map.cellAtTile(50 + dx, 50 + dy)).setType(TerrainType.GRASS, null, -1);
        var fb = createFireball(cx, cy, 64, false);
        fb.setLifespan(1);
        fb.update(); // lifespan goes to 0, explode + destroy
        assertTrue(destroyed);
    }

    @Test
    void wreckDestroysOnDeepSea() {
        int cx = 50 * 256 + 128, cy = 50 * 256 + 128;
        // Deep sea is the default
        var fb = createFireball(cx, cy, 64, false);
        fb.update(); // wreck should detect deep sea and destroy
        assertTrue(destroyed);
    }

    @Test
    void wreckConvertsBoatToRiver() {
        int cx = 50 * 256 + 128, cy = 50 * 256 + 128;
        var cell = (WorldMapCell) map.cellAtTile(50, 50);
        cell.setType(TerrainType.BOAT, null, -1);
        var fb = createFireball(cx, cy, 64, false);
        fb.update(); // wreck on even tick
        assertEquals(TerrainType.RIVER, cell.type());
    }

    @Test
    void wreckConvertsForestToGrass() {
        int cx = 50 * 256 + 128, cy = 50 * 256 + 128;
        var cell = (WorldMapCell) map.cellAtTile(50, 50);
        cell.setType(TerrainType.FOREST, null, -1);
        var fb = createFireball(cx, cy, 64, false);
        fb.update();
        assertEquals(TerrainType.GRASS, cell.type());
    }

    @Test
    void getDirection16th() {
        var fb = createFireball(5120, 5120, 17, false);
        assertEquals(1, fb.getDirection16th());
    }
}

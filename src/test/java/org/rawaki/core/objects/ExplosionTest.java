package org.rawaki.core.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.world.World;

import static org.junit.jupiter.api.Assertions.*;

class ExplosionTest {

    private World world;
    private boolean destroyed;

    @BeforeEach
    void setUp() {
        destroyed = false;
        world = new World() {
            public WorldMap map() { return null; }
            public void soundEffect(SoundEffect sfx, int x, int y, BoloObject owner) {}
            public <T extends BoloObject> T spawn(Class<T> type, Object... args) { return null; }
            public void destroy(BoloObject obj) { destroyed = true; }
            public void insert(BoloObject obj) {}
            public boolean authority() { return true; }
        };
    }

    @Test
    void spawnSetsLifespanTo23() {
        var e = new Explosion(world);
        e.spawn(100, 200);
        assertEquals(23, e.lifespan());
    }

    @Test
    void spawnSetsPosition() {
        var e = new Explosion(world);
        e.spawn(100, 200);
        assertEquals(100, e.x());
        assertEquals(200, e.y());
    }

    @Test
    void styledIsFalse() {
        var e = new Explosion(world);
        assertEquals(false, e.styled());
    }

    @Test
    void updateDecrementsLifespan() {
        var e = new Explosion(world);
        e.spawn(0, 0);
        e.update();
        assertEquals(22, e.lifespan());
    }

    @Test
    void destroysWhenLifespanReachesZero() {
        var e = new Explosion(world);
        e.spawn(0, 0);
        e.setLifespan(0);
        e.update();
        assertTrue(destroyed);
    }

    @Test
    void doesNotDestroyBeforeZero() {
        var e = new Explosion(world);
        e.spawn(0, 0);
        e.setLifespan(1);
        e.update();
        assertFalse(destroyed);
    }

    @Test
    void getTileVariesWithLifespan() {
        var e = new Explosion(world);
        e.spawn(0, 0);
        e.setLifespan(23);
        assertArrayEquals(new int[]{20, 3}, e.getTile()); // floor(23/3) = 7
        e.setLifespan(2);
        assertArrayEquals(new int[]{19, 4}, e.getTile()); // floor(2/3) = 0
    }

    @Test
    void getTileAtEachPhase() {
        var e = new Explosion(world);
        e.spawn(0, 0);
        e.setLifespan(21); assertArrayEquals(new int[]{20, 3}, e.getTile()); // 7
        e.setLifespan(18); assertArrayEquals(new int[]{21, 3}, e.getTile()); // 6
        e.setLifespan(15); assertArrayEquals(new int[]{20, 4}, e.getTile()); // 5
        e.setLifespan(12); assertArrayEquals(new int[]{21, 4}, e.getTile()); // 4
        e.setLifespan(9);  assertArrayEquals(new int[]{20, 5}, e.getTile()); // 3
        e.setLifespan(6);  assertArrayEquals(new int[]{21, 5}, e.getTile()); // 2
        e.setLifespan(3);  assertArrayEquals(new int[]{18, 4}, e.getTile()); // 1
        e.setLifespan(0);  assertArrayEquals(new int[]{19, 4}, e.getTile()); // 0
    }
}

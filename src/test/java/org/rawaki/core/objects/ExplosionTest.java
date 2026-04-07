package org.rawaki.core.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExplosionTest {

    private TestWorld world;

    @BeforeEach
    void setUp() {
        world = new TestWorld();
    }

    @Test void spawnSetsLifespanTo23() { var e = new Explosion(world); e.spawn(100, 200); assertEquals(23, e.lifespan()); }
    @Test void spawnSetsPosition() { var e = new Explosion(world); e.spawn(100, 200); assertEquals(100, e.x()); assertEquals(200, e.y()); }
    @Test void styledIsFalse() { assertEquals(false, new Explosion(world).styled()); }
    @Test void updateDecrementsLifespan() { var e = new Explosion(world); e.spawn(0, 0); e.update(); assertEquals(22, e.lifespan()); }

    @Test void destroysWhenLifespanReachesZero() {
        var e = new Explosion(world); e.spawn(0, 0); e.setLifespan(0); e.update();
        assertTrue(world.destroyed.contains(e));
    }

    @Test void doesNotDestroyBeforeZero() {
        var e = new Explosion(world); e.spawn(0, 0); e.setLifespan(1); e.update();
        assertFalse(world.destroyed.contains(e));
    }

    @Test void getTileVariesWithLifespan() {
        var e = new Explosion(world); e.spawn(0, 0);
        e.setLifespan(23); assertArrayEquals(new int[]{20, 3}, e.getTile());
        e.setLifespan(2);  assertArrayEquals(new int[]{19, 4}, e.getTile());
    }

    @Test void getTileAtEachPhase() {
        var e = new Explosion(world); e.spawn(0, 0);
        e.setLifespan(21); assertArrayEquals(new int[]{20, 3}, e.getTile());
        e.setLifespan(18); assertArrayEquals(new int[]{21, 3}, e.getTile());
        e.setLifespan(15); assertArrayEquals(new int[]{20, 4}, e.getTile());
        e.setLifespan(12); assertArrayEquals(new int[]{21, 4}, e.getTile());
        e.setLifespan(9);  assertArrayEquals(new int[]{20, 5}, e.getTile());
        e.setLifespan(6);  assertArrayEquals(new int[]{21, 5}, e.getTile());
        e.setLifespan(3);  assertArrayEquals(new int[]{18, 4}, e.getTile());
        e.setLifespan(0);  assertArrayEquals(new int[]{19, 4}, e.getTile());
    }
}

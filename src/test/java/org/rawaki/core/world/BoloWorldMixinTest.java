package org.rawaki.core.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.objects.BoloObject;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoloWorldMixinTest {

    private TestWorld world;

    // Concrete implementation for testing
    static class TestWorld implements BoloWorldMixin {
        final List<BoloObject> tanks = new ArrayList<>();
        final List<BoloObject> inserted = new ArrayList<>();
        final List<BoloObject> mapObjects = new ArrayList<>();
        boolean authorityFlag = true;
        int resolveCount = 0;

        public List<BoloObject> tanks() { return tanks; }
        public WorldMap map() { return null; }
        public void soundEffect(SoundEffect sfx, int x, int y, BoloObject owner) {}
        public <T extends BoloObject> T spawn(Class<T> type, Object... args) { return null; }
        public void destroy(BoloObject obj) {}
        public void insert(BoloObject obj) { inserted.add(obj); }
        public boolean authority() { return authorityFlag; }

        @Override
        public List<BoloObject> getAllMapObjects() { return mapObjects; }

        @Override
        public void resolveMapObjectOwners() { resolveCount++; }
    }

    static class FakeObject extends BoloObject {
        boolean spawnCalled, anySpawnCalled;
        FakeObject() { super(null); }
        @Override public void spawn(Object... args) { spawnCalled = true; }
        @Override public void anySpawn() { anySpawnCalled = true; }
    }

    @BeforeEach
    void setUp() {
        world = new TestWorld();
    }

    // ── addTank ───────────────────────────────────────────────────────────

    @Test
    void addTankAppends() {
        var t = new FakeObject();
        world.addTank(t);
        assertEquals(1, world.tanks().size());
        assertSame(t, world.tanks().get(0));
    }

    @Test
    void addTankAssignsIdx() {
        var t0 = new FakeObject();
        var t1 = new FakeObject();
        world.addTank(t0);
        world.addTank(t1);
        assertEquals(0, t0.idx());
        assertEquals(1, t1.idx());
    }

    @Test
    void addTankResolvesOwnersWhenAuthority() {
        world.authorityFlag = true;
        world.resolveCount = 0;
        world.addTank(new FakeObject());
        assertEquals(1, world.resolveCount);
    }

    @Test
    void addTankSkipsResolveWhenNotAuthority() {
        world.authorityFlag = false;
        world.resolveCount = 0;
        world.addTank(new FakeObject());
        assertEquals(0, world.resolveCount);
    }

    // ── removeTank ────────────────────────────────────────────────────────

    @Test
    void removeTankRemovesFromList() {
        var t0 = new FakeObject();
        var t1 = new FakeObject();
        world.addTank(t0);
        world.addTank(t1);
        world.removeTank(t0);
        assertEquals(1, world.tanks().size());
        assertSame(t1, world.tanks().get(0));
    }

    @Test
    void removeTankReindexesRemaining() {
        var t0 = new FakeObject();
        var t1 = new FakeObject();
        var t2 = new FakeObject();
        world.addTank(t0);
        world.addTank(t1);
        world.addTank(t2);
        world.removeTank(t0);
        assertEquals(0, t1.idx());
        assertEquals(1, t2.idx());
    }

    @Test
    void removeTankHandlesLastTank() {
        var t = new FakeObject();
        world.addTank(t);
        world.removeTank(t);
        assertTrue(world.tanks().isEmpty());
    }

    @Test
    void removeTankHandlesMiddleTank() {
        var t0 = new FakeObject();
        var t1 = new FakeObject();
        var t2 = new FakeObject();
        world.addTank(t0);
        world.addTank(t1);
        world.addTank(t2);
        world.removeTank(t1);
        assertEquals(2, world.tanks().size());
        assertEquals(0, t0.idx());
        assertEquals(1, t2.idx());
    }

    // ── spawnMapObjects ───────────────────────────────────────────────────

    @Test
    void spawnMapObjectsSetsWorld() {
        var obj = new FakeObject();
        world.mapObjects.add(obj);
        world.spawnMapObjects();
        assertSame(world, obj.world());
    }

    @Test
    void spawnMapObjectsCallsInsert() {
        var obj = new FakeObject();
        world.mapObjects.add(obj);
        world.spawnMapObjects();
        assertTrue(world.inserted.contains(obj));
    }

    @Test
    void spawnMapObjectsCallsSpawnAndAnySpawn() {
        var obj = new FakeObject();
        world.mapObjects.add(obj);
        world.spawnMapObjects();
        assertTrue(obj.spawnCalled);
        assertTrue(obj.anySpawnCalled);
    }

    @Test
    void spawnMapObjectsProcessesMultiple() {
        var o1 = new FakeObject();
        var o2 = new FakeObject();
        world.mapObjects.add(o1);
        world.mapObjects.add(o2);
        world.spawnMapObjects();
        assertTrue(o1.spawnCalled);
        assertTrue(o2.spawnCalled);
    }

    // ── getAllMapObjects ───────────────────────────────────────────────────

    @Test
    void getAllMapObjectsReturnsConfiguredList() {
        var obj = new FakeObject();
        world.mapObjects.add(obj);
        assertEquals(1, world.getAllMapObjects().size());
        assertSame(obj, world.getAllMapObjects().get(0));
    }

    @Test
    void getAllMapObjectsReturnsEmptyByDefault() {
        var defaultWorld = new TestWorld();
        defaultWorld.mapObjects.clear();
        assertTrue(defaultWorld.getAllMapObjects().isEmpty());
    }
}

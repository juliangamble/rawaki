package org.rawaki.core.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.world.World;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoloObjectTest {

    private World world;
    private List<SoundEffect> soundsPlayed;

    // Concrete subclass for testing
    static class TestObject extends BoloObject {
        boolean spawnCalled, updateCalled, destroyCalled, anySpawnCalled;

        TestObject(World world) { super(world); }

        @Override public void spawn(Object... args) { spawnCalled = true; }
        @Override public void update() { updateCalled = true; }
        @Override public void destroy() { destroyCalled = true; }
        @Override public void anySpawn() { anySpawnCalled = true; }
        @Override public int[] getTile() { return new int[]{1, 2}; }
    }

    @BeforeEach
    void setUp() {
        soundsPlayed = new ArrayList<>();
        world = new World() {
            public WorldMap map() { return null; }
            public void soundEffect(SoundEffect sfx, int x, int y, BoloObject owner) {
                soundsPlayed.add(sfx);
            }
            public <T extends BoloObject> T spawn(Class<T> type, Object... args) { return null; }
            public void destroy(BoloObject obj) {}
            public void insert(BoloObject obj) {}
            public boolean authority() { return true; }
        };
    }

    // ── constructor ───────────────────────────────────────────────────────

    @Test
    void setsWorldReference() {
        var obj = new TestObject(world);
        assertSame(world, obj.world());
    }

    @Test
    void defaultsStyledToNull() {
        var obj = new TestObject(world);
        assertNull(obj.styled());
    }

    @Test
    void defaultsTeamTo255() {
        var obj = new TestObject(world);
        assertEquals(255, obj.team());
    }

    @Test
    void defaultsPositionToNull() {
        var obj = new TestObject(world);
        assertNull(obj.x());
        assertNull(obj.y());
    }

    @Test
    void defaultsUpdatePriorityToZero() {
        var obj = new TestObject(world);
        assertEquals(0, obj.updatePriority());
    }

    // ── accessors ─────────────────────────────────────────────────────────

    @Test
    void canSetAndGetIdx() {
        var obj = new TestObject(world);
        obj.setIdx(42);
        assertEquals(42, obj.idx());
    }

    @Test
    void canSetAndGetPosition() {
        var obj = new TestObject(world);
        obj.setX(100);
        obj.setY(200);
        assertEquals(100, obj.x());
        assertEquals(200, obj.y());
    }

    @Test
    void canSetAndGetTeam() {
        var obj = new TestObject(world);
        obj.setTeam(1);
        assertEquals(1, obj.team());
    }

    @Test
    void canSetUpdatePriority() {
        var obj = new TestObject(world);
        obj.setUpdatePriority(20);
        assertEquals(20, obj.updatePriority());
    }

    // ── lifecycle ─────────────────────────────────────────────────────────

    @Test
    void spawnCallsSubclass() {
        var obj = new TestObject(world);
        obj.spawn();
        assertTrue(obj.spawnCalled);
    }

    @Test
    void updateCallsSubclass() {
        var obj = new TestObject(world);
        obj.update();
        assertTrue(obj.updateCalled);
    }

    @Test
    void destroyCallsSubclass() {
        var obj = new TestObject(world);
        obj.destroy();
        assertTrue(obj.destroyCalled);
    }

    @Test
    void anySpawnCallsSubclass() {
        var obj = new TestObject(world);
        obj.anySpawn();
        assertTrue(obj.anySpawnCalled);
    }

    // ── getTile ───────────────────────────────────────────────────────────

    @Test
    void getTileReturnsSubclassValue() {
        var obj = new TestObject(world);
        assertArrayEquals(new int[]{1, 2}, obj.getTile());
    }

    @Test
    void getTileDefaultsToNull() {
        var obj = new BoloObject(world) {};
        assertNull(obj.getTile());
    }

    // ── soundEffect ───────────────────────────────────────────────────────

    @Test
    void soundEffectDelegatesToWorld() {
        var obj = new TestObject(world);
        obj.setX(100);
        obj.setY(200);
        obj.soundEffect(SoundEffect.SHOOTING);
        assertEquals(1, soundsPlayed.size());
        assertEquals(SoundEffect.SHOOTING, soundsPlayed.get(0));
    }

    @Test
    void soundEffectDoesNothingWhenPositionNull() {
        var obj = new TestObject(world);
        obj.soundEffect(SoundEffect.SHOOTING);
        assertTrue(soundsPlayed.isEmpty());
    }

    // ── events ────────────────────────────────────────────────────────────

    @Test
    void onAndEmit() {
        var obj = new TestObject(world);
        boolean[] called = {false};
        obj.on("test", () -> called[0] = true);
        obj.emit("test");
        assertTrue(called[0]);
    }

    @Test
    void emitWithNoListenersDoesNotThrow() {
        var obj = new TestObject(world);
        assertDoesNotThrow(() -> obj.emit("nonexistent"));
    }

    @Test
    void multipleListeners() {
        var obj = new TestObject(world);
        int[] count = {0};
        obj.on("test", () -> count[0]++);
        obj.on("test", () -> count[0]++);
        obj.emit("test");
        assertEquals(2, count[0]);
    }

    @Test
    void removeListener() {
        var obj = new TestObject(world);
        boolean[] called = {false};
        Runnable listener = () -> called[0] = true;
        obj.on("test", listener);
        obj.removeListener("test", listener);
        obj.emit("test");
        assertFalse(called[0]);
    }

    @Test
    void removeAllListeners() {
        var obj = new TestObject(world);
        int[] count = {0};
        obj.on("test", () -> count[0]++);
        obj.on("test", () -> count[0]++);
        obj.removeAllListeners("test");
        obj.emit("test");
        assertEquals(0, count[0]);
    }

    // ── Ref ─────────────────────────────────────────────────────────────────────

    @Test
    void refHoldsTarget() {
        var owner = new TestObject(world);
        var target = new TestObject(world);
        var ref = owner.ref(target);
        assertSame(target, ref.get());
        assertTrue(ref.isPresent());
    }

    @Test
    void refClearNullsTarget() {
        var owner = new TestObject(world);
        var target = new TestObject(world);
        var ref = owner.ref(target);
        ref.clear();
        assertNull(ref.get());
        assertFalse(ref.isPresent());
    }

    @Test
    void refSetReplacesTarget() {
        var owner = new TestObject(world);
        var t1 = new TestObject(world);
        var t2 = new TestObject(world);
        var ref = owner.ref(t1);
        ref.set(t2);
        assertSame(t2, ref.get());
    }

    @Test
    void refClearsWhenTargetFinalized() {
        var owner = new TestObject(world);
        var target = new TestObject(world);
        var ref = owner.ref(target);
        target.emit("finalize");
        assertNull(ref.get());
    }

    @Test
    void refClearsWhenOwnerFinalized() {
        var owner = new TestObject(world);
        var target = new TestObject(world);
        var ref = owner.ref(target);
        owner.emit("finalize");
        assertNull(ref.get());
    }

    @Test
    void refSetToNullClears() {
        var owner = new TestObject(world);
        var target = new TestObject(world);
        var ref = owner.ref(target);
        ref.set(null);
        assertNull(ref.get());
    }

    @Test
    void refWithNullTarget() {
        var owner = new TestObject(world);
        var ref = owner.ref((TestObject) null);
        assertNull(ref.get());
        assertFalse(ref.isPresent());
    }
}

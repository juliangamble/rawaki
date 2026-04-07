package org.rawaki.core.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.Constants;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

import static org.junit.jupiter.api.Assertions.*;

class WorldBaseTest {

    private World world;
    private WorldMap map;

    // Fake tank that implements RefuelTarget
    static class FakeTank extends BoloObject implements WorldBase.RefuelTarget {
        int tankArmour, tankShells, tankMines;
        WorldMapCell tankCell;

        FakeTank(World world, int armour, int shells, int mines) {
            super(world);
            this.tankArmour = armour;
            this.tankShells = shells;
            this.tankMines = mines;
        }

        public WorldMapCell cell() { return tankCell; }
        public int armour() { return tankArmour; }
        public int shells() { return tankShells; }
        public int mines() { return tankMines; }
        public void addArmour(int a) { tankArmour += a; }
        public void addShells(int a) { tankShells += a; }
        public void addMines(int a) { tankMines += a; }
    }

    @BeforeEach
    void setUp() {
        map = new WorldMap();
        world = new World() {
            public WorldMap map() { return map; }
            public void soundEffect(SoundEffect sfx, int x, int y, BoloObject owner) {}
            public <T extends BoloObject> T spawn(Class<T> type, Object... args) { return null; }
            public void destroy(BoloObject obj) {}
            public void insert(BoloObject obj) {}
            public boolean authority() { return true; }
        };
    }

    private WorldBase createBase(int armour, int shells, int mines) {
        var base = new WorldBase(world);
        base.initFromMap(20, 20, 255, armour, shells, mines);
        base.anySpawn();
        return base;
    }

    // ── initFromMap ───────────────────────────────────────────────────────

    @Test
    void setsPositionFromTileCoordinates() {
        var base = new WorldBase(world);
        base.initFromMap(20, 30, 255, 90, 90, 90);
        assertEquals((int) ((20 + 0.5) * Constants.TILE_SIZE_WORLD), base.x());
        assertEquals((int) ((30 + 0.5) * Constants.TILE_SIZE_WORLD), base.y());
    }

    @Test
    void storesProperties() {
        var base = new WorldBase(world);
        base.initFromMap(10, 10, 2, 80, 70, 60);
        assertEquals(2, base.ownerIdx());
        assertEquals(80, base.armour());
        assertEquals(70, base.shells());
        assertEquals(60, base.mines());
    }

    // ── updateOwner ───────────────────────────────────────────────────────

    @Test
    void setsTeamFromOwner() {
        var base = createBase(90, 90, 90);
        var tank = new BoloObject(world) {};
        tank.setIdx(3);
        tank.setTeam(1);
        base.owner().set(tank);
        base.updateOwner();
        assertEquals(1, base.team());
        assertEquals(3, base.ownerIdx());
    }

    @Test
    void setsTeamTo255WhenNoOwner() {
        var base = createBase(90, 90, 90);
        base.owner().clear();
        base.updateOwner();
        assertEquals(255, base.team());
        assertEquals(255, base.ownerIdx());
    }

    // ── anySpawn ──────────────────────────────────────────────────────────

    @Test
    void setsCellReferenceAndMarksBase() {
        var base = createBase(90, 90, 90);
        assertNotNull(base.cell());
        assertSame(base, base.cell().base());
    }

    // ── takeShellHit ──────────────────────────────────────────────────────

    @Test
    void reducesArmourBy5() {
        var base = createBase(90, 90, 90);
        base.takeShellHit();
        assertEquals(85, base.armour());
    }

    @Test
    void doesNotReduceArmourBelow0() {
        var base = createBase(3, 90, 90);
        base.takeShellHit();
        assertEquals(0, base.armour());
    }

    @Test
    void returnsShotBuildingSound() {
        var base = createBase(90, 90, 90);
        assertEquals(SoundEffect.SHOT_BUILDING, base.takeShellHit());
    }

    // ── update refueling ──────────────────────────────────────────────────

    @Test
    void stopsRefuelingIfTankLeavesCell() {
        var base = createBase(90, 90, 90);
        var tank = new FakeTank(world, 40, 20, 5);
        tank.tankCell = (WorldMapCell) map.cellAtTile(99, 99); // different cell
        base.startRefueling(tank, 1);
        base.update();
        assertFalse(base.refueling().isPresent());
    }

    @Test
    void stopsRefuelingIfTankIsDead() {
        var base = createBase(90, 90, 90);
        var tank = new FakeTank(world, 255, 20, 5);
        tank.tankCell = base.cell();
        base.startRefueling(tank, 1);
        base.update();
        assertFalse(base.refueling().isPresent());
    }

    @Test
    void transfersArmourFirst() {
        var base = createBase(20, 90, 90);
        var tank = new FakeTank(world, 30, 40, 40);
        tank.tankCell = base.cell();
        base.startRefueling(tank, 1);
        base.update();
        assertEquals(35, tank.tankArmour);
        assertEquals(15, base.armour());
        assertEquals(46, base.refuelCounter());
    }

    @Test
    void capsArmourTransferAt5() {
        var base = createBase(90, 90, 90);
        var tank = new FakeTank(world, 10, 40, 40);
        tank.tankCell = base.cell();
        base.startRefueling(tank, 1);
        base.update();
        assertEquals(15, tank.tankArmour);
        assertEquals(85, base.armour());
    }

    @Test
    void capsArmourTransferToNotExceed40() {
        var base = createBase(90, 90, 90);
        var tank = new FakeTank(world, 38, 40, 40);
        tank.tankCell = base.cell();
        base.startRefueling(tank, 1);
        base.update();
        assertEquals(40, tank.tankArmour);
        assertEquals(88, base.armour());
    }

    @Test
    void transfersShellsWhenArmourFull() {
        var base = createBase(0, 10, 90);
        var tank = new FakeTank(world, 40, 5, 40);
        tank.tankCell = base.cell();
        base.startRefueling(tank, 1);
        base.update();
        assertEquals(6, tank.tankShells);
        assertEquals(9, base.shells());
        assertEquals(7, base.refuelCounter());
    }

    @Test
    void transfersMinesWhenArmourAndShellsFull() {
        var base = createBase(0, 0, 10);
        var tank = new FakeTank(world, 40, 40, 2);
        tank.tankCell = base.cell();
        base.startRefueling(tank, 1);
        base.update();
        assertEquals(3, tank.tankMines);
        assertEquals(9, base.mines());
        assertEquals(7, base.refuelCounter());
    }

    @Test
    void setsRefuelCounterTo1WhenNothingToTransfer() {
        var base = createBase(0, 0, 0);
        var tank = new FakeTank(world, 40, 40, 40);
        tank.tankCell = base.cell();
        base.startRefueling(tank, 1);
        base.update();
        assertEquals(1, base.refuelCounter());
    }

    @Test
    void doesNotTransferUntilCounterReachesZero() {
        var base = createBase(90, 90, 90);
        var tank = new FakeTank(world, 10, 20, 5);
        tank.tankCell = base.cell();
        base.startRefueling(tank, 5);
        base.update();
        assertEquals(10, tank.tankArmour); // no transfer yet
        assertEquals(4, base.refuelCounter());
    }
}

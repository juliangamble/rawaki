package org.rawaki.core.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.map.Start;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

import static org.junit.jupiter.api.Assertions.*;

class TankTest {

    private World world;
    private WorldMap map;
    private Tank tank;

    @BeforeEach
    void setUp() {
        map = new WorldMap();
        map.starts().add(new Start(50, 50, map.cellAtTile(50, 50), 0));
        world = new World() {
            public WorldMap map() { return map; }
            public void soundEffect(SoundEffect sfx, int x, int y, BoloObject owner) {}
            public <T extends BoloObject> T spawn(Class<T> type, Object... args) { return null; }
            public void destroy(BoloObject obj) {}
            public void insert(BoloObject obj) {}
            public boolean authority() { return true; }
            public java.util.List<BoloObject> tanks() { return java.util.List.of(); }
        };
        tank = new Tank(world);
        tank.reset();
        var cell = (WorldMapCell) map.cellAtTile(50, 50);
        cell.setType(org.rawaki.core.TerrainType.GRASS, null, -1);
        tank.setCell(cell);
    }

    // ── reset ─────────────────────────────────────────────────────────────

    @Test void resetsArmourTo40()    { assertEquals(40, tank.armour()); }
    @Test void resetsShellsTo40()    { assertEquals(40, tank.shells()); }
    @Test void resetsMinesTo0()      { assertEquals(0, tank.mines()); }
    @Test void resetsTreesTo0()      { assertEquals(0, tank.trees()); }
    @Test void resetsSpeedTo0()      { assertEquals(0.0, tank.speed()); }
    @Test void resetsOnBoatToTrue()  { assertTrue(tank.onBoat()); }
    @Test void resetsFiringRangeTo7(){ assertEquals(7.0, tank.firingRange()); }
    @Test void resetsShootingFalse() { assertFalse(tank.shooting()); }

    @Test void resetsAcceleratingAndBraking() {
        assertFalse(tank.accelerating());
        assertFalse(tank.braking());
    }

    @Test void resetsTurning() {
        assertFalse(tank.turningClockwise());
        assertFalse(tank.turningCounterClockwise());
    }

    @Test void resetsPosition() {
        assertNotNull(tank.x());
        assertNotNull(tank.y());
    }

    // ── getDirection16th ──────────────────────────────────────────────────

    @Test void direction16thFor1()   { tank.setDirection(1);  assertEquals(0, tank.getDirection16th()); }
    @Test void direction16thFor17()  { tank.setDirection(17); assertEquals(1, tank.getDirection16th()); }
    @Test void direction16thWraps()  { tank.setDirection(255); assertTrue(tank.getDirection16th() >= 0 && tank.getDirection16th() <= 15); }

    // ── range ─────────────────────────────────────────────────────────────

    @Test void increaseRange() {
        tank.setFiringRange(3);
        tank.increaseRange();
        assertEquals(3.5, tank.firingRange());
    }

    @Test void increaseRangeCapsAt7() {
        tank.setFiringRange(7);
        tank.increaseRange();
        assertEquals(7.0, tank.firingRange());
    }

    @Test void decreaseRange() {
        tank.setFiringRange(5);
        tank.decreaseRange();
        assertEquals(4.5, tank.firingRange());
    }

    @Test void decreaseRangeFloorAt1() {
        tank.setFiringRange(1);
        tank.decreaseRange();
        assertEquals(1.0, tank.firingRange());
    }

    // ── isAlly ────────────────────────────────────────────────────────────

    @Test void selfIsAlly() {
        assertTrue(tank.isAlly(tank));
    }

    @Test void sameTeamIsAlly() {
        var other = new Tank(world);
        tank.setTeam(1); other.setTeam(1);
        assertTrue(tank.isAlly(other));
    }

    @Test void differentTeamNotAlly() {
        var other = new Tank(world);
        tank.setTeam(0); other.setTeam(1);
        assertFalse(tank.isAlly(other));
    }

    @Test void team255NotAlly() {
        var other = new Tank(world);
        tank.setTeam(255); other.setTeam(255);
        assertFalse(tank.isAlly(other));
    }

    // ── getTile ───────────────────────────────────────────────────────────

    @Test void tileBoatRow() {
        tank.setOnBoat(true); tank.setDirection(1);
        assertEquals(1, tank.getTile()[1]);
    }

    @Test void tileTankRow() {
        tank.setOnBoat(false); tank.setDirection(1);
        assertEquals(0, tank.getTile()[1]);
    }

    @Test void tileVariesWithDirection() {
        tank.setDirection(1);   int tx1 = tank.getTile()[0];
        tank.setDirection(129); int tx2 = tank.getTile()[0];
        assertNotEquals(tx1, tx2);
    }

    // ── takeShellHit ──────────────────────────────────────────────────────

    @Test void shellHitReducesArmourBy5() {
        tank.setArmour(40);
        tank.takeShellHit(0);
        assertEquals(35, tank.armour());
    }

    @Test void shellHitSetsSlide() {
        tank.takeShellHit(128);
        assertEquals(8, tank.slideTicks());
        assertEquals(128, tank.slideDirection());
    }

    @Test void shellHitKillsWhenArmourBelow0() {
        tank.setArmour(3);
        tank.takeShellHit(0);
        assertEquals(255, tank.armour());
    }

    @Test void shellHitDestroysBoat() {
        tank.setOnBoat(true); tank.setArmour(40);
        tank.takeShellHit(0);
        assertFalse(tank.onBoat());
    }

    @Test void shellHitReturnsHitTankSound() {
        assertEquals(SoundEffect.HIT_TANK, tank.takeShellHit(0));
    }

    // ── takeMineHit ───────────────────────────────────────────────────────

    @Test void mineHitReducesArmourBy10() {
        tank.setArmour(40);
        tank.takeMineHit();
        assertEquals(30, tank.armour());
    }

    @Test void mineHitKillsWhenArmourBelow0() {
        tank.setArmour(5);
        tank.takeMineHit();
        assertEquals(255, tank.armour());
    }

    @Test void mineHitDestroysBoat() {
        tank.setOnBoat(true); tank.setArmour(40);
        tank.takeMineHit();
        assertFalse(tank.onBoat());
    }

    // ── kill ──────────────────────────────────────────────────────────────

    @Test void killSetsArmourTo255() { tank.kill(); assertEquals(255, tank.armour()); }
    @Test void killNullsPosition()   { tank.kill(); assertNull(tank.x()); assertNull(tank.y()); }
    @Test void killSetsRespawnTimer(){ tank.kill(); assertEquals(255, tank.respawnTimer()); }

    // ── death ─────────────────────────────────────────────────────────────

    @Test void deathReturnsFalseWhenAlive() { assertFalse(tank.death()); }

    @Test void deathReturnsTrueWhenDead() {
        tank.kill();
        assertTrue(tank.death());
    }

    @Test void deathDecrementsRespawnTimer() {
        tank.kill();
        int before = tank.respawnTimer();
        tank.death();
        assertEquals(before - 1, tank.respawnTimer());
    }

    // ── turn ──────────────────────────────────────────────────────────────

    @Test void turnCancelsWhenBothKeys() {
        tank.setDirection(128);
        tank.setTurningClockwise(true);
        tank.setTurningCounterClockwise(true);
        tank.turn();
        assertEquals(128, tank.direction());
        assertEquals(0, tank.turnSpeedup());
    }

    @Test void turnCounterClockwise() {
        tank.setDirection(128);
        tank.setTurningCounterClockwise(true);
        // First turn at half rate (turnSpeedup < 10), so run enough turns to see movement
        for (int i = 0; i < 3; i++) tank.turn();
        assertTrue(tank.direction() > 128);
    }

    @Test void turnClockwise() {
        tank.setDirection(128);
        tank.setTurningClockwise(true);
        tank.turn();
        assertTrue(tank.direction() < 128);
    }

    @Test void turnWrapsBelow0() {
        tank.setDirection(0);
        tank.setTurningClockwise(true);
        // Need enough speedup to get full turn rate
        for (int i = 0; i < 20; i++) tank.turn();
        assertTrue(tank.direction() >= 0 && tank.direction() < 256);
    }

    // ── accelerate ────────────────────────────────────────────────────────

    @Test void accelerateIncreasesSpeed() {
        tank.setSpeed(0); tank.setAccelerating(true);
        tank.accelerate();
        assertEquals(0.25, tank.speed());
    }

    @Test void brakeDecreasesSpeed() {
        tank.setSpeed(4); tank.setBraking(true);
        tank.accelerate();
        assertEquals(3.75, tank.speed());
    }

    @Test void speedDoesNotGoBelowZero() {
        tank.setSpeed(0); tank.setBraking(true);
        tank.accelerate();
        assertEquals(0.0, tank.speed());
    }

    @Test void speedDoesNotExceedMaxForTerrain() {
        // Grass max speed is 12
        tank.setSpeed(12); tank.setAccelerating(true);
        tank.accelerate();
        assertEquals(12.0, tank.speed());
    }

    @Test void accelerateAndBrakeCancelOut() {
        tank.setSpeed(4);
        tank.setAccelerating(true); tank.setBraking(true);
        tank.accelerate();
        assertEquals(4.0, tank.speed());
    }

    // ── shootOrReload ─────────────────────────────────────────────────────

    @Test void firesWhenReady() {
        tank.setShooting(true); tank.setReload(0); tank.setShells(10);
        tank.shootOrReload();
        assertEquals(9, tank.shells());
        assertEquals(13, tank.reload());
    }

    @Test void doesNotFireWhenReloading() {
        tank.setShooting(true); tank.setReload(5); tank.setShells(10);
        tank.shootOrReload();
        assertEquals(10, tank.shells());
        assertEquals(4, tank.reload());
    }

    @Test void doesNotFireWhenOutOfShells() {
        tank.setShooting(true); tank.setReload(0); tank.setShells(0);
        tank.shootOrReload();
        assertEquals(0, tank.shells());
    }

    @Test void decrementsReloadTimer() {
        tank.setReload(5); tank.setShooting(false);
        tank.shootOrReload();
        assertEquals(4, tank.reload());
    }

    // ── RefuelTarget interface ────────────────────────────────────────────

    @Test void addArmourIncreasesArmour() {
        tank.setArmour(30);
        tank.addArmour(5);
        assertEquals(35, tank.armour());
    }

    @Test void addShellsIncreasesShells() {
        tank.setShells(10);
        tank.addShells(1);
        assertEquals(11, tank.shells());
    }

    @Test void addMinesIncreasesMines() {
        tank.setMines(5);
        tank.addMines(1);
        assertEquals(6, tank.mines());
    }
}

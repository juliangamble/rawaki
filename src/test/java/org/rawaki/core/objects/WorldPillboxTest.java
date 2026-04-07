package org.rawaki.core.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.Constants;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

import static org.junit.jupiter.api.Assertions.*;

class WorldPillboxTest {

    private World world;
    private WorldMap map;

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
            public java.util.List<BoloObject> tanks() { return java.util.List.of(); }
        };
    }

    private WorldPillbox createPill(int armour, int speed) {
        var pill = new WorldPillbox(world);
        pill.initFromMap(20, 20, 255, armour, speed);
        return pill;
    }

    // ── initFromMap ───────────────────────────────────────────────────────

    @Test
    void setsPositionFromTileCoordinates() {
        var pill = createPill(15, 50);
        assertEquals((int) ((20 + 0.5) * Constants.TILE_SIZE_WORLD), pill.x());
        assertEquals((int) ((20 + 0.5) * Constants.TILE_SIZE_WORLD), pill.y());
    }

    @Test
    void storesOwnerIdxArmourSpeed() {
        var pill = new WorldPillbox(world);
        pill.initFromMap(10, 10, 3, 12, 40);
        assertEquals(3, pill.ownerIdx());
        assertEquals(12, pill.armour());
        assertEquals(40, pill.speed());
    }

    // ── updateOwner ───────────────────────────────────────────────────────

    @Test
    void setsTeamFromOwner() {
        var pill = createPill(15, 50);
        var tank = new BoloObject(world) {};
        tank.setIdx(5);
        tank.setTeam(1);
        pill.owner().set(tank);
        pill.updateOwner();
        assertEquals(1, pill.team());
        assertEquals(5, pill.ownerIdx());
    }

    @Test
    void setsTeamTo255WhenNoOwner() {
        var pill = createPill(15, 50);
        pill.owner().clear();
        pill.updateOwner();
        assertEquals(255, pill.team());
        assertEquals(255, pill.ownerIdx());
    }

    // ── updateCell ────────────────────────────────────────────────────────

    @Test
    void setsCellToNullWhenInTank() {
        var pill = createPill(15, 50);
        pill.anySpawn(); // sets cell
        assertNotNull(pill.cell());
        pill.setInTank(true);
        pill.updateCell();
        assertNull(pill.cell());
    }

    @Test
    void setsCellToNullWhenCarried() {
        var pill = createPill(15, 50);
        pill.anySpawn();
        pill.setCarried(true);
        pill.updateCell();
        assertNull(pill.cell());
    }

    // ── placeAt ───────────────────────────────────────────────────────────

    @Test
    void placeAtSetsInTankAndCarriedToFalse() {
        var pill = createPill(15, 50);
        pill.setInTank(true);
        pill.setCarried(true);
        var cell = (WorldMapCell) map.cellAtTile(30, 30);
        pill.placeAt(cell);
        assertFalse(pill.inTank());
        assertFalse(pill.carried());
    }

    @Test
    void placeAtSetsPositionFromCell() {
        var pill = createPill(15, 50);
        var cell = (WorldMapCell) map.cellAtTile(30, 40);
        pill.placeAt(cell);
        assertEquals((int) ((30 + 0.5) * Constants.TILE_SIZE_WORLD), pill.x());
        assertEquals((int) ((40 + 0.5) * Constants.TILE_SIZE_WORLD), pill.y());
    }

    @Test
    void placeAtResetsCoolDownAndReload() {
        var pill = createPill(15, 50);
        pill.setCoolDown(5);
        pill.setReload(10);
        pill.placeAt((WorldMapCell) map.cellAtTile(30, 30));
        assertEquals(32, pill.coolDown());
        assertEquals(0, pill.reload());
    }

    // ── reset ─────────────────────────────────────────────────────────────

    @Test
    void resetSetsCoolDownTo32() {
        var pill = createPill(15, 50);
        pill.setCoolDown(0);
        pill.reset();
        assertEquals(32, pill.coolDown());
    }

    @Test
    void resetSetsReloadTo0() {
        var pill = createPill(15, 50);
        pill.setReload(50);
        pill.reset();
        assertEquals(0, pill.reload());
    }

    // ── aggravate ─────────────────────────────────────────────────────────

    @Test
    void aggravatResetsCoolDownTo32() {
        var pill = createPill(15, 50);
        pill.setCoolDown(5);
        pill.aggravate();
        assertEquals(32, pill.coolDown());
    }

    @Test
    void aggravateHalvesSpeed() {
        var pill = createPill(15, 40);
        pill.aggravate();
        assertEquals(20, pill.speed());
    }

    @Test
    void aggravateDoesNotReduceSpeedBelow6() {
        var pill = createPill(15, 8);
        pill.aggravate();
        assertEquals(6, pill.speed());
    }

    // ── takeShellHit ──────────────────────────────────────────────────────

    @Test
    void takeShellHitReducesArmourBy1() {
        var pill = createPill(15, 50);
        pill.anySpawn();
        pill.takeShellHit();
        assertEquals(14, pill.armour());
    }

    @Test
    void takeShellHitDoesNotReduceArmourBelow0() {
        var pill = createPill(0, 50);
        pill.anySpawn();
        pill.takeShellHit();
        assertEquals(0, pill.armour());
    }

    @Test
    void takeShellHitReturnsShotBuildingSound() {
        var pill = createPill(15, 50);
        pill.anySpawn();
        assertEquals(SoundEffect.SHOT_BUILDING, pill.takeShellHit());
    }

    @Test
    void takeShellHitAggravates() {
        var pill = createPill(15, 40);
        pill.anySpawn();
        pill.setCoolDown(5);
        pill.takeShellHit();
        assertEquals(32, pill.coolDown());
        assertEquals(20, pill.speed());
    }

    // ── takeExplosionHit ──────────────────────────────────────────────────

    @Test
    void takeExplosionHitReducesArmourBy5() {
        var pill = createPill(15, 50);
        pill.anySpawn();
        pill.takeExplosionHit();
        assertEquals(10, pill.armour());
    }

    @Test
    void takeExplosionHitDoesNotReduceArmourBelow0() {
        var pill = createPill(3, 50);
        pill.anySpawn();
        pill.takeExplosionHit();
        assertEquals(0, pill.armour());
    }

    // ── repair ────────────────────────────────────────────────────────────

    @Test
    void repairIncreasesArmourByTimes4() {
        var pill = createPill(3, 50);
        pill.anySpawn();
        pill.repair(2);
        assertEquals(11, pill.armour());
    }

    @Test
    void repairCapsArmourAt15() {
        var pill = createPill(10, 50);
        pill.anySpawn();
        pill.repair(10);
        assertEquals(15, pill.armour());
    }

    @Test
    void repairReturnsTreesUsed() {
        var pill = createPill(10, 50);
        pill.anySpawn();
        // Need ceil((15-10)/4) = ceil(1.25) = 2 trees
        assertEquals(2, pill.repair(10));
    }

    @Test
    void repairReturns0WhenFullArmour() {
        var pill = createPill(15, 50);
        pill.anySpawn();
        assertEquals(0, pill.repair(5));
    }

    @Test
    void repairUsesOnlyAvailableTrees() {
        var pill = createPill(0, 50);
        pill.anySpawn();
        // Need ceil(15/4) = 4 trees, but only have 2
        assertEquals(2, pill.repair(2));
        assertEquals(8, pill.armour());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test
    void updateSkipsWhenInTank() {
        var pill = createPill(15, 50);
        pill.setInTank(true);
        pill.setReload(0);
        pill.update();
        assertEquals(0, pill.reload());
    }

    @Test
    void updateSkipsWhenCarried() {
        var pill = createPill(15, 50);
        pill.setCarried(true);
        pill.setReload(0);
        pill.update();
        assertEquals(0, pill.reload());
    }

    @Test
    void updateIncrementsReload() {
        var pill = createPill(15, 50);
        pill.anySpawn();
        pill.setReload(10);
        pill.update();
        assertEquals(11, pill.reload());
    }

    @Test
    void updateDecrementsCoolDown() {
        var pill = createPill(15, 50);
        pill.anySpawn();
        pill.setCoolDown(10);
        pill.update();
        assertEquals(9, pill.coolDown());
    }

    @Test
    void updateResetsCoolDownAndIncrementsSpeedAtZero() {
        var pill = createPill(15, 50);
        pill.anySpawn();
        pill.setCoolDown(1);
        pill.update();
        assertEquals(32, pill.coolDown());
        assertEquals(51, pill.speed());
    }

    @Test
    void updateCapsSpeedAt100() {
        var pill = createPill(15, 100);
        pill.anySpawn();
        pill.setCoolDown(1);
        pill.update();
        assertEquals(100, pill.speed());
    }
}

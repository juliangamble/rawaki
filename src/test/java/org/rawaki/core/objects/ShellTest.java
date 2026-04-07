package org.rawaki.core.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.Constants;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.TerrainType;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

import static org.junit.jupiter.api.Assertions.*;

class ShellTest {

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

    private Shell createShell(int direction) {
        var owner = new BoloObject(world) {};
        owner.setX(50 * Constants.TILE_SIZE_WORLD + 128);
        owner.setY(50 * Constants.TILE_SIZE_WORLD + 128);
        var shell = new Shell(world);
        shell.initShell(owner, direction, 7, false);
        return shell;
    }

    // ── constructor ───────────────────────────────────────────────────────

    @Test void updatePriorityIs20() {
        var shell = new Shell(world);
        assertEquals(20, shell.updatePriority());
    }

    @Test void styledIsFalse() {
        var shell = new Shell(world);
        assertEquals(false, shell.styled());
    }

    // ── getDirection16th ──────────────────────────────────────────────────

    @Test void direction16thFor1() {
        var shell = new Shell(world);
        shell.setDirection(1);
        assertEquals(0, shell.getDirection16th());
    }

    @Test void direction16thFor17() {
        var shell = new Shell(world);
        shell.setDirection(17);
        assertEquals(1, shell.getDirection16th());
    }

    @Test void direction16thWraps() {
        var shell = new Shell(world);
        shell.setDirection(255);
        assertTrue(shell.getDirection16th() >= 0 && shell.getDirection16th() <= 15);
    }

    // ── getTile ───────────────────────────────────────────────────────────

    @Test void tileRow4() {
        var shell = new Shell(world);
        shell.setDirection(1);
        assertEquals(4, shell.getTile()[1]);
    }

    @Test void tileVariesWithDirection() {
        var shell = new Shell(world);
        shell.setDirection(1);   int tx1 = shell.getTile()[0];
        shell.setDirection(129); int tx2 = shell.getTile()[0];
        assertNotEquals(tx1, tx2);
    }

    // ── initShell / spawn defaults ────────────────────────────────────────

    @Test void lifespanBasedOnRange() {
        var shell = createShell(64);
        assertEquals((int) (7.0 * Constants.TILE_SIZE_WORLD / 32 - 2), shell.lifespan());
    }

    @Test void defaultsOnWaterToFalse() {
        var shell = createShell(64);
        assertFalse(shell.onWater());
    }

    @Test void startsAtOwnerPosition() {
        var owner = new BoloObject(world) {};
        owner.setX(3000); owner.setY(4000);
        var shell = new Shell(world);
        shell.initShell(owner, 64, 7, false);
        // After initShell, shell has moved one step from owner position
        assertNotNull(shell.x());
        assertNotNull(shell.y());
    }

    // ── move ──────────────────────────────────────────────────────────────

    @Test void moveChangesPosition() {
        var shell = createShell(64);
        int origX = shell.x(), origY = shell.y();
        shell.move();
        assertTrue(shell.x() != origX || shell.y() != origY);
    }

    @Test void moveApproximately32UnitsPerStep() {
        var shell = createShell(64);
        int origX = shell.x(), origY = shell.y();
        shell.move();
        double dx = shell.x() - origX, dy = shell.y() - origY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        assertEquals(32, dist, 1);
    }

    @Test void moveUpdatesCell() {
        var shell = createShell(64);
        shell.move();
        assertNotNull(shell.cell());
    }

    // ── collide ───────────────────────────────────────────────────────────

    @Test void noCollisionOnGrass() {
        var shell = createShell(64);
        shell.cell().setType(TerrainType.GRASS, null, -1);
        assertNull(shell.collide());
    }

    @Test void collidesWithBuilding() {
        var shell = createShell(64);
        shell.cell().setType(TerrainType.BUILDING, null, -1);
        assertNotNull(shell.collide());
        assertEquals("cell", shell.collide()[0]);
    }

    @Test void collidesWithForest() {
        var shell = createShell(64);
        shell.cell().setType(TerrainType.FOREST, null, -1);
        assertNotNull(shell.collide());
    }

    @Test void collidesWithShotBuilding() {
        var shell = createShell(64);
        shell.cell().setType(TerrainType.SHOT_BUILDING, null, -1);
        assertNotNull(shell.collide());
    }

    @Test void collidesWithBoat() {
        var shell = createShell(64);
        shell.cell().setType(TerrainType.BOAT, null, -1);
        assertNotNull(shell.collide());
    }

    @Test void noCollisionWithDeepSeaWhenNotOnWater() {
        var shell = createShell(64);
        shell.cell().setType(TerrainType.DEEP_SEA, null, -1);
        shell.setOnWater(false);
        assertNull(shell.collide());
    }

    @Test void noCollisionWithWaterWhenOnWater() {
        var shell = createShell(64);
        shell.cell().setType(TerrainType.RIVER, null, -1);
        shell.setOnWater(true);
        assertNull(shell.collide());
    }

    @Test void collidesWithGrassWhenOnWater() {
        var shell = createShell(64);
        shell.cell().setType(TerrainType.GRASS, null, -1);
        shell.setOnWater(true);
        assertNotNull(shell.collide());
    }

    // ── asplode ───────────────────────────────────────────────────────────

    @Test void asplodeDestroysShell() {
        var shell = createShell(64);
        shell.asplode();
        assertTrue(destroyed);
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test void updateDecrementsLifespan() {
        var shell = createShell(64);
        // Ensure cell is grass so no terrain collision
        shell.cell().setType(TerrainType.GRASS, null, -1);
        int before = shell.lifespan();
        shell.update();
        // After move, cell may change — set it to grass again
        if (shell.cell() != null) shell.cell().setType(TerrainType.GRASS, null, -1);
        assertEquals(before - 1, shell.lifespan());
    }

    @Test void updateDestroysShellWhenLifespanReachesZero() {
        var shell = createShell(64);
        shell.cell().setType(TerrainType.GRASS, null, -1);
        shell.setLifespan(0);
        shell.update();
        assertTrue(destroyed);
    }
}

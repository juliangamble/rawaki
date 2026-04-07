package org.rawaki.core.objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.TerrainType;
import org.rawaki.core.map.Start;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

import static org.junit.jupiter.api.Assertions.*;

class BuilderTest {

    private World world;
    private WorldMap map;

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
    }

    private Builder createBuilder() {
        var b = new Builder(world);
        b.setOrder(Builder.IN_TANK);
        b.setAnimation(0);
        b.setTrees(0);
        b.setHasMine(false);
        return b;
    }

    // ── states ────────────────────────────────────────────────────────────

    @Test void inTankIs0()      { assertEquals(0, Builder.IN_TANK); }
    @Test void waitingIs1()     { assertEquals(1, Builder.WAITING); }
    @Test void returningIs2()   { assertEquals(2, Builder.RETURNING); }
    @Test void parachutingIs3() { assertEquals(3, Builder.PARACHUTING); }
    @Test void actionsStartAt10() { assertEquals(10, Builder.ACTION_FOREST); }
    @Test void mineIs16()       { assertEquals(16, Builder.ACTION_MINE); }

    // ── getTile ───────────────────────────────────────────────────────────

    @Test void parachuteTile() {
        var b = createBuilder();
        b.setOrder(Builder.PARACHUTING);
        assertArrayEquals(new int[]{16, 1}, b.getTile());
    }

    @Test void animationTile() {
        var b = createBuilder();
        b.setOrder(Builder.WAITING);
        b.setAnimation(0);
        assertArrayEquals(new int[]{17, 0}, b.getTile());
    }

    @Test void animationTileVaries() {
        var b = createBuilder();
        b.setOrder(Builder.RETURNING);
        b.setAnimation(6);
        assertEquals(2, b.getTile()[1]); // floor(6/3) = 2
    }

    // ── kill ──────────────────────────────────────────────────────────────

    @Test void killSetsOrderToParachuting() {
        var b = createBuilder();
        b.setOrder(Builder.ACTION_ROAD);
        b.setX(5120); b.setY(5120);
        b.setCell((WorldMapCell) map.cellAtTile(20, 20));
        b.kill();
        assertEquals(Builder.PARACHUTING, b.order());
    }

    @Test void killClearsTreesAndMine() {
        var b = createBuilder();
        b.setOrder(Builder.ACTION_ROAD);
        b.setX(5120); b.setY(5120);
        b.setCell((WorldMapCell) map.cellAtTile(20, 20));
        b.setTrees(5);
        b.setHasMine(true);
        b.kill();
        assertEquals(0, b.trees());
        assertFalse(b.hasMine());
    }

    @Test void killDoesNothingWhenNotAuthority() {
        var noAuthWorld = new World() {
            public WorldMap map() { return map; }
            public void soundEffect(SoundEffect sfx, int x, int y, BoloObject owner) {}
            public <T extends BoloObject> T spawn(Class<T> type, Object... args) { return null; }
            public void destroy(BoloObject obj) {}
            public void insert(BoloObject obj) {}
            public boolean authority() { return false; }
            public java.util.List<BoloObject> tanks() { return java.util.List.of(); }
        };
        var b = new Builder(noAuthWorld);
        b.setOrder(Builder.ACTION_ROAD);
        b.kill();
        assertEquals(Builder.ACTION_ROAD, b.order());
    }

    // ── reached - returning ───────────────────────────────────────────────

    @Test void reachedReturningGoesInTank() {
        var b = createBuilder();
        b.setOrder(Builder.RETURNING);
        b.setTrees(3);
        b.setHasMine(false);
        b.reached();
        assertEquals(Builder.IN_TANK, b.order());
        assertNull(b.x());
        assertNull(b.y());
    }

    @Test void reachedReturnsClearsTreesAndMine() {
        var b = createBuilder();
        b.setOrder(Builder.RETURNING);
        b.setTrees(3);
        b.setHasMine(true);
        b.reached();
        assertEquals(0, b.trees());
        assertFalse(b.hasMine());
    }

    // ── reached - build actions ───────────────────────────────────────────

    @Test void harvestForest() {
        var b = createBuilder();
        b.setOrder(Builder.ACTION_FOREST);
        var cell = (WorldMapCell) map.cellAtTile(60, 60);
        cell.setType(TerrainType.FOREST, null, -1);
        b.setCell(cell);
        b.setX(60 * 256 + 128); b.setY(60 * 256 + 128);
        b.reached();
        assertEquals(TerrainType.GRASS, cell.type());
        assertEquals(4, b.trees());
        assertEquals(Builder.WAITING, b.order());
    }

    @Test void buildRoadOnGrass() {
        var b = createBuilder();
        b.setOrder(Builder.ACTION_ROAD);
        var cell = (WorldMapCell) map.cellAtTile(60, 60);
        cell.setType(TerrainType.GRASS, null, -1);
        b.setCell(cell);
        b.setX(60 * 256 + 128); b.setY(60 * 256 + 128);
        b.reached();
        assertEquals(TerrainType.ROAD, cell.type());
        assertEquals(Builder.WAITING, b.order());
    }

    @Test void repairShotBuilding() {
        var b = createBuilder();
        b.setOrder(Builder.ACTION_REPAIR);
        var cell = (WorldMapCell) map.cellAtTile(60, 60);
        cell.setType(TerrainType.SHOT_BUILDING, null, -1);
        b.setCell(cell);
        b.setX(60 * 256 + 128); b.setY(60 * 256 + 128);
        b.reached();
        assertEquals(TerrainType.BUILDING, cell.type());
    }

    @Test void buildBoatOnWater() {
        var b = createBuilder();
        b.setOrder(Builder.ACTION_BOAT);
        var cell = (WorldMapCell) map.cellAtTile(60, 60);
        cell.setType(TerrainType.RIVER, null, -1);
        b.setCell(cell);
        b.setX(60 * 256 + 128); b.setY(60 * 256 + 128);
        b.reached();
        assertEquals(TerrainType.BOAT, cell.type());
    }

    @Test void buildBuildingOnGrass() {
        var b = createBuilder();
        b.setOrder(Builder.ACTION_BUILDING);
        var cell = (WorldMapCell) map.cellAtTile(60, 60);
        cell.setType(TerrainType.GRASS, null, -1);
        b.setCell(cell);
        b.setX(60 * 256 + 128); b.setY(60 * 256 + 128);
        b.reached();
        assertEquals(TerrainType.BUILDING, cell.type());
    }

    @Test void doesNotBuildBuildingOnForest() {
        var b = createBuilder();
        b.setOrder(Builder.ACTION_BUILDING);
        var cell = (WorldMapCell) map.cellAtTile(60, 60);
        cell.setType(TerrainType.FOREST, null, -1);
        b.setCell(cell);
        b.setX(60 * 256 + 128); b.setY(60 * 256 + 128);
        b.reached();
        // Forest is in exclusion list, so cell stays forest
        assertEquals(TerrainType.FOREST, cell.type());
    }

    @Test void layMineOnGrass() {
        var b = createBuilder();
        b.setOrder(Builder.ACTION_MINE);
        b.setHasMine(true);
        var cell = (WorldMapCell) map.cellAtTile(60, 60);
        cell.setType(TerrainType.GRASS, null, -1);
        b.setCell(cell);
        b.setX(60 * 256 + 128); b.setY(60 * 256 + 128);
        b.reached();
        assertTrue(cell.mine());
        assertFalse(b.hasMine());
    }

    @Test void doesNotLayMineOnWater() {
        var b = createBuilder();
        b.setOrder(Builder.ACTION_MINE);
        b.setHasMine(true);
        var cell = (WorldMapCell) map.cellAtTile(60, 60);
        cell.setType(TerrainType.RIVER, null, -1);
        b.setCell(cell);
        b.setX(60 * 256 + 128); b.setY(60 * 256 + 128);
        b.reached();
        // River is in exclusion list for mine
        assertFalse(cell.mine());
    }

    @Test void mineExplosionOnMinedCell() {
        var b = createBuilder();
        b.setOrder(Builder.ACTION_ROAD);
        var cell = (WorldMapCell) map.cellAtTile(60, 60);
        cell.setType(TerrainType.GRASS, true, -1);
        b.setCell(cell);
        b.setX(60 * 256 + 128); b.setY(60 * 256 + 128);
        b.reached();
        assertEquals(Builder.WAITING, b.order());
        assertEquals(20, b.waitTimer());
    }

    // ── update ────────────────────────────────────────────────────────────

    @Test void updateDoesNothingWhenInTank() {
        var b = createBuilder();
        b.setOrder(Builder.IN_TANK);
        b.setAnimation(0);
        b.update();
        assertEquals(0, b.animation());
    }

    @Test void updateIncrementsAnimation() {
        var b = createBuilder();
        b.setOrder(Builder.WAITING);
        b.setWaitTimer(10);
        b.setAnimation(0);
        b.update();
        assertEquals(1, b.animation());
    }

    @Test void updateWrapsAnimationAt9() {
        var b = createBuilder();
        b.setOrder(Builder.WAITING);
        b.setWaitTimer(10);
        b.setAnimation(8);
        b.update();
        assertEquals(0, b.animation());
    }

    @Test void updateDecrementsWaitTimer() {
        var b = createBuilder();
        b.setOrder(Builder.WAITING);
        b.setWaitTimer(5);
        b.update();
        assertEquals(4, b.waitTimer());
    }

    @Test void updateTransitionsToReturningWhenWaitTimerHits0() {
        var b = createBuilder();
        b.setOrder(Builder.WAITING);
        b.setWaitTimer(0);
        b.update();
        assertEquals(Builder.RETURNING, b.order());
    }
}

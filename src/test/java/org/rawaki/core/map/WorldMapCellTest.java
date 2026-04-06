package org.rawaki.core.map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rawaki.core.Constants;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.TerrainType;
import static org.junit.jupiter.api.Assertions.*;

class WorldMapCellTest {

    private GameMap map;
    private WorldMapCell cell;

    record FakeTank(int armour, boolean onBoat, WorldMapCell cell) implements WorldMapCell.TankLike {}

    static WorldMapCell.PillLike fakePill(int armour) {
        return new WorldMapCell.PillLike() {
            public int armour() { return armour; }
            public void takeExplosionHit() {}
        };
    }

    @BeforeEach
    void setUp() {
        map = new GameMap() {
            @Override protected MapCell createCell(int x, int y) {
                return new WorldMapCell(this, x, y);
            }
        };
        cell = (WorldMapCell) map.cellAtTile(50, 50);
    }

    // ── constructor ───────────────────────────────────────────────────────

    @Test
    void initializesLifeToZero() {
        assertEquals(0, cell.life());
    }

    @Test
    void inheritsFromMapCell() {
        assertEquals(50, cell.x());
        assertEquals(50, cell.y());
    }

    // ── setType life tracking ─────────────────────────────────────────────

    @Test
    void setsLifeTo5ForGrass() {
        cell.setType(TerrainType.GRASS, null, -1);
        assertEquals(5, cell.life());
    }

    @Test
    void setsLifeTo5ForShotBuilding() {
        cell.setType(TerrainType.SHOT_BUILDING, null, -1);
        assertEquals(5, cell.life());
    }

    @Test
    void setsLifeTo5ForRubble() {
        cell.setType(TerrainType.RUBBLE, null, -1);
        assertEquals(5, cell.life());
    }

    @Test
    void setsLifeTo4ForSwamp() {
        cell.setType(TerrainType.SWAMP, null, -1);
        assertEquals(4, cell.life());
    }

    @Test
    void setsLifeTo0ForOtherTerrain() {
        cell.setType(TerrainType.FOREST, null, -1);
        assertEquals(0, cell.life());
    }

    // ── isObstacle ────────────────────────────────────────────────────────

    @Test
    void buildingIsObstacle() {
        cell.setType(TerrainType.BUILDING, null, -1);
        assertTrue(cell.isObstacle());
    }

    @Test
    void grassIsNotObstacle() {
        cell.setType(TerrainType.GRASS, null, -1);
        assertFalse(cell.isObstacle());
    }

    @Test
    void pillboxWithArmourIsObstacle() {
        cell.setType(TerrainType.GRASS, null, -1);
        cell.setPill(fakePill(10));
        assertTrue(cell.isObstacle());
    }

    @Test
    void destroyedPillboxIsNotObstacle() {
        cell.setType(TerrainType.GRASS, null, -1);
        cell.setPill(fakePill(0));
        assertFalse(cell.isObstacle());
    }

    // ── getPixelCoordinates / getWorldCoordinates ─────────────────────────

    @Test
    void pixelCoordinates() {
        double[] coords = cell.getPixelCoordinates();
        assertEquals((50 + 0.5) * Constants.TILE_SIZE_PIXELS, coords[0]);
        assertEquals((50 + 0.5) * Constants.TILE_SIZE_PIXELS, coords[1]);
    }

    @Test
    void worldCoordinates() {
        double[] coords = cell.getWorldCoordinates();
        assertEquals((50 + 0.5) * Constants.TILE_SIZE_WORLD, coords[0]);
        assertEquals((50 + 0.5) * Constants.TILE_SIZE_WORLD, coords[1]);
    }

    // ── getTankSpeed ──────────────────────────────────────────────────────

    @Test
    void tankSpeedZeroForPillboxWithArmour() {
        cell.setType(TerrainType.GRASS, null, -1);
        cell.setPill(fakePill(10));
        assertEquals(0, cell.getTankSpeed(new FakeTank(40, false, cell)));
    }

    @Test
    void tankSpeedForGrass() {
        cell.setType(TerrainType.GRASS, null, -1);
        assertEquals(12, cell.getTankSpeed(new FakeTank(40, false, cell)));
    }

    @Test
    void tankSpeed16ForBoatOnWater() {
        cell.setType(TerrainType.RIVER, null, -1);
        assertEquals(16, cell.getTankSpeed(new FakeTank(40, true, cell)));
    }

    @Test
    void tankSpeedZeroForBuilding() {
        cell.setType(TerrainType.BUILDING, null, -1);
        assertEquals(0, cell.getTankSpeed(new FakeTank(40, false, cell)));
    }

    // ── getTankTurn ───────────────────────────────────────────────────────

    @Test
    void tankTurnZeroForPillbox() {
        cell.setType(TerrainType.GRASS, null, -1);
        cell.setPill(fakePill(10));
        assertEquals(0.00, cell.getTankTurn(new FakeTank(40, false, cell)));
    }

    @Test
    void tankTurnForGrass() {
        cell.setType(TerrainType.GRASS, null, -1);
        assertEquals(1.00, cell.getTankTurn(new FakeTank(40, false, cell)));
    }

    @Test
    void tankTurn1ForBoatOnWater() {
        cell.setType(TerrainType.RIVER, null, -1);
        assertEquals(1.00, cell.getTankTurn(new FakeTank(40, true, cell)));
    }

    // ── getManSpeed ───────────────────────────────────────────────────────

    @Test
    void manSpeedZeroForPillbox() {
        cell.setType(TerrainType.GRASS, null, -1);
        cell.setPill(fakePill(10));
        var man = (WorldMapCell.ManLike) () -> new FakeTank(40, false, cell);
        assertEquals(0, cell.getManSpeed(man));
    }

    @Test
    void manSpeedForGrass() {
        cell.setType(TerrainType.GRASS, null, -1);
        var man = (WorldMapCell.ManLike) () -> new FakeTank(40, false, cell);
        assertEquals(16, cell.getManSpeed(man));
    }

    @Test
    void manSpeedZeroForWater() {
        cell.setType(TerrainType.RIVER, null, -1);
        var man = (WorldMapCell.ManLike) () -> new FakeTank(40, false, cell);
        assertEquals(0, cell.getManSpeed(man));
    }

    // ── takeShellHit ──────────────────────────────────────────────────────

    @Test
    void damagesGrass() {
        cell.setType(TerrainType.GRASS, null, -1);
        int lifeBefore = cell.life();
        cell.takeShellHit(0);
        assertEquals(lifeBefore - 1, cell.life());
    }

    @Test
    void convertsGrassToSwampAfterDamage() {
        cell.setType(TerrainType.GRASS, null, -1);
        for (int i = 0; i < 5; i++) cell.takeShellHit(0);
        assertEquals(TerrainType.SWAMP, cell.type());
    }

    @Test
    void convertsForestToGrass() {
        cell.setType(TerrainType.FOREST, null, -1);
        SoundEffect sfx = cell.takeShellHit(0);
        assertEquals(TerrainType.GRASS, cell.type());
        assertEquals(SoundEffect.SHOT_TREE, sfx);
    }

    @Test
    void convertsBuildingToShotBuilding() {
        cell.setType(TerrainType.BUILDING, null, -1);
        SoundEffect sfx = cell.takeShellHit(0);
        assertEquals(TerrainType.SHOT_BUILDING, cell.type());
        assertEquals(SoundEffect.SHOT_BUILDING, sfx);
    }

    @Test
    void convertsBoatToRiver() {
        cell.setType(TerrainType.BOAT, null, -1);
        cell.takeShellHit(0);
        assertEquals(TerrainType.RIVER, cell.type());
    }

    @Test
    void convertsShotBuildingToRubble() {
        cell.setType(TerrainType.SHOT_BUILDING, null, -1);
        for (int i = 0; i < 5; i++) cell.takeShellHit(0);
        assertEquals(TerrainType.RUBBLE, cell.type());
    }

    // ── takeExplosionHit ──────────────────────────────────────────────────

    @Test
    void explosionConvertsBoatToRiver() {
        cell.setType(TerrainType.BOAT, null, -1);
        cell.takeExplosionHit();
        assertEquals(TerrainType.RIVER, cell.type());
    }

    @Test
    void explosionConvertsTerrainToCrater() {
        cell.setType(TerrainType.GRASS, null, -1);
        cell.takeExplosionHit();
        assertEquals(TerrainType.CRATER, cell.type());
    }

    @Test
    void explosionDoesNotAffectWater() {
        cell.setType(TerrainType.RIVER, null, -1);
        cell.takeExplosionHit();
        assertEquals(TerrainType.RIVER, cell.type());
    }

    @Test
    void explosionDoesNotAffectDeepSea() {
        cell.setType(TerrainType.DEEP_SEA, null, -1);
        cell.takeExplosionHit();
        assertEquals(TerrainType.DEEP_SEA, cell.type());
    }

    @Test
    void explosionDelegatesToPillbox() {
        boolean[] called = {false};
        cell.setPill(new WorldMapCell.PillLike() {
            public int armour() { return 10; }
            public void takeExplosionHit() { called[0] = true; }
        });
        cell.takeExplosionHit();
        assertTrue(called[0]);
    }

    // ── hasTankOnBoat ─────────────────────────────────────────────────────

    @Test
    void hasTankOnBoatReturnsFalseWhenEmpty() {
        assertFalse(cell.hasTankOnBoat(java.util.List.of()));
    }

    @Test
    void hasTankOnBoatReturnsTrueWhenTankOnBoatAtCell() {
        var tank = new FakeTank(40, true, cell);
        assertTrue(cell.hasTankOnBoat(java.util.List.of(tank)));
    }

    @Test
    void hasTankOnBoatReturnsFalseWhenTankNotOnBoat() {
        var tank = new FakeTank(40, false, cell);
        assertFalse(cell.hasTankOnBoat(java.util.List.of(tank)));
    }

    @Test
    void hasTankOnBoatReturnsFalseForDeadTank() {
        var tank = new FakeTank(255, true, cell);
        assertFalse(cell.hasTankOnBoat(java.util.List.of(tank)));
    }
}

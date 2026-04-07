package org.rawaki.core.map;

import org.rawaki.core.Constants;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.TerrainType;

public class WorldMapCell extends MapCell {

    private int life;

    // Back-reference to world for mapChanged/spawn notifications.
    // Using Object to avoid circular dependency with world package — cast at call site.
    public interface WorldRef {
        void mapChanged(WorldMapCell cell, TerrainType oldType, boolean hadMine, int oldLife);
        void spawnFloodFill(WorldMapCell cell);
        java.util.List<?> tanks();
    }

    public WorldMapCell(GameMap map, int x, int y) {
        super(map, x, y);
        this.life = 0;
    }

    public int life() { return life; }

    public boolean isObstacle() {
        if (pill() != null) {
            if (pill() instanceof PillLike p && p.armour() > 0) return true;
        }
        return type().tankSpeed() == 0;
    }

    public boolean hasTankOnBoat(java.util.List<? extends TankLike> tanks) {
        for (TankLike tank : tanks) {
            if (tank.armour() != 255 && tank.cell() == this && tank.onBoat()) return true;
        }
        return false;
    }

    public int getTankSpeed(TankLike tank) {
        if (pill() instanceof PillLike p && p.armour() > 0) return 0;
        if (base() instanceof BaseLike b && b.hasOwner()) {
            if (!b.isOwnerAlly(tank) && b.armour() > 9) return 0;
        }
        if (tank.onBoat() && isType('^', ' ')) return 16;
        return type().tankSpeed();
    }

    public double getTankTurn(TankLike tank) {
        if (pill() instanceof PillLike p && p.armour() > 0) return 0.00;
        if (base() instanceof BaseLike b && b.hasOwner()) {
            if (!b.isOwnerAlly(tank) && b.armour() > 9) return 0.00;
        }
        if (tank.onBoat() && isType('^', ' ')) return 1.00;
        return type().tankTurn();
    }

    public int getManSpeed(ManLike man) {
        if (pill() instanceof PillLike p && p.armour() > 0) return 0;
        if (base() instanceof BaseLike b && b.hasOwner()) {
            if (!b.isOwnerAlly(man.ownerTank()) && b.armour() > 9) return 0;
        }
        return type().manSpeed();
    }

    public double[] getPixelCoordinates() {
        return new double[]{
            (x() + 0.5) * Constants.TILE_SIZE_PIXELS,
            (y() + 0.5) * Constants.TILE_SIZE_PIXELS
        };
    }

    public double[] getWorldCoordinates() {
        return new double[]{
            (x() + 0.5) * Constants.TILE_SIZE_WORLD,
            (y() + 0.5) * Constants.TILE_SIZE_WORLD
        };
    }

    @Override
    public void setType(TerrainType newType, Boolean mine, int retileRadius) {
        TerrainType oldType = type();
        boolean hadMine = mine();
        int oldLife = this.life;
        super.setType(newType, mine, retileRadius);
        this.life = switch (type()) {
            case GRASS, SHOT_BUILDING, RUBBLE -> 5;
            case SWAMP -> 4;
            default -> 0;
        };
    }

    public SoundEffect takeShellHit(int shellDirection) {
        SoundEffect sfx = SoundEffect.SHOT_BUILDING;
        if (isType('.', '}', ':', '~')) {
            if (--life == 0) {
                TerrainType next = switch (type()) {
                    case GRASS -> TerrainType.SWAMP;
                    case SHOT_BUILDING -> TerrainType.RUBBLE;
                    case RUBBLE, SWAMP -> TerrainType.RIVER;
                    default -> null;
                };
                if (next != null) setType(next);
            }
        } else if (isType('#')) {
            setType(TerrainType.GRASS);
            sfx = SoundEffect.SHOT_TREE;
        } else if (isType('=')) {
            MapCell neigh;
            if (shellDirection >= 224 || shellDirection < 32) neigh = neigh(1, 0);
            else if (shellDirection >= 32 && shellDirection < 96) neigh = neigh(0, -1);
            else if (shellDirection >= 96 && shellDirection < 160) neigh = neigh(-1, 0);
            else neigh = neigh(0, 1);
            if (neigh.isType(' ', '^')) setType(TerrainType.RIVER);
        } else {
            TerrainType next = switch (type()) {
                case BUILDING -> TerrainType.SHOT_BUILDING;
                case BOAT -> TerrainType.RIVER;
                default -> null;
            };
            if (next != null) setType(next);
        }
        return sfx;
    }

    public void takeExplosionHit() {
        if (pill() instanceof PillLike p) {
            p.takeExplosionHit();
            return;
        }
        if (isType('b')) setType(TerrainType.RIVER);
        else if (!isType(' ', '^', 'b')) setType(TerrainType.CRATER);
        else return;
        // FloodFill spawn would be triggered here via WorldRef
    }

    // ── Interfaces for game objects (avoids circular dependencies) ─────

    public interface PillLike {
        int armour();
        void takeExplosionHit();
        default int repair(int trees) { return 0; }
    }

    public interface BaseLike {
        boolean hasOwner();
        boolean isOwnerAlly(Object tank);
        int armour();
    }

    public interface TankLike {
        int armour();
        boolean onBoat();
        WorldMapCell cell();
    }

    public interface ManLike {
        TankLike ownerTank();
    }
}

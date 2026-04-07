package org.rawaki.core.objects;

import org.rawaki.core.SoundEffect;
import org.rawaki.core.TerrainType;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

import static java.lang.Math.*;

public class Fireball extends BoloObject {

    private int direction;
    private boolean largeExplosion;
    private int lifespan;
    private Integer dx, dy;

    public Fireball(World world) {
        super(world);
        styled = null; // not drawn directly
    }

    @Override
    public void spawn(Object... args) {
        if (args.length >= 4) {
            x = (Integer) args[0];
            y = (Integer) args[1];
            direction = (Integer) args[2];
            largeExplosion = (Boolean) args[3];
        }
        lifespan = 80;
    }

    public int getDirection16th() {
        return ((int) round((direction - 1) / 16.0)) % 16;
    }

    @Override
    public void update() {
        if (lifespan-- % 2 == 0) {
            if (wreck()) return;
            move();
        }
        if (lifespan == 0) {
            explode();
            world().destroy(this);
        }
    }

    private boolean wreck() {
        if (x == null || y == null) return true;
        // Explosion spawn deferred
        var cell = (WorldMapCell) world().map().cellAtWorld(x, y);
        if (cell.isType('^')) {
            world().destroy(this);
            soundEffect(SoundEffect.TANK_SINKING);
            return true;
        } else if (cell.isType('b')) {
            cell.setType(TerrainType.RIVER);
            soundEffect(SoundEffect.SHOT_BUILDING);
        } else if (cell.isType('#')) {
            cell.setType(TerrainType.GRASS);
            soundEffect(SoundEffect.SHOT_TREE);
        }
        return false;
    }

    private void move() {
        if (x == null || y == null) return;
        if (dx == null) {
            double radians = (256 - direction) * 2 * PI / 256;
            dx = (int) round(cos(radians) * 48);
            dy = (int) round(sin(radians) * 48);
        }

        int newx = x + dx, newy = y + dy;

        if (dx != 0) {
            int aheadX = dx > 0 ? newx + 24 : newx - 24;
            var ahead = (WorldMapCell) world().map().cellAtWorld(aheadX, newy);
            if (!ahead.isObstacle()) x = newx;
        }
        if (dy != 0) {
            int aheadY = dy > 0 ? newy + 24 : newy - 24;
            var ahead = (WorldMapCell) world().map().cellAtWorld(x, aheadY);
            if (!ahead.isObstacle()) y = newy;
        }
    }

    private void explode() {
        if (x == null || y == null) return;
        var cell = (WorldMapCell) world().map().cellAtWorld(x, y);
        cell.takeExplosionHit();
        // Explosion spawn and builder kill deferred

        if (largeExplosion && dx != null && dy != null) {
            int ndx = dx > 0 ? 1 : -1;
            int ndy = dy > 0 ? 1 : -1;
            cell.neigh(ndx, 0).retile(); // placeholder — should call takeExplosionHit
            cell.neigh(0, ndy).retile();
            cell.neigh(ndx, ndy).retile();
            soundEffect(SoundEffect.BIG_EXPLOSION);
        } else {
            soundEffect(SoundEffect.MINE_EXPLOSION);
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public int direction()          { return direction; }
    public boolean largeExplosion() { return largeExplosion; }
    public int lifespan()           { return lifespan; }

    public void setDirection(int d)          { this.direction = d; }
    public void setLargeExplosion(boolean v) { this.largeExplosion = v; }
    public void setLifespan(int l)           { this.lifespan = l; }
}

package org.rawaki.core.objects;

import org.rawaki.core.SoundEffect;
import org.rawaki.core.map.MapCell;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

public class MineExplosion extends BoloObject {

    private int lifespan;
    private WorldMapCell cell;

    public MineExplosion(World world) {
        super(world);
        styled = null;
    }

    @Override
    public void spawn(Object... args) {
        if (args.length > 0 && args[0] instanceof WorldMapCell c) {
            double[] coords = c.getWorldCoordinates();
            x = (int) coords[0];
            y = (int) coords[1];
        }
        lifespan = 10;
    }

    @Override
    public void anySpawn() {
        if (x != null && y != null) {
            cell = (WorldMapCell) world().map().cellAtWorld(x, y);
        }
    }

    @Override
    public void update() {
        if (lifespan-- == 0) {
            if (cell != null && cell.mine()) {
                asplode();
            }
            world().destroy(this);
        }
    }

    private void asplode() {
        // Clear the mine
        cell.setType(cell.type(), false, 0);

        // Terrain damage
        cell.takeExplosionHit();

        // Tank/builder damage deferred — requires typed tank list
        // Explosion spawn deferred

        soundEffect(SoundEffect.MINE_EXPLOSION);
        spread();
    }

    private void spread() {
        if (cell == null) return;
        // Spawn MineExplosion on non-edge neighbours
        // Deferred — requires world.spawn integration
        MapCell[] neighbours = {
            cell.neigh(1, 0), cell.neigh(0, 1),
            cell.neigh(-1, 0), cell.neigh(0, -1)
        };
        for (MapCell n : neighbours) {
            if (!n.isEdgeCell()) {
                // world().spawn(MineExplosion.class, n) — deferred
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public int lifespan()           { return lifespan; }
    public WorldMapCell cell()      { return cell; }

    public void setLifespan(int l)  { this.lifespan = l; }
}

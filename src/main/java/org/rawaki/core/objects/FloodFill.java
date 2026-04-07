package org.rawaki.core.objects;

import org.rawaki.core.TerrainType;
import org.rawaki.core.map.MapCell;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

public class FloodFill extends BoloObject {

    private int lifespan;
    private WorldMapCell cell;
    private MapCell[] neighbours;

    public FloodFill(World world) {
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
        lifespan = 16;
    }

    @Override
    public void anySpawn() {
        if (x != null && y != null) {
            cell = (WorldMapCell) world().map().cellAtWorld(x, y);
            neighbours = new MapCell[]{
                cell.neigh(1, 0), cell.neigh(0, 1),
                cell.neigh(-1, 0), cell.neigh(0, -1)
            };
        }
    }

    @Override
    public void update() {
        if (lifespan-- == 0) {
            flood();
            world().destroy(this);
        }
    }

    public boolean canGetWet() {
        if (neighbours == null) return false;
        for (MapCell n : neighbours) {
            if (n.base() == null && n.pill() == null && n.isType(' ', '^', 'b')) {
                return true;
            }
        }
        return false;
    }

    private void flood() {
        if (canGetWet()) {
            cell.setType(TerrainType.RIVER, false, 1);
            spread();
        }
    }

    private void spread() {
        if (neighbours == null) return;
        for (MapCell n : neighbours) {
            if (n.base() == null && n.pill() == null && n.isType('%')) {
                // Spawn another FloodFill on the crater neighbour
                // Deferred — requires world.spawn integration
            }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public int lifespan()           { return lifespan; }
    public WorldMapCell cell()      { return cell; }
    public MapCell[] neighbours()   { return neighbours; }

    public void setLifespan(int l)  { this.lifespan = l; }
}

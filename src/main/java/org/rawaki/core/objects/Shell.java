package org.rawaki.core.objects;

import org.rawaki.core.Constants;
import org.rawaki.core.Helpers;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

import java.util.List;

import static java.lang.Math.*;

public class Shell extends BoloObject {

    private int direction;
    private int lifespan;
    private boolean onWater;
    private WorldMapCell cell;
    private Double radians;
    private Ref<BoloObject> owner;
    private Ref<BoloObject> attribution;

    public Shell(World world) {
        super(world);
        setUpdatePriority(20);
        styled = false;
        owner = ref(null);
        attribution = ref(null);
    }

    // ── Cell management ───────────────────────────────────────────────────

    public void updateCell() {
        if (x != null && y != null) {
            cell = (WorldMapCell) world().map().cellAtWorld(x, y);
        }
    }

    // ── Direction ─────────────────────────────────────────────────────────

    public int getDirection16th() {
        return ((int) Math.round((direction - 1) / 16.0)) % 16;
    }

    @Override
    public int[] getTile() {
        return new int[]{getDirection16th(), 4};
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public void initShell(BoloObject shellOwner, int direction, double range, boolean onWater) {
        owner.set(shellOwner);
        attribution.set(shellOwner); // simplified — full version checks owner_idx
        this.direction = direction;
        this.lifespan = (int) (range * Constants.TILE_SIZE_WORLD / 32 - 2);
        this.onWater = onWater;
        this.x = shellOwner.x();
        this.y = shellOwner.y();
        move();
    }

    @Override
    public void update() {
        move();
        String[] collision = collide();
        if (collision != null) {
            asplode();
        } else if (lifespan-- == 0) {
            asplode();
        }
    }

    // ── Movement ──────────────────────────────────────────────────────────

    public void move() {
        if (x == null || y == null) return;
        if (radians == null) {
            radians = (256 - direction) * 2 * PI / 256;
        }
        x = x + (int) round(cos(radians) * 32);
        y = y + (int) round(sin(radians) * 32);
        updateCell();
    }

    // ── Collision detection ───────────────────────────────────────────────

    public String[] collide() {
        if (cell == null) return null;

        // Check pillbox collision (not our owner)
        if (cell.pill() instanceof WorldMapCell.PillLike pill) {
            if (pill.armour() > 0 && cell.pill() != (owner.isPresent() ? owner.get() : null)) {
                double[] coords = cell.getWorldCoordinates();
                if (Helpers.distance(x, y, coords[0], coords[1]) <= 127) {
                    return new String[]{"cell", "pill"};
                }
            }
        }

        // Check tank collision (not our owner)
        // Deferred — requires typed tank list

        // Check terrain collision
        boolean terrainCollision;
        if (onWater) {
            terrainCollision = !cell.isType('^', ' ', '%');
        } else {
            terrainCollision = cell.isType('|', '}', '#', 'b');
        }
        if (terrainCollision) return new String[]{"cell", "terrain"};

        return null;
    }

    // ── Explosion ─────────────────────────────────────────────────────────

    public void asplode() {
        // Builder kill logic deferred — requires typed tank/builder list
        // Explosion/MineExplosion spawning deferred
        world().destroy(this);
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public int direction()              { return direction; }
    public int lifespan()               { return lifespan; }
    public boolean onWater()            { return onWater; }
    public WorldMapCell cell()          { return cell; }
    public Ref<BoloObject> owner()      { return owner; }
    public Ref<BoloObject> attribution(){ return attribution; }

    public void setDirection(int d)     { this.direction = d; }
    public void setLifespan(int l)      { this.lifespan = l; }
    public void setOnWater(boolean v)   { this.onWater = v; }
    public void setCell(WorldMapCell c) { this.cell = c; }
}

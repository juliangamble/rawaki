package org.rawaki.core.objects;

import org.rawaki.core.Constants;
import org.rawaki.core.Helpers;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

public class WorldPillbox extends BoloObject implements WorldMapCell.PillLike {

    private int ownerIdx = 255;
    private int armour;
    private int speed;
    private int coolDown;
    private int reload;
    private boolean inTank;
    private boolean carried;
    private boolean haveTarget;
    private WorldMapCell cell;
    private Ref<BoloObject> owner;

    public WorldPillbox(World world) {
        super(world);
        this.owner = ref(null);
    }

    // Map-object constructor form (called during map load)
    public void initFromMap(int tileX, int tileY, int ownerIdx, int armour, int speed) {
        this.ownerIdx = ownerIdx;
        this.armour = armour;
        this.speed = speed;
        this.x = (int) ((tileX + 0.5) * Constants.TILE_SIZE_WORLD);
        this.y = (int) ((tileY + 0.5) * Constants.TILE_SIZE_WORLD);
    }

    // ── Cell management ───────────────────────────────────────────────────

    public void updateCell() {
        if (cell != null) {
            cell.setPill(null);
            cell.retile();
        }
        if (inTank || carried) {
            cell = null;
        } else if (x != null && y != null) {
            cell = (WorldMapCell) world().map().cellAtWorld(x, y);
            cell.setPill(this);
            cell.retile();
        }
    }

    public void updateOwner() {
        if (owner.isPresent()) {
            ownerIdx = owner.get().idx();
            team = owner.get().team();
        } else {
            ownerIdx = 255;
            team = 255;
        }
        if (cell != null) cell.retile();
    }

    public void placeAt(WorldMapCell targetCell) {
        inTank = false;
        carried = false;
        double[] coords = targetCell.getWorldCoordinates();
        x = (int) coords[0];
        y = (int) coords[1];
        updateCell();
        reset();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void spawn(Object... args) {
        reset();
    }

    public void reset() {
        coolDown = 32;
        reload = 0;
    }

    @Override
    public void anySpawn() {
        updateCell();
    }

    @Override
    public void update() {
        if (inTank || carried) return;

        if (armour == 0) {
            haveTarget = false;
            // Look for a tank to pick us up
            // (Requires typed tank list — deferred to integration with Tank class)
            return;
        }

        reload = Math.min(speed, reload + 1);
        if (--coolDown == 0) {
            coolDown = 32;
            speed = Math.min(100, speed + 1);
        }
        if (reload < speed) return;

        // Targeting logic deferred to integration with Tank/Shell classes
        // For now, just reset reload when ready
    }

    // ── Combat ────────────────────────────────────────────────────────────

    public void aggravate() {
        coolDown = 32;
        speed = Math.max(6, Math.round(speed / 2.0f));
    }

    @Override
    public int armour() { return armour; }

    @Override
    public void takeExplosionHit() {
        armour = Math.max(0, armour - 5);
        if (cell != null) cell.retile();
    }

    public SoundEffect takeShellHit() {
        aggravate();
        armour = Math.max(0, armour - 1);
        if (cell != null) cell.retile();
        return SoundEffect.SHOT_BUILDING;
    }

    public int repair(int trees) {
        int used = Math.min(trees, (int) Math.ceil((15 - armour) / 4.0));
        armour = Math.min(15, armour + used * 4);
        if (cell != null) cell.retile();
        return used;
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public int ownerIdx()         { return ownerIdx; }
    public int speed()            { return speed; }
    public int coolDown()         { return coolDown; }
    public int reload()           { return reload; }
    public boolean inTank()       { return inTank; }
    public boolean carried()      { return carried; }
    public boolean haveTarget()   { return haveTarget; }
    public WorldMapCell cell()    { return cell; }
    public Ref<BoloObject> owner() { return owner; }

    public void setArmour(int armour)     { this.armour = armour; }
    public void setSpeed(int speed)       { this.speed = speed; }
    public void setCoolDown(int coolDown) { this.coolDown = coolDown; }
    public void setReload(int reload)     { this.reload = reload; }
    public void setInTank(boolean v)      { this.inTank = v; }
    public void setCarried(boolean v)     { this.carried = v; }
    public void setHaveTarget(boolean v)  { this.haveTarget = v; }
    public void setCell(WorldMapCell c)   { this.cell = c; }
}

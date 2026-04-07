package org.rawaki.core.objects;

import org.rawaki.core.Constants;
import org.rawaki.core.Helpers;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.TerrainType;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

public class WorldBase extends BoloObject implements WorldMapCell.BaseLike {

    private int ownerIdx = 255;
    private int armour;
    private int shells;
    private int mines;
    private int refuelCounter;
    private WorldMapCell cell;
    private Ref<BoloObject> owner;
    private Ref<BoloObject> refueling;

    public WorldBase(World world) {
        super(world);
        this.owner = ref(null);
        this.refueling = ref(null);
    }

    // Map-object constructor form (called during map load)
    public void initFromMap(int tileX, int tileY, int ownerIdx, int armour, int shells, int mines) {
        this.ownerIdx = ownerIdx;
        this.armour = armour;
        this.shells = shells;
        this.mines = mines;
        this.x = (int) ((tileX + 0.5) * Constants.TILE_SIZE_WORLD);
        this.y = (int) ((tileY + 0.5) * Constants.TILE_SIZE_WORLD);
    }

    // ── Owner management ──────────────────────────────────────────────────

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

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void anySpawn() {
        if (x != null && y != null) {
            cell = (WorldMapCell) world().map().cellAtWorld(x, y);
            cell.setBase(this);
        }
    }

    @Override
    public void update() {
        // Stop refueling if tank left or died
        if (refueling.isPresent()) {
            var tank = refueling.get();
            if (!(tank instanceof RefuelTarget rt) || rt.cell() != cell || rt.armour() == 255) {
                refueling.clear();
            }
        }

        if (!refueling.isPresent()) {
            findSubject();
            return;
        }

        if (--refuelCounter != 0) return;

        // Transfer resources
        var tank = (RefuelTarget) refueling.get();
        if (armour > 0 && tank.armour() < 40) {
            int amount = Math.min(5, Math.min(armour, 40 - tank.armour()));
            tank.addArmour(amount);
            armour -= amount;
            refuelCounter = 46;
        } else if (shells > 0 && tank.shells() < 40) {
            tank.addShells(1);
            shells -= 1;
            refuelCounter = 7;
        } else if (mines > 0 && tank.mines() < 40) {
            tank.addMines(1);
            mines -= 1;
            refuelCounter = 7;
        } else {
            refuelCounter = 1;
        }
    }

    // ── Combat ────────────────────────────────────────────────────────────

    public SoundEffect takeShellHit() {
        // Aggravate nearby allied pillboxes
        if (owner.isPresent() && owner.get() instanceof Tank ownerTank) {
            for (var pill : world().map().pills()) {
                if (pill.cell() != null) {
                    var wp = (WorldPillbox) world().map().cellAtTile(pill.x(), pill.y()).pill();
                    // Simplified — full aggravation needs WorldPillbox typed access from map.pills
                }
            }
        }
        armour = Math.max(0, armour - 5);
        return SoundEffect.SHOT_BUILDING;
    }

    private void findSubject() {
        java.util.List<Tank> tanksOnCell = new java.util.ArrayList<>();
        for (BoloObject obj : world().tanks()) {
            if (obj instanceof Tank tank && tank.armour() != 255 && tank.cell() == cell) {
                tanksOnCell.add(tank);
            }
        }
        for (Tank tank : tanksOnCell) {
            if (owner.isPresent() && owner.get() instanceof Tank ownerTank && ownerTank.isAlly(tank)) {
                refueling.set(tank);
                refuelCounter = 46;
                break;
            } else {
                boolean canClaim = true;
                for (Tank other : tanksOnCell) {
                    if (other != tank && !tank.isAlly(other)) { canClaim = false; break; }
                }
                if (canClaim) {
                    owner.set(tank); updateOwner();
                    tank.on("destroy", () -> { owner.clear(); updateOwner(); });
                    refueling.set(tank);
                    refuelCounter = 46;
                    break;
                }
            }
        }
    }

    // ── BaseLike interface ────────────────────────────────────────────────

    @Override
    public boolean hasOwner() { return owner.isPresent(); }

    @Override
    public boolean isOwnerAlly(Object tank) {
        // Simplified — full implementation needs typed tank comparison
        return false;
    }

    @Override
    public int armour() { return armour; }

    // ── Refueling interface for tanks ─────────────────────────────────────

    public interface RefuelTarget {
        WorldMapCell cell();
        int armour();
        int shells();
        int mines();
        void addArmour(int amount);
        void addShells(int amount);
        void addMines(int amount);
    }

    public void startRefueling(BoloObject tank, int counter) {
        refueling.set(tank);
        refuelCounter = counter;
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public int ownerIdx()            { return ownerIdx; }
    public int shells()              { return shells; }
    public int mines()               { return mines; }
    public int refuelCounter()       { return refuelCounter; }
    public WorldMapCell cell()       { return cell; }
    public Ref<BoloObject> owner()   { return owner; }
    public Ref<BoloObject> refueling() { return refueling; }

    public void setArmour(int armour)     { this.armour = armour; }
    public void setShells(int shells)     { this.shells = shells; }
    public void setMines(int mines)       { this.mines = mines; }
    public void setRefuelCounter(int c)   { this.refuelCounter = c; }
    public void setCell(WorldMapCell c)   { this.cell = c; }
}

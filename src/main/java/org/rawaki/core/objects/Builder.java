package org.rawaki.core.objects;

import org.rawaki.core.Helpers;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.TerrainType;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

import static java.lang.Math.*;

public class Builder extends BoloObject implements WorldMapCell.ManLike {

    // State constants
    public static final int IN_TANK      = 0;
    public static final int WAITING      = 1;
    public static final int RETURNING    = 2;
    public static final int PARACHUTING  = 3;

    // Action constants
    public static final int ACTION_FOREST   = 10;
    public static final int ACTION_ROAD     = 11;
    public static final int ACTION_REPAIR   = 12;
    public static final int ACTION_BOAT     = 13;
    public static final int ACTION_BUILDING = 14;
    public static final int ACTION_PILLBOX  = 15;
    public static final int ACTION_MINE     = 16;

    private int order;
    private int trees;
    private boolean hasMine;
    private int targetX, targetY;
    private int waitTimer;
    private int animation;
    private WorldMapCell cell;
    private Ref<BoloObject> owner;
    private Ref<BoloObject> pillbox;

    public Builder(World world) {
        super(world);
        styled = true;
        owner = ref(null);
        pillbox = ref(null);
    }

    // ── Cell management ───────────────────────────────────────────────────

    public void updateCell() {
        cell = (x != null && y != null)
            ? (WorldMapCell) world().map().cellAtWorld(x, y)
            : null;
    }

    // ── Tile ──────────────────────────────────────────────────────────────

    @Override
    public int[] getTile() {
        if (order == PARACHUTING) return new int[]{16, 1};
        return new int[]{17, (int) floor(animation / 3.0)};
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void spawn(Object... args) {
        if (args.length > 0 && args[0] instanceof BoloObject o) {
            owner.set(o);
        }
        order = IN_TANK;
    }

    @Override
    public void anySpawn() {
        if (owner.isPresent()) team = owner.get().team();
        animation = 0;
    }

    @Override
    public void update() {
        if (order == IN_TANK) return;
        animation = (animation + 1) % 9;

        switch (order) {
            case WAITING -> {
                if (waitTimer-- == 0) order = RETURNING;
            }
            case PARACHUTING -> {
                parachutingIn();
            }
            case RETURNING -> {
                if (owner.isPresent() && owner.get() instanceof Tank ownerTank && ownerTank.armour() != 255) {
                    moveToward(ownerTank.x(), ownerTank.y(), 128);
                }
            }
            default -> {
                moveToward(targetX, targetY, 16);
            }
        }
    }

    // ── Build reached ─────────────────────────────────────────────────────

    public void reached() {
        if (order == RETURNING) {
            order = IN_TANK;
            x = null; y = null;
            // Return pillbox, trees, mine to owner
            if (owner.isPresent() && owner.get() instanceof Tank ownerTank) {
                ownerTank.setTrees(Math.min(40, ownerTank.trees() + trees));
                if (hasMine) ownerTank.setMines(Math.min(40, ownerTank.mines() + 1));
            }
            trees = 0;
            hasMine = false;
            return;
        }

        // Mine check
        if (cell != null && cell.mine()) {
            // MineExplosion spawn deferred
            order = WAITING;
            waitTimer = 20;
            return;
        }

        // Build actions
        switch (order) {
            case ACTION_FOREST -> {
                if (cell.base() != null || cell.pill() != null || !cell.isType('#')) break;
                cell.setType(TerrainType.GRASS);
                trees = 4;
                soundEffect(SoundEffect.FARMING_TREE);
            }
            case ACTION_ROAD -> {
                if (cell.base() != null || cell.pill() != null ||
                    cell.isType('|', '}', 'b', '^', '#', '=')) break;
                cell.setType(TerrainType.ROAD);
                trees = 0;
                soundEffect(SoundEffect.MAN_BUILDING);
            }
            case ACTION_REPAIR -> {
                if (cell.pill() instanceof WorldMapCell.PillLike pill) {
                    int used = pill.repair(trees);
                    trees -= used;
                } else if (cell.isType('}')) {
                    cell.setType(TerrainType.BUILDING);
                    trees = 0;
                } else break;
                soundEffect(SoundEffect.MAN_BUILDING);
            }
            case ACTION_BOAT -> {
                if (!cell.isType(' ')) break;
                cell.setType(TerrainType.BOAT);
                trees = 0;
                soundEffect(SoundEffect.MAN_BUILDING);
            }
            case ACTION_BUILDING -> {
                if (cell.base() != null || cell.pill() != null ||
                    cell.isType('b', '^', '#', '}', '|', ' ')) break;
                cell.setType(TerrainType.BUILDING);
                trees = 0;
                soundEffect(SoundEffect.MAN_BUILDING);
            }
            case ACTION_PILLBOX -> {
                if (cell.pill() != null || cell.base() != null ||
                    cell.isType('b', '^', '#', '|', '}', ' ')) break;
                if (pillbox.isPresent() && pillbox.get() instanceof WorldPillbox wp) {
                    wp.setArmour(15);
                    wp.placeAt(cell);
                    trees = 0;
                    pillbox.clear();
                }
                soundEffect(SoundEffect.MAN_BUILDING);
            }
            case ACTION_MINE -> {
                if (cell.base() != null || cell.pill() != null ||
                    cell.isType('^', ' ', '|', 'b', '}')) break;
                cell.setType(cell.type(), true, 0);
                hasMine = false;
                soundEffect(SoundEffect.MAN_LAY_MINE);
            }
        }

        order = WAITING;
        waitTimer = 20;
    }

    // ── Movement ──────────────────────────────────────────────────────────

    private void moveToward(int tx, int ty, int targetRadius) {
        if (x == null || y == null || cell == null) return;
        double dist = Helpers.distance(x, y, tx, ty);
        if (dist <= targetRadius) {
            reached();
            return;
        }
        double rad = Helpers.heading(x, y, tx, ty);
        int speed = cell.getManSpeed(this);
        if (speed == 0) { order = RETURNING; return; }
        speed = (int) Math.min(speed, dist);
        x = x + (int) Math.round(Math.cos(rad) * Math.ceil(speed));
        y = y + (int) Math.round(Math.sin(rad) * Math.ceil(speed));
        updateCell();
    }

    // ── Kill ──────────────────────────────────────────────────────────────

    public void kill() {
        if (!world().authority()) return;
        soundEffect(SoundEffect.MAN_DYING);
        order = PARACHUTING;
        trees = 0;
        hasMine = false;
        if (pillbox.isPresent() && pillbox.get() instanceof WorldPillbox wp && cell != null) {
            wp.placeAt(cell);
            pillbox.clear();
        }
        if (owner.isPresent() && owner.get().x() != null) {
            targetX = owner.get().x();
            targetY = owner.get().y();
        } else if (x != null && y != null) {
            targetX = x;
            targetY = y;
        }
        var start = world().map().getRandomStart();
        if (start != null) {
            double[] coords = ((WorldMapCell) start.cell()).getWorldCoordinates();
            x = (int) coords[0];
            y = (int) coords[1];
        }
    }

    // ── Parachuting ───────────────────────────────────────────────────────

    private void parachutingIn() {
        if (x == null || y == null) return;
        double dist = Helpers.distance(x, y, targetX, targetY);
        if (dist <= 16) {
            order = RETURNING;
        } else {
            double rad = Helpers.heading(x, y, targetX, targetY);
            x = x + (int) round(cos(rad) * 3);
            y = y + (int) round(sin(rad) * 3);
            updateCell();
        }
    }

    // ── ManLike interface ─────────────────────────────────────────────────

    @Override
    public WorldMapCell.TankLike ownerTank() {
        if (owner.isPresent() && owner.get() instanceof WorldMapCell.TankLike t) return t;
        return null;
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public int order()                  { return order; }
    public int trees()                  { return trees; }
    public boolean hasMine()            { return hasMine; }
    public int targetX()                { return targetX; }
    public int targetY()                { return targetY; }
    public int waitTimer()              { return waitTimer; }
    public int animation()              { return animation; }
    public WorldMapCell cell()          { return cell; }
    public Ref<BoloObject> owner()      { return owner; }
    public Ref<BoloObject> pillbox()    { return pillbox; }

    public void setOrder(int o)         { this.order = o; }
    public void setTrees(int t)         { this.trees = t; }
    public void setHasMine(boolean v)   { this.hasMine = v; }
    public void setTargetX(int x)       { this.targetX = x; }
    public void setTargetY(int y)       { this.targetY = y; }
    public void setWaitTimer(int t)     { this.waitTimer = t; }
    public void setAnimation(int a)     { this.animation = a; }
    public void setCell(WorldMapCell c) { this.cell = c; }

    // repair helper for PillLike — delegates to WorldPillbox
    private int repair(int t) { return 0; }
}

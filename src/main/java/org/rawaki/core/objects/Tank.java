package org.rawaki.core.objects;

import org.rawaki.core.Constants;
import org.rawaki.core.Helpers;
import org.rawaki.core.SoundEffect;
import org.rawaki.core.map.WorldMapCell;
import org.rawaki.core.world.World;

import static java.lang.Math.*;

public class Tank extends BoloObject implements WorldMapCell.TankLike, WorldBase.RefuelTarget {

    private WorldMapCell cell;
    private double direction;
    private double speed;
    private int slideTicks;
    private int slideDirection;
    private boolean accelerating;
    private boolean braking;
    private boolean turningClockwise;
    private boolean turningCounterClockwise;
    private int turnSpeedup;
    private int shells;
    private int mines;
    private int armourVal;
    private int trees;
    private int reload;
    private boolean shooting;
    private double firingRange;
    private int waterTimer;
    private boolean onBoat;
    private int respawnTimer;

    private Ref<BoloObject> builder;

    public Tank(World world) {
        super(world);
        styled = true;
        builder = ref(null);
    }

    // ── Cell management ───────────────────────────────────────────────────

    public void updateCell() {
        cell = (x != null && y != null)
            ? (WorldMapCell) world().map().cellAtWorld(x, y)
            : null;
    }

    // ── Reset / spawn ─────────────────────────────────────────────────────

    public void reset() {
        var start = world().map().getRandomStart();
        if (start != null) {
            double[] coords = ((WorldMapCell) start.cell()).getWorldCoordinates();
            x = (int) coords[0];
            y = (int) coords[1];
            direction = start.direction() * 16;
        }
        updateCell();

        speed = 0.00;
        slideTicks = 0;
        slideDirection = 0;
        accelerating = false;
        braking = false;
        turningClockwise = false;
        turningCounterClockwise = false;
        turnSpeedup = 0;

        shells = 40;
        mines = 0;
        armourVal = 40;
        trees = 0;

        reload = 0;
        shooting = false;
        firingRange = 7;

        waterTimer = 0;
        onBoat = true;
    }

    @Override
    public void spawn(Object... args) {
        if (args.length > 0 && args[0] instanceof Integer t) {
            team = t;
        }
        reset();
        // Builder spawning deferred to integration
    }

    @Override
    public void anySpawn() {
        updateCell();
        // Tank registration with world deferred to integration
    }

    @Override
    public void update() {
        if (death()) return;
        shootOrReload();
        turn();
        accelerate();
        fixPosition();
        move();
    }

    // ── Direction ─────────────────────────────────────────────────────────

    public int getDirection16th() {
        return ((int) Math.round((direction - 1) / 16.0)) % 16;
    }

    public int getSlideDirection16th() {
        return ((int) Math.round((slideDirection - 1) / 16.0)) % 16;
    }

    // ── Tile ──────────────────────────────────────────────────────────────

    @Override
    public int[] getTile() {
        int tx = getDirection16th();
        int ty = onBoat ? 1 : 0;
        return new int[]{tx, ty};
    }

    // ── Ally detection ────────────────────────────────────────────────────

    public boolean isAlly(Tank other) {
        return other == this || (team != 255 && other.team == team);
    }

    // ── Range ─────────────────────────────────────────────────────────────

    public void increaseRange() { firingRange = min(7, firingRange + 0.5); }
    public void decreaseRange() { firingRange = max(1, firingRange - 0.5); }

    // ── Combat ────────────────────────────────────────────────────────────

    public SoundEffect takeShellHit(int shellDirection) {
        armourVal -= 5;
        if (armourVal < 0) {
            kill();
        } else {
            slideTicks = 8;
            slideDirection = shellDirection;
            if (onBoat) {
                onBoat = false;
                speed = 0;
                if (cell != null && cell.isType('^')) sink();
            }
        }
        return SoundEffect.HIT_TANK;
    }

    public void takeMineHit() {
        armourVal -= 10;
        if (armourVal < 0) {
            kill();
        } else if (onBoat) {
            onBoat = false;
            speed = 0;
            if (cell != null && cell.isType('^')) sink();
        }
    }

    public void kill() {
        // dropPillboxes() deferred to integration
        x = null;
        y = null;
        armourVal = 255;
        respawnTimer = 255;
    }

    public boolean death() {
        if (armourVal != 255) return false;
        if (world().authority() && --respawnTimer == 0) {
            reset();
            return false;
        }
        return true;
    }

    private void sink() {
        if (x != null && y != null) {
            world().soundEffect(SoundEffect.TANK_SINKING, x, y, this);
        }
        kill();
    }

    // ── Shooting ──────────────────────────────────────────────────────────

    public void shootOrReload() {
        if (reload > 0) reload--;
        if (!shooting || reload != 0 || shells <= 0) return;
        shells--;
        reload = 13;
        // Shell spawning deferred to integration
        soundEffect(SoundEffect.SHOOTING);
    }

    // ── Turning ───────────────────────────────────────────────────────────

    public void turn() {
        if (cell == null) return;
        double maxTurn = cell.getTankTurn(this);

        if (turningClockwise == turningCounterClockwise) {
            turnSpeedup = 0;
            return;
        }

        double acceleration;
        if (turningCounterClockwise) {
            acceleration = maxTurn;
            if (turnSpeedup < 10) acceleration /= 2;
            if (turnSpeedup < 0) turnSpeedup = 0;
            turnSpeedup++;
        } else {
            acceleration = -maxTurn;
            if (turnSpeedup > -10) acceleration /= 2;
            if (turnSpeedup > 0) turnSpeedup = 0;
            turnSpeedup--;
        }

        direction += acceleration;
        while (direction < 0) direction += 256;
        if (direction >= 256) direction %= 256;
    }

    // ── Acceleration ──────────────────────────────────────────────────────

    public void accelerate() {
        if (cell == null) return;
        double maxSpeed = cell.getTankSpeed(this);
        double accel;
        if (speed > maxSpeed) accel = -0.25;
        else if (accelerating == braking) accel = 0.00;
        else if (accelerating) accel = 0.25;
        else accel = -0.25;

        if (accel > 0.00 && speed < maxSpeed) {
            speed = min(maxSpeed, speed + accel);
        } else if (accel < 0.00 && speed > 0.00) {
            speed = max(0.00, speed + accel);
        }
    }

    // ── Position fixing ───────────────────────────────────────────────────

    public void fixPosition() {
        if (cell == null || x == null || y == null) return;
        if (cell.getTankSpeed(this) == 0) {
            int halftile = Constants.TILE_SIZE_WORLD / 2;
            if (x % Constants.TILE_SIZE_WORLD >= halftile) x++; else x--;
            if (y % Constants.TILE_SIZE_WORLD >= halftile) y++; else y--;
            speed = max(0.00, speed - 1);
        }
    }

    // ── Movement ──────────────────────────────────────────────────────────

    public void move() {
        if (x == null || y == null) return;
        int dx = 0, dy = 0;
        if (speed > 0) {
            double rad = (256 - getDirection16th() * 16) * 2 * PI / 256;
            dx += round(cos(rad) * ceil(speed));
            dy += round(sin(rad) * ceil(speed));
        }
        if (slideTicks > 0) {
            double rad = (256 - getSlideDirection16th() * 16) * 2 * PI / 256;
            dx += round(cos(rad) * 16);
            dy += round(sin(rad) * 16);
            slideTicks--;
        }
        if (dx == 0 && dy == 0) return;

        int newx = x + dx, newy = y + dy;
        boolean slowDown = true;

        if (dx != 0) {
            int aheadX = dx > 0 ? newx + 64 : newx - 64;
            var ahead = (WorldMapCell) world().map().cellAtWorld(aheadX, newy);
            if (ahead.getTankSpeed(this) != 0) {
                slowDown = false;
                if (!(onBoat && !ahead.isType(' ', '^') && speed < 16)) {
                    x = newx;
                }
            }
        }
        if (dy != 0) {
            int aheadY = dy > 0 ? newy + 64 : newy - 64;
            var ahead = (WorldMapCell) world().map().cellAtWorld(x, aheadY);
            if (ahead.getTankSpeed(this) != 0) {
                slowDown = false;
                if (!(onBoat && !ahead.isType(' ', '^') && speed < 16)) {
                    y = newy;
                }
            }
        }

        if (slowDown) {
            speed = max(0.00, speed - 1);
        }

        var oldCell = cell;
        updateCell();
        if (oldCell != cell) checkNewCell(oldCell);

        if (!onBoat && speed <= 3 && cell != null && cell.isType(' ')) {
            if (++waterTimer == 15) {
                if (shells != 0 || mines != 0) soundEffect(SoundEffect.BUBBLES);
                shells = max(0, shells - 1);
                mines = max(0, mines - 1);
                waterTimer = 0;
            }
        } else {
            waterTimer = 0;
        }
    }

    private void checkNewCell(WorldMapCell oldCell) {
        if (cell == null) return;
        if (onBoat) {
            if (!cell.isType(' ', '^')) leaveBoat(oldCell);
        } else {
            if (cell.isType('^')) { sink(); return; }
            if (cell.isType('b')) enterBoat();
        }
        // Mine explosion deferred to integration
    }

    private void leaveBoat(WorldMapCell oldCell) {
        if (cell.isType('b')) {
            cell.setType(org.rawaki.core.TerrainType.RIVER, false, 0);
            // Explosion spawning deferred
        } else {
            if (oldCell != null && oldCell.isType(' ')) {
                oldCell.setType(org.rawaki.core.TerrainType.BOAT, false, 0);
            }
            onBoat = false;
        }
    }

    private void enterBoat() {
        if (cell != null) cell.setType(org.rawaki.core.TerrainType.RIVER, false, 0);
        onBoat = true;
    }

    // ── TankLike interface ────────────────────────────────────────────────

    @Override public int armour()          { return armourVal; }
    @Override public boolean onBoat()      { return onBoat; }
    @Override public WorldMapCell cell()   { return cell; }

    // ── RefuelTarget interface ────────────────────────────────────────────

    @Override public int shells()          { return shells; }
    @Override public int mines()           { return mines; }
    @Override public void addArmour(int a) { armourVal += a; }
    @Override public void addShells(int a) { shells += a; }
    @Override public void addMines(int a)  { mines += a; }

    // ── Accessors ─────────────────────────────────────────────────────────

    public double direction()                  { return direction; }
    public double speed()                   { return speed; }
    public int slideTicks()                 { return slideTicks; }
    public int slideDirection()             { return slideDirection; }
    public boolean accelerating()           { return accelerating; }
    public boolean braking()                { return braking; }
    public boolean turningClockwise()       { return turningClockwise; }
    public boolean turningCounterClockwise(){ return turningCounterClockwise; }
    public int turnSpeedup()                { return turnSpeedup; }
    public int trees()                      { return trees; }
    public int reload()                     { return reload; }
    public boolean shooting()               { return shooting; }
    public double firingRange()             { return firingRange; }
    public int waterTimer()                 { return waterTimer; }
    public int respawnTimer()               { return respawnTimer; }
    public Ref<BoloObject> builder()        { return builder; }

    public void setDirection(double d)                   { this.direction = d; }
    public void setSpeed(double s)                    { this.speed = s; }
    public void setSlideTicks(int t)                  { this.slideTicks = t; }
    public void setSlideDirection(int d)              { this.slideDirection = d; }
    public void setAccelerating(boolean v)            { this.accelerating = v; }
    public void setBraking(boolean v)                 { this.braking = v; }
    public void setTurningClockwise(boolean v)        { this.turningClockwise = v; }
    public void setTurningCounterClockwise(boolean v) { this.turningCounterClockwise = v; }
    public void setShells(int s)                      { this.shells = s; }
    public void setMines(int m)                       { this.mines = m; }
    public void setArmour(int a)                      { this.armourVal = a; }
    public void setTrees(int t)                       { this.trees = t; }
    public void setReload(int r)                      { this.reload = r; }
    public void setShooting(boolean v)                { this.shooting = v; }
    public void setFiringRange(double r)              { this.firingRange = r; }
    public void setOnBoat(boolean v)                  { this.onBoat = v; }
    public void setCell(WorldMapCell c)               { this.cell = c; }
}

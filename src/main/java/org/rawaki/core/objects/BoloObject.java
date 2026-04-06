package org.rawaki.core.objects;

import org.rawaki.core.SoundEffect;
import org.rawaki.core.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class BoloObject {

    private World world;
    private int idx;
    private int updatePriority;

    // Whether drawn using base tilemap (false), styled tilemap (true), or not drawn (null).
    protected Boolean styled = null;
    protected int team = 255;

    // World coordinates. Null means not physical / not in the world (e.g. dead tank).
    protected Integer x;
    protected Integer y;

    // Simple event system
    private final Map<String, List<Runnable>> listeners = new HashMap<>();

    public BoloObject(World world) {
        this.world = world;
    }

    // ── Lifecycle (override in subclasses) ────────────────────────────────

    public void spawn(Object... args) {}
    public void update() {}
    public void destroy() {}
    public void anySpawn() {}

    // ── Events ────────────────────────────────────────────────────────────

    public void on(String event, Runnable listener) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
    }

    public void emit(String event) {
        List<Runnable> list = listeners.get(event);
        if (list != null) {
            for (Runnable r : list) r.run();
        }
    }

    public void removeListener(String event, Runnable listener) {
        List<Runnable> list = listeners.get(event);
        if (list != null) list.remove(listener);
    }

    public void removeAllListeners(String event) {
        listeners.remove(event);
    }

    // ── Sound ─────────────────────────────────────────────────────────────

    public void soundEffect(SoundEffect sfx) {
        if (world != null && x != null && y != null) {
            world.soundEffect(sfx, x, y, this);
        }
    }

    // ── Tile (override in subclasses) ─────────────────────────────────────

    public int[] getTile() { return null; }

    // ── Accessors ─────────────────────────────────────────────────────────

    public World world()              { return world; }
    public void setWorld(World world) { this.world = world; }
    public int idx()                  { return idx; }
    public void setIdx(int idx)       { this.idx = idx; }
    public int updatePriority()       { return updatePriority; }
    public void setUpdatePriority(int p) { this.updatePriority = p; }
    public Boolean styled()           { return styled; }
    public int team()                 { return team; }
    public void setTeam(int team)     { this.team = team; }
    public Integer x()                { return x; }
    public Integer y()                { return y; }
    public void setX(Integer x)       { this.x = x; }
    public void setY(Integer y)       { this.y = y; }
}

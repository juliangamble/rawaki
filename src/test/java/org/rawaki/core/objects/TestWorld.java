package org.rawaki.core.objects;

import org.rawaki.core.SoundEffect;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared mock World for tests. Tracks spawns, destroys, and sounds.
 */
public class TestWorld implements World {
    public final WorldMap map;
    public final List<BoloObject> tanks = new ArrayList<>();
    public final List<BoloObject> destroyed = new ArrayList<>();
    public final List<SoundEffect> sounds = new ArrayList<>();
    public boolean authorityFlag = true;

    public TestWorld() { this.map = new WorldMap(); }
    public TestWorld(WorldMap map) { this.map = map; }

    @Override public WorldMap map() { return map; }
    @Override public List<BoloObject> tanks() { return tanks; }
    @Override public boolean authority() { return authorityFlag; }
    @Override public void soundEffect(SoundEffect sfx, int x, int y, BoloObject owner) { sounds.add(sfx); }
    @Override public void destroy(BoloObject obj) { destroyed.add(obj); }
    @Override public void insert(BoloObject obj) {}
    @Override public <T extends BoloObject> T spawn(Class<T> type, Object... args) { return null; }
}

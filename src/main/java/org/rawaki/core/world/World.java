package org.rawaki.core.world;

import org.rawaki.core.SoundEffect;
import org.rawaki.core.map.WorldMap;
import org.rawaki.core.objects.BoloObject;

public interface World {
    WorldMap map();
    void soundEffect(SoundEffect sfx, int x, int y, BoloObject owner);
    <T extends BoloObject> T spawn(Class<T> type, Object... args);
    void destroy(BoloObject obj);
    void insert(BoloObject obj);
    boolean authority();
}

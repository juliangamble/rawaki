package org.rawaki.core.world;

import org.rawaki.core.objects.BoloObject;

import java.util.ArrayList;
import java.util.List;

public interface BoloWorldMixin extends World {

    List<BoloObject> tanks();

    default void addTank(BoloObject tank) {
        tank.setIdx(tanks().size());
        tanks().add(tank);
        if (authority()) resolveMapObjectOwners();
    }

    default void removeTank(BoloObject tank) {
        int idx = tank.idx();
        tanks().remove(idx);
        for (int i = idx; i < tanks().size(); i++) {
            tanks().get(i).setIdx(i);
        }
        if (authority()) resolveMapObjectOwners();
    }

    default List<BoloObject> getAllMapObjects() {
        List<BoloObject> all = new ArrayList<>();
        // Map pills and bases are BoloObjects in the world implementation
        // Subclasses provide the actual typed lists
        return all;
    }

    default void spawnMapObjects() {
        for (BoloObject obj : getAllMapObjects()) {
            obj.setWorld(this);
            insert(obj);
            obj.spawn();
            obj.anySpawn();
        }
    }

    default void resolveMapObjectOwners() {
        // Subclasses implement owner resolution using their typed map objects
    }
}

package org.rawaki.core.objects;

import java.util.List;

public final class ObjectRegistry {

    private static final List<Class<? extends BoloObject>> TYPES = List.of(
        WorldPillbox.class,
        WorldBase.class,
        FloodFill.class,
        Tank.class,
        Explosion.class,
        MineExplosion.class,
        Shell.class,
        Fireball.class,
        Builder.class
    );

    public static List<Class<? extends BoloObject>> types() {
        return TYPES;
    }

    public static int indexOf(Class<? extends BoloObject> type) {
        int idx = TYPES.indexOf(type);
        if (idx == -1) throw new IllegalArgumentException("Unknown object type: " + type.getName());
        return idx;
    }

    public static Class<? extends BoloObject> typeAt(int index) {
        if (index < 0 || index >= TYPES.size()) {
            throw new IllegalArgumentException("Invalid type index: " + index);
        }
        return TYPES.get(index);
    }

    public static int size() {
        return TYPES.size();
    }

    private ObjectRegistry() {}
}

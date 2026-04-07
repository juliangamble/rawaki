package org.rawaki.core.objects;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ObjectRegistryTest {

    @Test
    void hasNineTypes() {
        assertEquals(9, ObjectRegistry.size());
    }

    @Test
    void indicesMatchCoffeeScriptOrder() {
        assertEquals(0, ObjectRegistry.indexOf(WorldPillbox.class));
        assertEquals(1, ObjectRegistry.indexOf(WorldBase.class));
        assertEquals(2, ObjectRegistry.indexOf(FloodFill.class));
        assertEquals(3, ObjectRegistry.indexOf(Tank.class));
        assertEquals(4, ObjectRegistry.indexOf(Explosion.class));
        assertEquals(5, ObjectRegistry.indexOf(MineExplosion.class));
        assertEquals(6, ObjectRegistry.indexOf(Shell.class));
        assertEquals(7, ObjectRegistry.indexOf(Fireball.class));
        assertEquals(8, ObjectRegistry.indexOf(Builder.class));
    }

    @Test
    void typeAtRoundTrips() {
        for (int i = 0; i < ObjectRegistry.size(); i++) {
            assertEquals(i, ObjectRegistry.indexOf(ObjectRegistry.typeAt(i)));
        }
    }

    @Test
    void indexOfThrowsForUnknownType() {
        class FakeObject extends BoloObject {
            FakeObject() { super(null); }
        }
        assertThrows(IllegalArgumentException.class, () -> ObjectRegistry.indexOf(FakeObject.class));
    }

    @Test
    void typeAtThrowsForNegativeIndex() {
        assertThrows(IllegalArgumentException.class, () -> ObjectRegistry.typeAt(-1));
    }

    @Test
    void typeAtThrowsForTooLargeIndex() {
        assertThrows(IllegalArgumentException.class, () -> ObjectRegistry.typeAt(99));
    }
}

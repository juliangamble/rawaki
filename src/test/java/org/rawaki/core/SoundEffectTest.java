package org.rawaki.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SoundEffectTest {

    @Test
    void hasTwelveEffects() {
        assertEquals(12, SoundEffect.values().length);
    }

    @Test
    void ordinalsMatchNetworkIds() {
        assertEquals(0, SoundEffect.BIG_EXPLOSION.ordinal());
        assertEquals(1, SoundEffect.BUBBLES.ordinal());
        assertEquals(2, SoundEffect.FARMING_TREE.ordinal());
        assertEquals(3, SoundEffect.HIT_TANK.ordinal());
        assertEquals(4, SoundEffect.MAN_BUILDING.ordinal());
        assertEquals(5, SoundEffect.MAN_DYING.ordinal());
        assertEquals(6, SoundEffect.MAN_LAY_MINE.ordinal());
        assertEquals(7, SoundEffect.MINE_EXPLOSION.ordinal());
        assertEquals(8, SoundEffect.SHOOTING.ordinal());
        assertEquals(9, SoundEffect.SHOT_BUILDING.ordinal());
        assertEquals(10, SoundEffect.SHOT_TREE.ordinal());
        assertEquals(11, SoundEffect.TANK_SINKING.ordinal());
    }
}

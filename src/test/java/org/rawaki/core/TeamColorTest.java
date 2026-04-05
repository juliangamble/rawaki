package org.rawaki.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class TeamColorTest {

    @Test
    void hasSixColors() {
        assertEquals(6, TeamColor.values().length);
    }

    @Test
    void redValues() {
        assertEquals(255, TeamColor.RED.r());
        assertEquals(0, TeamColor.RED.g());
        assertEquals(0, TeamColor.RED.b());
    }

    @Test
    void blueValues() {
        assertEquals(0, TeamColor.BLUE.r());
        assertEquals(0, TeamColor.BLUE.g());
        assertEquals(255, TeamColor.BLUE.b());
    }

    @Test
    void greenValues() {
        assertEquals(0, TeamColor.GREEN.r());
        assertEquals(255, TeamColor.GREEN.g());
        assertEquals(0, TeamColor.GREEN.b());
    }

    @Test
    void cyanValues() {
        assertEquals(0, TeamColor.CYAN.r());
        assertEquals(255, TeamColor.CYAN.g());
        assertEquals(255, TeamColor.CYAN.b());
    }

    @Test
    void yellowValues() {
        assertEquals(255, TeamColor.YELLOW.r());
        assertEquals(255, TeamColor.YELLOW.g());
        assertEquals(0, TeamColor.YELLOW.b());
    }

    @Test
    void magentaValues() {
        assertEquals(255, TeamColor.MAGENTA.r());
        assertEquals(0, TeamColor.MAGENTA.g());
        assertEquals(255, TeamColor.MAGENTA.b());
    }

    @ParameterizedTest
    @EnumSource(TeamColor.class)
    void rgbValuesInRange(TeamColor color) {
        assertTrue(color.r() >= 0 && color.r() <= 255);
        assertTrue(color.g() >= 0 && color.g() <= 255);
        assertTrue(color.b() >= 0 && color.b() <= 255);
    }
}

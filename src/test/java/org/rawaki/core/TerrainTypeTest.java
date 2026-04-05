package org.rawaki.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

class TerrainTypeTest {

    @Test
    void hasElevenTypes() {
        assertEquals(11, TerrainType.values().length);
    }

    @Test
    void ordinalMatchesBmapIndex() {
        assertEquals(0, TerrainType.BUILDING.ordinal());
        assertEquals(1, TerrainType.RIVER.ordinal());
        assertEquals(2, TerrainType.SWAMP.ordinal());
        assertEquals(3, TerrainType.CRATER.ordinal());
        assertEquals(4, TerrainType.ROAD.ordinal());
        assertEquals(5, TerrainType.FOREST.ordinal());
        assertEquals(6, TerrainType.RUBBLE.ordinal());
        assertEquals(7, TerrainType.GRASS.ordinal());
        assertEquals(8, TerrainType.SHOT_BUILDING.ordinal());
        assertEquals(9, TerrainType.BOAT.ordinal());
        assertEquals(10, TerrainType.DEEP_SEA.ordinal());
    }

    @Test
    void asciiCodes() {
        assertEquals('|', TerrainType.BUILDING.ascii());
        assertEquals(' ', TerrainType.RIVER.ascii());
        assertEquals('~', TerrainType.SWAMP.ascii());
        assertEquals('%', TerrainType.CRATER.ascii());
        assertEquals('=', TerrainType.ROAD.ascii());
        assertEquals('#', TerrainType.FOREST.ascii());
        assertEquals(':', TerrainType.RUBBLE.ascii());
        assertEquals('.', TerrainType.GRASS.ascii());
        assertEquals('}', TerrainType.SHOT_BUILDING.ascii());
        assertEquals('b', TerrainType.BOAT.ascii());
        assertEquals('^', TerrainType.DEEP_SEA.ascii());
    }

    @Test
    void descriptions() {
        assertEquals("building", TerrainType.BUILDING.description());
        assertEquals("river", TerrainType.RIVER.description());
        assertEquals("grass", TerrainType.GRASS.description());
        assertEquals("deep sea", TerrainType.DEEP_SEA.description());
        assertEquals("river with boat", TerrainType.BOAT.description());
        assertEquals("shot building", TerrainType.SHOT_BUILDING.description());
    }

    @Test
    void tankSpeedValues() {
        assertEquals(0, TerrainType.BUILDING.tankSpeed());
        assertEquals(3, TerrainType.RIVER.tankSpeed());
        assertEquals(3, TerrainType.SWAMP.tankSpeed());
        assertEquals(3, TerrainType.CRATER.tankSpeed());
        assertEquals(16, TerrainType.ROAD.tankSpeed());
        assertEquals(6, TerrainType.FOREST.tankSpeed());
        assertEquals(3, TerrainType.RUBBLE.tankSpeed());
        assertEquals(12, TerrainType.GRASS.tankSpeed());
        assertEquals(0, TerrainType.SHOT_BUILDING.tankSpeed());
        assertEquals(16, TerrainType.BOAT.tankSpeed());
        assertEquals(3, TerrainType.DEEP_SEA.tankSpeed());
    }

    @Test
    void tankTurnValues() {
        assertEquals(0.00, TerrainType.BUILDING.tankTurn());
        assertEquals(0.25, TerrainType.RIVER.tankTurn());
        assertEquals(1.00, TerrainType.ROAD.tankTurn());
        assertEquals(0.50, TerrainType.FOREST.tankTurn());
        assertEquals(1.00, TerrainType.GRASS.tankTurn());
        assertEquals(0.50, TerrainType.DEEP_SEA.tankTurn());
    }

    @Test
    void manSpeedValues() {
        assertEquals(0, TerrainType.BUILDING.manSpeed());
        assertEquals(0, TerrainType.RIVER.manSpeed());
        assertEquals(4, TerrainType.SWAMP.manSpeed());
        assertEquals(16, TerrainType.ROAD.manSpeed());
        assertEquals(8, TerrainType.FOREST.manSpeed());
        assertEquals(16, TerrainType.GRASS.manSpeed());
        assertEquals(0, TerrainType.DEEP_SEA.manSpeed());
    }

    @ParameterizedTest
    @EnumSource(TerrainType.class)
    void fromAsciiRoundTrips(TerrainType type) {
        assertEquals(type, TerrainType.fromAscii(type.ascii()));
    }

    @ParameterizedTest
    @EnumSource(TerrainType.class)
    void fromOrdinalRoundTrips(TerrainType type) {
        assertEquals(type, TerrainType.fromOrdinal(type.ordinal()));
    }

    @Test
    void fromAsciiThrowsForUnknown() {
        assertThrows(IllegalArgumentException.class, () -> TerrainType.fromAscii('?'));
    }

    @Test
    void fromOrdinalThrowsForNegative() {
        assertThrows(IllegalArgumentException.class, () -> TerrainType.fromOrdinal(-1));
    }

    @Test
    void fromOrdinalThrowsForTooLarge() {
        assertThrows(IllegalArgumentException.class, () -> TerrainType.fromOrdinal(99));
    }

    @ParameterizedTest
    @EnumSource(TerrainType.class)
    void eachTypeHasUniqueAscii(TerrainType type) {
        long count = 0;
        for (TerrainType t : TerrainType.values()) {
            if (t.ascii() == type.ascii()) count++;
        }
        assertEquals(1, count, "Duplicate ascii for " + type);
    }
}

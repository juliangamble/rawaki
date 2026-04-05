package org.rawaki.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HelpersTest {

    @Test
    void distanceSamePoint() {
        assertEquals(0, Helpers.distance(5, 5, 5, 5));
    }

    @Test
    void distanceHorizontal() {
        assertEquals(3, Helpers.distance(0, 0, 3, 0));
    }

    @Test
    void distanceVertical() {
        assertEquals(4, Helpers.distance(0, 0, 0, 4));
    }

    @Test
    void distanceDiagonal345() {
        assertEquals(5, Helpers.distance(0, 0, 3, 4));
    }

    @Test
    void distanceIsCommutative() {
        assertEquals(
            Helpers.distance(10, 20, 30, 40),
            Helpers.distance(30, 40, 10, 20)
        );
    }

    @Test
    void distanceNegativeCoordinates() {
        assertEquals(5, Helpers.distance(-3, 0, 0, 4));
    }

    @Test
    void headingDueEast() {
        assertEquals(0, Helpers.heading(0, 0, 10, 0));
    }

    @Test
    void headingDueSouth() {
        assertEquals(Math.PI / 2, Helpers.heading(0, 0, 0, 10), 0.001);
    }

    @Test
    void headingDueWest() {
        assertEquals(Math.PI, Helpers.heading(0, 0, -10, 0), 0.001);
    }

    @Test
    void headingDueNorth() {
        assertEquals(-Math.PI / 2, Helpers.heading(0, 0, 0, -10), 0.001);
    }

    @Test
    void heading45Degrees() {
        assertEquals(Math.PI / 4, Helpers.heading(0, 0, 10, 10), 0.001);
    }

    @Test
    void headingNonOriginPoints() {
        double h1 = Helpers.heading(0, 0, 5, 5);
        double h2 = Helpers.heading(100, 100, 105, 105);
        assertEquals(h1, h2, 0.001);
    }
}

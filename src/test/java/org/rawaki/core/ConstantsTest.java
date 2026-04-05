package org.rawaki.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstantsTest {

    @Test
    void pixelSizeWorld() {
        assertEquals(8, Constants.PIXEL_SIZE_WORLD);
    }

    @Test
    void tileSizePixels() {
        assertEquals(32, Constants.TILE_SIZE_PIXELS);
    }

    @Test
    void tileSizeWorldIsDerived() {
        assertEquals(Constants.TILE_SIZE_PIXELS * Constants.PIXEL_SIZE_WORLD, Constants.TILE_SIZE_WORLD);
        assertEquals(256, Constants.TILE_SIZE_WORLD);
    }

    @Test
    void mapSizeTiles() {
        assertEquals(256, Constants.MAP_SIZE_TILES);
    }

    @Test
    void mapSizePixelsIsDerived() {
        assertEquals(Constants.MAP_SIZE_TILES * Constants.TILE_SIZE_PIXELS, Constants.MAP_SIZE_PIXELS);
        assertEquals(8192, Constants.MAP_SIZE_PIXELS);
    }

    @Test
    void mapSizeWorldIsDerived() {
        assertEquals(Constants.MAP_SIZE_TILES * Constants.TILE_SIZE_WORLD, Constants.MAP_SIZE_WORLD);
        assertEquals(65536, Constants.MAP_SIZE_WORLD);
    }

    @Test
    void tickLengthMs() {
        assertEquals(20, Constants.TICK_LENGTH_MS);
    }
}

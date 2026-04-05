package org.rawaki.core;

public final class Constants {

    public static final int PIXEL_SIZE_WORLD = 8;

    public static final int TILE_SIZE_PIXELS = 32;
    public static final int TILE_SIZE_WORLD = TILE_SIZE_PIXELS * PIXEL_SIZE_WORLD;

    public static final int MAP_SIZE_TILES = 256;
    public static final int MAP_SIZE_PIXELS = MAP_SIZE_TILES * TILE_SIZE_PIXELS;
    public static final int MAP_SIZE_WORLD = MAP_SIZE_TILES * TILE_SIZE_WORLD;

    public static final int TICK_LENGTH_MS = 20;

    private Constants() {}
}

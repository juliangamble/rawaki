package org.rawaki.core.map;

import org.rawaki.core.Constants;

public class WorldMap extends GameMap {

    // Back-reference set by the world after construction
    private Object world;

    @Override
    protected MapCell createCell(int x, int y) {
        return new WorldMapCell(this, x, y);
    }

    public WorldMapCell cellAtPixel(int x, int y) {
        return (WorldMapCell) cellAtTile(
            Math.floorDiv(x, Constants.TILE_SIZE_PIXELS),
            Math.floorDiv(y, Constants.TILE_SIZE_PIXELS)
        );
    }

    public WorldMapCell cellAtWorld(int x, int y) {
        return (WorldMapCell) cellAtTile(
            Math.floorDiv(x, Constants.TILE_SIZE_WORLD),
            Math.floorDiv(y, Constants.TILE_SIZE_WORLD)
        );
    }

    public Start getRandomStart() {
        if (starts().isEmpty()) return null;
        int idx = (int) Math.round(Math.random() * (starts().size() - 1));
        return starts().get(idx);
    }

    public Object world()            { return world; }
    public void setWorld(Object world) { this.world = world; }
}

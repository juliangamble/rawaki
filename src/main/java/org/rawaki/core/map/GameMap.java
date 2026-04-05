package org.rawaki.core.map;

import org.rawaki.core.Constants;
import org.rawaki.core.TerrainType;

public class GameMap {

    private final MapCell[][] cells;

    public GameMap() {
        cells = new MapCell[Constants.MAP_SIZE_TILES][Constants.MAP_SIZE_TILES];
        for (int y = 0; y < Constants.MAP_SIZE_TILES; y++) {
            for (int x = 0; x < Constants.MAP_SIZE_TILES; x++) {
                cells[y][x] = createCell(x, y);
            }
        }
    }

    protected MapCell createCell(int x, int y) {
        return new MapCell(this, x, y);
    }

    public MapCell cellAtTile(int x, int y) {
        if (x >= 0 && x < Constants.MAP_SIZE_TILES && y >= 0 && y < Constants.MAP_SIZE_TILES) {
            return cells[y][x];
        }
        return new MapCell(this, x, y);
    }

    public void retile(int sx, int sy, int ex, int ey) {
        int startX = Math.max(0, sx);
        int startY = Math.max(0, sy);
        int endX = Math.min(Constants.MAP_SIZE_TILES - 1, ex);
        int endY = Math.min(Constants.MAP_SIZE_TILES - 1, ey);
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                cells[y][x].retile();
            }
        }
    }

    public MapCell[][] cells() { return cells; }
}

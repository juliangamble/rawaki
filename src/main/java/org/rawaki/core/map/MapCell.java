package org.rawaki.core.map;

import org.rawaki.core.Constants;
import org.rawaki.core.TerrainType;

public class MapCell {

    private final GameMap map;
    private final int x;
    private final int y;
    private final int idx;
    private TerrainType type;
    private boolean mine;

    // Set by game objects (WorldPillbox, WorldBase) — null when unoccupied
    private Object pill;
    private Object base;

    public MapCell(GameMap map, int x, int y) {
        this.map = map;
        this.x = x;
        this.y = y;
        this.type = TerrainType.DEEP_SEA;
        this.mine = isEdgeCell();
        this.idx = y * Constants.MAP_SIZE_TILES + x;
    }

    public MapCell neigh(int dx, int dy) {
        return map.cellAtTile(x + dx, y + dy);
    }

    public boolean isType(TerrainType... types) {
        if (type == null) return false;
        for (TerrainType t : types) {
            if (this.type == t) return true;
        }
        return false;
    }

    public boolean isType(char... asciiTypes) {
        if (type == null) return false;
        for (char c : asciiTypes) {
            if (this.type.ascii() == c) return true;
        }
        return false;
    }

    public boolean isEdgeCell() {
        return x <= 20 || x >= 236 || y <= 20 || y >= 236;
    }

    public int getNumericType() {
        if (type == TerrainType.DEEP_SEA) return -1;
        int num = type.ordinal();
        if (mine) num += 8;
        return num;
    }

    public void setType(TerrainType newType) {
        setType(newType, null, 1);
    }

    public void setType(char ascii) {
        setType(TerrainType.fromAscii(ascii), null, 1);
    }

    public void setType(char ascii, Boolean mine) {
        setType(TerrainType.fromAscii(ascii), mine, 1);
    }

    public void setType(int numericType) {
        setType(numericType, 1);
    }

    public void setType(int numericType, int retileRadius) {
        boolean mineFlag;
        if (numericType >= 10) {
            numericType -= 8;
            mineFlag = true;
        } else {
            mineFlag = false;
        }
        TerrainType t = TerrainType.fromOrdinal(numericType);
        setType(t, mineFlag, retileRadius);
    }

    public void setType(TerrainType newType, Boolean mine, int retileRadius) {
        if (newType != null) {
            this.type = newType;
        }
        if (mine != null) {
            this.mine = mine;
        }
        if (isEdgeCell()) {
            this.mine = true;
        }
        if (retileRadius >= 0) {
            map.retile(x - retileRadius, y - retileRadius, x + retileRadius, y + retileRadius);
        }
    }

    public void retile() {
        // Rendering callback — implemented by subclass or view system
    }

    // Accessors
    public int x()              { return x; }
    public int y()              { return y; }
    public int idx()            { return idx; }
    public TerrainType type()   { return type; }
    public boolean mine()       { return mine; }
    public GameMap map()        { return map; }
    public Object pill()        { return pill; }
    public Object base()        { return base; }
    public void setPill(Object pill) { this.pill = pill; }
    public void setBase(Object base) { this.base = base; }
}

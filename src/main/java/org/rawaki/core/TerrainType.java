package org.rawaki.core;

import java.util.HashMap;
import java.util.Map;

public enum TerrainType {
    BUILDING       ('|', "building",         0,  0.00,  0),
    RIVER          (' ', "river",            3,  0.25,  0),
    SWAMP          ('~', "swamp",            3,  0.25,  4),
    CRATER         ('%', "crater",           3,  0.25,  4),
    ROAD           ('=', "road",            16,  1.00, 16),
    FOREST         ('#', "forest",           6,  0.50,  8),
    RUBBLE         (':', "rubble",           3,  0.25,  4),
    GRASS          ('.', "grass",           12,  1.00, 16),
    SHOT_BUILDING  ('}', "shot building",    0,  0.00,  0),
    BOAT           ('b', "river with boat", 16,  1.00, 16),
    DEEP_SEA       ('^', "deep sea",         3,  0.50,  0);

    private static final Map<Character, TerrainType> BY_ASCII = new HashMap<>();

    static {
        for (TerrainType t : values()) {
            BY_ASCII.put(t.ascii, t);
        }
    }

    private final char ascii;
    private final String description;
    private final int tankSpeed;
    private final double tankTurn;
    private final int manSpeed;

    TerrainType(char ascii, String description, int tankSpeed, double tankTurn, int manSpeed) {
        this.ascii = ascii;
        this.description = description;
        this.tankSpeed = tankSpeed;
        this.tankTurn = tankTurn;
        this.manSpeed = manSpeed;
    }

    public char ascii()          { return ascii; }
    public String description()  { return description; }
    public int tankSpeed()       { return tankSpeed; }
    public double tankTurn()     { return tankTurn; }
    public int manSpeed()        { return manSpeed; }

    public static TerrainType fromAscii(char c) {
        TerrainType t = BY_ASCII.get(c);
        if (t == null) throw new IllegalArgumentException("Unknown terrain type: " + c);
        return t;
    }

    public static TerrainType fromOrdinal(int index) {
        TerrainType[] vals = values();
        if (index < 0 || index >= vals.length) {
            throw new IllegalArgumentException("Invalid terrain type index: " + index);
        }
        return vals[index];
    }
}

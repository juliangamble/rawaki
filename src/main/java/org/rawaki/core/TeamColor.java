package org.rawaki.core;

public enum TeamColor {
    RED     (255,   0,   0),
    BLUE    (  0,   0, 255),
    GREEN   (  0, 255,   0),
    CYAN    (  0, 255, 255),
    YELLOW  (255, 255,   0),
    MAGENTA (255,   0, 255);

    private final int r, g, b;

    TeamColor(int r, int g, int b) {
        this.r = r; this.g = g; this.b = b;
    }

    public int r() { return r; }
    public int g() { return g; }
    public int b() { return b; }
}

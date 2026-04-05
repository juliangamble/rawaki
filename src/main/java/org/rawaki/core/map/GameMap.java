package org.rawaki.core.map;

import org.rawaki.core.Constants;
import org.rawaki.core.TerrainType;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GameMap {

    private static final byte[] BMAP_MAGIC = "BMAPBOLO".getBytes();

    private final MapCell[][] cells;
    private List<Pillbox> pills = new ArrayList<>();
    private List<Base> bases = new ArrayList<>();
    private List<Start> starts = new ArrayList<>();

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

    public void each(Consumer<MapCell> cb) {
        each(cb, 0, 0, Constants.MAP_SIZE_TILES - 1, Constants.MAP_SIZE_TILES - 1);
    }

    public void each(Consumer<MapCell> cb, int sx, int sy, int ex, int ey) {
        sx = Math.max(0, sx);
        sy = Math.max(0, sy);
        ex = Math.min(Constants.MAP_SIZE_TILES - 1, ex);
        ey = Math.min(Constants.MAP_SIZE_TILES - 1, ey);
        for (int y = sy; y <= ey; y++) {
            for (int x = sx; x <= ex; x++) {
                cb.accept(cells[y][x]);
            }
        }
    }

    public void clear() {
        each(cell -> cell.setType(TerrainType.DEEP_SEA, null, -1));
    }

    public void clear(int sx, int sy, int ex, int ey) {
        each(cell -> cell.setType(TerrainType.DEEP_SEA, null, -1), sx, sy, ex, ey);
    }

    public void retile(int sx, int sy, int ex, int ey) {
        each(MapCell::retile, sx, sy, ex, ey);
    }

    public MapCell findCenterCell() {
        int t = Constants.MAP_SIZE_TILES - 1, l = Constants.MAP_SIZE_TILES - 1;
        int b = 0, r = 0;
        for (int y = 0; y < Constants.MAP_SIZE_TILES; y++) {
            for (int x = 0; x < Constants.MAP_SIZE_TILES; x++) {
                MapCell c = cells[y][x];
                if (c.x() < l) l = c.x();
                if (c.x() > r) r = c.x();
                if (c.y() < t) t = c.y();
                if (c.y() > b) b = c.y();
            }
        }
        if (l > r) { t = l = 0; b = r = Constants.MAP_SIZE_TILES - 1; }
        int cx = Math.round(l + (r - l) / 2.0f);
        int cy = Math.round(t + (b - t) / 2.0f);
        return cellAtTile(cx, cy);
    }

    // ── BMAP serialization ────────────────────────────────────────────────

    public byte[] dump(boolean noPills, boolean noBases, boolean noStarts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Header
        out.writeBytes(BMAP_MAGIC);
        List<Pillbox> dumpPills = noPills ? List.of() : pills;
        List<Base> dumpBases = noBases ? List.of() : bases;
        List<Start> dumpStarts = noStarts ? List.of() : starts;
        out.write(1); // version
        out.write(dumpPills.size());
        out.write(dumpBases.size());
        out.write(dumpStarts.size());

        for (Pillbox p : dumpPills) {
            out.write(p.x()); out.write(p.y());
            out.write(p.ownerIdx()); out.write(p.armour()); out.write(p.speed());
        }
        for (Base b : dumpBases) {
            out.write(b.x()); out.write(b.y());
            out.write(b.ownerIdx()); out.write(b.armour()); out.write(b.shells()); out.write(b.mines());
        }
        for (Start s : dumpStarts) {
            out.write(s.x()); out.write(s.y()); out.write(s.direction());
        }

        // Map data rows
        for (int y = 0; y < Constants.MAP_SIZE_TILES; y++) {
            dumpRow(out, y);
        }

        // Sentinel
        out.write(4); out.write(0xFF); out.write(0xFF); out.write(0xFF);

        return out.toByteArray();
    }

    public byte[] dump() {
        return dump(false, false, false);
    }

    private void dumpRow(ByteArrayOutputStream out, int y) {
        // Collect runs of nibbles for this row, split by deep sea gaps
        List<int[]> segments = collectSegments(y);

        for (int[] seg : segments) {
            int sx = seg[0];
            int ex = seg[1];
            List<Integer> nibbles = new ArrayList<>();
            int x = sx;
            while (x < ex) {
                int type = cells[y][x].getNumericType();
                int count = 1;
                while (x + count < ex && cells[y][x + count].getNumericType() == type) count++;

                if (count > 2) {
                    // Long sequences
                    while (count > 2) {
                        int seqLen = Math.min(count, 9);
                        nibbles.add(seqLen + 6);
                        nibbles.add(type);
                        x += seqLen;
                        count -= seqLen;
                    }
                }
                // Short sequence (remaining)
                List<Integer> shortSeq = new ArrayList<>();
                while (count > 0) {
                    shortSeq.add(type);
                    if (shortSeq.size() == 8) {
                        nibbles.add(shortSeq.size() - 1);
                        nibbles.addAll(shortSeq);
                        shortSeq.clear();
                    }
                    count--;
                    x++;
                }
                if (!shortSeq.isEmpty()) {
                    nibbles.add(shortSeq.size() - 1);
                    nibbles.addAll(shortSeq);
                }
            }

            // Encode nibbles to bytes
            byte[] encoded = encodeNibbles(nibbles);
            out.write(encoded.length + 4);
            out.write(y);
            out.write(sx);
            out.write(ex);
            out.writeBytes(encoded);
        }
    }

    private List<int[]> collectSegments(int y) {
        List<int[]> segments = new ArrayList<>();
        int sx = -1;
        for (int x = 0; x < Constants.MAP_SIZE_TILES; x++) {
            int num = cells[y][x].getNumericType();
            if (num == -1) {
                if (sx != -1) { segments.add(new int[]{sx, x}); sx = -1; }
            } else {
                if (sx == -1) sx = x;
            }
        }
        if (sx != -1) segments.add(new int[]{sx, Constants.MAP_SIZE_TILES});
        return segments;
    }

    private static byte[] encodeNibbles(List<Integer> nibbles) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < nibbles.size(); i += 2) {
            int hi = nibbles.get(i) & 0x0F;
            if (i + 1 < nibbles.size()) {
                int lo = nibbles.get(i + 1) & 0x0F;
                out.write((hi << 4) | lo);
            } else {
                out.write(hi << 4);
            }
        }
        return out.toByteArray();
    }

    // ── BMAP loading ──────────────────────────────────────────────────────

    public static GameMap load(byte[] buffer) {
        int pos = 0;

        // Validate magic
        for (int i = 0; i < BMAP_MAGIC.length; i++) {
            if (pos >= buffer.length || buffer[pos++] != BMAP_MAGIC[i]) {
                throw new IllegalArgumentException("Not a Bolo map.");
            }
        }

        // Header
        if (pos + 4 > buffer.length) throw new IllegalArgumentException("Incomplete header");
        int version = buffer[pos++] & 0xFF;
        int numPills = buffer[pos++] & 0xFF;
        int numBases = buffer[pos++] & 0xFF;
        int numStarts = buffer[pos++] & 0xFF;
        if (version != 1) throw new IllegalArgumentException("Unsupported map version: " + version);

        GameMap map = new GameMap();

        // Read map objects
        int[][] pillsData = new int[numPills][5];
        for (int i = 0; i < numPills; i++) {
            for (int j = 0; j < 5; j++) pillsData[i][j] = buffer[pos++] & 0xFF;
        }
        int[][] basesData = new int[numBases][6];
        for (int i = 0; i < numBases; i++) {
            for (int j = 0; j < 6; j++) basesData[i][j] = buffer[pos++] & 0xFF;
        }
        int[][] startsData = new int[numStarts][3];
        for (int i = 0; i < numStarts; i++) {
            for (int j = 0; j < 3; j++) startsData[i][j] = buffer[pos++] & 0xFF;
        }

        // Read map data
        while (true) {
            if (pos + 4 > buffer.length) throw new IllegalArgumentException("Incomplete map data");
            int dataLen = buffer[pos++] & 0xFF;
            int y = buffer[pos++] & 0xFF;
            int sx = buffer[pos++] & 0xFF;
            int ex = buffer[pos++] & 0xFF;
            dataLen -= 4;
            if (dataLen == 0 && y == 0xFF && sx == 0xFF && ex == 0xFF) break;

            byte[] run = new byte[dataLen];
            System.arraycopy(buffer, pos, run, 0, dataLen);
            pos += dataLen;

            // Decode nibbles
            double runPos = 0;
            int x = sx;
            while (x < ex) {
                int seqLen = takeNibble(run, runPos);
                runPos += 0.5;
                if (seqLen < 8) {
                    for (int i = 0; i < seqLen + 1; i++) {
                        int type = takeNibble(run, runPos);
                        runPos += 0.5;
                        map.cellAtTile(x++, y).setType(type, -1);
                    }
                } else {
                    int type = takeNibble(run, runPos);
                    runPos += 0.5;
                    for (int i = 0; i < seqLen - 6; i++) {
                        map.cellAtTile(x++, y).setType(type, -1);
                    }
                }
            }
        }

        // Instantiate map objects
        for (int[] d : pillsData) {
            map.pills.add(new Pillbox(d[0], d[1], map.cellAtTile(d[0], d[1]), d[2], d[3], d[4]));
        }
        for (int[] d : basesData) {
            map.bases.add(new Base(d[0], d[1], map.cellAtTile(d[0], d[1]), d[2], d[3], d[4], d[5]));
        }
        for (int[] d : startsData) {
            map.starts.add(new Start(d[0], d[1], map.cellAtTile(d[0], d[1]), d[2]));
        }

        return map;
    }

    private static int takeNibble(byte[] run, double runPos) {
        int index = (int) runPos;
        if (index == runPos) {
            return (run[index] & 0xF0) >> 4;
        } else {
            return run[index] & 0x0F;
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────

    public MapCell[][] cells()    { return cells; }
    public List<Pillbox> pills()  { return pills; }
    public List<Base> bases()     { return bases; }
    public List<Start> starts()   { return starts; }
}

package org.rawaki.core;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class BinaryPacker {

    public static Packer buildPacker() {
        return new Packer();
    }

    public static Unpacker buildUnpacker(byte[] data, int offset) {
        return new Unpacker(data, offset);
    }

    public static Unpacker buildUnpacker(byte[] data) {
        return new Unpacker(data, 0);
    }

    public static byte[] pack(String fmt, int... values) {
        Packer p = buildPacker();
        int vi = 0;
        for (int i = 0; i < fmt.length(); i++) {
            p.write(fmt.charAt(i), values[vi++]);
        }
        return p.finish();
    }

    public static UnpackResult unpack(String fmt, byte[] data, int offset) {
        Unpacker u = buildUnpacker(data, offset);
        int[] values = new int[fmt.length()];
        for (int i = 0; i < fmt.length(); i++) {
            values[i] = u.read(fmt.charAt(i));
        }
        return new UnpackResult(values, u.finish());
    }

    public static UnpackResult unpack(String fmt, byte[] data) {
        return unpack(fmt, data, 0);
    }

    public record UnpackResult(int[] values, int bytesRead) {}

    public static class Packer {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private int bits = -1;
        private int bitIndex = 0;

        public void write(char type, int value) {
            if (type == 'f') {
                if (bits == -1) {
                    bits = (value != 0) ? 1 : 0;
                    bitIndex = 1;
                } else {
                    if (value != 0) bits |= 1 << bitIndex;
                    bitIndex++;
                    if (bitIndex == 8) flushBits();
                }
            } else {
                flushBits();
                switch (type) {
                    case 'B' -> out.write(value & 0xFF);
                    case 'H' -> { out.write((value >> 8) & 0xFF); out.write(value & 0xFF); }
                    case 'I' -> {
                        out.write((value >> 24) & 0xFF);
                        out.write((value >> 16) & 0xFF);
                        out.write((value >> 8) & 0xFF);
                        out.write(value & 0xFF);
                    }
                    default -> throw new IllegalArgumentException("Unknown format: " + type);
                }
            }
        }

        public byte[] finish() {
            flushBits();
            return out.toByteArray();
        }

        private void flushBits() {
            if (bits == -1) return;
            out.write(bits);
            bits = -1;
            bitIndex = 0;
        }
    }

    public static class Unpacker {
        private final byte[] data;
        private final int startOffset;
        private int idx;
        private int bitIndex = 0;

        Unpacker(byte[] data, int offset) {
            this.data = data;
            this.startOffset = offset;
            this.idx = offset;
        }

        public int read(char type) {
            if (type == 'f') {
                int bit = (1 << bitIndex) & (data[idx] & 0xFF);
                int value = bit > 0 ? 1 : 0;
                bitIndex++;
                if (bitIndex == 8) { idx++; bitIndex = 0; }
                return value;
            } else {
                if (bitIndex != 0) { idx++; bitIndex = 0; }
                return switch (type) {
                    case 'B' -> data[idx++] & 0xFF;
                    case 'H' -> {
                        int v = ((data[idx] & 0xFF) << 8) | (data[idx + 1] & 0xFF);
                        idx += 2;
                        yield v;
                    }
                    case 'I' -> {
                        int v = ((data[idx] & 0xFF) << 24) | ((data[idx + 1] & 0xFF) << 16)
                              | ((data[idx + 2] & 0xFF) << 8) | (data[idx + 3] & 0xFF);
                        idx += 4;
                        yield v;
                    }
                    default -> throw new IllegalArgumentException("Unknown format: " + type);
                };
            }
        }

        public int finish() {
            if (bitIndex != 0) { idx++; }
            return idx - startOffset;
        }
    }

    private BinaryPacker() {}
}

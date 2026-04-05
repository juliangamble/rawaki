package org.rawaki.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StructTest {

    // ── pack / unpack convenience ─────────────────────────────────────────

    @Test
    void packByte() {
        byte[] data = Struct.pack("B", 0xAB);
        assertEquals(1, data.length);
        assertEquals((byte) 0xAB, data[0]);
    }

    @Test
    void packUint16BigEndian() {
        byte[] data = Struct.pack("H", 0x1234);
        assertEquals(2, data.length);
        assertEquals((byte) 0x12, data[0]);
        assertEquals((byte) 0x34, data[1]);
    }

    @Test
    void packUint32BigEndian() {
        byte[] data = Struct.pack("I", 0x12345678);
        assertEquals(4, data.length);
        assertEquals((byte) 0x12, data[0]);
        assertEquals((byte) 0x34, data[1]);
        assertEquals((byte) 0x56, data[2]);
        assertEquals((byte) 0x78, data[3]);
    }

    @Test
    void packMultipleValues() {
        byte[] data = Struct.pack("BHI", 1, 2, 3);
        assertEquals(7, data.length);
    }

    @Test
    void unpackByte() {
        byte[] data = { 42 };
        var result = Struct.unpack("B", data);
        assertEquals(42, result.values()[0]);
        assertEquals(1, result.bytesRead());
    }

    @Test
    void unpackUint16() {
        byte[] data = { 0x01, (byte) 0x00 };
        var result = Struct.unpack("H", data);
        assertEquals(256, result.values()[0]);
        assertEquals(2, result.bytesRead());
    }

    @Test
    void unpackUint32() {
        byte[] data = { 0x00, 0x00, 0x01, 0x00 };
        var result = Struct.unpack("I", data);
        assertEquals(256, result.values()[0]);
        assertEquals(4, result.bytesRead());
    }

    @Test
    void packUnpackRoundTripByte() {
        byte[] data = Struct.pack("B", 255);
        var result = Struct.unpack("B", data);
        assertEquals(255, result.values()[0]);
    }

    @Test
    void packUnpackRoundTripUint16() {
        byte[] data = Struct.pack("H", 65535);
        var result = Struct.unpack("H", data);
        assertEquals(65535, result.values()[0]);
    }

    @Test
    void packUnpackRoundTripUint32() {
        byte[] data = Struct.pack("I", 0x7FFFFFFF);
        var result = Struct.unpack("I", data);
        assertEquals(0x7FFFFFFF, result.values()[0]);
    }

    @Test
    void packUnpackMixed() {
        byte[] data = Struct.pack("BHB", 10, 5000, 20);
        var result = Struct.unpack("BHB", data);
        assertEquals(10, result.values()[0]);
        assertEquals(5000, result.values()[1]);
        assertEquals(20, result.values()[2]);
        assertEquals(4, result.bytesRead());
    }

    @Test
    void unpackWithOffset() {
        byte[] data = { 0x00, 0x00, 42 };
        var result = Struct.unpack("B", data, 2);
        assertEquals(42, result.values()[0]);
        assertEquals(1, result.bytesRead());
    }

    // ── bit fields ────────────────────────────────────────────────────────

    @Test
    void packSingleBitTrue() {
        byte[] data = Struct.pack("f", 1);
        assertEquals(1, data.length);
        assertEquals(1, data[0] & 0x01);
    }

    @Test
    void packSingleBitFalse() {
        byte[] data = Struct.pack("f", 0);
        assertEquals(1, data.length);
        assertEquals(0, data[0]);
    }

    @Test
    void packMultipleBits() {
        // true, false, true = bits 0,2 set = 0b00000101 = 5
        byte[] data = Struct.pack("fff", 1, 0, 1);
        assertEquals(1, data.length);
        assertEquals(5, data[0] & 0xFF);
    }

    @Test
    void packEightBitsFillsOneByte() {
        byte[] data = Struct.pack("ffffffff", 1, 1, 1, 1, 1, 1, 1, 1);
        assertEquals(1, data.length);
        assertEquals(0xFF, data[0] & 0xFF);
    }

    @Test
    void packNineBitsSpillsToSecondByte() {
        byte[] data = Struct.pack("fffffffff", 1, 1, 1, 1, 1, 1, 1, 1, 1);
        assertEquals(2, data.length);
        assertEquals(0xFF, data[0] & 0xFF);
        assertEquals(1, data[1] & 0xFF);
    }

    @Test
    void unpackBitFields() {
        byte[] data = { 5 }; // bits 0 and 2 set
        var result = Struct.unpack("fff", data);
        assertEquals(1, result.values()[0]); // bit 0
        assertEquals(0, result.values()[1]); // bit 1
        assertEquals(1, result.values()[2]); // bit 2
    }

    @Test
    void packUnpackBitFieldRoundTrip() {
        byte[] data = Struct.pack("ffffff", 1, 0, 1, 1, 0, 0);
        var result = Struct.unpack("ffffff", data);
        assertEquals(1, result.values()[0]);
        assertEquals(0, result.values()[1]);
        assertEquals(1, result.values()[2]);
        assertEquals(1, result.values()[3]);
        assertEquals(0, result.values()[4]);
        assertEquals(0, result.values()[5]);
    }

    // ── mixed bytes and bit fields ────────────────────────────────────────

    @Test
    void packBytesThenBits() {
        byte[] data = Struct.pack("Bff", 42, 1, 0);
        assertEquals(2, data.length);
        assertEquals(42, data[0] & 0xFF);
        assertEquals(1, data[1] & 0xFF); // bit 0 set
    }

    @Test
    void packBitsThenBytes() {
        byte[] data = Struct.pack("ffB", 1, 0, 42);
        assertEquals(2, data.length);
        assertEquals(1, data[0] & 0xFF); // bit 0 set
        assertEquals(42, data[1] & 0xFF);
    }

    @Test
    void packUnpackMixedBytesAndBits() {
        byte[] data = Struct.pack("BffHffB", 10, 1, 0, 500, 0, 1, 20);
        var result = Struct.unpack("BffHffB", data);
        assertEquals(10, result.values()[0]);
        assertEquals(1, result.values()[1]);
        assertEquals(0, result.values()[2]);
        assertEquals(500, result.values()[3]);
        assertEquals(0, result.values()[4]);
        assertEquals(1, result.values()[5]);
        assertEquals(20, result.values()[6]);
    }

    // ── streaming packer ──────────────────────────────────────────────────

    @Test
    void streamingPackerBasic() {
        var p = Struct.buildPacker();
        p.write('B', 1);
        p.write('H', 2);
        byte[] data = p.finish();
        assertEquals(3, data.length);
    }

    @Test
    void streamingUnpackerBasic() {
        byte[] data = Struct.pack("BH", 1, 2);
        var u = Struct.buildUnpacker(data);
        assertEquals(1, u.read('B'));
        assertEquals(2, u.read('H'));
        assertEquals(3, u.finish());
    }

    // ── error handling ────────────────────────────────────────────────────

    @Test
    void packUnknownFormatThrows() {
        var p = Struct.buildPacker();
        assertThrows(IllegalArgumentException.class, () -> p.write('Z', 0));
    }

    @Test
    void unpackUnknownFormatThrows() {
        byte[] data = { 0 };
        var u = Struct.buildUnpacker(data);
        assertThrows(IllegalArgumentException.class, () -> u.read('Z'));
    }
}

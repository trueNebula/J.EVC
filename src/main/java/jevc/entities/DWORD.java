package jevc.entities;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DWORD {
    private final WORD high;
    private final WORD low;

    public DWORD (short high, short low) {
        this.low = new WORD(high);
        this.high = new WORD(low);
    }

    public DWORD (WORD high, WORD low) {
        this.high = high;
        this.low = low;
    }

    public DWORD (int value) {
        this.low = new WORD((short) ((value >> 16) & 0xFFFF));
        this.high = new WORD((short) (value & 0xFFFF));
    }

    public DWORD (String value) {
        byte[] byteArray = value.getBytes(StandardCharsets.US_ASCII);
        this.high = new WORD(byteArray[0], byteArray[1]);
        this.low = new WORD(byteArray[2], byteArray[3]);

    }

    public DWORD (byte[] value) {
        this.high = new WORD(value[0], value[1]);
        this.low = new WORD(value[2], value[3]);
    }

    public byte[] byteValue() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put(this.high.byteValue());
        buffer.put(this.low.byteValue());
        return buffer.array();
    }

    public int intValue() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put(this.high.byteValue());
        buffer.put(this.low.byteValue());
        return buffer.getInt(0);
    }

    @Override
    public String toString() {
        return high + String.valueOf(low);
    }
}

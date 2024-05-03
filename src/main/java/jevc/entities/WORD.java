package jevc.entities;

import java.nio.ByteBuffer;

public class WORD {
    private byte high;
    private byte low;

    public WORD (short value) {
        this.low = (byte) ((value >> 8) & 0xFF);
        this.high = (byte) (value & 0xFF);
    }

    public WORD (byte high, byte low) {
        this.high = high;
        this.low = low;
    }

    public byte[] byteValue() {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put(this.high);
        buffer.put(this.low);
        return buffer.array();
    }

    public short shortValue() {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.put(this.high);
        buffer.put(this.low);
        return buffer.getShort(0);
    }

    @Override
    public String toString() {
        return high + String.valueOf(low);
    }
}

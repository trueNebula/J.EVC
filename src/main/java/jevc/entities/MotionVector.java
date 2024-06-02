package jevc.entities;

import java.nio.ByteBuffer;

public class MotionVector {
    private byte byteVal;

    public MotionVector(int x, int y) {
        // [x, y] are the values of the motion vector, represented by the delta of block positions
        // x,y are in the range [-8 * searchRadius, 8 * searchRadius], with searchRadius being defined in BlockBuffer.java
        // We divide them by 8 to get the delta in the block space
        // x,y should fit in 4 bits each, so searchRadius can be at most 15
        // Concatenate the bits of x and y into a single byte such that x occupies the first 4 bits and y occupies the last 4 bits
        this.byteVal = (byte) (((x / 8) & 0x0F) << 4 | ((y / 8) & 0x0F));
    }

    public MotionVector(byte byteVal) {
        this.byteVal = byteVal;
    }

    public byte byteValue() {
       return this.byteVal;
    }

    public int[] decompress() {
        // byteVal is represented in binary as 4 bits of x and 4 bits of y
        // We need to decompress it to get the original x and y values
        int x = (byteVal >> 4) & 0x0F;
        int y = byteVal & 0x0F;

        if (x > 7) x -= 16;
        if (y > 7) y -= 16;

        return new int[]{x, y};
    }
}

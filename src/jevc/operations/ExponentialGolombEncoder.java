package jevc.operations;

import jevc.entities.Block;
import jevc.entities.ExponentialGolombBlock;

import java.util.ArrayList;

public class ExponentialGolombEncoder {
    public static final int[] ZIGZAG_ORDER = {
            0,  1,  8,  16,  9,  2,  3, 10,
            17, 24, 32, 25, 18, 11,  4,  5,
            12, 19, 26, 33, 40, 48, 41, 34,
            27, 20, 13,  6,  7, 14, 21, 28,
            35, 42, 49, 56, 57, 50, 43, 36,
            29, 22, 15, 23, 30, 37, 44, 51,
            58, 59, 52, 45, 38, 31, 39, 46,
            53, 60, 61, 54, 47, 55, 62, 63
    };

    public int encode(int value) {
        final int unsignedMapping = value < 0 ? -2 * value  : 2 * value - 1;
        final String binary = Integer.toBinaryString(unsignedMapping + 1);
        final int length = binary.length();
        final String encodedValue = "0".repeat(length - 1) + binary;

        return Integer.parseInt(encodedValue, 2);
    }

    public ExponentialGolombBlock encodeBlock(Block block) {
        ArrayList<Integer> encodedBlock = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            int line = ZIGZAG_ORDER[i] / 8;
            int col = ZIGZAG_ORDER[i] % 8;
            encodedBlock.add(encode(block.getData()[line][col]));
        }

        return new ExponentialGolombBlock(encodedBlock);
    }

    public int[] encodeMotionVector(int[] motionVector) {
        int[] encodedMotionVector = new int[2];
        encodedMotionVector[0] = encode(motionVector[0]);
        encodedMotionVector[1] = encode(motionVector[1]);

        return encodedMotionVector;
    }

}

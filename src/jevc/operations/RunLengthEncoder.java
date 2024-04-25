package jevc.operations;

import jevc.entities.Block;
import jevc.entities.RunLength;
import jevc.entities.RunLengthBlock;

import java.util.ArrayList;
import java.util.Arrays;

public class RunLengthEncoder {
    private final int[] lastDCvalues = {0, 0, 0}; // [0] - last Y value; [1] - last Cb value; [2] - last Cr value
    public static final int[] ZIGZAG_ORDER = {
            0,  1,  8,  16,  9,  2,  3, 10,
            17, 24, 32, 25, 18, 11,  4,  5,
            12, 19, 26, 33, 40, 48, 41, 34,
            27, 20, 13,  6,  7, 14, 21, 28,
            35, 42, 49, 56, 57, 50, 43, 36,
            29, 22, 15, 23, 30, 37, 44, 51,
            58, 59, 52, 45, 38, 31, 39, 46,
            53, 60, 61, 54, 47, 55, 62, 63};

    public RunLengthBlock encode(Block block) {
        int[][] blockdata = block.getData();
        int type = switch (block.getType()) {
            case 'Y' -> 0;
            case 'U' -> 1;
            case 'V' -> 2;
            default -> -1;
        };
        ArrayList<RunLength> runlengthElements = new ArrayList<>();

        // encode the DC coefficient
        int amplitude = blockdata[0][0]-lastDCvalues[type];
        lastDCvalues[type] = blockdata[0][0]; // save current DC value
        int size = getSizeForAmplitude(amplitude);
        if (amplitude==0) {
            // IMPORTANT!!!: The size of a DC amplitude equal to ZERO is 0. But the size of a
            // AC amplitude equal to zero is actually 1! See pag.93-94 of the JPEG standard.
            // Of course, if the amplitude of the AC coefficient is 0, this AC coefficient
            // will be encoded as a part of a runlength of another non-zero AC coefficient.
            size = 0;
        }

        RunLength rlElem = new RunLength(-1, size, amplitude);
        runlengthElements.add(rlElem);

        // encode AC coefficients
        int i = 1;
        int run = 0;
        while (i<64) {
            int line = ZIGZAG_ORDER[i] / 8;
            int col = ZIGZAG_ORDER[i] % 8;
            if (blockdata[line][col]==0) {
                run++;
            } else {
                rlElem = new RunLength(run, getSizeForAmplitude(blockdata[line][col]), blockdata[line][col]);
                runlengthElements.add(rlElem);
                run = 0;
            }
            i++;
        }
        if (run>0) {
            rlElem = new RunLength(0, 0, 0); // End-Of-Block symbol
            runlengthElements.add(rlElem);
        }

        return new RunLengthBlock(runlengthElements, block.getType());
    }

    private int getSizeForAmplitude(int amplitude) {
        int size = 0;
        if (amplitude == 0) return 1;
        
        amplitude = Math.abs(amplitude);
        while (amplitude>0) {
            amplitude >>= 1;
            size++;
        }
        return size;
    }

    public Block decode(RunLengthBlock rleBlock) {
        int[][] data = new int[8][8];
        RunLength runLength = rleBlock.getData().get(0);
        if (runLength.getRunlength() != -1) {
            System.err.println("RunLengthEncoder::decode() run length block " +
                    "doesn't start with DC coefficient!");
        }
        System.out.println("RunLengthEncoder::decode() Processing RunLengthBlock with size " +
                rleBlock.getSize());

        int type = switch (rleBlock.getType()) {
            case 'Y' -> 0;
            case 'U' -> 1;
            case 'V' -> 2;
            default -> -1;
        };
        data[0][0] = runLength.getAmplitude() + lastDCvalues[type];
        lastDCvalues[type] = data[0][0];
        rleBlock.getData().remove(0);

        int i = 1;
        for(RunLength rlb: rleBlock.getData()) {
            int run = rlb.getRunlength();
            System.out.println("RunLength ["+rlb.getRunlength()+","+rlb.getSize()+","+rlb.getAmplitude()+"] i="+i);
            while (run>0) {
                data[ZIGZAG_ORDER[i]/8][ZIGZAG_ORDER[i]%8] = 0;
                run--;
                i++;
            }
            data[ZIGZAG_ORDER[i]/8][ZIGZAG_ORDER[i]%8] = rlb.getAmplitude();
            i++;
        }
        RunLength rlb = rleBlock.getData().get(rleBlock.getSize()-1);
        if ((i<64) && ((rlb.getRunlength()!=0) || (rlb.getSize()!=0) || (rlb.getAmplitude()!=0))) {
            System.err.println("RunLengthEncoder::decode() less than 64 coefficients and no End-Of-Block!");
        }
        while (i<64) {
            data[ZIGZAG_ORDER[i]/8][ZIGZAG_ORDER[i]%8] = 0;
            i++;
        }

        return new Block(data, rleBlock.getType());
    }
}

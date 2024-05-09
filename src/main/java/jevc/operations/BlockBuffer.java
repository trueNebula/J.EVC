package jevc.operations;

import jevc.entities.Block;

import java.util.HashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static jevc.entities.Globals.MAX_WIDTH;
import static jevc.entities.Globals.MAX_HEIGHT;

public class BlockBuffer {
    private final HashMap<Character, Block[][]> buffer;
    // searchRadius = 3 => max search area = 7x7 centered
    private final int searchRadius = 3;

    public BlockBuffer() {
        buffer = new HashMap<>();
        buffer.put('Y', new Block[MAX_HEIGHT / 8 + 1][MAX_WIDTH / 8 + 1]);
        buffer.put('U', new Block[MAX_HEIGHT / 8 + 1][MAX_WIDTH / 8 + 1]);
        buffer.put('V', new Block[MAX_HEIGHT / 8 + 1][MAX_WIDTH / 8 + 1]);
    }

    public void save(Block block) {
        buffer.get(block.getType())[block.getPosY() / 8][block.getPosX() / 8] = block;
    }

    // This might not be needed as we can just overwrite the buffers
//    public void flush() {
//        blockBufferY.clear();
//        blockBufferU.clear();
//        blockBufferV.clear();
//    }

    public Block getSimilarBlock(Block block) {
        int y = block.getPosY();
        int x = block.getPosX();
//        System.out.println("Current Block: [" + x + ", " + y + "]");
        double[][] MSEValues = new double[searchRadius * 2 + 1][searchRadius * 2 + 1];

        int top = y/8 - searchRadius;
        int bottom = y/8 + searchRadius;
        int left = x/8 - searchRadius;
        int right = x/8 + searchRadius;

        // check if block is on any edges or corners of the frame

        if (top < 0) top = 0;
        if (bottom >= MAX_HEIGHT / 8) bottom = MAX_HEIGHT / 8 - 1;
        if (left < 0) left = 0;
        if (right >= MAX_WIDTH / 8) right = MAX_WIDTH / 8 - 1;

        Block currentMinMSEBlock = buffer.get(block.getType())[y / 8][x / 8];
        double currentMinMSE = 99999999.9;

        // if not, we have a safe square around it to check
        for (int i = top; i <= bottom; i++) {
            for (int j = left; j <= right; j++) {
                double currentMSE = computeMeanSquaredError(block, buffer.get(block.getType())[i][j]);
                if (currentMSE < currentMinMSE) {
                    currentMinMSE = currentMSE;
                    currentMinMSEBlock = buffer.get(block.getType())[i][j];
                }
//                MSEValues[i][j] = currentMSE;
            }
        }

        // DEBUG
//        for (int i = top; i <= bottom; i++) {
//            for (int j = left; j <= right; j++) {
//                System.out.print(MSEValues[i][j] + " ");
//            }
//            System.out.println();
//        }
        return currentMinMSEBlock;
    }

    private double computeMeanSquaredError(Block A, Block B) {
        double MSE = 0.0;
        int[] aData = Stream.of(A.getData())
                .flatMapToInt(IntStream::of)
                .toArray();

        int[] bData = Stream.of(B.getData())
                .flatMapToInt(IntStream::of)
                .toArray();
        int len = aData.length;

        for (int i = 0; i < len; i++) {
            MSE += Math.pow((aData[i] - bData[i]), 2);
        }

        MSE /= len;

        return MSE;
    }
}

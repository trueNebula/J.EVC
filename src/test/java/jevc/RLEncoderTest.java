import jevc.entities.Block;
import jevc.entities.RunLength;
import jevc.entities.RunLengthBlock;
import jevc.operations.RunLengthEncoder;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;

public class RLEncoderTest extends TestCase {
    private RunLengthEncoder runLengthEncoder;
    public void testEncode() {
        runLengthEncoder = new RunLengthEncoder('P');
        int[][] data1 = new int[][] {
                {-63, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0,},
                {0, 0, 0, 0, 0, 0, 0, 0,},
                {0, 0, 0, 0, 0, 0, 0, 0,},
                {0, 0, 0, 0, 0, 0, 0, 0,},
                {0, 0, 0, 0, 0, 0, 0, 0,},
                {0, 0, 0, 0, 0, 0, 0, 0,},
                {0, 0, 0, 0, 0, 0, 0, 0,},
        };
        int[][] data2 = new int[][] {
                {-50, 1, 0, -1, 0, 0, 0, 0},
                {-2, -2, 1, -1, 0, 0, 0, 0},
                {-4, 1, 0, 0, 0, 0, 0, 0},
                {1, 1, -1, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0 ,0},
                {0, 0, 0, 0, 0, 0, 0, 0,},
                {0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0},
        };

        Block block1 = new Block(data1, 'Y');
        Block block2 = new Block(data2, 'Y');
        RunLengthBlock encodedData1 = runLengthEncoder.encode(block1);
        RunLengthBlock encodedData2 = runLengthEncoder.encode(block2);

        assertEquals(-63, encodedData1.getData().get(0).getAmplitude());
        assertEquals(6, encodedData1.getData().get(0).getSize());
        assertEquals(-1, encodedData1.getData().get(0).getRunlength());

        assertEquals(0, encodedData1.getData().get(1).getAmplitude());
        assertEquals(0, encodedData1.getData().get(1).getSize());
        assertEquals(0, encodedData1.getData().get(1).getRunlength());

        RunLengthBlock assumedResult = new RunLengthBlock();
        RunLength rl0 = new RunLength(-1, 6, -50);
        RunLength rl1 = new RunLength(0, 1, 1);
        RunLength rl2 = new RunLength(0, 2, -2);
        RunLength rl3 = new RunLength(0, 3, -4);
        RunLength rl4 = new RunLength(0, 2, -2);
        RunLength rl5 = new RunLength(1, 1, -1);
        RunLength rl6 = new RunLength(0, 1, 1);
        RunLength rl7 = new RunLength(0, 1, 1);
        RunLength rl8 = new RunLength(0, 1, 1);
        RunLength rl9 = new RunLength(1, 1, 1);
        RunLength rl10 = new RunLength(1, 1, -1);
        RunLength rl11 = new RunLength(4, 1, -1);
        RunLength rl12 = new RunLength(0, 0, 0);
        ArrayList<RunLength> assumedData = new ArrayList<>(Arrays.asList(rl0, rl1, rl2, rl3, rl4, rl5, rl6, rl7, rl8, rl9, rl10, rl11, rl12));
        assumedResult.setData(assumedData);

        for (int i = 0; i < assumedResult.getSize(); i++) {
            assertEquals(assumedResult.getData().get(i).getAmplitude(), encodedData2.getData().get(i).getAmplitude());
            assertEquals(assumedResult.getData().get(i).getSize(), encodedData2.getData().get(i).getSize());
            assertEquals(assumedResult.getData().get(i).getRunlength(), encodedData2.getData().get(i).getRunlength());
        }

    }

    public void testDecode() {
        runLengthEncoder = new RunLengthEncoder();
        // {-63, 0, 0, 0, 0, 0, 0, 0}
        // {0, 0, 0, 0, 0, 0, 0, 0}
        // {0, 0, 0, 0, 0, 0, 0, 0}
        // {0, 0, 0, 0, 0, 0, 0, 0}
        // {0, 0, 0, 0, 0, 0, 0, 0}
        // {0, 0, 0, 0, 0, 0, 0, 0}
        // {0, 0, 0, 0, 0, 0, 0, 0}
        // {0, 0, 0, 0, 0, 0, 0, 0}

        RunLengthBlock rleBlock = new RunLengthBlock();
        RunLength rl0 = new RunLength(-1, 6, -63);
        RunLength rl1 = new RunLength(0, 0, 0);
        ArrayList<RunLength> rleData = new ArrayList<>(Arrays.asList(rl0, rl1));
        rleBlock.setData(rleData);
        rleBlock.setType('Y');

        Block block1 = runLengthEncoder.decode(rleBlock);
        assertEquals(-63, block1.getData()[0][0]);
        assertEquals(0, block1.getData()[0][1]);
        assertEquals(8, block1.getData().length);
        assertEquals(8, block1.getData()[0].length);
    }
}
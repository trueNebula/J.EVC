import jevc.entities.*;
import jevc.operations.HuffmanEncoder;
import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HuffmanEncoderTest extends TestCase {
    private HuffmanEncoder huffmanEncoder;

    public void testEncodeBlock() {
        RunLengthBlock block = new RunLengthBlock();
        block.setType('Y');
        // [-1, 0, 0] [0, 1, 1] [0, 2, -2] [0, 3, -4] [0, 2, -2] [1, 1, -1] [0, 1, 1] [0, 1, 1] [0, 1, 1] [1, 1, 1] [1, 1, -1] [4, 1, -1] [0, 0, 0]
        block.setData(new ArrayList<>(List.of(
                new RunLength(-1, 0, 0),
                new RunLength(0, 1, 1),
                new RunLength(0, 2, -2),
                new RunLength(0, 3, -4),
                new RunLength(0, 2, -2),
                new RunLength(1, 1, -1),
                new RunLength(0, 1, 1),
                new RunLength(0, 1, 1),
                new RunLength(0, 1, 1),
                new RunLength(1, 1, 1),
                new RunLength(1, 1, -1),
                new RunLength(4, 1, -1),
                new RunLength(0, 0, 0)
        )));
        block.print();

        InternalFrameBuffer outputBuffer = new InternalFrameBuffer();

        huffmanEncoder = new HuffmanEncoder();
        huffmanEncoder.encode(outputBuffer, block);
        byte[] encodedData = outputBuffer.dumpStreamToBuffer();
        System.out.println("Encoded data: " + Arrays.toString(encodedData));

        ByteBuffer dataBuffer = ByteBuffer.allocate(6);
        dataBuffer.put((byte) 10);
        dataBuffer.put((byte) -58);
        dataBuffer.put((byte) -72);
        dataBuffer.put((byte) 36);
        dataBuffer.put((byte) -25);
        dataBuffer.put((byte) 29);
        byte[] assumedResult = dataBuffer.array();
        assertEquals(assumedResult.length, encodedData.length);
        for (int i = 0; i < assumedResult.length; i++) {
            assertEquals(assumedResult[i], encodedData[i]);
        }
    }

    public void testDecodeBlock() {
        ByteBuffer dataBuffer = ByteBuffer.allocate(9);
        dataBuffer.put((byte) 51);
        dataBuffer.put((byte) -128);
        dataBuffer.put((byte) -7);
        dataBuffer.put((byte) 63);
        dataBuffer.put((byte) -42);
        dataBuffer.put((byte) 33);
        dataBuffer.put((byte) -7);
        dataBuffer.put((byte) 127);
        dataBuffer.put((byte) -113);
        byte[] data = dataBuffer.array();

        System.out.println("Input data: " + Arrays.toString(data));

        huffmanEncoder = new HuffmanEncoder();
        huffmanEncoder.setSamplingFactors(new int[]{1, 1, 1}, new int[]{1, 1, 1});
        huffmanEncoder.resetIndices();
        RunLengthBlock block = new RunLengthBlock();
        block.setType('Y');
        ArrayList<RunLengthBlock> blocks = huffmanEncoder.decode(data, Globals.SAMPLING, Globals.MAX_WIDTH, Globals.MAX_HEIGHT);
        for (RunLengthBlock b : blocks) {
            b.print();
        }

        RunLengthBlock assumedResult = new RunLengthBlock();
        assumedResult.setType('Y');
        assumedResult.setData(new ArrayList<>(List.of(
                new RunLength(-1, 6, -63)
        )));
//        assertEquals(assumedResult.getData().size(), block.getData().size());

        ByteBuffer dataBuffer2 = ByteBuffer.allocate(1);
        dataBuffer2.put((byte) -92);
        byte[] data2 = dataBuffer2.array();

        System.out.println("Input data: " + Arrays.toString(data2));

        huffmanEncoder = new HuffmanEncoder();
        huffmanEncoder.setSamplingFactors(new int[]{1, 1, 1}, new int[]{1, 1, 1});
        huffmanEncoder.resetIndices();
        block = new RunLengthBlock();
        block.setType('Y');
        blocks = huffmanEncoder.decode(data2, Globals.SAMPLING, Globals.MAX_WIDTH, Globals.MAX_HEIGHT);
        for (RunLengthBlock b : blocks) {
            b.print();
        }
    }
}

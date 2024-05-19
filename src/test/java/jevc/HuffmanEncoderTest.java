import jevc.entities.RunLength;
import jevc.entities.RunLengthBlock;
import jevc.operations.HuffmanEncoder;
import junit.framework.TestCase;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HuffmanEncoderTest extends TestCase {
    private HuffmanEncoder huffmanEncoder;

    public void testDecodeBlock() {
        ByteBuffer dataBuffer = ByteBuffer.allocate(1);
        dataBuffer.put((byte) -32);
        byte[] data = dataBuffer.array();

        System.out.println("Input data: " + Arrays.toString(data));

        huffmanEncoder = new HuffmanEncoder();
        huffmanEncoder.setSamplingFactors(new int[]{1, 1, 1}, new int[]{1, 1, 1});
        huffmanEncoder.resetIndices();
        RunLengthBlock block = new RunLengthBlock();
        block.setType('Y');
        huffmanEncoder.decodeBlock(data, block);
        block.print();

        RunLengthBlock assumedResult = new RunLengthBlock();
        assumedResult.setType('Y');
        assumedResult.setData(new ArrayList<>(List.of(
                new RunLength(-1, 6, -63)
        )));
        assertEquals(assumedResult.getData().size(), block.getData().size());
    }
}

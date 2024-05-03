package jevc.operations;

import jevc.entities.Block;

import java.util.ArrayList;

public class BlockBuffer {
    private ArrayList<Block> blockBuffer;

    public BlockBuffer() {
        blockBuffer = new ArrayList<>();
    }

    public void save(Block block) {
        blockBuffer.add(block);
    }

    public void flush() {
        blockBuffer.clear();
    }

    public Block getSimilarBlock(Block block) {
        return null;
    }
}

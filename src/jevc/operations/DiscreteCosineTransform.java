package jevc.operations;

import jevc.entities.Block;

public class DiscreteCosineTransform {
    public void forward(Block block) {
        double val;
        int blockdata[][];
        int data[][] = new int[Block.BLOCKSIZE][Block.BLOCKSIZE];

        prepareBeforeDCT(block);
        for(int u=0; u < Block.BLOCKSIZE; u++) {
            for (int v=0; v < Block.BLOCKSIZE; v++) {
                val = 0;
                blockdata = block.getData();
                for (int i = 0; i < Block.BLOCKSIZE; i++) {
                    for (int j = 0; j < Block.BLOCKSIZE; j++) {
                        val += blockdata[i][j] * Math.cos((double) ((2 * i + 1) * u * Math.PI / 16.0)) *
                                                 Math.cos((double) ((2 * j + 1) * v * Math.PI / 16.0));
                    }
                }
                val *= (1 / 4.0) * alpha(u) * alpha(v);
                data[u][v] = (int) val;
            }
        }
        block.setData(data);
    }

    public void inverse(Block block) {
        double val;
        int blockdata[][];
        int data[][] = new int[Block.BLOCKSIZE][Block.BLOCKSIZE];
        for(int i=0; i < Block.BLOCKSIZE; i++) {
            for (int j=0; j < Block.BLOCKSIZE; j++) {
                val = 0;
                blockdata = block.getData();
                for (int u = 0; u < Block.BLOCKSIZE; u++) {
                    for (int v = 0; v < Block.BLOCKSIZE; v++) {
                        val += alpha(u) * alpha(v) * blockdata[u][v] *
                                    Math.cos((double) ((2 * i + 1) * u * Math.PI / 16.0)) *
                                    Math.cos((double) ((2 * j + 1) * v * Math.PI / 16.0));
                    }
                }
                val *= 1 / 4.0;
                data[i][j] = (int) Math.rint(val);
            }
        }
        block.setData(data);
        prepareAfterIDCT(block);
    }

    private double alpha(int u) {
        if (u==0)
            return 1/Math.sqrt(2);
        else
            return 1;
    }
    
    private void prepareBeforeDCT(Block block) {
        int blockdata[][] = block.getData();
        for(int i=0; i<Block.BLOCKSIZE; i++) {
            for(int j=0; j<Block.BLOCKSIZE; j++) {
                blockdata[i][j] -= 128;
            }
        }
    }
    private void prepareAfterIDCT(Block block) {
        int blockdata[][] = block.getData();
        for(int i=0; i<Block.BLOCKSIZE; i++) {
            for(int j=0; j<Block.BLOCKSIZE; j++) {
                blockdata[i][j] += 128;
            }
        }
    }

}

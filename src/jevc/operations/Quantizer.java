package jevc.operations;

import jevc.entities.Block;

public class Quantizer {
    /* The normative quantization matrix for luminance blocks. */
    public int[] QUANTUM_LUMINANCE = {
            16,  11,  10,  16,  24,  40,  51,  61,
            12,  12,  14,  19,  26,  58,  60,  55,
            14,  13,  16,  24,  40,  57,  69,  56,
            14,  17,  22,  29,  51,  87,  80,  62,
            18,  22,  37,  56,  68, 109, 103,  77,
            24,  35,  55,  64,  81, 104, 113,  92,
            49,  64,  78,  87, 103, 121, 120, 101,
            72,  92,  95,  98, 112, 100, 103,  99};

    /* The normative quantization matrix for chrominance blocks. */
    public int[] QUANTUM_CHROMINANCE = {
            17, 18, 24, 47, 99, 99, 99, 99,
            18, 21, 26, 66, 99, 99, 99, 99,
            24, 26, 56, 99, 99, 99, 99, 99,
            47, 66, 99, 99, 99, 99, 99, 99,
            99, 99, 99, 99, 99, 99, 99, 99,
            99, 99, 99, 99, 99, 99, 99, 99,
            99, 99, 99, 99, 99, 99, 99, 99,
            99, 99, 99, 99, 99, 99, 99, 99};


    public Quantizer() { }

    public Quantizer(int qualityFactor) {
        /*int val;
        double S = 0;
        if (qualityFactor < 50) S = 5000 / qualityFactor;
        else S = 200 - 2 * qualityFactor;
        if (qualityFactor < 1 || qualityFactor >= 100) {
            System.err.println("The quality factor specified is out of the [1..100) range.");
        } else {
            for (int i = 0; i < 64; i++) {
                // compute the luminance coefficients
                val = (int) (QUANTUM_LUMINANCE[i] * S + 50) / 100;
                val = (1 > val) ? 1 : val;
                QUANTUM_LUMINANCE[i] = 255 < val ? 255 : val;

                // compute the chrominance coefficients
                val = (int) (QUANTUM_CHROMINANCE[i] * S + 50) / 100;
                val = (1 > val) ? 1 : val;
                QUANTUM_CHROMINANCE[i] = 255 < val ? 255 : val;
            }
        }*/
    }

    public void quantize(Block block) {
        int blockdata[][] = block.getData();
        if (block.getType()=='Y') {
            for (int i = 0; i < Block.BLOCKSIZE; i++) {
                for (int j = 0; j < Block.BLOCKSIZE; j++) {
                    blockdata[i][j] /= QUANTUM_LUMINANCE[i * 8 + j];
                    //System.out.print(blockdata[i][j]+ " ");
                }
                //System.out.println();
            }
        } else {
            for (int i = 0; i < Block.BLOCKSIZE; i++) {
                for (int j = 0; j < Block.BLOCKSIZE; j++) {
                    blockdata[i][j] /= QUANTUM_CHROMINANCE[i * 8 + j];
                }
            }
        }
    }

    public void dequantize(Block block) {
        int blockdata[][] = block.getData();
        if (block.getType()=='Y') {
            for (int i = 0; i < Block.BLOCKSIZE; i++) {
                for (int j = 0; j < Block.BLOCKSIZE; j++) {
                    blockdata[i][j] *= QUANTUM_LUMINANCE[i * 8 + j];
                }
            }
        } else {
            for (int i = 0; i < Block.BLOCKSIZE; i++) {
                for (int j = 0; j < Block.BLOCKSIZE; j++) {
                    blockdata[i][j] *= QUANTUM_CHROMINANCE[i * 8 + j];
                }
            }
        }
    }

}

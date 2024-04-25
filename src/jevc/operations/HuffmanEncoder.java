package jevc.operations;

import jevc.entities.RunLength;
import jevc.entities.RunLengthBlock;
import jevc.entities.YCbCrImage;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/*
 * JPEG entropy encoding of an 8x8 block works in the following way:
 * 1. The DC coefficient is predictively encoded (i.e. the current DC is predicted
 * from the DC of the previous block) as:
 *          (SIZE), (AMPLITUDE)
 * where AMPLITUDE is the differential DC value (i.e. current DC minus the DC of
 * the previous block) represented in binary on a variable (minimum) number of bits
 * and SIZE is the Huffman codeword for the number of bits used in the AMPLITUDE binary
 * representation (i.e. so the actual size/length in bits of the AMPLITUDE is the
 * symbol and we encode it as a Huffman codeword).
 *
 * 2. A single runlength of AC coefficients (RUN, AMPLITUDE) is encoded in the
 * following way:
 *          (RUN,SIZE), (AMPLITUDE)
 * where similarly as for DC, the AMPLITUDE is the actual value of the current non
 * zero AC coefficient represented in binary on a variable (minimum) number of bits,
 * RUN is the runlength (i.e. number of consecutive zeroes before this AC coefficient)
 * and SIZE is the number of bits used in the AMPLITUDE binary representation. The
 * (RUN, SIZE) pair is encoded as a variable bit Huffman codeword.
 *
 * The Huffman codewords for the (SIZE) symbol in DC coefficient representation and the
 * (RUN, SIZE) pair in the non-zero AC coefficient representation can be computed on
 * the fly by the JPEG encoder or the encoder can use the standard ones described in
 * the JPEG standard on pages 149-157. The JPEG standard describes 4 tables of Huffman
 * codewords:
 *      o for the luminance DC coefficients
 *      o for the chrominance DC coefficients
 *      o for the luminance AC coefficients
 *      o for the chrominance AC coefficients
 *
 * The JPEG standard computes each standard table of Huffman codewords (i.e. each of
 * those 4 standard Huffman tables mentioned above: luma DC, chroma DC, luma AC, chroma
 * AC) out of 2 vectors:
 *      o BITS[] - is a list of numbers/counts of Huffman codewords with a specific length
 *                 (in number of bits); BITS[i] is the number of Huffman codewords of
 *                 length "i" (i.e. codewords represented on "i" bits)
 *      o VALS[] - is the list of symbol values that will be associated with Huffman
 *                 codewords (i.e. the symbols that are to be encoded with Huffman codewords).
 *                 IMPORTANT: the symbol values are placed in this list in order of increasing
 *                 code length! Codeword lengths greater than 16 bits are not allowed.
 *
 * The JPEG standard describes on pages 50-53 algorithms for obtaining a Huffman codeword
 * table out of the corresponding BITS and VALS vectors.
 */
public class HuffmanEncoder {
    /* The following fields are used by the Huffman encoder */

    /**\
     * The following are lists of numbers of Huffman codewords with a specific length
     * (in number of bits). So, for example, BITS_DC_LUMINANCE[i] is the number of
     * Huffman codewords of length "i" (i.e. codewords represented on "i" bits). The
     * explanations for these values are in the JPEG standard on pages 158-159. Please
     * note that the value from index 0 of the following arrays is not used in the
     * JPEG encoding algorithm (i.e. there is no Huffman codeword with 0 bits).
     **/
    public static final int[] BITS_DC_LUMINANCE = {0x00, 0, 1, 5, 1, 1,1,1,1,1,0,0,0,0,0,0,0};
    public static final int[] BITS_DC_CHROMINANCE = {0x01,0,3,1,1,1,1,1,1,1,1,1,0,0,0,0,0};
    public static final int[] BITS_AC_LUMINANCE = {0x10,0,2,1,3,3,2,4,3,5,5,4,4,0,0,1,0x7d};
    public static final int[] BITS_AC_CHROMINANCE = {0x11,0,2,1,2,4,4,3,4,7,5,4,4,0,1,2,0x77};


    /*
     * The following are lists of symbol values that will be associated with Huffman
     * codewords (i.e. the symbols that are to be encoded with Huffman codewords).
     * IMPORTANT: the symbol values are placed in these lists in order of increasing
     * code length!. There are 4 such lists, corresponding to the 4 BITS lists from
     * above: for luma DC, for chroma DC, for luma AC and for chroma AC.
     * The explanations for these values are in the JPEG standard on pages 158-159.
     * Let's take an example:
     *      VALS_AC_LUMINANCE[0] = 0x01
     *      VALS_AC_LUMINANCE[1] = 0x02
     *      VALS_AC_LUMINANCE[2] = 0x03
     *      VALS_AC_LUMINANCE[3] = 0x00
     *      VALS_AC_LUMINANCE[4] = 0x04
     *      VALS_AC_LUMINANCE[4] = 0x11
     * You can see in the BITS_AC_LUMINANCE list above that: there are 0 Huffman
     * codewords of length 1, 2 Huffman codewords of length 2, 1 Huffman codewords
     * of length 3, 3 Huffman codewords of length 4, etc.
     * The above VALS_AC_LIMINANCE values (0x01, 0x02, 0x03, 0x00, 0x04) represent
     * the (RUN,SIZE) symbols: (0,1), (0,2), (0,3), (0,0)=End-Of-Block, (0,4), (1,1)
     * because in the Huffman codeword table:
     *         - the codeword for (0,1) has the length 2 (in bits)
     *         - the codeword for (0,2) has the length 2 (in bits)
     *         - the codeword for (0,3) has the length 3 (in bits)
     *         - the codeword for (0,0) has the length 4 (in bits)
     *         - the codeword for (0,4) has the length 4 (in bits)
     *         - the codeword for (1,1) has the length 4 (in bits)
     * ... hence the values BITS_AC_LUMINANCE[1:4] = {0, 2, 1, 3}
     *
     * The JPEG standard computes a Huffman codeword table out of BITS_DC_LUMINANCE
     * and VALS_DC_LUMINANCE, and similarly for the other lists (DC chroma, AC luma,
     * AC chroma).
     **/
    public static final int[] VALS_DC_LUMINANCE = {0,1,2,3,4,5,6,7,8,9,10,11};
    public static final int[] VALS_DC_CHROMINANCE = {0,1,2,3,4,5,6,7,8,9,10,11};
    public static final int[] VALS_AC_LUMINANCE = {
            0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12,
            0x21, 0x31, 0x41, 0x06, 0x13, 0x51, 0x61, 0x07,
            0x22, 0x71, 0x14, 0x32, 0x81, 0x91, 0xa1, 0x08,
            0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0,
            0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16,
            0x17, 0x18, 0x19, 0x1a, 0x25, 0x26, 0x27, 0x28,
            0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
            0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49,
            0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59,
            0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68, 0x69,
            0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79,
            0x7a, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89,
            0x8a, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98,
            0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6, 0xa7,
            0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6,
            0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5,
            0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4,
            0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2,
            0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea,
            0xf1, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
            0xf9, 0xfa};
    public static final int[] VALS_AC_CHROMINANCE = {
            0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21,
            0x31, 0x06, 0x12, 0x41, 0x51, 0x07, 0x61, 0x71,
            0x13, 0x22, 0x32, 0x81, 0x08, 0x14, 0x42, 0x91,
            0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0,
            0x15, 0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34,
            0xe1, 0x25, 0xf1, 0x17, 0x18, 0x19, 0x1a, 0x26,
            0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38,
            0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
            0x49, 0x4a, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58,
            0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67, 0x68,
            0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78,
            0x79, 0x7a, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87,
            0x88, 0x89, 0x8a, 0x92, 0x93, 0x94, 0x95, 0x96,
            0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5,
            0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4,
            0xb5, 0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3,
            0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca, 0xd2,
            0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda,
            0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9,
            0xea, 0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8,
            0xf9, 0xfa};

    private int prewriteBuffer;
    private int bitcount; // number of bits in the prewriteBuffer

    // the following fields are used by both encoder and decoder
    private int[][] DCLumaHuffmanTable; // Hufmann codewords and sizes for DC luma coefficients
    private int[][] DCChromaHuffmanTable; // Hufmann codewords and sizes for DC chroma coefficients
    private int[][] ACLumaHuffmanTable; // Hufmann codewords and sizes for AC luma coefficients
    private int[][] ACChromaHuffmanTable; // Hufmann codewords and sizes for AC chroma coefficients

    /* The following fields are used by the Huffman decoder (i.e. by the decode() method) */
    private int noOfBitsLeftInCurrentByte;  // the number of bits that are left (i.e. not consumed) in the current
                                    // byte of the `encodedBitstream` (i.e. encodedBitstream[i]);
                                    // these bits are always saved starting with the most-significant-bit
                                    // of encodedBitstream[i];
    private int idxCurrentByte; // the index of the current byte in encodedBitstream
    private int previousByte;   // the value of the byte encodedStream[idxCurrentByte-1]

    private int currentBlockTypeIdx = -1; // is an index into the EncodedBlocksOrder vector
    private int[] horizontalSamplingFactors, verticalSamplingFactors;
    private int noBlocksInMCU;

    public HuffmanEncoder() {
        initializeHuffmanTables();
        prewriteBuffer = 0;
        bitcount = 0;
    }

    private void initializeHuffmanTables() {
        DCLumaHuffmanTable = createHuffmanTable(BITS_DC_LUMINANCE, VALS_DC_LUMINANCE, true);
        DCChromaHuffmanTable = createHuffmanTable(BITS_DC_CHROMINANCE, VALS_DC_CHROMINANCE, true);
        ACLumaHuffmanTable = createHuffmanTable(BITS_AC_LUMINANCE, VALS_AC_LUMINANCE, false);
        ACChromaHuffmanTable = createHuffmanTable(BITS_AC_CHROMINANCE, VALS_AC_CHROMINANCE, false);

        //printHuffmanTables();
    }

    public void initializeHuffmanTables(int[] bits_dc_luminance, int[] bits_dc_chrominance,
                                        int[] bits_ac_luminance, int[] bits_ac_chrominance,
                                        int[] vals_dc_luminance, int[] vals_dc_chrominance,
                                        int[] vals_ac_luminance, int[] vals_ac_chrominance) {
        DCLumaHuffmanTable = createHuffmanTable(bits_dc_luminance, vals_dc_luminance, true);
        DCChromaHuffmanTable = createHuffmanTable(bits_dc_chrominance, vals_dc_chrominance, true);
        ACLumaHuffmanTable = createHuffmanTable(bits_ac_luminance, vals_ac_luminance, false);
        ACChromaHuffmanTable = createHuffmanTable(bits_ac_chrominance, vals_ac_chrominance, false);

        System.out.print("\nBITS_DC_LUMINANCE: ");
        for (int j : bits_dc_luminance) {
            System.out.print(j + ", ");
        }
        System.out.print("\nBITS_DC_CHROMINANCE: ");
        for (int j : bits_dc_chrominance) {
            System.out.print(j + ", ");
        }
        System.out.print("\nBITS_AC_LUMINANCE: ");
        for (int j : bits_ac_luminance) {
            System.out.print(j + ", ");
        }
        System.out.print("\nBITS_AC_CHROMINANCE: ");
        for (int j : bits_ac_chrominance) {
            System.out.print(j + ", ");
        }

        System.out.print("\nVALS_DC_LUMINANCE: ");
        for (int j : vals_dc_luminance) {
            System.out.print(j + ", ");
        }
        System.out.print("\nVALS_DC_CHROMINANCE: ");
        for (int j : vals_dc_chrominance) {
            System.out.print(j + ", ");
        }
        System.out.print("\nVALS_AC_LUMINANCE: ");
        for (int j : vals_ac_luminance) {
            System.out.print(j + ", ");
        }
        System.out.print("\nVALS_AC_CHROMINANCE: ");
        for (int j : vals_ac_chrominance) {
            System.out.print(j + ", ");
        }
        System.out.println();

        printHuffmanTables();
    }

    private int[][] createHuffmanTable(int[] bits, int[] vals, boolean DCtable) {
        // this is the implementation of algorithms from pages 50-53 in the JPEG standards
        int i, j, k;
        int lastk, si, code;
        int[] huffsize = new int[163];
        int[] huffcode = new int[163];
        int[][] HuffmanTable;
        k = 0;

        // build the vector of Huffman codeword sizes
        for(i=1; i<bits.length; i++) {
            for(j=1; j<=bits[i]; j++) {
                huffsize[k] = i;
                k++;
            }
        }
        huffsize[k] = 0;
        lastk = k;

        k = 0;
        code = 0;
        si = huffsize[0];

        // build the Huffman codes
        while (huffsize[k] != 0) {
            while (huffsize[k] == si) {
                huffcode[k++] = code;
                code++;
            }

            code = code << 1;
            si++;
        }

        if (DCtable)
            // create Huffman table for DC coefficients
            HuffmanTable = new int[12][2];
        else
            // create Huffman table for run-levels of AC coefficients
            HuffmanTable = new int[255][2];

        for (k=0; k<lastk; k++) {
            HuffmanTable[vals[k]][0] = huffcode[k];
            HuffmanTable[vals[k]][1] = huffsize[k];
        }

        return HuffmanTable;
    }

    private void printHuffmanTables() {
        System.out.println("The DC luminance Huffman codewords are:");
        printHuffmanTable(DCLumaHuffmanTable);
        System.out.println("The DC chrominance Huffman codewords are:");
        printHuffmanTable(DCChromaHuffmanTable);
        System.out.println("The AC luminance Huffman codewords are:");
        printHuffmanTable(ACLumaHuffmanTable);
        System.out.println("The AC chrominance Huffman codewords are:");
        printHuffmanTable(ACChromaHuffmanTable);
    }

    private void printHuffmanTable(int[][] table) {
        for(int i=0; i<table.length; i++) {
            System.out.print(i + ":[" + table[i][0] + "," + table[i][1] + "], ");
        }
        System.out.println();
    }

    public void setSamplingFactors(int[] horizSamplingFactors, int[] vertSamplingFactors) {
        horizontalSamplingFactors = new int[3];
        verticalSamplingFactors = new int[3];
        noBlocksInMCU = 0;
        for (int i=0; i<3; i++) {
            horizontalSamplingFactors[i] = horizSamplingFactors[i];
            verticalSamplingFactors[i] = vertSamplingFactors[i];
            noBlocksInMCU += horizontalSamplingFactors[i] * verticalSamplingFactors[i];
        }

        System.out.print("        	A MCU contains the following blocks in this order: ");
        for (int i=0; i<3; i++) {
            char blocktype = switch (i) {
                case 0 -> 'Y';
                case 1 -> 'U';
                case 2 -> 'V';
                default -> ' ';
            };
            for (int j=0; j<horizontalSamplingFactors[i]*verticalSamplingFactors[i]; j++) {
                System.out.print(blocktype + ", ");
            }
        }
        System.out.println();
    }

    public void encode (BufferedOutputStream outputStream, RunLengthBlock block) throws IOException {
        int[][] DCHuffmanTable;
        int[][] ACHuffmanTable;

        switch (block.getType()) {
            case 'Y' -> {
                DCHuffmanTable = DCLumaHuffmanTable;
                ACHuffmanTable = ACLumaHuffmanTable;
            }
            case 'U', 'V' -> {
                DCHuffmanTable = DCChromaHuffmanTable;
                ACHuffmanTable = ACChromaHuffmanTable;
            }
            default -> throw new IllegalArgumentException("HuffmanEncoder - Illegal RunLength block type.");
        }
        // encode the DC coefficient first
        RunLength rlElem = block.getData().get(0);
        // write the huffman code for (Size)
        writeToStream(outputStream, DCHuffmanTable[rlElem.getSize()][0], DCHuffmanTable[rlElem.getSize()][1]);
//        System.out.println("Write DCSize in outputstream: " + getBinary(DCHuffmanTable[rlElem.getSize()][0],
//                DCHuffmanTable[rlElem.getSize()][1]));
        // write the binary value of amplitude; if amplitude>=0, write it binary;
        // if amplitude<0 write amplitude-1 and without the sign bit
        int amplitude = rlElem.getAmplitude();
        if (amplitude<0) {
            amplitude = amplitude -1;
        }
        writeToStream(outputStream, amplitude, rlElem.getSize());
//        System.out.println("Write DCAmplitude in outputstream: " + getBinary(amplitude,
//                rlElem.getSize()));

        // encode the AC run lengths elements
        for (int i=1; i<block.getSize(); i++) {
            rlElem = block.getData().get(i);
            int run = rlElem.getRunlength();
            int size = rlElem.getSize();
            amplitude = rlElem.getAmplitude();
            while (run > 15) {
                // store Huffman code for a run of 16 zeroes
                writeToStream(outputStream, ACHuffmanTable[0xF0][0],ACHuffmanTable[0xF0][1]);
//                System.out.println("Write ZRL in outputstream: " + getBinary(ACHuffmanTable[0xF0][0],
//                        ACHuffmanTable[0xF0][1]));
                run -= 16;
            }
            int tableIndex = (run << 4) + size;
            // write the huffman code of (Run,Size)
            writeToStream(outputStream, ACHuffmanTable[tableIndex][0],ACHuffmanTable[tableIndex][1]);
//            System.out.println("Write (Run,Size) in outputstream: " + getBinary(ACHuffmanTable[tableIndex][0],
//                    ACHuffmanTable[tableIndex][1]));
            // write the binary value of amplitude (only if current elem is not End-Of-Block)
            if ((i<block.getSize()-1) || (run!=0 && size!=0 && amplitude!=0)) {
                if (amplitude<0) {
                    amplitude = amplitude -1;
                }
                writeToStream(outputStream, amplitude, size);
//                System.out.println("Write ACAmplitude in outputstream: " + getBinary(amplitude,
//                        size));
            }

        }
    }

    /*
     * The codewords are first buffered in `prewriteBuffer` and when we get at
     * least 8 bits in this buffer, they are written in the `outputStream`. `bitcount`
     * holds the number of bits in the `prewriteBuffer`.
     */
    private void writeToStream(BufferedOutputStream outputStream, int codeword, int nbits) throws IOException {
        codeword &= (1<<nbits) - 1; // zero out non important bits
        // In the `prewriteBuffer` the `bitcount` most-significant-bits are occupied.
        // We need to add `nbits` of `codeword` to this.

        // concatenate `codeword` to the occupied bits of the `prewriteBuffer`
        // prewriteBuffer = prewriteBuffer |  (codeword << (32-bitcount-nbits));
        prewriteBuffer = prewriteBuffer |  (codeword << (32-bitcount-nbits));
        bitcount += nbits;

        writeBuffer(outputStream);

    }

    public void flushBuffer(BufferedOutputStream outputStream) throws IOException {
        writeBuffer(outputStream);
        if (bitcount > 0) {
            int c = ((prewriteBuffer >> 24) & 0xff); // get the MSB (Most Significant Byte)
            outputStream.write(c);
        }
    }

    private void writeBuffer(BufferedOutputStream outputStream) throws IOException {
        while (bitcount >= 8) {
            int c = ((prewriteBuffer >> 24) & 0xff); // get the MSB (Most Significant Byte)
            outputStream.write(c);
            if (c == 0xff)
                // 0xff is a segment prefix, so if it appears in the pixels bitstream,
                // it must be immediately followed by a 0x00 byte.
                outputStream.write(0);
            prewriteBuffer <<= 8;
            bitcount -= 8;
        }
    }

    /* The function decodes a bitstream into a list of RunLength blocks.
     * Arguments:
     * @encodedBitstream - the encoded bitstream
     * @sampling - the sampling of this image as read from the SOF segment (YUV444Sampling,
     *          YUV422Sampling, YUV411Sampling); the sampling gives the order of blocks (Y,U,V)
     *          in the encoded bitstream.
     * @width - width of the image
     * @height - height of the image
     */
    public ArrayList<RunLengthBlock> decode(byte[] encodedBitstream, int sampling, int width, int height) {
        ArrayList<RunLengthBlock> rleBlocksArray = new ArrayList<>();
        noOfBitsLeftInCurrentByte = 8;  // the number of bits that are left (i.e. not consumed) in the current
                                        // byte of the `encodedBitstream` (i.e. encodedBitstream[i]);
                                        // these bits are always saved starting with the most-significant-bit
                                        // of encodedBitstream[i];
        idxCurrentByte = 0; // the index of the current byte in encodedBitstream
        previousByte = encodedBitstream[0];
        currentBlockTypeIdx = -1;

        int i = 0;
        int noBlocks = getNumberOfDecodingBlocks(sampling, width, height);
        System.out.println("HuffmanEncoder::decode() We will decode " + noBlocks + " 8x8 blocks [" +
                "width=" + width + " height=" + height + " sampling=" + sampling + "]");
        while (idxCurrentByte<encodedBitstream.length) {
            /* At the end of the compressed bitstream there might be stuffed bits to the byte border (i.e. maximum
             * 7 stuffed bits). We need to stop after we have read and decoded enough blocks. Below is a code
             * that does this, but I have found a better way of stopping the decoding when we reached stuffed bits.
             * See below.
             * if (i==noBlocks) {
             *   // we have finished decoding/reading all the blocks; the remaining bits are just stuffed bits
             *   System.out.println("HuffmanEncoder::decode() FINISHING reading/recoding all required blocks. " +
             *           "Leftover bits: " + (noOfBitsLeftInCurrentByte + (encodedBitstream.length-idxCurrentByte-1)*8));
             *   break;
             * }
             */
            if ((idxCurrentByte==encodedBitstream.length-1) && (noOfBitsLeftInCurrentByte<8)) {
                /* A minimum block has at least 5 or 7 bits: DC: (2bit size, 1bit amplitude), AC: (2bit EOB)
                 * for chroma blocks and DC: (2bit size, 1bit amplitude), AC: (4bit EOB) for luma block
                 */
                if (i==noBlocks) {
                    // this is the end of the compressed bitstream; the rest are stuffed bits, we don't care
                    System.out.println("HuffmanEncoder::decode() FINISHING reading/recoding all required blocks. " +
                            "Leftover bits: " + noOfBitsLeftInCurrentByte);
                    break;
                }
            }
            System.out.println("idxCurrentByte= " + idxCurrentByte);
            RunLengthBlock rleBlock = new RunLengthBlock();
            rleBlock.setType(getNextDecodedBlockType());
            decodeBlock(encodedBitstream, rleBlock);
            rleBlocksArray.add(rleBlock);
            System.out.println("HuffmanEncoder::decode() decoded block " + i + " of type " + rleBlock.getType() +
                    " with " + rleBlock.getSize() + " runlength elements.");
            i++;
        }

        return rleBlocksArray;
    }

    private void decodeBlock(byte[] encodedBitstream, RunLengthBlock rleBlock) {
        int sizeDC, runlength, sizeAC, amplitude, x;

        if (idxCurrentByte >= encodedBitstream.length) return;

        // first decode the DC coefficient RunLength element
        sizeDC = decodeHuffmanCodeword(encodedBitstream, rleBlock.getType(), true);
        amplitude = decodeAmplitudeValue(encodedBitstream, sizeDC);
        rleBlock.getData().add(new RunLength(-1, sizeDC, amplitude));
        System.out.println("[-1," + sizeDC + "," + amplitude + "] " );

        // decode the AC coefficients RunLength elements
        int acCoefcount = 0;
        int ZRL = 0; // for ZeroRunLength symbol (just the Size and RunLength, without Amplitude)
        while ((idxCurrentByte < encodedBitstream.length) && (acCoefcount<63)) {
            x = decodeHuffmanCodeword(encodedBitstream, rleBlock.getType(), false);
            runlength = (x & 0xf0) >> 4;
            sizeAC = x & 0x0f;
            if ((runlength==0) && (sizeAC==0)) {
                // EOB symbol read
                System.out.println(" End-Of-Block red!");
                rleBlock.getData().add(new RunLength(runlength, sizeAC, 0));
                break;
            }
            if (x == 0xf0) {
                // (F,0) - ZRL code (Zero RunLength) - we must add 16 AC coefficients equal to ZERO
                System.out.println(" ZeroRunLength code red!");
                ZRL += 16;
            } else {
                if (ZRL>0) {
                    runlength += ZRL;
                    ZRL = 0;
                }
                amplitude = decodeAmplitudeValue(encodedBitstream, sizeAC);
                if (amplitude==0) {
                    System.out.println("HuffmanEncoder::decodeBlock() Error - amplitude is 0!");
                }
                rleBlock.getData().add(new RunLength(runlength, sizeAC, amplitude));
                acCoefcount = acCoefcount + 1 + runlength;
                System.out.println("[" + runlength + "," + sizeAC + "," + amplitude + "] " );
            }
        }
        System.out.println();
    }

    private int decodeHuffmanCodeword(byte[] encodedBitstream, char HuffmanTableType, boolean isDC) {
        // HuffmanTableType can be 'Y', 'U' or 'V' and is the same as the block type
        if ((HuffmanTableType!='Y') && (HuffmanTableType!='U') && (HuffmanTableType!='V')) {
            System.out.println("HuffmanEncoder::decodeHuffmanCodeword() Unknown HuffmanTableType(" +
                    HuffmanTableType + ")! Nothing to do!");
            return -1;
        }
        // isDC = {0 = is AC; 1 = is DC}
        // first, let's select the appropriate Huffman table that we will be using for decoding
        int[][] huffmanTable = DCLumaHuffmanTable;
        switch (HuffmanTableType) {
            case 'Y' -> {
                if (!isDC) huffmanTable = ACLumaHuffmanTable;
            }
            case 'U', 'V' -> {
                if (isDC) huffmanTable = DCChromaHuffmanTable;
                else huffmanTable = ACChromaHuffmanTable;
            }
        }
        System.out.println("HuffmanEncoder::decodeHuffmanCodeword() using Huffman table " +
                isDC + " " + HuffmanTableType);

        int codeBuffer = 0;     // the largest Huffman codeword has 16 bits
        // codeBuffer stores binary values in the least significant bits
        int codeBufferSize = 0; // the number of bits occupied in the `codeBuffer`

        /* The idea is that we take each bit from encodedBitstream, one at a time, starting with
         * the most-significant-bit of encodedBitstream[0] and so one, we deposit this bit into
         * `codeBuffer` (`codeBufferSize` keeps track of the number of bits occupied from
         * `codeBuffer`) and once we have a complete Huffman codeword in `codeBuffer`, we decode
         * it. `noOfBitsLeftInCurrentByte` stores the number of bits that are left (i.e. not
         * consumed) int the current byte of `encodedBitstream`. After we decode a Huffman code
         * (which can be a Size for a DC coef. or a (Runlength,Size) for an AC coef), we then
         * decode the amplitude of the current coefficient.
         */

        int symbol = -1;
        while (idxCurrentByte < encodedBitstream.length) {
            // each iteration adds a new bit to codeBuffer

            // take the Most-Significant-Bit of encodedBitstream[i]
            int msbbit = encodedBitstream[idxCurrentByte] & 0x80;
            msbbit >>= 7;
            msbbit &= 0x01;
            codeBuffer &= (1 << codeBufferSize) - 1; // zero out non important bits
            // add the MSB to codeBuffer
            codeBuffer = (codeBuffer << 1) | msbbit;
            codeBufferSize++;

            // remove bit from encodedBitstream[i]
            encodedBitstream[idxCurrentByte] <<= 1;
            noOfBitsLeftInCurrentByte--;

            //System.out.println("trying to find a huffman symbol for a codeword " +
            //        codeBuffer + " of " + codeBufferSize + " bits..");
            symbol = findHuffmanSymbol(huffmanTable, codeBuffer, codeBufferSize);

            //System.out.println("idxCurrentByte=" + idxCurrentByte +
            //        " noOfBitsLeftInCurrentByte=" + noOfBitsLeftInCurrentByte);
            decodeByte(encodedBitstream);
            if (symbol != -1) {
                // codeword found in Huffman tables
                System.out.println("codeword="+getBinary(codeBuffer,codeBufferSize) +
                        " idxCurrentByte=" + idxCurrentByte + " noOfBitsLeftInCurrentByte=" + noOfBitsLeftInCurrentByte);
                return symbol;
            }
        }
        return symbol;
    }

    private void decodeByte(byte[] encodedBitstream) {
        if (noOfBitsLeftInCurrentByte == 0) { // move to the following byte in encodedBitstream
            idxCurrentByte++;
            noOfBitsLeftInCurrentByte = 8;
            // if we have 0xff followed by 0x00 in the encoded byte stream, we ignore the 0x00 byte
            if ((previousByte==0xff) && (idxCurrentByte<encodedBitstream.length) &&
                    (encodedBitstream[idxCurrentByte]==0x00)) {
                idxCurrentByte++;
            }
            if (idxCurrentByte<encodedBitstream.length) {
                previousByte = encodedBitstream[idxCurrentByte] & 0xff;
                System.out.println("current decoding byte is: " + getBinary(encodedBitstream[idxCurrentByte], 8));
            }
        }
    }

    // Find the symbol from the `HuffmanTable` encoded with that specific `codeword`
    private int findHuffmanSymbol(int[][] HuffmanTable, int codeword, int codewordSize) {
        int symbol = -1;

        //HuffmanTable[i][0] -> stores the codeword for symbol `i`
        //HuffmanTable[i][1] -> stores the length of the codeword of the symbol `i`
        int i = 0;
        while (i<HuffmanTable.length) {
            if ((HuffmanTable[i][0]==codeword) && (HuffmanTable[i][1]==codewordSize)) {
                symbol = i;
                //System.out.println("HuffmanEncoder::findHuffmanSymbol() decoded a symbol with codeword size "+codewordSize);
                break;
            }
            /* Doar dupa ce reordonez tabelele huffman dupa codeword size, pot sa folosesc codul de mai jos...
            if (HuffmanTable[i][1]>codewordSize) {
                break;
            }*/
            i++;
        }

        return symbol;
    }

    // Read `size` bits from encodedBitstream and return them as integer value
    private int decodeAmplitudeValue(byte[] encodedBitstream, int size) {
        int amplitude = 0;
        int origsize = size;

        if ((idxCurrentByte>=encodedBitstream.length) || (size<=0))
            return 0;

        int firstbit = encodedBitstream[idxCurrentByte] & 0x80;
        while ((size > 0) && (idxCurrentByte < encodedBitstream.length)) {
                // each iteration adds a new bit to amplitude

                // take the Most-Significant-Bit of encodedBitstream[i]
                int msbbit = encodedBitstream[idxCurrentByte] & 0x80;
                msbbit >>= 7;
                msbbit &= 0x01;
                // add the MSB to amplitude
                amplitude = (amplitude << 1) | msbbit;
                size--;

                // remove bit from encodedBitstream[i]
                encodedBitstream[idxCurrentByte] <<= 1;
                noOfBitsLeftInCurrentByte--;

            decodeByte(encodedBitstream);
        }

        if (firstbit==0) {
            // the amplitude is negative (the bits represent Amplitude-1, but without the sign bit)
            int mask =  (1<<origsize) - 1 ;
            mask = ~mask;
            amplitude = amplitude | mask;  // we negativate manually amplitude (a negative number uses 2 complementary representation)
            amplitude += 1;
        }

        System.out.println("amplitude="+getBinary(amplitude,origsize) +
                " idxCurrentByte=" + idxCurrentByte + " noOfBitsLeftInCurrentByte=" + noOfBitsLeftInCurrentByte);
        return amplitude;
    }

    private char getNextDecodedBlockType() {
        /* This function returns the type (Y, U or V) of the next decoded block from the
         * encoded bitstream based on the horizontal sampling factor and vertival sampling
         * factor for each component as specified in the JPG file. The order of blocks
         * in the encoded stream depends on the sampling of this image and is specified
         * by the JPEG standard. When the number of components of the image (Y, Cb, Cr)
         * is larger than 1 (i.e. grayscale image), the types of blocks are always interleaved
         * in the compressed bitstream. As examples, some of the most common encoding orders
         * of blocks is detailed below (this is just a selection, the most common one, it
         * does not present all the possible encoding orders of blocks in JPEG, although
         * this function is general and the code treats all possible encoding orders of blocks) :
         *
         * o If sampling == YUV444Sampling the blocks will appear in this order in the compressed bitstream:
         *      Y0, Cb0, Cr0, Y1, Cb1, Cr1, Y2, Cb2, Cr2, ...
         * o If sampling == YUV422HorizSampling/YUV422VertSampling the blocks will appear in this order
         * in the compressed bitstream:
         *      Y0, Y1, Y2, Y3, Cb0, Cb1, Cr0, Cr1, Y4, Y5, Y6, Y7, Cb2, Cb3, Cr2, Cr3, ...
         * o If sampling == YUV111Sampling the blocks will appear in this order in the compressed bitstream:
         *      Y0, Y1, Y2, Y3, Cb0, Cr0, Y4, Y5, Y6, Y7, Cb1, Cr1, ...
         *
         */

        currentBlockTypeIdx++;
        currentBlockTypeIdx = currentBlockTypeIdx % noBlocksInMCU;
        char blocktype;

        if (currentBlockTypeIdx < horizontalSamplingFactors[0]*verticalSamplingFactors[0]) {
            blocktype = 'Y';
        } else if (currentBlockTypeIdx < horizontalSamplingFactors[0]*verticalSamplingFactors[0] +
                        horizontalSamplingFactors[1]*verticalSamplingFactors[1]) {
            blocktype = 'U';
        } else {
            blocktype = 'V';
        }

        return blocktype;
    }

    private int getNumberOfDecodingBlocks(int sampling, int width, int height) {
        int luma, chroma;
        luma = (width*height) / (8*8);
        luma = luma==0 ? 1 : luma;
        if (sampling==YCbCrImage.YUV444Sampling) {
            return 3 * luma;    // it's actually (luma + 2*chroma), but chroma==luma in this case
        } else if ((sampling==YCbCrImage.YUV422HorizSampling) || (sampling==YCbCrImage.YUV422VertSampling)) {
            chroma = (width*height) / (2*8*8);
            chroma = chroma==0 ? 1 : chroma;
            return  luma + 2 * chroma;
        } else if (sampling==YCbCrImage.YUV411Sampling) {
            chroma = (width*height) / (4*8*8);
            chroma = chroma==0 ? 1 : chroma;
            return  luma + 2 * chroma;
        }
        return -1;
    }

    private String getBinary(int x, int nbits) {
        byte[] buffer = new byte[nbits];
        int mask = 1;
        for (int i=0; i<nbits; i++) {
            buffer[nbits-1-i] = (byte) ((x & mask) >> i);
            buffer[nbits-1-i] += '0';
            mask <<= 1;
        }
        return new String(buffer);
    }
}


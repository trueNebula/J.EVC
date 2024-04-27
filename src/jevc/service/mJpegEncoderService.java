package jevc.service;

import jevc.entities.Block;
import jevc.entities.ExponentialGolombBlock;
import jevc.entities.RunLengthBlock;
import jevc.entities.YCbCrImage;
import jevc.operations.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

public class mJpegEncoderService {
    // the Start Of Image marker
    private static final byte[] SOI = {(byte) 0xff,(byte) 0xd8};
    // the Define Quantization Table marker
    private static final byte[] DQT = {(byte) 0xff,(byte) 0xdb};
    // the Define Huffman Table marker
    private static final byte[] DHT = {(byte) 0xff,(byte) 0xc4};
    // the COMment marker
    private static final byte[] COM = {(byte) 0xff,(byte) 0xfe};
    // the APP0 marker (JFIF marker)
    private static final byte[] JFIF = {(byte) 0xff,(byte) 0xe0};
    // the Start Of Frame marker
    private static final byte[] SOF = {(byte) 0xff,(byte) 0xc0};
    // the Start Of Scan marker
    private static final byte[] SOS = {(byte) 0xff,(byte) 0xda};
    // the End Of Image marker
    private static final byte[] EOI = {(byte) 0xff,(byte) 0xd9};
    // the JFIF identifier in the APP0 marker segment
    private static final byte[] JFIF_ID = {(byte) 0x4a,(byte) 0x46,(byte) 0x49,(byte) 0x46,(byte) 0x00}; //"JFIF"#0
    // the default encoder comment string
    private static final String ENCODER_INFO = "JPEG Test Encoder";
    // the component IDs used to label the components
    private static final int[] COMPONENT_IDS = {1,2,3};


    private final File[] files;
    private final DiscreteCosineTransform DCT;
    private final Quantizer quantizer;
    private RunLengthEncoder runlengthEncoder;
    private HuffmanEncoder huffmanEncoder;
    private ExponentialGolombEncoder exponentialGolombEncoder;
    private BlockBuffer blockBuffer;
    private MotionEstimator motionEstimator;
    private BufferedOutputStream outputStream;
    private final String outputFolder;

    public mJpegEncoderService(File[] files, String outfile, String outputFolder) {
        this.files = files;
        this.outputFolder = outputFolder;
        DCT = new DiscreteCosineTransform();
        int DEFAULT_QUALITY_FACTOR = 80;
        quantizer = new Quantizer(DEFAULT_QUALITY_FACTOR);
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(outfile));
        } catch (Exception ex) {
            System.out.println("Error opening the output stream!");
        }
    }

    public void compress() throws IOException {
        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("log.txt"))));
        int frameIndex = 0;
        for (File f: this.files) {
            // set Frame type
            char frameType = 'P';
            if (frameIndex % 10 == 0) {
                frameType = 'I';
            }
            frameType = 'I';
            frameIndex++;

            // init encoders
            runlengthEncoder = new RunLengthEncoder();
            huffmanEncoder = new HuffmanEncoder();
            exponentialGolombEncoder = new ExponentialGolombEncoder();
            blockBuffer = new BlockBuffer();
            System.out.println(f.getName());

            // read image and convert to YCbCr
            BufferedImage img = ImageIO.read(f);
            int[][] pixels = new int[img.getHeight()][img.getWidth()];

            for (int i = 0; i < img.getHeight(); i++)
                for(int j = 0; j < img.getWidth(); j++) {
                    pixels[i][j] = img.getRGB(j, i);
                }

            YCbCrImage frame = new YCbCrImage(pixels, img.getWidth(), img.getHeight());
            BufferedOutputStream frameOutputStream = new BufferedOutputStream(
                    new FileOutputStream(outputFolder + f.getName().replace(".png", ".jpg"))
            );

            // scale and subsample image
            frame.ScaleImage();
            //image.PerformSubsampling(YCbCrImage.YUV411Sampling);
            frame.PerformSubsampling(YCbCrImage.YUV444Sampling);

            // split frame into blocks
            ArrayList<Block> blocks = frame.PerformBlockSplitting();
//            System.out.println(Arrays.deepToString(blocks.get(0).getData()));
            RunLengthBlock rleBlock;
            ExponentialGolombBlock egBlock;
            ArrayList<Block> DebugColorBlocks = new ArrayList<>();
            ArrayList<Block> DebugDCTBlocks = new ArrayList<>();
            ArrayList<Block> DebugQBlocks = new ArrayList<>();

            // write JPEG header sections (REMOVE THIS)
            writeHeaderSections(frameOutputStream, frame);

            // process each block
            for (Block block: blocks) {
                boolean enablePrint = true;

                // I frame => perform DCT, quantization, inverses, push to buffer, proceed to VLC
                // P frame => search in block buffer, estimate motion, subtract, perform DCT, quantization,
                //            proceed to VLC
                if (frameType == 'P') {
                    motionEstimator = new MotionEstimator();
                    // skip to motion estimation
                    // look through block buffer for the most similar block
                    Block similarBlock = blockBuffer.getSimilarBlock(block);
                    // compute motion vector
//                    int[] motionVector = motionEstimator.computeMotionVector(block, similarBlock);
                    // subtract found block from input block
                    block.subtract(similarBlock);
                    // perform DCT
                    DCT.forward(block);
                    // quantize DCT coefficients
                    quantizer.quantize(block);
                    // write to buffer
                    // send motion vector to VLC

                    // write to buffer
                } else if (frameType == 'I'){
                    if (enablePrint) {
                        System.out.println("YCbCr block:");
                        block.print();
                    }
                    DebugColorBlocks.add(block.getCopy());

                    // perform DCT
                    DCT.forward(block);
                    if (enablePrint) {
                        System.out.println("DCT block:");
                        block.print();
                    }
                    DebugDCTBlocks.add(block.getCopy());

                    // quantize DCT coefficients
                    quantizer.quantize(block);
                    if (enablePrint) {
                        System.out.println("Quantized block:");
                        block.print();
                    }
                    DebugQBlocks.add(block.getCopy());

                    // inverse quantize and inverse DCT a given block
                    Block savedBlock = block.getCopy();
                    quantizer.dequantize(savedBlock);
                    DCT.inverse(savedBlock);

                    // save it in a block buffer
                    blockBuffer.save(savedBlock);
                    // proceed to VLC
                }

                // VLC encode block
//                rleBlock = runlengthEncoder.encode(block);
                egBlock = exponentialGolombEncoder.encodeBlock(block);
                if (enablePrint) {
                    egBlock.print();
                }
//                huffmanEncoder.encode(frameOutputStream, rleBlock);
                outputStream.write(egBlock.toString().getBytes());
            }

            // flush buffers
            huffmanEncoder.flushBuffer(frameOutputStream);
            blockBuffer.flush();

            // write JPEG trailer section (REMOVE THIS)
            writeTrailerSection(frameOutputStream);
        }
    }

    private void writeHeaderSections(BufferedOutputStream outputStream, YCbCrImage frame) throws IOException {
        outputStream.write(SOI);
        writeAPP0Segment(outputStream);
        writeCOMSegment(outputStream);
        writeDQTSegment(outputStream);
        writeSOF0Segment(outputStream, frame);
        writeDHTSegment(outputStream);
        writeSOSSegment(outputStream);
    }

    /** write the JFIF segment (APP0 marker)
     * Structure of the JFIF segment:
     * - JFIF marker (2 bytes)
     * - length of the segment without JFIF marker and including this 2-byte length field (2 bytes)
     * - zero terminated string "JFIF" that uniquely identifies this APP0 marker (5 bytes) : 0x4a, 0x46, 0x49, 0x46, 0x00
     * - JFIF version (2 bytes)
     * - units for X and Y density (1 byte) - 0 for pixels
     * - X density (2 bytes)
     * - Y density (2 bytes)
     * - Xthumbnail: thumbnail horizontal pixel count (1 byte)
     * - Ythumbnail: thumbnail vertical pixel count (1 byte)
     * - packed RGB values for the thumbnail pixels (3*Xthumbnail*Ythumbnail bytes)
     */
    private void writeAPP0Segment(BufferedOutputStream outputStream) throws IOException {
        byte[] JFIFSegment = new byte[18];
        JFIFSegment[0] = JFIF[0];
        JFIFSegment[1] = JFIF[1];
        JFIFSegment[2] = 0x00;  // length of JFIF segment (MSB)
        JFIFSegment[3] = 0x10;  // length of JFIF segment (LSB)
        JFIFSegment[4] = JFIF_ID[0];
        JFIFSegment[5] = JFIF_ID[1];
        JFIFSegment[6] = JFIF_ID[2];
        JFIFSegment[7] = JFIF_ID[3];
        JFIFSegment[8] = JFIF_ID[4];
        JFIFSegment[9] = 0x01;
        JFIFSegment[10] = 0x00;  // JFIF version 1.0
        JFIFSegment[11] = 0x00;  // X and Y density - pixels
        JFIFSegment[12] = 0x00;
        JFIFSegment[13] = 0x01;  // X density - 1 pixel
        JFIFSegment[14] = 0x00;
        JFIFSegment[15] = 0x01;  // Y density - 1 pixel
        JFIFSegment[16] = 0x00;  // no thumbnail horizontal pixel count
        JFIFSegment[17] = 0x00;  // no thumbnail vertical pixel count
        outputStream.write(JFIFSegment);
    }

    private void writeCOMSegment(BufferedOutputStream outputStream) throws IOException {
        byte[] commentSegment = new byte[4 + ENCODER_INFO.length()];
        commentSegment[0] = COM[0];
        commentSegment[1] = COM[1];
        commentSegment[2] = (byte) (((ENCODER_INFO.length()+2) >> 8) & 0xff);
        commentSegment[3] = (byte) ((ENCODER_INFO.length()+2) & 0xff);
        System.arraycopy(ENCODER_INFO.getBytes(),0,commentSegment,4,ENCODER_INFO.length());
        outputStream.write(commentSegment);
    }

    /** the structure of the SOF0 segment is:
     * -the SOF0 marker (0xff,0xc0) - 2 bytes
     * -the length of the segment (2 bytes) without the SOF0 marker, but including these
     *   2 bytes for representing the length - equal to 8+component*3
     * -data precision (1 byte) - the bits/sample, usually 8 or 12
     * -image height (2 bytes) - must be >0
     * -image width (2 bytes) - must be >0
     * -number of components (1 byte) - usually 1 (gray scaled), 3 (YCbCr or YIQ), 4 (CMYK)
     * -each component is described by 3 bytes of data:
     *	    o id (1 byte): 1=Y, 2=Cb, 3=Cr, 4=I, 5=Q
     *	    o sampling factors (1 byte): bits 0-3 vertical, 4-7 horizontal
     *	    o quantization table number (1 byte)
     */
    private void writeSOF0Segment(BufferedOutputStream outputStream, YCbCrImage frame) throws IOException {
        byte[] SOF0Segment = new byte[19];
        SOF0Segment[0] = SOF[0];
        SOF0Segment[1] = SOF[1];
        SOF0Segment[2] = (byte) 0;
        SOF0Segment[3] = (byte) 17;
        SOF0Segment[4] = (byte) 8;    // sample precision
        SOF0Segment[5] = (byte) ((frame.getHeight() >> 8) & 0xff);
        SOF0Segment[6] = (byte) (frame.getHeight() & 0xff);
        SOF0Segment[7] = (byte) ((frame.getWidth() >> 8) & 0xff);
        SOF0Segment[8] = (byte) (frame.getWidth() & 0xff);
        SOF0Segment[9] = (byte) 3; // number of components
        int[] horizSamplingFactors = {1,1,1};
        int[] vertSamplingFactors = {1,1,1};
//        YCbCrImage.computeSamplingFactors(frames.getSampling(), horizSamplingFactors, vertSamplingFactors);
        int index = 10;
        for (int i=0;i < SOF0Segment[9];i++) {
            SOF0Segment[index++] = (byte) COMPONENT_IDS[i];
            SOF0Segment[index++] = (byte) ((horizSamplingFactors[i] << 4) + vertSamplingFactors[i]);
            SOF0Segment[index++] = (byte) (i == 0 ? 0:1);
        }
        outputStream.write(SOF0Segment);
    }

    /**
     * the structure of an SOS segment:
     * -the SOS marker (0xff,0xda) - 2 bytes (buffer doesn't contain 0xff, it starts with 0xda)
     * -length (2 bytes) of the SOS segment without the SOS marker, but including the bytes
     *   for this field - must be equal to 6+2*(no. of components in scan)
     * -number of components in scan (1 byte) - must be 1,2,3 or 4, otherwise error
     * -each component (2 bytes):
     *	    o component ID, 1 byte: (1=Y, 2=Cb, 3=Cr, 4=I, 5=Q)
     *	    o Huffman tables to use, 1 byte: bit 0..3 - AC table (0..3)
     *					bit 4..7 - DC table (0..3)
     * -ignorable bytes (3 bytes):
     *	    o start of spectral selection (1 byte)
     *	    o end of spectral selection (1 byte)
     *	    o 1 ignorable byte (?)
     */
    private void writeSOSSegment(BufferedOutputStream outputStream) throws IOException {
        byte[] SOSSegment = new byte[14];
        SOSSegment[0] = SOS[0];
        SOSSegment[1] = SOS[1];
        SOSSegment[2] = (byte) 0;
        SOSSegment[3] = (byte) 12;
        SOSSegment[4] = (byte) COMPONENT_IDS.length;
        int index = 5;
        for (int i=0; i < SOSSegment[4]; i++) {
            SOSSegment[index++] = (byte) COMPONENT_IDS[i];
            int tableIndex = (i == 0 ? 0:1);
            // 0 for luminance tables, 1 for chrominance tables
            SOSSegment[index++] = (byte) ((tableIndex << 4) + tableIndex);
        }
        SOSSegment[index++] = (byte) 0;  // start of spectral selection
        SOSSegment[index++] = (byte) 63; // end of spectral selection
        SOSSegment[index] = (byte) 0;  // ignorable byte, Ah + Al
        outputStream.write(SOSSegment);
    }

    /**
     * the structure of the DQT segment is:
     * -the DQT marker (0xff,0xdb) - 2 bytes (buffer doesn't contain 0xff, it starts with 0xdb)
     * -length (2 bytes) of the DQT segment without the DQT marker, but including the bytes
     *   for this field
     * -multiple QT tables, each of them containing:
     *	    o QT information, 1 byte: bit 0..3 - number of this QT (0,1,2,3)
     *		           	              bit 4..7 - precision of QT (0 = 8bit, otherwise = 16bit)
     *	    o QT bytes, 64*(precision+1) bytes: the QT values
     */
    private void writeDQTSegment(BufferedOutputStream outputStream) throws IOException {
        byte[] DQTSegment = new byte[134];
        DQTSegment[0] = DQT[0];
        DQTSegment[1] = DQT[1];
        DQTSegment[2] = (byte) 0;
        DQTSegment[3] = (byte) 132;  // 2 + (1+64) + (1+64)
        int currPos = 4;
        for (int i=0; i<2; i++) {
            DQTSegment[currPos++] = (byte) ((0 << 4) + i);
            int[] quantTable = (i == 0 ? quantizer.QUANTUM_LUMINANCE : quantizer.QUANTUM_CHROMINANCE);

            for (int j=0; j < quantTable.length; j++) {
                DQTSegment[currPos++] = (byte) quantTable[RunLengthEncoder.ZIGZAG_ORDER[j]];
            }
        }
        outputStream.write(DQTSegment);
    }

    /**
     * the structure of the DHT segment is:
     * -the DHT marker (0xff,0xc4) - 2 bytes (buffer doesn't contain 0xff, it starts with 0xc4)
     * -length (2 bytes) of the DHT segment without the DHT marker, but including the bytes
     *   for this field
     * -multiple HT tables, each of them containing:
     *	   HT information, 1 byte: bit 0..3 - number of this HT (0,1,2,3)
     *				 bit 4 - type of this HT (0=DC table, 1=AC table)
     *				 bit 5..7 - not used, must be zero
     *	   Number of symbols, 16 bytes: number of symbols with codes of length 1,2..16.
     *				      The sum (let this sum be n) of these bytes is the
     *				      total number of codes, which must be <=256
     *	   Symbols, n bytes (see above): table containing the symbols in order of increasing
     *				       code length (n=total number of codes)
     */
    private void writeDHTSegment(BufferedOutputStream outputStream) throws IOException {
        Vector<int[]> bits = new Vector<>();
        bits.addElement(HuffmanEncoder.BITS_DC_LUMINANCE);
        bits.addElement(HuffmanEncoder.BITS_AC_LUMINANCE);
        bits.addElement(HuffmanEncoder.BITS_DC_CHROMINANCE);
        bits.addElement(HuffmanEncoder.BITS_AC_CHROMINANCE);
        Vector<int[]> vals = new Vector<>();
        vals.addElement(HuffmanEncoder.VALS_DC_LUMINANCE);
        vals.addElement(HuffmanEncoder.VALS_AC_LUMINANCE);
        vals.addElement(HuffmanEncoder.VALS_DC_CHROMINANCE);
        vals.addElement(HuffmanEncoder.VALS_AC_CHROMINANCE);

        byte[] dhtSeg1, dhtSeg2, dhtSeg3, dhtSeg4;
        int index = 4, bitsIndex = 4, valsIndex;
        // all bits-arrays are of the same length
        dhtSeg1 = new byte[HuffmanEncoder.BITS_DC_LUMINANCE.length];
        dhtSeg4 = new byte[4];
        dhtSeg4[0] = DHT[0];
        dhtSeg4[1] = DHT[1];
        for (int i=0;i < bits.size();i++) {
            int bytes = 0;
            int[] currBits = bits.elementAt(i);
            dhtSeg1[index++ - bitsIndex] = (byte) currBits[0];
            for (int j=1; j < currBits.length; j++) {
                dhtSeg1[index++ - bitsIndex] = (byte) currBits[j];
                bytes += currBits[j];
            }
            valsIndex = index;
            dhtSeg2 = new byte[bytes];
            int[] currVals = vals.elementAt(i);
            for (int j=0;j < bytes;j++) {
                dhtSeg2[index++ - valsIndex] = (byte) currVals[j];
            }
            dhtSeg3 = new byte[index];
            System.arraycopy(dhtSeg4,0,dhtSeg3,0,bitsIndex);
            System.arraycopy(dhtSeg1,0,dhtSeg3,bitsIndex,dhtSeg1.length);
            System.arraycopy(dhtSeg2,0,dhtSeg3,valsIndex,bytes);
            dhtSeg4 = dhtSeg3;
            bitsIndex = index;
        }
        // set the length of the DHT segment
        dhtSeg4[2] = (byte) (((index-2) >> 8) & 0xff);
        dhtSeg4[3] = (byte) ((index-2) & 0xff);
        outputStream.write(dhtSeg4);
    }

    private void writeTrailerSection(BufferedOutputStream outputStream) throws IOException {
        outputStream.write(EOI);
        outputStream.flush();
    }
}

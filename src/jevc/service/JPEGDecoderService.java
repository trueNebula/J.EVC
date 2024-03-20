package jevc.service;

import jevc.entities.*;
import jevc.operations.DiscreteCosineTransform;
import jevc.operations.HuffmanEncoder;
import jevc.operations.Quantizer;
import jevc.operations.RunLengthEncoder;
import jevc.utils.JPEGHeader;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class JPEGDecoderService extends JPEGHeader {
    private final String inputFile;

    private final DiscreteCosineTransform DCT;
    private final Quantizer quantizer;
    private final RunLengthEncoder runlengthEncoder;
    private final HuffmanEncoder huffmanEncoder;
    private byte[] pixelByteArray;

    private int height;
    private int width;
    private ImageComponent[] imageComponents;
    private int YMCUIndex, CbMCUIndex, CrMCUIndex;
    private int YinnerMCUIdx, CbinnerMCUIdx, CrinnerMCUIdx;

    /* An MCU (Minimum Coded Unit) is defined for each component (Y, Cb and Cr). And in our code
     * the size of an MCU of one component can be different from the size of the MCU of another
     * component. In our code, the MCU of component[i] is a rectangle of 8x8 blocks formed by
     * component[i].verticalSampling * component[i].horizontalSampling blocks, and it has
     * component[i].verticalSampling 8x8 blocks on OY axis and component[i].horizontalSampling
     * 8x8 blocks on the OX axis.
     * Note that this is different from what is specified in the JPEG standard where the MCU is
     * just an image region and is the same for all components. For example, in JPEG standard
     * for 411 subsampling, one MCU is a 4x4 blocks region from the whole image, and it contains:
     * 4 Y type blocks, 1 Cb type block and 1 Cr type block; but the region in the image is the
     * same for all three components. While in our code, for 411 sampling one MCU of type Y is
     * a region from the whole image containing 4x8x8 pixels (i.e. 4 8x8 blocks), one MCU of type
     * Cb is a region from the whole image containing 1 8x8 Cb block and one MCU of type Cr is
     * a region from the whole image containing 1 8x8 Cr block. So the MCU regions differ for
     * the three components. Again, if we have 411 sampling, in our code, the region [0,0]-[63,63]
     * of the image includes: 1 MCU of 4 Y blocks, 4 MCUs of 1 Cb block and 4 MCUs of 1 Cr block.
     * Although this might seem like a mistake, the Cb and Cr MCUs will be expanded in the
     * upsampling phase when the pixel region expanded from one Cb/Cr MCU will equal in size the
     * pixel region of one Y MCU (e.g. 1 MCU containing 1 Cb block will be expanded to a region of
     * [0,0]-[63,63] pixels which is the size of 1 MCU of type Y).
     *
     * In order to properly place 8x8 blocks on the decoded image we store for each component (Y, Cb, Cr):
     * the MCUIndex of the current MCU (for the current component) and the innerMCUIdx which is a
     * block index into the current MCU (for the current component).
     *
     */

    //public enum Sampling {YUV444Sampling, YUV422Sampling, YUV411Sampling};
    private int sampling; // YCbCrImage.YUV444Sampling, YCbCrImage.YUV422Sampling, YCbCrImage.YUV411Sampling

    int[] Huffman_bits_dc_luminance = new int[17];
    int[] Huffman_bits_dc_chrominance = new int[17];
    int[] Huffman_bits_ac_luminance = new int[17];
    int[] Huffman_bits_ac_chrominance = new int[17];
    int[] Huffman_vals_dc_luminance = new int[12];
    int[] Huffman_vals_dc_chrominance = new int[12];
    int[] Huffman_vals_ac_luminance = new int[162];
    int[] Huffman_vals_ac_chrominance = new int[162];

    public JPEGDecoderService(String inputFile) {
        this.inputFile = inputFile;
        DCT = new DiscreteCosineTransform();
        quantizer = new Quantizer();
        runlengthEncoder = new RunLengthEncoder();
        huffmanEncoder = new HuffmanEncoder();
        sampling = YCbCrImage.YUV444Sampling;

        Huffman_bits_dc_luminance[0] = 0x00;
        Huffman_bits_dc_chrominance[0] = 0x01;
        Huffman_bits_ac_luminance[0] = 0x10;
        Huffman_bits_ac_chrominance[0] = 0x11;
    }

    public void readBitStream() throws IOException {
        RandomAccessFile f = new RandomAccessFile(inputFile, "r");
        readJPEGHeaders(f);
        huffmanEncoder.initializeHuffmanTables(
                Huffman_bits_dc_luminance, Huffman_bits_dc_chrominance, Huffman_bits_ac_luminance, Huffman_bits_ac_chrominance,
                Huffman_vals_dc_luminance, Huffman_vals_dc_chrominance, Huffman_vals_ac_luminance, Huffman_vals_ac_chrominance);
        readPixelsBitStream(f);
    }

    private void readPixelsBitStream(RandomAccessFile fjpg) throws IOException {
        int bitstreamSize = (int) (fjpg.length() - fjpg.getFilePointer());
        byte[] buffer = new byte[bitstreamSize];
        pixelByteArray = new byte[bitstreamSize-2];
        int i = 0;
        try {
            while (true) {
                buffer[i] = (byte) fjpg.readUnsignedByte();
                i++;
            }
        } catch(EOFException e) {
            // check if the last two bytes from the file are PREFIX,EOI
            if ((buffer[i-2] == PREFIX) && (buffer[i-1] == EOI)) {
                System.out.println("\nThe size of the compressed image data is: " + (i - 2) + " bytes.");
                System.out.println("EOI --------------------------------------------------------");
            } else {
                System.out.println("Warning: could not find the EOI marker!");
            }
        }
        System.arraycopy(buffer, 0, pixelByteArray, 0, bitstreamSize - 2);
        /*System.out.println("The compressed bitstream (including EOI) is:");
        for (i=0; i<buffer.length; i++)
            System.out.print(printHex(buffer[i]) + " ");
        System.out.println("\n");*/
    }

    public RGBImage decode() {
        ArrayList<RunLengthBlock> rleBlocksArray;
        ArrayList<Block> blocks = new ArrayList<>();
        Block block;
        YMCUIndex = CbMCUIndex = CrMCUIndex = 0;
        YinnerMCUIdx = CbinnerMCUIdx = CrinnerMCUIdx = 0;

        System.out.println("sampling is: " + sampling);
        rleBlocksArray = huffmanEncoder.decode(pixelByteArray, sampling, width, height);
        int i = 0;
        for (RunLengthBlock rleBlock: rleBlocksArray) {
            System.out.println("Decoding RunLengthBlock " + i + " ..");
            rleBlock.print();
            block = runlengthEncoder.decode(rleBlock);
            int[] pos = computeNextBlockPosition(block.getType());
            block.setPos(pos[0], pos[1]);
            System.out.println("Quantized block:");
            block.print();
            quantizer.dequantize(block);
            System.out.println("DCT block:");
            block.print();
            DCT.inverse(block);
            System.out.println("YCbCr block:");
            block.print();
            blocks.add(block);
            i++;
        }

        YCbCrImage image = new YCbCrImage(blocks, height, width, sampling);
        image.PerformUpsampling();
        return image.convertToRGBImage();
    }

    private void readJPEGHeaders(RandomAccessFile fjpg) {
        byte val;
        byte[] buffer = new byte[max_segm_size];
        int i,j,x,size;
        int sos = 0;
        try {
            /* read the SOI */
            fjpg.readFully(buffer,0,2);
            if(buffer[0]!=PREFIX || buffer[1]!=SOI) {
                System.err.println("The file does not start with SOI.");
                return;
            }
            System.out.println("SOI --------------------------------------------------------");

            /* read JFIF segment (APP0 marker) */
            /*
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
            fjpg.readFully(buffer,0,4);
            if(buffer[0]!=PREFIX || buffer[1]!=JFIF) {
                System.err.println("The file does not have a APP0 marker segment.");
                return;
            }
            i = buffer[2]<<8;
            i |= buffer[3];		// i contains the length of the JFIF marker segment
            j = 1;
            System.out.println("        APP0 marker segment (length="+i+") :");

            /* read the JFIF0 identifier */
            fjpg.readFully(buffer,0,5);
            j += 5;
            if(!equal(buffer, JFIF_ID, 5)) {
                System.err.println("The APP0 marker segment does not have the identifier \"JFIF#0\".");
                return;
            }
            System.out.println("        	identifier: "+new String(buffer,0,5));

            /* read the JFIF version */
            fjpg.readFully(buffer,0,2);
            j += 2;
            System.out.println("        	JFIF version:"+(int)buffer[0]+"."+(int)buffer[1]);

            /* read the units */
            val = fjpg.readByte();
            j += 1;
            System.out.println("        	units:"+(int)val);

            /* read X density */
            fjpg.readFully(buffer,0,2);
            j += 2;
            x = buffer[0]<<8;
            x |= buffer[1];
            System.out.println("        	horizontal pixel density:"+x);

            /* read Y density */
            fjpg.readFully(buffer,0,2);
            j += 2;
            x = buffer[0]<<8;
            x |= buffer[1];
            System.out.println("        	vertical pixel density:"+x);

            /* read X thumbnail */
            val = fjpg.readByte();
            j += 1;
            x = val;
            System.out.println("        	thumbnail horizontal pixel count:"+(int)val);

            /* read Y thumbnail */
            val = fjpg.readByte();
            j += 1;
            x *= val;
            System.out.println("        	thumbnail vertical pixel count:"+(int)val);

            /* packed RGB values for the thumbnail pixels (3*Xthumbnail*Ythumbnail), if are there any */
            if (x>0) {
                fjpg.readFully(buffer,0,3*x);
                j += 3*x;
            }
            if (j!=i-1) {System.err.println("Corrupted JFIF segment."); return; }

            /* read segments or markers */
            fjpg.readFully(buffer,0,1);
            if (buffer[0]!=PREFIX) {
                System.err.println("Unrecognized prefix for marker:"+buffer[0]);
                return;
            }
            /* we keep reading segments until we reach the compressed image data, that is after we parse the SOS segment */
            while (sos==0) {
                while (buffer[0]==PREFIX) {
                    fjpg.readFully(buffer,0,1);
                    /* if a 0x00 byte follows a 0xff byte, the zero byte (0x00) must be discarded.
                     * this happens only in the compressed image data, following the SOS marker
                     */
                    if (buffer[0]==0x00) fjpg.readFully(buffer,0,1);
                }
                i = 1;
                /*
                 * Depending on the marker found in buffer[0], we take two kinds of actions:
                 * o if buffer[0] != SOS, we read bytes until we find a PREFIX (which means that a new segment starts)
                 * o if buffer[0] == SOS, this means that this is the last segment from the JFIF file and immediately
                 *   after it the compressed image data follows and then the [0xff,0xd9] marker which completes the
                 *   JFIF file. In this case, we determine the size of the segment by reading the first two bytes
                 *   following the segment marker (buffer[0]) and then we read as many bytes as the calculated size
                 *   tells us to.
                 */
                if (buffer[0] != SOS) {
                    while (buffer[i - 1] != PREFIX) buffer[i++] = fjpg.readByte();
                } else {
                    /* read the SOS segment, the last segment before the image date */
                    fjpg.readFully(buffer,1,2);
                    i += 2;
                    size = ((int) buffer[1]) & 0xff;
                    size <<= 8;
                    size |= ((int) buffer[2]) & 0xff;
                    fjpg.readFully(buffer,i,size-2);
                    i += size-2;
                }
                switch (buffer[0]) {
                    case APP1 -> dumpAPPsegment("APP1", buffer, i - 1);
                    case APP2 -> dumpAPPsegment("APP2", buffer, i - 1);
                    case APP3 -> dumpAPPsegment("APP3", buffer, i - 1);
                    case APP4 -> dumpAPPsegment("APP4", buffer, i - 1);
                    case APP5 -> dumpAPPsegment("APP5", buffer, i - 1);
                    case APP6 -> dumpAPPsegment("APP6", buffer, i - 1);
                    case APP7 -> dumpAPPsegment("APP7", buffer, i - 1);
                    case APP8 -> dumpAPPsegment("APP8", buffer, i - 1);
                    case APP9 -> dumpAPPsegment("APP9", buffer, i - 1);
                    case APP10 -> dumpAPPsegment("APP10", buffer, i - 1);
                    case APP11 -> dumpAPPsegment("APP11", buffer, i - 1);
                    case APP12 -> dumpAPPsegment("APP12", buffer, i - 1);
                    case APP13 -> dumpAPPsegment("APP13", buffer, i - 1);
                    case APP14 -> dumpAPPsegment("APP14", buffer, i - 1);
                    case APP15 -> dumpAPPsegment("APP15", buffer, i - 1);
                    case SOF0 -> parseSOFsegment("SOF0", buffer, i - 1);
                    case SOF1 -> {
                        parseSOFsegment("SOF1", buffer, i - 1);
                        System.out.println("SOF1 segment found! This file is not encoded with the Baseline DCT algorithm (i.e. SOF0 segment required). Exiting..");
                        System.exit(-1);
                    }
                    case SOF2 -> {
                        parseSOFsegment("SOF2", buffer, i - 1);
                        System.out.println("SOF2 segment found! This file is not encoded with the Baseline DCT algorithm (i.e. SOF0 segment required). Exiting..");
                        System.exit(-1);
                    }
                    case SOF3 -> {
                        parseSOFsegment("SOF3", buffer, i - 1);
                        System.out.println("SOF3 segment found! This file is not encoded with the Baseline DCT algorithm (i.e. SOF0 segment required). Exiting..");
                        System.exit(-1);
                    }
                    case SOF5 -> {
                        parseSOFsegment("SOF5", buffer, i - 1);
                        System.out.println("SOF5 segment found! This file is not encoded with the Baseline DCT algorithm (i.e. SOF0 segment required). Exiting..");
                        System.exit(-1);
                    }
                    case SOF6 -> {
                        parseSOFsegment("SOF6", buffer, i - 1);
                        System.out.println("SOF6 segment found! This file is not encoded with the Baseline DCT algorithm (i.e. SOF0 segment required). Exiting..");
                        System.exit(-1);
                    }
                    case SOF7 -> {
                        parseSOFsegment("SOF7", buffer, i - 1);
                        System.out.println("SOF7 segment found! This file is not encoded with the Baseline DCT algorithm (i.e. SOF0 segment required). Exiting..");
                        System.exit(-1);
                    }
                    case SOF9 -> {
                        parseSOFsegment("SOF9", buffer, i - 1);
                        System.out.println("SOF9 segment found! This file is not encoded with the Baseline DCT algorithm (i.e. SOF0 segment required). Exiting..");
                        System.exit(-1);
                    }
                    case SOF10 -> {
                        parseSOFsegment("SOF10", buffer, i - 1);
                        System.out.println("SOF10 segment found! This file is not encoded with the Baseline DCT algorithm (i.e. SOF0 segment required). Exiting..");
                        System.exit(-1);
                    }
                    case SOF11 -> {
                        parseSOFsegment("SOF11", buffer, i - 1);
                        System.out.println("SOF11 segment found! This file is not encoded with the Baseline DCT algorithm (i.e. SOF0 segment required). Exiting..");
                        System.exit(-1);
                    }
                    case SOF13 -> {
                        parseSOFsegment("SOF13", buffer, i - 1);
                        System.out.println("SOF13 segment found! This file is not encoded with the Baseline DCT algorithm (i.e. SOF0 segment required). Exiting..");
                        System.exit(-1);
                    }
                    case SOF14 -> {
                        parseSOFsegment("SOF14", buffer, i - 1);
                        System.out.println("SOF14 segment found! This file is not encoded with the Baseline DCT algorithm (i.e. SOF0 segment required). Exiting..");
                        System.exit(-1);
                    }
                    case SOF15 -> {
                        parseSOFsegment("SOF15", buffer, i - 1);
                        System.out.println("SOF15 segment found! This file is not encoded with the Baseline DCT algorithm (i.e. SOF0 segment required). Exiting..");
                        System.exit(-1);
                    }
                    case DHT -> parseDHTsegment(buffer, i - 1);
                    case DQT -> parseDQTsegment(buffer, i - 1);
                    case JPG -> dumpJPGsegment("JPG", buffer, i - 1);
                    case JPG0 -> dumpJPGsegment("JPG0", buffer, i - 1);
                    case JPG1 -> dumpJPGsegment("JPG1", buffer, i - 1);
                    case JPG2 -> dumpJPGsegment("JPG2", buffer, i - 1);
                    case JPG3 -> dumpJPGsegment("JPG3", buffer, i - 1);
                    case JPG4 -> dumpJPGsegment("JPG4", buffer, i - 1);
                    case JPG5 -> dumpJPGsegment("JPG5", buffer, i - 1);
                    case JPG6 -> dumpJPGsegment("JPG6", buffer, i - 1);
                    case JPG7 -> dumpJPGsegment("JPG7", buffer, i - 1);
                    case JPG8 -> dumpJPGsegment("JPG8", buffer, i - 1);
                    case JPG9 -> dumpJPGsegment("JPG9", buffer, i - 1);
                    case JPG10 -> dumpJPGsegment("JPG10", buffer, i - 1);
                    case JPG11 -> dumpJPGsegment("JPG11", buffer, i - 1);
                    case JPG12 -> dumpJPGsegment("JPG12", buffer, i - 1);
                    case JPG13 -> dumpJPGsegment("JPG13", buffer, i - 1);
                    case DAC -> dumpDACsegment(buffer, i - 1);
                    case DNL -> dumpDNLsegment(buffer, i - 1);
                    case DRI -> dumpDRIsegment(buffer, i - 1);
                    case DHP -> dumpDHPsegment(buffer, i - 1);
                    case EXP -> dumpEXPsegment(buffer, i - 1);
                    case RST0 -> dumpRSTsegment("RST0", i - 1);
                    case RST1 -> dumpRSTsegment("RST1", i - 1);
                    case RST2 -> dumpRSTsegment("RST2", i - 1);
                    case RST3 -> dumpRSTsegment("RST3", i - 1);
                    case RST4 -> dumpRSTsegment("RST4", i - 1);
                    case RST5 -> dumpRSTsegment("RST5", i - 1);
                    case RST6 -> dumpRSTsegment("RST6", i - 1);
                    case RST7 -> dumpRSTsegment("RST7", i - 1);
                    case TEM -> dumpTEMsegment(i - 1);
                    case COM -> dumpCOMsegment(buffer, i - 1);
                    case SOS -> {
                        parseSOSsegment(buffer, i);
                        sos = 1;
                    }
                    default -> System.err.println("Marker not recognized!");
                }
                buffer[0]=PREFIX; // put the last byte read (PREFIX) in the first element of the buffer

            }
            /* Dump compressed image data (the rest of the bytes until the two EOI bytes that end the .jpg file) */
            System.out.println("Image data (scans) length: " + (fjpg.length()-2-fjpg.getFilePointer()));
            //System.out.println("The compressed bitstream follows:");
            System.out.println("========================================================================");

        } catch(EOFException e) {
            System.err.println("EOF reached.");
        } catch(IOException e) {
            System.err.println("Error: " + e.getMessage());
        }

    }

    private boolean equal(byte[] first, byte[] second, int length) {
        for(int i=0; i<length; i++)
            if(first[i]!=second[i]) return false;
        return true;
    }

    /**
     * Dumps on the screen the content of a given APP (Application) segment.
     * Since the APP segments are application dependent, we only print the
     * type of the segment (APP1,APP2,APP3,APP4,APP5,APP6,APP7,APP8,APP9,
     * APP10,APP11,APP12,APP13,APP14,APP15) and its length. The type of the
     * APP segment is specified by the type parameter (also, the type is
     * specified by buffer[0]).
     * @param type the type of the APP segment
     * @param buffer the buffer containing the segment data
     * @param len the length of buffer
     */
    private void dumpAPPsegment(String type,byte[] buffer,int len) {
        int size;
        /* the size of the segment without the APPn marker is contained in buffer[1]
         * and buffer[2](in big endian format - big byte first) */
        size = ((int) buffer[1]) & 0xff;
        size <<= 8;
        size |= ((int) buffer[2] & 0xff);
        /* check if size==len -1 */
        if (size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
        System.out.println("        "+type+" segment encountered (length="+size+").");
    }

    /**
     * Dumps on the screen the content of a given SOF (Start of Frame) segment.
     * We only print the content of a SOF0 segment, because the others are usually
     * not supported.
     * @param type the type of the SOF segment. Must be oane of: SOF0,SOF1,SOF2,
     *		   SOF3,SOF5,SOF6,SOF7,SOF9,SOF10,SOF11,SOF13,SOF14,SOF15
     * @param buffer the buffer containing the segment data
     * @param len the length of buffer
     */
    private void parseSOFsegment(String type, byte[] buffer, int len) {
        int x,z;
        if (type!=null && type.equals("SOF0")) {
            System.out.println("        "+type+" segment encountered:");
            /* the structure of the SOF0 segment is:
             * o the SOF0 marker (0xff,0xc0) - 2 bytes (buffer starts only from 0xc0, so buffer[0]=0xc0)
             * o the length of the segment (2 bytes) without the SOF0 marker, but including these
             *   2 bytes for representing the length - equal to 8+component*3
             * o data precision (1 byte) - the bits/sample, usually 8 or 12
             * o image height (2 bytes) - must be >0
             * o image width (2 bytes) - must be >0
             * o number of components (1 byte) - usually 1 (gray scaled), 3 (YUV or YIQ), 4 (CMYK)
             * o each component is described by 3 bytes of data:
             *	    .id (1 byte): 1=Y, 2=Cb, 3=Cr, 4=I, 5=Q
             *	    .sampling factors (1 byte): bits 0-3 vertical, 4-7 horizontal
             *	    .quantization table number (1 byte)
             */
            x = ((int) buffer[1]) & 0xff;
            x <<= 8;
            x |= ((int) buffer[2] & 0xff);
            System.out.println("        	.length="+x);
            /* check if size==len -1 */
            if(x != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
            x = ((int) buffer[3]) & 0xff;
            System.out.println("        	.data precision (bits/sample)="+x);
            x = ((int) buffer[4]) & 0xff;
            x <<= 8;
            x |= ((int) buffer[5] & 0xff);
            System.out.println("        	.image height = "+x);
            this.height = x;
            x = ((int) buffer[6]) & 0xff;
            x <<= 8;
            x |= ((int) buffer[7] & 0xff);
            System.out.println("        	.image width = "+x);
            this.width = x;
            // 1 (gray scaled), 3 (YUV or YIQ), 4 (CMYK)
            int noOfComponents = buffer[8];
            switch (buffer[8]) {
                case 1 -> System.out.println("        	.the image is gray scaled");
                case 3 -> System.out.println("        	.the image is in YCbCr or YIQ format");
                case 4 -> System.out.println("        	.the image is in CMYK format");
                default -> System.out.println("        	.the image has " + buffer[8] + " components");
            }
            System.out.println("        	.the image components (id= 1(Y), 2(Cb), 3(Cr), 4(I), 5(Q))");
            this.imageComponents = new ImageComponent[6];
            for (int k=0; k<=5; k++) {
                this.imageComponents[k] = new ImageComponent();
            }
            for(int i=9; i<=len-3; i+=3) {
                x=((int) buffer[i+1]) & 0x0f;
                z=((int) buffer[i+1]) & 0xf0;
                z >>= 4;
                System.out.println("        		*id="+buffer[i]+",vert_sampl_factor="+x+" horiz_sampl_factor="+z+",Q table no.="+buffer[i+2]);
                this.imageComponents[buffer[i]].id = buffer[i];
                this.imageComponents[buffer[i]].verticalSampling = x;
                this.imageComponents[buffer[i]].horizontalSampling = z;
                this.imageComponents[buffer[i]].quantizationTblIxd = buffer[i+2];
            }
            if ((noOfComponents !=3) || (this.imageComponents[1].id==-1) ||
                    (this.imageComponents[2].id==-1) || (this.imageComponents[3].id==-1)) {
                System.err.println("The components of this image are not Y Cb Cr! Exiting..");
                System.exit(-1);
            }
            int[] horizSamplingFactors = new int[3];
            int[] vertSamplingFactors = new int[3];
            for(int i=0; i<3; i++) {
                horizSamplingFactors[i] = this.imageComponents[i+1].horizontalSampling;
                vertSamplingFactors[i] = this.imageComponents[i+1].verticalSampling;
            }
            this.sampling = YCbCrImage.computeSamplingFromFactors(horizSamplingFactors, vertSamplingFactors);
            huffmanEncoder.setSamplingFactors(horizSamplingFactors, vertSamplingFactors);
            /* Here, we adjust the width and height of the decoded internal image representation based on
             * the sampling. Before we save the decoded image in the output .png file, we should set the width
             * and the height of the image back to the ones read from the SOF0 segment in the .jpg file.
             * I will skip this for simplicity reasons and allow some padding for width and height of the
             * decoded image.
             */
            int[] tuple = YCbCrImage.adjustResolutionBasedonSampling(this.width, this.height, this.sampling);
            this.width = tuple[0];
            this.height = tuple[1];
            System.out.println("        	.image width (adjusted based on sampling) = " + this.width);
            System.out.println("        	.image height (adjusted based on sampling) = " + this.height);

            System.out.println("        End of SOF0 segment");
        } else {
            /* we only print the type of the SOF segment and its size*/
            x = ((int) buffer[1]) & 0xff;
            x <<= 8;
            x |= ((int) buffer[2] & 0xff);
            /* check if size==len -1 */
            if(x != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
            System.out.println("        "+type+" segment encountered (length="+x+").");
        }
    }

    /**
     * Dumps on the screen the content of a given JPG segment. We only print
     * the type of the segment (JPG, JPG0 .. JPG13) and its length. The type
     * of the JPG segment is specified by the type parameter (also, the type
     * is specified by buffer[0]).
     * @param type the type of this JPG segment
     * @param buffer the buffer containing the segment data
     * @param len the length of buffer
     */
    private void dumpJPGsegment(String type,byte[] buffer,int len) {
        int size;
        /* the size of the segment without the JPG marker is contained in buffer[1]
         * and buffer[2](in big endian format - big byte first) */
        size = ((int) buffer[1]) & 0xff;
        size <<= 8;
        size |= ((int) buffer[2] & 0xff);
        /* check if size==len -1 */
        if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
        System.out.println("        "+type+" segment encountered (length="+size+").");
    }

    /**
     * Dumps on the screen the size of the DAC (Define Arithmetic Table)
     * segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpDACsegment(byte[] buffer,int len) {
        int size;
        /* the size of the segment without the DAC marker is contained in buffer[1]
         * and buffer[2](in big endian format - big byte first) */
        size = ((int) buffer[1]) & 0xff;
        size <<= 8;
        size |= ((int) buffer[2] & 0xff);
        /* check if size==len -1 */
        if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
        System.out.println("        Define Arithmetic Table segment encountered (length="+size+").");
    }

    /**
     * Dumps on the screen the size of the DNL segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpDNLsegment(byte[] buffer,int len) {
        int size;
        /* the size of the segment without the DNL marker is contained in buffer[1]
         * and buffer[2](in big endian format - big byte first) */
        size = ((int) buffer[1]) & 0xff;
        size <<= 8;
        size |= ((int) buffer[2] & 0xff);
        /* check if size==len -1 */
        if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
        System.out.println("        DNL segment encountered (length="+size+").");
    }

    /**
     * Dumps on the screen the size of the DHP segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpDHPsegment(byte[] buffer,int len) {
        int size;
        /* the size of the segment without the DHP marker is contained in buffer[1]
         * and buffer[2](in big endian format - big byte first) */
        size = ((int) buffer[1]) & 0xff;
        size <<= 8;
        size |= ((int) buffer[2] & 0xff);
        /* check if size==len -1 */
        if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
        System.out.println("        DHP segment encountered (length="+size+").");
    }

    /**
     * Dumps on the screen the size of the EXP segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpEXPsegment(byte[] buffer,int len) {
        int size;
        /* the size of the segment without the EXP marker is contained in buffer[1]
         * and buffer[2](in big endian format - big byte first) */
        size = ((int) buffer[1]) & 0xff;
        size <<= 8;
        size |= ((int) buffer[2] & 0xff);
        /* check if size==len -1 */
        if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
        System.out.println("        EXP segment encountered (length="+size+").");
    }

    /**
     * Dumps on the screen a RSTn segment. Since this kind of segment
     * does not have 2 bytes for its size, we just print len-1 as
     * its size and hope for the best:)
     * @param type the type of this RST segment. Must be one of
     * 		   RST0,RST1,RST2,RST3,RST4,RST5,RST6,RST7.
     * @param len the length of the buffer
     */
    private void dumpRSTsegment(String type, int len) {
        System.out.println("        "+type+" segment encountered (length="+(len-1)+").");
    }

    /**
     * Dumps on the screen a TEM segment. Since this kind of segment
     * does not have 2 bytes for its size, we just print len-1 as
     * its size and hope for the best:)
     * @param len the length of the buffer
     */
    private void dumpTEMsegment(int len) {
        System.out.println("        TEM segment encountered (length="+(len-1)+").");
    }

    /**
     * Dumps on the screen a COM (Comment) segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpCOMsegment(byte[] buffer,int len) {
        int size;
        /*
         * the structure of the COM segment is:
         * -the COM marker (0xff,0xfe) - 2 bytes (buffer starts only from 0xfe, so buffer[0]=0xfe)
         * -the length of the segment (2 bytes) without the COM marker, but including these
         *   2 bytes for representing the length
         * -the comment itself as a sequence of length-2 bytes
         */
        size = ((int) buffer[1]) & 0xff;
        size <<= 8;
        size |= ((int) buffer[2]) & 0xff;
        /* check if size==len -1 */
        if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
        //System.out.println("COM: size="+size+" len="+len);
        System.out.println("        COM segment encountered (length="+size+"): "+new String(buffer,3,size-2));
    }

    /**
     * Dumps on the screen the content of a DRI (Define Restart Interval) segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void dumpDRIsegment(byte[] buffer,int len) {
        /*
         * the structure of the DRI segment is:
         * -the DRI marker (0xff,0xdd) - 2 bytes (buffer starts only from 0xdd, so buffer[0]=0xdd)
         * -the length of the segment (2 bytes) without the DRI marker, but including these
         *   2 bytes for representing the length - it must be 4
         * -restart interval (2 bytes) - this is in units of MCU blocks, means that every n MCU
         *   blocks a RSTn marker can be found. The first marker will be RST0, then RST1 etc.,
         *   after RST7 repeating from RST0
         */
        int x,y;
        x = ((int) buffer[1]) & 0xff;
        x <<= 8;
        x |= ((int) buffer[2] & 0xff);
        /* check if size==len -1 */
        if(x != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
        y = ((int) buffer[3]) & 0xff;
        y <<= 8;
        y |= ((int) buffer[4] & 0xff);
        if(x!=4) System.out.println("        DRI segment encountered but is corrupted (length="+x+").");
        else System.out.println("        DRI segment encountered: length="+x+" restart interval="+y);
    }

    /**
     * Dumps on the screen the content of a DQT (Define Quantization Table) segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void parseDQTsegment(byte[] buffer, int len) {
        /*
         * the structure of the DQT segment is:
         * o the DQT marker (0xff,0xdb) - 2 bytes (buffer doesn't contain 0xff, it starts with 0xdb)
         * o length (2 bytes) of the DQT segment without the DQT marker, but including the bytes
         *   for this field
         * o multiple QT tables, each of them containing:
         *	    .QT information, 1 byte: bit 0..3 - number of this QT (0,1,2,3)
         *			                bit 4..7 - precision of QT (0 = 8bit, otherwise = 16bit)
         *	    .QT bytes, 64*(precision+1) bytes: the QT values
         */
        int size,i,j,no,precision,value_width,val;
        size = ((int) buffer[1]) & 0xff;
        size <<= 8;
        size |= ((int) buffer[2] & 0xff);
        /* check if size==len -1 */
        if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
        System.out.println("        DQT segment encountered (length="+size+")");
        System.out.println("        	.The Quantization Tables in ZIGZAG order:");
        i = 3;
        while(i<=size) {
            /* print one Q table */
            /* determine the no. and precision for this QT */
            no = ((int) buffer[i]) & 0x0f;
            precision = ((int) buffer[i]) & 0xf0;
            System.out.println("        	Q TABLE no. "+no+", precision "+(precision==0?"8bit":"16bit")+":");
            int[] quantTable = (no == 0 ? quantizer.QUANTUM_LUMINANCE : quantizer.QUANTUM_CHROMINANCE);
            // if there are more than 2 quantization table, this code has a problem!

            /* set the width of a Q value (1 byte or 2 bytes) */
            if(precision==0) value_width=1;
            else value_width=2;
            /* print the Q values */
            System.out.print("        	");
            int k = 0;
            /* this code is rather complicated because I wanted to catch both cases (precision=0 & precision=1) in one for run */
            for(j=i+1; j<64*value_width+i+1 && (j<=(size+1)-value_width && j<=len-value_width); j+=value_width) {
                if(precision==0) val=((int) buffer[j]) & 0xff;
                else {
                    val = ((int) buffer[j] <<8 ) & 0xff00;
                    val |=((int) buffer[j+1]) & 0xff;
                }
                System.out.print(val+",  ");
                quantTable[RunLengthEncoder.ZIGZAG_ORDER[k++]] = val;
                /* line up the new Q table */
                if (j!=i+1 && (j+value_width-(i+1))%(value_width*8)==0) {
                    System.out.println(); System.out.print("                ");
                }
            }
            i = j;
            System.out.println();
        }
        System.out.println("        DQT segment end.");
    }

    /**
     * Dumps on the screen the content of a DHT (Define Huffman Table) segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void parseDHTsegment(byte[] buffer, int len) {
        /*
         * the structure of the DHT segment is:
         * o the DHT marker (0xff,0xc4) - 2 bytes (buffer doesn't contain 0xff, it starts with 0xc4)
         * o length (2 bytes) of the DHT segment without the DHT marker, but including the bytes
         *   for this field
         * o multiple HT tables, each of them containing:
         *	    .HT information, 1 byte: bit 0..3 - number of this HT (0,1,2,3)
         *				        bit 4 - type of this HT (0=DC table, 1=AC table)
         *				        bit 5..7 - not used, must be zero
         *	    .Number of symbols, 16 bytes: number of symbols with codes of length 1,2..16.
         *				        The sum (let this sum be n) of these bytes is the
         *				        total number of codes, which must be <=256
         *	    .Symbols, n bytes (see above): table containing the symbols in order of increasing
         *				        code length (n=total number of codes)
         */

        int[] currentBitsArray = Huffman_bits_dc_luminance;
        int[] currentValsArray = Huffman_vals_dc_luminance;
        int size,i,j,k,x,no,type/* type=0 -> DC || type=1 -> AC */;
        int n;
        int[] symbol_no ={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}; // symbol_no[i] is the no. of symbols with codes of length i+1

        size = ((int) buffer[1]) & 0xff;
        size <<= 8;
        size |= ((int) buffer[2]) & 0xff;
        /* check if size==len -1 */
        if(size != len -1) {System.out.println("        Internal error: size of the segment is different than length of the buffer -1 !"); return;}
        System.out.println("        DHT segment encountered (length="+size+")");
        System.out.println("        	.The Huffman Tables are:");
        i=3;
        while(i<=size) {
            /* print one H table */
            /* determine no. and type for this HT */
            no = ((int) buffer[i]) & 0x0f;
            type = ((int) buffer[i]) & 0x10;
            type >>= 4;
            x = ((int) buffer[i]) & 0xe0;
            x >>= 5;
            if(x!=0) {
                System.out.println("        	Corrupted Huffman table!");
                return;
            }
            System.out.println("        	H table no. "+no+" type "+(type==0?"DC":"AC")+":");
            i += 1;
            /* fill the symbol_no array with proper values */
            n = 0;
            System.out.print("            Number of symbols:");
            for(j=0; j<16 && (i+j)<len ; j++) {
                symbol_no[j] = buffer[i+j] & 0xff;
                n += symbol_no[j];
                System.out.print("(" + symbol_no[j] + " symbs. with code of length " + (j+1) + ") ");
            }
            System.out.println();

            if (type==0) {
                if (no==0) {
                    currentBitsArray = Huffman_bits_dc_luminance;
                    currentValsArray = Huffman_vals_dc_luminance;
                } else {
                    currentBitsArray = Huffman_bits_dc_chrominance;
                    currentValsArray = Huffman_vals_dc_chrominance;
                }
            }
            if (type==1) {
                if (no==0) {
                    currentBitsArray = Huffman_bits_ac_luminance;
                    currentValsArray = Huffman_vals_ac_luminance;
                } else {
                    currentBitsArray = Huffman_bits_ac_chrominance;
                    currentValsArray = Huffman_vals_ac_chrominance;
                }
            }
            System.arraycopy(symbol_no, 0, currentBitsArray, 1, 16);

            if(n>256) {System.out.println("            The no. of symbols in HT is >256!"); return;}
            i += j;
            k = 0;
            /* reading the symbols (n bytes) */
            for(j=0; j<16; j++) {
                System.out.print("        	");
                if(symbol_no[j]==0) {
                    System.out.print("-");
                } else {
                    for (int initial_k = k; (k-initial_k < symbol_no[j]) && (i < len && i <= size); k++) {
                        /* reading symbol_no[j] of length j from buffer */
                        currentValsArray[k] = buffer[i] & 0xff;
                        System.out.print(printHex(buffer[i]) + ",  ");
                        i++;
                    }
                }
                System.out.println();
            }

        }
        System.out.println("        DHT segment end.");
    }

    /**
     * Dumps on the screen the content of an SOS (Start of Scan) segment.
     * @param buffer the buffer containing the segment data
     * @param len the length of the buffer
     */
    private void parseSOSsegment(byte[] buffer, int len) {
        /*
         * the structure of an SOS segment:
         * -the SOS marker (0xff,0xda) - 2 bytes (buffer doesn't contain 0xff, it starts with 0xda)
         * -length (2 bytes) of the SOS segment without the SOS marker, but including the bytes
         *   for this field - must be equal to 6+2*(no. of components in scan)
         * -number of components in scan (1 byte) - must be 1,2,3 or 4, otherwise error
         * -each component (2 bytes):
         *	.component ID, 1 byte: (1=Y, 2=Cb, 3=Cr, 4=I, 5=Q)
         *	.Huffman tables to use, 1 byte: bit 0..3 - AC table (0..3)
         *					bit 4..7 - DC table (0..3)
         * -ignorable bytes (3 bytes):
         *	.start of spectral selection (1 byte)
         *	.end of spectral selection (1 byte)
         *	.1 ignorable byte (?)
         *
         * After the SOS segment, the compressed bits of the image follow (i.e. the scan pixels)
         * until we get to the two EOI bytes (0xff 0xd9) that complete the file.
         */
        int size,no,i,j,id,huff,dc,ac,ss,es;
        /* compute size */
        size = ((int) buffer[1]) & 0xff;
        size <<= 8;
        size |= ((int) buffer[2] & 0xff);
        no = ((int) buffer[3]) & 0xff;
        System.out.println("        SOS (Start of Scan) segment encountered: length "+size+" no. of components "+no);
        /* compute number of components */
        if(size!=6+2*no) {System.err.println("        SOS segment is corrupted!"); return;}
        i = 4;
        /* read each component */
        for(j=0; j<no && (i<size-1 && i<len-1); j++) {
            id=((int) buffer[i]) & 0xff;
            huff=((int) buffer[i+1]) & 0xff;
            i += 2;
            dc = (huff & 0xf0) >>4;
            ac = (huff & 0x0f);
            imageComponents[id].DChuffmanTblIdx = dc;
            imageComponents[id].AChuffmanTblIdx = ac;
            System.out.println("        	.Component "+id+" AC table="+ac+" DC table="+dc);
        }
        /* if those 3 ignorable bytes are not present, error? */
        if(i+3 != len) System.out.println("        SOS segment corrupted.");
        ss = ((int) buffer[i]) & 0xff;
        es = ((int) buffer[i+1]) & 0xff;
        System.out.println("        	.Start of spectral selection: "+ss);
        System.out.println("        	.End of spectral selection: "+es);
        System.out.println("        End of SOS segment");
    }

    private int[] computeNextBlockPosition(char blocktype) {
        int Ypos, Xpos;
        int samplingWidth = width;
        int hSamplingFactor, vSamplingFactor, MCUIndex, innerMCUIdx;
        int MCUtopPos, MCUleftPos, OYoffset, OXoffset;
        MCUIndex = innerMCUIdx = 0;
        hSamplingFactor = vSamplingFactor = 1;

        if ((blocktype!='Y') && (blocktype!='U') && (blocktype!='V'))
            return new int[] {-1, -1};
        switch (blocktype) {
            case 'Y' -> {
                hSamplingFactor = imageComponents[1].horizontalSampling;
                vSamplingFactor = imageComponents[1].verticalSampling;
                MCUIndex = YMCUIndex;
                innerMCUIdx = YinnerMCUIdx;
            }
            case 'U' -> {
                hSamplingFactor = imageComponents[2].horizontalSampling;
                vSamplingFactor = imageComponents[2].verticalSampling;
                MCUIndex = CbMCUIndex;
                innerMCUIdx = CbinnerMCUIdx;
            }
            case 'V' -> {
                hSamplingFactor = imageComponents[3].horizontalSampling;
                vSamplingFactor = imageComponents[3].verticalSampling;
                MCUIndex = CrMCUIndex;
                innerMCUIdx = CrinnerMCUIdx;
            }
        }

        // here we don't deal with all possible (horizontalSamplingFactor,verticalSamplingFactor)
        // combinations, only the most common ones, but this should be ok in most JPG pictures
        if ((sampling==YCbCrImage.YUV422HorizSampling) || (sampling==YCbCrImage.YUV411Sampling)) {
            if (blocktype!='Y') samplingWidth = width / 2;
        }

        MCUtopPos = (MCUIndex * hSamplingFactor * 8) / samplingWidth;
        MCUtopPos *= vSamplingFactor * 8;
        MCUleftPos = (MCUIndex * hSamplingFactor * 8) % samplingWidth;

        OYoffset = (innerMCUIdx / hSamplingFactor) * 8;
        OXoffset = (innerMCUIdx % hSamplingFactor) * 8;

        Ypos = MCUtopPos + OYoffset;
        Xpos = MCUleftPos + OXoffset;

        if ((Ypos%8!=0) || (Xpos%8!=0)) {
            System.out.println("Decoder::computeNextBlockPosition() something has gone wrong, " +
                    "block start positions are not multiple of 8!");
        }

        // update MCUIndex and innerMCUIdx for the next block
        innerMCUIdx++;
        if (innerMCUIdx >= hSamplingFactor*vSamplingFactor) {
            innerMCUIdx = 0;
            MCUIndex++;
        }

        // update the global MCUIndex and innerMCUIdx
        switch (blocktype) {
            case 'Y' -> {
                YMCUIndex = MCUIndex;
                YinnerMCUIdx = innerMCUIdx;
            }
            case 'U' -> {
                CbMCUIndex = MCUIndex;
                CbinnerMCUIdx = innerMCUIdx;
            }
            case 'V' -> {
                CrMCUIndex = MCUIndex;
                CrinnerMCUIdx = innerMCUIdx;
            }
        }

        return new int[] {Xpos, Ypos};
    }

    /**
     * A function that returns a byte's representation in hex format
     * (because I couldn't find one in the millions of java classes from the jdk)
     */
    private String printHex(byte b) {
        char[] digits={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] s =new char[4];
        int i,j;
        i = ((int) b) & 0xf0;
        i >>= 4;
        j = ((int) b) & 0x0f;
        s[0]='0'; s[1]='x'; s[2]=digits[i]; s[3]=digits[j];
        return new String(s);
    }

}

package jevc.service;

import jevc.entities.*;
import jevc.operations.*;
import jevc.utils.ByteConverter;
import jevc.utils.JVidWriter;
import jevc.utils.ProgressStatus;

import java.io.*;
import java.util.ArrayList;

public class JVidDecoderService {
    private int bitstreamSize;

    private File file;
    private RandomAccessFile fileAccess;
    private final String outputFolder;
    private String outputFile;
    private BufferedOutputStream outputStream;

    private final InternalFrameBuffer internalFrameBuffer;
    private final DiscreteCosineTransform DCT;
    private final Quantizer quantizer;
    private RunLengthEncoder runLengthEncoder;
    private HuffmanEncoder huffmanEncoder;
    private final BlockBuffer blockBuffer;
    private MotionEstimator motionEstimator;

    private final JVidWriter jVidWriter;
    private ProgressStatus progressStatus;

    private ImageComponent[] imageComponents;
    private int YMCUIndex, CbMCUIndex, CrMCUIndex;
    private int YinnerMCUIdx, CbinnerMCUIdx, CrinnerMCUIdx;

    int[] Huffman_bits_dc_luminance = new int[17];
    int[] Huffman_bits_dc_chrominance = new int[17];
    int[] Huffman_bits_ac_luminance = new int[17];
    int[] Huffman_bits_ac_chrominance = new int[17];
    int[] Huffman_vals_dc_luminance = new int[12];
    int[] Huffman_vals_dc_chrominance = new int[12];
    int[] Huffman_vals_ac_luminance = new int[162];
    int[] Huffman_vals_ac_chrominance = new int[162];

    public JVidDecoderService(File file, String outputFolder) throws FileNotFoundException {
        this.file = file;
        this.outputFolder = outputFolder;
        this.internalFrameBuffer = new InternalFrameBuffer();
        this.DCT = new DiscreteCosineTransform();
        this.quantizer = new Quantizer();
        this.huffmanEncoder = new HuffmanEncoder();
        this.jVidWriter = new JVidWriter();
        this.blockBuffer = new BlockBuffer();
        this.progressStatus = new ProgressStatus();

        // set jpeg compression parameters
        huffmanEncoder.setSamplingFactors(new int[] {1, 1, 1}, new int[] {1, 1, 1});
        Huffman_bits_dc_luminance[0] = 0x00;
        Huffman_bits_dc_chrominance[0] = 0x01;
        Huffman_bits_ac_luminance[0] = 0x10;
        Huffman_bits_ac_chrominance[0] = 0x11;
//        huffmanEncoder.initializeHuffmanTables(
//                Huffman_bits_dc_luminance, Huffman_bits_dc_chrominance, Huffman_bits_ac_luminance, Huffman_bits_ac_chrominance,
//                Huffman_vals_dc_luminance, Huffman_vals_dc_chrominance, Huffman_vals_ac_luminance, Huffman_vals_ac_chrominance
//        );

        // read header values
        this.fileAccess = new RandomAccessFile(System.getProperty("user.dir") + file.getPath(), "r");
        int[] headerValues = jVidWriter.readHeader(fileAccess);
        int bitstreamSize = headerValues[0];
        int fps = headerValues[1];
        int frameCount = headerValues[2];
        int bitrate = headerValues[3];
        int width = headerValues[4];
        int height = headerValues[5];

        // ensure header values are valid
        if (bitstreamSize == -1 || fps == -1 || frameCount == -1 || bitrate == -1 || width == -1 || height == -1) {
            throw new IllegalArgumentException("Invalid header values");
        }

        // set globals
        this.bitstreamSize = bitstreamSize;
        Globals.FRAMERATE = fps;
        Globals.FRAMECOUNT = frameCount;
        Globals.BITRATE = bitrate;
        Globals.MAX_WIDTH = width;
        Globals.MAX_HEIGHT = height;
    }

    public void decompress() throws IOException, InterruptedException {
        // Read the bitstream
        // First 4 bytes = sdat
        // From here, read the rest of the headers (8 bytes) then read the data chunk
        // Process the data chunk => frame
        // Repeat until EOF

        int index = 0;
        byte[] buffer = new byte[4];

        while (true) {
            try {
                index++;

                // sdat, size, type
                if (!ByteConverter.checkString(fileAccess, buffer, "sdat")) {
                    throw new IOException("Invalid JVid Stream Data Chunk");
                }
                int chunkSize = ByteConverter.readInt(fileAccess, buffer);
                System.out.println("Chunk Size: " + chunkSize);

                String type = ByteConverter.readString(fileAccess, buffer);
                System.out.println("Chunk Type: " + type);

                // Read the data chunk
                byte[] data = new byte[chunkSize];
                fileAccess.readFully(data, 0, chunkSize);
                processFrame(data, type, chunkSize, index);
            } catch (IOException e) {
                // EOF
                break;
            }

        }

        // Done processing frames,  close the file and play video
        fileAccess.close();

        // Play video
        // TODO: Refactor this to take into account the OS
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", "ffplay", "-framerate", "25", "-i", "frame%03d.png");
        processBuilder.directory(new File(outputFolder));
        processBuilder.start();
    }

    private void processFrame(byte[] data, String type, int chunkSize, int index) throws IOException {
        // Process the data chunk => frame
        runLengthEncoder = new RunLengthEncoder();
        ArrayList<RunLengthBlock> rleBlocksArray;
        ArrayList<Block> blocks = new ArrayList<>();
        Block block;

        // Check frame type
        if (type.equals("comp")) {
            // comp => I-frame -> JPEG decode

            YMCUIndex = CbMCUIndex = CrMCUIndex = 0;
            YinnerMCUIdx = CbinnerMCUIdx = CrinnerMCUIdx = 0;

            // Huffman Decode
            rleBlocksArray = huffmanEncoder.decode(data, Globals.SAMPLING, Globals.MAX_WIDTH, Globals.MAX_HEIGHT);
            for (RunLengthBlock rleBlock: rleBlocksArray) {
                // RL Decode
                block = runLengthEncoder.decode(rleBlock);
                int[] pos = computeNextBlockPosition(block.getType());
                block.setPos(pos[0], pos[1]);

                //Dequantize
                quantizer.dequantize(block);

                // IDCT
                DCT.inverse(block);

                blocks.add(block);

                // Add to block buffer
                blockBuffer.save(block);
            }

        } else {
            // inte => P-frame -> motion compensation
            //            -> Parse codeword change
            //            -> Obtain motion vector
            //            -> Huffman Decode Error
            //            -> RL Decode Error
            //            -> Dequantize Error
            //            -> IDCT Error
            //            -> Motion Compensation

        }

        // Upsample
        YCbCrImage image = new YCbCrImage(blocks, Globals.MAX_HEIGHT, Globals.MAX_WIDTH, Globals.SAMPLING);
        image.PerformUpsampling();

        // Convert to RGB
        RGBImage decodedImage = image.convertToRGBImage();

        // Write to file
        decodedImage.writePNGFile(String.format("%sframe%03d.png", outputFolder, index));
    }

    private int[] computeNextBlockPosition(char blocktype) {
        int Ypos, Xpos;
        int samplingWidth = Globals.MAX_WIDTH;
        int hSamplingFactor, vSamplingFactor, MCUIndex, innerMCUIdx;
        int MCUtopPos, MCUleftPos, OYoffset, OXoffset;
        MCUIndex = innerMCUIdx = 0;
        hSamplingFactor = vSamplingFactor = 1;

        if ((blocktype!='Y') && (blocktype!='U') && (blocktype!='V'))
            return new int[] {-1, -1};
        switch (blocktype) {
            case 'Y' -> {
                // hSamplingFactor = imageComponents[1].horizontalSampling;
                // vSamplingFactor = imageComponents[1].verticalSampling;
                MCUIndex = YMCUIndex;
                innerMCUIdx = YinnerMCUIdx;
            }
            case 'U' -> {
                // hSamplingFactor = imageComponents[2].horizontalSampling;
                // vSamplingFactor = imageComponents[2].verticalSampling;
                MCUIndex = CbMCUIndex;
                innerMCUIdx = CbinnerMCUIdx;
            }
            case 'V' -> {
                // hSamplingFactor = imageComponents[3].horizontalSampling;
                // vSamplingFactor = imageComponents[3].verticalSampling;
                MCUIndex = CrMCUIndex;
                innerMCUIdx = CrinnerMCUIdx;
            }
        }

        // here we don't deal with all possible (horizontalSamplingFactor,verticalSamplingFactor)
        // combinations, only the most common ones, but this should be ok in most JPG pictures
        if ((Globals.SAMPLING== YCbCrImage.YUV422HorizSampling) || (Globals.SAMPLING==YCbCrImage.YUV411Sampling)) {
            if (blocktype!='Y') samplingWidth = Globals.MAX_WIDTH / 2;
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


}

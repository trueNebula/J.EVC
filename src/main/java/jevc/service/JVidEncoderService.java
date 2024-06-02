package jevc.service;

import com.google.common.base.Stopwatch;
import jevc.entities.*;
import jevc.operations.*;
import jevc.utils.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class JVidEncoderService {
    // I/O
    private final File[] files;
    private final InternalFrameBuffer internalFrameBuffer;
    private BufferedOutputStream outputStream;
    private BufferedOutputStream tempOutputStream;
    private final String outputFolder;
    private final String outputFile;
    private final Logger logger;

    // Operations
    private final DiscreteCosineTransform DCT;
    private final Quantizer quantizer;
    private RunLengthEncoder runlengthEncoder;
    private HuffmanEncoder huffmanEncoder;
    private final BlockBuffer blockBuffer;
    private MotionEstimator motionEstimator;
    private final JpgWriter jpgWriter;
    private final AVIWriter aviWriter;
    private final JVidWriter jVidWriter;
    private final LapStopwatch stopwatch;

    // Flags
    private final boolean compressToMjpeg;
    private final boolean enableBenchmarking;
    private final boolean isDebug;
    private final boolean perFrame;
    private final boolean perGop;
    private final boolean frameOperation;

    public JVidEncoderService(File[] files, String output, String parallelization, boolean compressToMjpeg, boolean enableBenchmarking, String export, boolean isDebug, boolean isQuiet) {
        this.files = files;
        this.outputFile = getOutputFile(output);
        this.outputFolder = getOutputFolder(output);
        this.internalFrameBuffer = new InternalFrameBuffer();

        DCT = new DiscreteCosineTransform();
        int DEFAULT_QUALITY_FACTOR = 80;
        quantizer = new Quantizer(DEFAULT_QUALITY_FACTOR);

        this.jpgWriter = new JpgWriter(quantizer);
        this.aviWriter = new AVIWriter();
        this.jVidWriter = new JVidWriter();
        this.blockBuffer = new BlockBuffer();

        this.stopwatch = new LapStopwatch();

        // Flags
        this.compressToMjpeg = compressToMjpeg;
        this.enableBenchmarking = enableBenchmarking;
        this.isDebug = isDebug;
        this.perFrame = parallelization.contains("f");
        this.perGop = parallelization.contains("g");
        this.frameOperation = parallelization.contains("o");
        this.logger = new Logger(isQuiet, enableBenchmarking, export);

        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(output));
            tempOutputStream = new BufferedOutputStream(
                    new FileOutputStream(outputFolder + "temp/temp")
            );
        } catch (Exception ex) {
            logger.error("Error opening the output stream!");
        }
    }

    public void compress() throws IOException {
        logger.log("Starting encoding");

        DWORD videoWidth = new DWORD(0);
        DWORD videoHeight = new DWORD(0);
        BufferedImage img;

        int frameIndex = 0;
        for (File f: this.files) {
            // set Frame type
            char frameType = 'P';
            if (frameIndex % 10 == 0) {
                frameType = 'I';
            }
            frameIndex++;

            // read image and convert to YCbCr
            img = ImageIO.read(f);
            videoWidth = new DWORD(img.getWidth());
            videoHeight = new DWORD(img.getHeight() + 8);

            int[][] pixels = new int[img.getHeight()][img.getWidth()];

            for (int i = 0; i < img.getHeight(); i++)
                for(int j = 0; j < img.getWidth(); j++) {
                    pixels[i][j] = img.getRGB(j, i);
                }
            YCbCrImage frame = new YCbCrImage(pixels, img.getWidth(), img.getHeight());

            stopwatch.reset();
            stopwatch.start();

            if (this.compressToMjpeg) {
                encodeFrameMjpg(frame, f.getName());
            } else {
                encodeFrameJvid(frame, frameType, f.getName());
            }

            stopwatch.stop();
            logger.benchmark(stopwatch, f.getName());
        }


        logger.log("Writing file header");

        if (this.compressToMjpeg) {
            aviWriter.writeAVIHeader(outputStream, this.files.length, videoWidth, videoHeight);
        } else {
            jVidWriter.writeJvidHeader(outputStream, this.files.length, videoWidth, videoHeight);
        }

        logger.log("File header written, writing frames");
        File temp = new File(outputFolder + "temp/temp");
        Files.copy(temp.toPath(), outputStream);
        outputStream.flush();

        if (this.compressToMjpeg) {
            logger.log("Frames written, writing Idx1");
            aviWriter.writeIdx1(outputStream, this.files.length);
        }

        logger.log("Output file: " + outputFile);

    }

    private void encodeFrameMjpg(YCbCrImage frame, String frameName) throws IOException {
        // DEBUG
        // BufferedOutputStream frameOutputStream = new BufferedOutputStream(
        //         new FileOutputStream(outputFolder + f.getName().replace(".png", ".jpg"))
        // );

        // init encoders
        logger.updateProgressStatus(0, "Initializing Encoders...", frameName);
        runlengthEncoder = new RunLengthEncoder();
        huffmanEncoder = new HuffmanEncoder();

        // scale and subsample image
        logger.updateProgressStatus(2, "Subsampling Image...", frameName);
        frame.ScaleImage();
        //image.PerformSubsampling(YCbCrImage.YUV411Sampling);
        frame.PerformSubsampling(YCbCrImage.YUV444Sampling);

        // split frame into blocks
        logger.updateProgressStatus(5, "Performing block splitting...", frameName);
        ArrayList<Block> blocks = frame.PerformBlockSplitting();
        RunLengthBlock rleBlock;

        // write JPEG header sections
        logger.updateProgressStatus(8, "Writing frame header...", frameName);
        jpgWriter.writeHeaderSections(internalFrameBuffer, frame);
        logger.updateProgressStatus(10, "Processing blocks...", frameName);

        // process each block
        int blockIndex = 0;
        for (Block block: blocks) {
            blockIndex++;
            logger.updateProgressStatus((int) ((double) blockIndex / blocks.size() * 100 * 0.9), "Processing blocks...", frameName);

            // perform DCT
            DCT.forward(block);

            // quantize DCT coefficients
            quantizer.quantize(block);

            // inverse quantize and inverse DCT a given block
            Block savedBlock = block.getCopy();
            quantizer.dequantize(savedBlock);
            DCT.inverse(savedBlock);

            // save it in a block buffer
            blockBuffer.save(savedBlock);
            // proceed to VLC

            // VLC encode block
            rleBlock = runlengthEncoder.encode(block);
            huffmanEncoder.encode(internalFrameBuffer, rleBlock);
        }

        logger.updateProgressStatus(90, "Finishing up...", frameName);

        // flush buffers
        huffmanEncoder.flushBuffer(internalFrameBuffer);

        // DEBUG
        // internalFrameBuffer.dumpBufferToStreamWithoutFlushing(frameOutputStream);

        // write JPEG trailer section
        jpgWriter.writeTrailerSection(internalFrameBuffer);
        logger.updateProgressStatus(101, "Finished!", frameName);

        aviWriter.writeDataChunk(tempOutputStream, internalFrameBuffer);
        tempOutputStream.flush();
    }
    private void encodeFrameJvid(YCbCrImage frame, char frameType, String frameName) throws IOException {
        // debug
        BufferedOutputStream frameOutputStream = null;
        InternalFrameBuffer frameOutputBuffer = null;
        if (isDebug) {
            frameOutputStream = new BufferedOutputStream(
                    new FileOutputStream(outputFolder + frameName.replace(".png", ".jpg"))
            );
            frameOutputBuffer = new InternalFrameBuffer();
        }

        // init encoders
        logger.updateProgressStatus(0, "Initializing Encoders...", frameName);
        if (frameType == 'I') {
            runlengthEncoder = new RunLengthEncoder();
        } else if (frameType == 'P') {
            runlengthEncoder = new RunLengthEncoder('P');
        }
        huffmanEncoder = new HuffmanEncoder();

        // scale and subsample image
        logger.updateProgressStatus(2, "Subsampling Image...", frameName);
        frame.ScaleImage();
        //image.PerformSubsampling(YCbCrImage.YUV411Sampling);
        frame.PerformSubsampling(YCbCrImage.YUV444Sampling);

        // split frame into blocks
        logger.updateProgressStatus(5, "Performing block splitting...", frameName);
        ArrayList<Block> blocks = frame.PerformBlockSplitting();
        RunLengthBlock rleBlock;
        stopwatch.lap(TimeUnit.MILLISECONDS);

        // process each block
        logger.updateProgressStatus(10, "Processing blocks...", frameName);

        // This is used to determine if we have to write a new codeword for the block
        // pBlockCodeword = mvec => only motion vector is written
        // pBlockCodeword = errb => motion vector and error block data is written
        // only write the codeword once per change
        String pBlockCodeword = "";
        boolean codewordChanged;

        int blockIndex = 0;
        for (Block block: blocks) {
            blockIndex++;
            logger.updateProgressStatus((int) (((double) blockIndex / blocks.size() * 100 * 0.9)), "Processing blocks...", frameName);

            // I frame => perform DCT, quantization, inverses, push to buffer, proceed to VLC
            // P frame => search in block buffer, estimate motion, subtract, perform DCT, quantization,
            //            proceed to VLC
            if (frameType == 'P') {
                motionEstimator = new MotionEstimator();

                // look through block buffer for the most similar block
                Block similarBlock = blockBuffer.getSimilarBlock(block);

                // compute motion vector
                MotionVector motionVector = motionEstimator.computeMotionVector(block, similarBlock);

                // subtract found block from input block
                block.subtract(similarBlock);

                // if the error is 0, don't write it
                if (!block.isEmpty()) {
                    codewordChanged = !pBlockCodeword.equals("errb");
                    pBlockCodeword = "errb";

                    // perform DCT
                    DCT.forward(block);

                    // quantize DCT coefficients
                    quantizer.quantize(block);
                } else {
                    codewordChanged = !pBlockCodeword.equals("mvec");
                    pBlockCodeword = "mvec";
                }

                // write motion vector to buffer
                if (codewordChanged) {
                    // codeword changed, write it
                    internalFrameBuffer.write(new DWORD(pBlockCodeword).byteValue());
                }
                internalFrameBuffer.write(motionVector.byteValue());
            } else if (frameType == 'I'){
                // save a copy in the block buffer
                Block savedBlock = block.getCopy();

                // perform DCT
                DCT.forward(block);

                // quantize DCT coefficients
                quantizer.quantize(block);

                // save it in a block buffer
                blockBuffer.save(savedBlock);
            }

            if (frameType == 'I') {
                // VLC encode block
                rleBlock = runlengthEncoder.encode(block);
                huffmanEncoder.encode(internalFrameBuffer, rleBlock);
            } else if (frameType == 'P' && !block.isEmpty()) {
                // VLC encode block
                rleBlock = runlengthEncoder.encode(block);
                huffmanEncoder.encode(internalFrameBuffer, rleBlock);
                // EOB
                internalFrameBuffer.write(new WORD((byte) 255, (byte) 255).byteValue());
            }
        }

        stopwatch.lap(TimeUnit.MILLISECONDS);
        logger.updateProgressStatus(90, "Finishing up...", frameName);

        // flush buffers
        huffmanEncoder.flushBuffer(internalFrameBuffer);

        // debug
        if (isDebug) {
            internalFrameBuffer.dumpBufferToStreamWithoutFlushing(frameOutputStream);
            jpgWriter.writeTrailerSection(frameOutputBuffer);
            frameOutputBuffer.dumpBufferToStream(frameOutputStream);
            frameOutputStream.flush();
        }

        logger.updateProgressStatus(100, "Finished!", frameName);

        jVidWriter.writeDataChunk(tempOutputStream, internalFrameBuffer, frameType);
        tempOutputStream.flush();
        stopwatch.lap(TimeUnit.MILLISECONDS);
    }

    private static String getOutputFile(String filePath) {
        String[] parts = filePath.split("[/\\\\]");
        if (parts.length < 2) {
            // Handle cases where there is no folder in the path
            return filePath;
        }
        return parts[parts.length - 1];
    }

    private static String getOutputFolder(String filePath) {
        String[] parts = filePath.split("[/\\\\]");
        if (parts.length < 2) {
            // Handle cases where there is no folder in the path
            return "";
        }
        return filePath.substring(0, filePath.length() - parts[parts.length - 1].length());
    }

}

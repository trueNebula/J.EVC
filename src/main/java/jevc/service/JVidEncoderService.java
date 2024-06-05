package jevc.service;

import jevc.entities.*;
import jevc.operations.*;
import jevc.utils.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final BlockBuffer blockBuffer;
    private MotionEstimator motionEstimator;
    private final JpgWriter jpgWriter;
    private final AVIWriter aviWriter;
    private final JVidWriter jVidWriter;
    private final LapStopwatch stopwatch;

    // Multithreading
    private final ExecutorService executorService;
    private ConcurrentHashMap<Integer, InternalFrameBuffer> internalFrameBuffers;

    // Flags
    private final boolean compressToMjpeg;
    private final boolean isDebug;
    private final boolean perFrame;
    private final boolean perGop;
    private final boolean frameOperation;
    private final boolean combination;

    public JVidEncoderService(File[] files, String output, String parallelization, boolean compressToMjpeg, boolean enableBenchmarking, String export, boolean isDebug, boolean isQuiet) {
        this.files = files;
        this.outputFile = getOutputFile(output);
        this.outputFolder = getOutputFolder(output);
        this.internalFrameBuffer = new InternalFrameBuffer();

        this.internalFrameBuffers = new ConcurrentHashMap<>();

        DCT = new DiscreteCosineTransform();
        int DEFAULT_QUALITY_FACTOR = 80;
        quantizer = new Quantizer(DEFAULT_QUALITY_FACTOR);

        this.jpgWriter = new JpgWriter(quantizer);
        this.aviWriter = new AVIWriter();
        this.jVidWriter = new JVidWriter();
        this.blockBuffer = new BlockBuffer();

        this.stopwatch = new LapStopwatch(TimeUnit.MILLISECONDS);

        if (parallelization.length() > 0) {
            this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        } else {
            this.executorService = null;
        }

        // Flags
        this.compressToMjpeg = compressToMjpeg;
        this.isDebug = isDebug;

        this.perFrame = parallelization.contains("f");
        this.perGop = parallelization.contains("g");
        this.frameOperation = parallelization.contains("o");
        this.combination = parallelization.contains("c");

        this.logger = new Logger(isQuiet, enableBenchmarking, export, parallelization.length() > 0);

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
        if (perFrame) {
            compressPerFrame();
        }
        if (perGop) {
            compressPerGop();
        }
        if (frameOperation) {
//            compressFrameOperation();
        }
        if (combination) {
//            compressCombination();
        }
        if (!perFrame && !perGop && !frameOperation && !combination) {
            compressSequentially();
        }
    }

    public void compressSequentially() throws IOException {
        logger.log("Starting encoding");

        List<File> gop = new ArrayList<>();
        DWORD videoWidth = new DWORD(0);
        DWORD videoHeight = new DWORD(0);
        YCbCrImage frame;
        String frameName;

        int frameIndex = 0;

        while (frameIndex < this.files.length) {
            // Read GOP
            gop.clear();

            for (int i = 0; i < Globals.GOP_SIZE; i++) {
                gop.add(this.files[frameIndex]);
                frameIndex++;
            }

            // Process I frame
            frame = readImage(gop.get(0));
            videoWidth = new DWORD(frame.getWidth());
            videoHeight = new DWORD(frame.getHeight() + 8);
            frameName = gop.get(0).getName();

            stopwatch.reset();
            stopwatch.start();
            stopwatch.startFrame(0);

            if (this.compressToMjpeg) {
                processFrameMjpeg(frame, frameName);
            } else {
                processIFrameJvid(frame, frameName);
            }
            logger.benchmark(stopwatch, frameName, 0);

            // Process P frames
            for (int i = 1; i < Globals.GOP_SIZE; i++) {
                frame = readImage(gop.get(i));
                stopwatch.startFrame(i);
                if (this.compressToMjpeg) {
                    processFrameMjpeg(frame, gop.get(i).getName());
                } else {
                    processPFrameJvid(frame, gop.get(i).getName(), i);
                }
                stopwatch.stopFrame(i);
                logger.benchmark(stopwatch, frameName, i);
            }
        }

        stopwatch.stop();
        logger.benchmark(stopwatch);
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
    public void compressPerFrame() throws IOException {
        logger.log("Starting encoding");

        List<File> gop = new ArrayList<>();
        DWORD videoWidth = new DWORD(0);
        DWORD videoHeight = new DWORD(0);
        YCbCrImage frame;
        String frameName;

        int frameIndex = 0;

        while (frameIndex < this.files.length) {
            // Read GOP
            gop.clear();

            for (int i = 0; i < Globals.GOP_SIZE; i++) {
                gop.add(this.files[frameIndex]);
                frameIndex++;
            }

            // Process I frame
            frame = readImage(gop.get(0));
            videoWidth = new DWORD(frame.getWidth());
            videoHeight = new DWORD(frame.getHeight() + 8);
            frameName = gop.get(0).getName();

            stopwatch.reset();
            stopwatch.start();
            stopwatch.startFrame(0);

            if (this.compressToMjpeg) {
                processFrameMjpeg(frame, frameName);
            } else {
                processIFrameJvid(frame, frameName);
            }
            stopwatch.stopFrame(0);
            logger.benchmark(stopwatch, frameName, 0);

            // Process P frames
            for (int i = 1; i < Globals.GOP_SIZE; i++) {
                frame = readImage(gop.get(i));
                frameName = gop.get(i).getName();

                if (this.compressToMjpeg) {
                    processFrameMjpeg(frame, frameName);
                } else {
                    executorService.execute(runnableProcessPFrameJvid(i, frame, frameName));
//                    processPFrameJvid(frame, gop.get(i).getName());
                }
            }
        }
        while(!executorService.isTerminated()) {
            executorService.shutdown();
        }

        stopwatch.stop();
        logger.benchmark(stopwatch);

        for(InternalFrameBuffer buffer : internalFrameBuffers.values()) {
            jVidWriter.writeDataChunk(tempOutputStream, buffer, 'P');
        }
        tempOutputStream.flush();

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
    public void compressPerGop() throws IOException {
        logger.log("Starting encoding");

        ConcurrentHashMap<Integer, List<File>> gops = new ConcurrentHashMap<>();
        DWORD videoWidth = new DWORD(0);
        DWORD videoHeight = new DWORD(0);
        YCbCrImage frame;
        String frameName;

        int frameIndex = 0;

        while (frameIndex < this.files.length) {

        }
    }

    private void processFrameMjpeg(YCbCrImage frame, String frameName) throws IOException {
        // DEBUG
        BufferedOutputStream frameOutputStream = null;
        if (isDebug) {
            frameOutputStream = new BufferedOutputStream(
                    new FileOutputStream(outputFolder + frameName.replace(".png", ".jpg"))
            );
        }
        // init encoders
        logger.updateProgressStatus(0, "Initializing Encoders...", frameName);
        RunLengthEncoder runLengthEncoder = new RunLengthEncoder();
        HuffmanEncoder huffmanEncoder = new HuffmanEncoder();

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
            rleBlock = runLengthEncoder.encode(block);
            huffmanEncoder.encode(internalFrameBuffer, rleBlock);
        }

        logger.updateProgressStatus(90, "Finishing up...", frameName);

        // flush buffers
        huffmanEncoder.flushBuffer(internalFrameBuffer);

        // DEBUG
        if (isDebug) {
            internalFrameBuffer.dumpBufferToStreamWithoutFlushing(frameOutputStream);
        }

        // write JPEG trailer section
        jpgWriter.writeTrailerSection(internalFrameBuffer);
        aviWriter.writeDataChunk(tempOutputStream, internalFrameBuffer);
        tempOutputStream.flush();

        logger.updateProgressStatus(101, "Finished!", frameName);

    }
    private void processFrameJvid(YCbCrImage frame, char frameType, String frameName) throws IOException {
        // DEBUG
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
        RunLengthEncoder runLengthEncoder = null;
        if (frameType == 'I') {
            runLengthEncoder = new RunLengthEncoder();
        } else if (frameType == 'P') {
            runLengthEncoder = new RunLengthEncoder('P');
        }
        HuffmanEncoder huffmanEncoder = new HuffmanEncoder();

        // scale and subsample image
        logger.updateProgressStatus(2, "Subsampling Image...", frameName);
        frame.ScaleImage();
        //image.PerformSubsampling(YCbCrImage.YUV411Sampling);
        frame.PerformSubsampling(YCbCrImage.YUV444Sampling);

        // split frame into blocks
        logger.updateProgressStatus(5, "Performing block splitting...", frameName);
        ArrayList<Block> blocks = frame.PerformBlockSplitting();
        RunLengthBlock rleBlock;
        stopwatch.lap();

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
                rleBlock = runLengthEncoder.encode(block);
                huffmanEncoder.encode(internalFrameBuffer, rleBlock);
            } else if (frameType == 'P' && !block.isEmpty()) {
                // VLC encode block
                rleBlock = runLengthEncoder.encode(block);
                huffmanEncoder.encode(internalFrameBuffer, rleBlock);
                // EOB
                internalFrameBuffer.write(new WORD((byte) 255, (byte) 255).byteValue());
            }
        }

        stopwatch.lap();
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

        jVidWriter.writeDataChunk(tempOutputStream, internalFrameBuffer, frameType);
        tempOutputStream.flush();

        logger.updateProgressStatus(100, "Finished!", frameName);
        stopwatch.lap();
    }
    private void processIFrameJvid(YCbCrImage frame, String frameName) throws IOException {
        // DEBUG
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
        RunLengthEncoder runLengthEncoder = new RunLengthEncoder();
        HuffmanEncoder huffmanEncoder = new HuffmanEncoder();

        // scale and subsample image
        logger.updateProgressStatus(2, "Subsampling Image...", frameName);
        frame.ScaleImage();
        frame.PerformSubsampling(YCbCrImage.YUV444Sampling);

        // split frame into blocks
        logger.updateProgressStatus(5, "Performing block splitting...", frameName);
        ArrayList<Block> blocks = frame.PerformBlockSplitting();
        RunLengthBlock rleBlock;
        stopwatch.lapFrame(0);

        // process each block
        logger.updateProgressStatus(10, "Processing blocks...", frameName);

        int blockIndex = 0;
        for (Block block: blocks) {
            blockIndex++;
            logger.updateProgressStatus((int) (((double) blockIndex / blocks.size() * 100 * 0.9)), "Processing blocks...", frameName);

            // I frame => perform DCT, quantization, inverses, push to buffer, proceed to VLC

            // save a copy in the block buffer
            Block savedBlock = block.getCopy();
            blockBuffer.save(savedBlock);

            // perform DCT
            DCT.forward(block);

            // quantize DCT coefficients
            quantizer.quantize(block);

            // VLC encode block
            rleBlock = runLengthEncoder.encode(block);
            huffmanEncoder.encode(internalFrameBuffer, rleBlock);
        }

        stopwatch.lapFrame(0);
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

        jVidWriter.writeDataChunk(tempOutputStream, internalFrameBuffer, 'I');
        tempOutputStream.flush();
        stopwatch.lapFrame(0);
    }
    private void processPFrameJvid(YCbCrImage frame, String frameName, Integer frameIndex) throws IOException {
        // DEBUG
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
        RunLengthEncoder runLengthEncoder = new RunLengthEncoder('P');
        HuffmanEncoder huffmanEncoder = new HuffmanEncoder();

        // scale and subsample image
        logger.updateProgressStatus(2, "Subsampling Image...", frameName);
        frame.ScaleImage();
        frame.PerformSubsampling(YCbCrImage.YUV444Sampling);

        // split frame into blocks
        logger.updateProgressStatus(5, "Performing block splitting...", frameName);
        ArrayList<Block> blocks = frame.PerformBlockSplitting();
        RunLengthBlock rleBlock;
        stopwatch.lapFrame(frameIndex);

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

            // P frame => search in block buffer, estimate motion, subtract, perform DCT, quantization,
            //            proceed to VLC
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

            // VLC encode block
            rleBlock = runLengthEncoder.encode(block);
            huffmanEncoder.encode(internalFrameBuffer, rleBlock);
            // EOB
            internalFrameBuffer.write(new WORD((byte) 255, (byte) 255).byteValue());
        }

        stopwatch.lapFrame(frameIndex);
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

        jVidWriter.writeDataChunk(tempOutputStream, internalFrameBuffer, 'P');
        tempOutputStream.flush();

        logger.updateProgressStatus(100, "Finished!", frameName);
        stopwatch.lapFrame(frameIndex);
    }
    private void parallelProcessPFrameJvid(int frameIndex, YCbCrImage frame, String frameName) throws IOException {
//        System.out.println("Processing frame " + frameIndex + " " + frameName);
        // DEBUG
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
        RunLengthEncoder runLengthEncoder = new RunLengthEncoder('P');
        HuffmanEncoder huffmanEncoder = new HuffmanEncoder();
        internalFrameBuffers.put(frameIndex, new InternalFrameBuffer());

        // scale and subsample image
        logger.updateProgressStatus(2, "Subsampling Image...", frameName);
        frame.ScaleImage();
        frame.PerformSubsampling(YCbCrImage.YUV444Sampling);

        // split frame into blocks
        logger.updateProgressStatus(5, "Performing block splitting...", frameName);
        ArrayList<Block> blocks = frame.PerformBlockSplitting();
        RunLengthBlock rleBlock;
        stopwatch.lapFrame(frameIndex);

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

            // P frame => search in block buffer, estimate motion, subtract, perform DCT, quantization,
            //            proceed to VLC
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
                internalFrameBuffers.get(frameIndex).write(new DWORD(pBlockCodeword).byteValue());
            }
            internalFrameBuffers.get(frameIndex).write(motionVector.byteValue());

            if (!block.isEmpty()) {
                // VLC encode block
                rleBlock = runLengthEncoder.encode(block);
                huffmanEncoder.encode(internalFrameBuffers.get(frameIndex), rleBlock);
                // EOB
                internalFrameBuffers.get(frameIndex).write(new WORD((byte) 255, (byte) 255).byteValue());
            }
        }

        stopwatch.lapFrame(frameIndex);
        logger.updateProgressStatus(90, "Finishing up...", frameName);

        // flush buffers
        huffmanEncoder.flushBuffer(internalFrameBuffers.get(frameIndex));

        // debug
        if (isDebug) {
            internalFrameBuffer.dumpBufferToStreamWithoutFlushing(frameOutputStream);
            jpgWriter.writeTrailerSection(frameOutputBuffer);
            frameOutputBuffer.dumpBufferToStream(frameOutputStream);
            frameOutputStream.flush();
        }

        logger.updateProgressStatus(100, "Finished!", frameName);
        stopwatch.lapFrame(frameIndex);
        System.out.println("Finished processing frame " + frameIndex + " " + frameName);
    }

    private Runnable runnableProcessFrameMjpeg(YCbCrImage frame, String frameName) {
        return () -> {
            try {
                processFrameMjpeg(frame, frameName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }
    private Runnable runnableProcessPFrameJvid(int frameIndex, YCbCrImage frame, String frameName) {
        return () -> {
            try {
                stopwatch.startFrame(frameIndex);
                parallelProcessPFrameJvid(frameIndex, frame, frameName);
                stopwatch.stopFrame(frameIndex);
                logger.benchmark(stopwatch, frameName, frameIndex);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
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

    private YCbCrImage readImage(File file) throws IOException {
        BufferedImage img = ImageIO.read(file);

        int[][] pixels = new int[img.getHeight()][img.getWidth()];

        for (int i = 0; i < img.getHeight(); i++)
            for (int j = 0; j < img.getWidth(); j++) {
                pixels[i][j] = img.getRGB(j, i);
            }
        return new YCbCrImage(pixels, img.getWidth(), img.getHeight());
    }
}

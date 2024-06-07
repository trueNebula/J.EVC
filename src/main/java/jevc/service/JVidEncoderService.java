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
import java.util.Objects;
import java.util.concurrent.*;

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
    private final ConcurrentHashMap<Integer, InternalFrameBuffer> internalFrameBuffers;
    private final ConcurrentHashMap<Integer, BufferedOutputStream> tempOutputStreams;

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
            this.outputStream = new BufferedOutputStream(new FileOutputStream(output));
            this.tempOutputStream = new BufferedOutputStream(
                    new FileOutputStream(outputFolder + "temp/temp")
            );
        } catch (Exception ex) {
            logger.error("Error opening the output stream!");
        }
        this.tempOutputStreams = new ConcurrentHashMap<>();
    }

    public void compress() {
        try {
            if (perFrame) {
                compressPerFrame();
            }
            if (perGop) {
                compressPerGop();
            }
            if (frameOperation) {
                compressFrameOperation();
            }
            if (combination) {
                compressCombination();
            }
            if (!perFrame && !perGop && !frameOperation && !combination) {
                compressSequentially();
            }
        } catch (Exception ex) {
            logger.error("Error compressing the video!" + ex.getMessage());
        }
    }

    public void compressSequentially() throws IOException {
        logger.log("Starting encoding");
        stopwatch.reset();

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

            if (!stopwatch.isRunning()) {
                stopwatch.start();
            }
            stopwatch.startFrame(0);

            if (this.compressToMjpeg) {
                processFrameMjpeg(frame, frameName, 0);
            } else {
                processIFrameJvid(frame, frameName);
            }
            logger.benchmark(stopwatch, frameName, 0);

            // Process P frames
            for (int i = 1; i < Globals.GOP_SIZE; i++) {
                frame = readImage(gop.get(i));
                frameName = gop.get(i).getName();
                stopwatch.startFrame(i);
                if (this.compressToMjpeg) {
                    processFrameMjpeg(frame, gop.get(i).getName(), 0);
                } else {
                    processPFrameJvid(frame, gop.get(i).getName(), 0, i, false);
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
        stopwatch.reset();

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
                if (frameIndex >= this.files.length) {
                    break;
                }
                gop.add(this.files[frameIndex]);
                frameIndex++;
            }

            // Process I frame
            frame = readImage(gop.get(0));
            videoWidth = new DWORD(frame.getWidth());
            videoHeight = new DWORD(frame.getHeight() + 8);
            frameName = gop.get(0).getName();

            if (!stopwatch.isRunning()) {
                stopwatch.start();
            }

            stopwatch.startFrame(0);

            if (this.compressToMjpeg) {
                processFrameMjpeg(frame, frameName, 0);
            } else {
                processIFrameJvid(frame, frameName);
            }
            stopwatch.stopFrame(0);
            logger.benchmark(stopwatch, frameName, 0);

            // Create a CountDownLatch for the number of P frames
            CountDownLatch latch = new CountDownLatch(gop.size() - 1);

            // Process P frames
            for (int i = 1; i < gop.size(); i++) {
                frame = readImage(gop.get(i));
                frameName = gop.get(i).getName();

                if (this.compressToMjpeg) {
                    processFrameMjpeg(frame, frameName, 0);
                } else {
                    executorService.execute(runnableProcessPFrameJvid(i, frame, frameName, latch));
//                    processPFrameJvid(frame, gop.get(i).getName());
                }
            }
            try {
                latch.await();
            } catch (InterruptedException ex) {
                logger.error("Error waiting for threads to finish!");
                Thread.currentThread().interrupt();
            }
            for(InternalFrameBuffer buffer : internalFrameBuffers.values()) {
                jVidWriter.writeDataChunk(tempOutputStream, buffer, 'P');
            }
            tempOutputStream.flush();
        }

        while (!executorService.isTerminated()) {
            executorService.shutdown();
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
    public void compressPerGop() throws IOException {
        logger.log("Starting encoding");
        stopwatch.reset();

        ConcurrentHashMap<Integer, List<File>> gops = new ConcurrentHashMap<>();

        // Get resolution from first frame
        YCbCrImage frame = readImage(this.files[0]);
        DWORD videoWidth = new DWORD(frame.getWidth());
        DWORD videoHeight = new DWORD(frame.getHeight() + 8);

        int frameIndex = 0;
        // Split files into GOPs
        // Compute the index and offset for each thread
        // this.files.length max files, split into Globals.GOP_SIZE gops
        // Each thread gets one gop to process sequentially
        // Maybe loading every gop into memory will create a leak
        // Oh well

        int MAX_GOPS_AT_ONCE = 1;
        int usedGops = 0;

        while (frameIndex < this.files.length) {
            // Split files into GOPs
            for (int i = 0; i < MAX_GOPS_AT_ONCE; i++) {
                usedGops++;
                if (!Objects.isNull(gops.get(i))) {
                    gops.get(i).clear();
                }
                gops.put(i, new ArrayList<>());
                for (int j = 0; j < Globals.GOP_SIZE; j++) {
                    if (frameIndex >= this.files.length) {
                        break;
                    }
                    gops.get(i).add(this.files[frameIndex]);
                    frameIndex++;
                }
                if (frameIndex >= this.files.length) {
                    break;
                }
            }

            // Dispatch threads
            for (int i = 0; i < usedGops; i++) {
                if (Objects.isNull(tempOutputStreams.get(i))) {
                    try {
                        tempOutputStreams.put(i, new BufferedOutputStream(
                                new FileOutputStream(outputFolder + "temp/temp" + i)
                        ));
                    } catch (Exception ex) {
                        logger.error("Error opening the output stream!");
                    }
                }
                if (Objects.isNull(internalFrameBuffers.get(i))) {
                    internalFrameBuffers.put(i, new InternalFrameBuffer());
                }
                executorService.execute(runnableProcessGop(i, gops.get(i)));
            }

            // Wait for threads to finish
            while(!executorService.isTerminated()) {
                executorService.shutdown();
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

        for (int i = 0; i < usedGops; i++) {
            File temp = new File(outputFolder + "temp/temp" + i);
            Files.copy(temp.toPath(), outputStream);
        }

        outputStream.flush();

        if (this.compressToMjpeg) {
            logger.log("Frames written, writing Idx1");
            aviWriter.writeIdx1(outputStream, this.files.length);
        }

        logger.log("Output file: " + outputFile);

    }
    public void compressFrameOperation() throws IOException {
        logger.log("Starting encoding");
        stopwatch.reset();

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

            if (!stopwatch.isRunning()) {
                stopwatch.start();
            }
            stopwatch.startFrame(0);

            if (this.compressToMjpeg) {
                processFrameMjpeg(frame, frameName, 0);
            } else {
                parallelProcessIFrameOperations(frame, frameName, -1);
            }
            logger.benchmark(stopwatch, frameName, 0);

            // Process P frames
            for (int i = 1; i < Globals.GOP_SIZE; i++) {
                frame = readImage(gop.get(i));
                frameName = gop.get(i).getName();
                stopwatch.startFrame(i);
                if (this.compressToMjpeg) {
                    processFrameMjpeg(frame, gop.get(i).getName(), 0);
                } else {
                    parallelProcessPFrameOperations(i, frame, frameName);
                }
                stopwatch.stopFrame(i);
                logger.benchmark(stopwatch, frameName, i);
            }
        }

        while (!executorService.isTerminated()) {
            executorService.shutdown();
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
    public void compressCombination() throws IOException {
        logger.log("Starting encoding");
        stopwatch.reset();

        // The plan
        // We have to support even old hardware with 4 threads.
        // The best performance increase we get is by parallelizing frame operations
        // So we'll leave those at the end
        // GOP reading - sequential
        //      - Read GOPs at once or until we're done with the files
        // GOP processing - parallel
        //      - 2 GOPs at once
        // For each GOP, process the I frame then the P frames
        // P Frame - parallel
        //      - 2 frames at once
        // For each I and P frame, use the rest of the available threads to process frame ops
        // GOP data is written to temp files
        // Frame data is written to internalFrameBuffers
        // Block data is handled in the respective methods

        int MAX_GOPS_AT_ONCE = 2;
        int MAX_FRAMES_AT_ONCE = 2;

        ConcurrentHashMap<Integer, List<File>> gops = new ConcurrentHashMap<>();
        ExecutorService gopService = Executors.newFixedThreadPool(MAX_GOPS_AT_ONCE);

        // Get resolution from first frame
        YCbCrImage frame = readImage(this.files[0]);
        DWORD videoWidth = new DWORD(frame.getWidth());
        DWORD videoHeight = new DWORD(frame.getHeight() + 8);

        int frameIndex = 0;
        int usedGops = 0;

        while (frameIndex < this.files.length) {
            // Split files into GOPs
            for (int i = 0; i < MAX_GOPS_AT_ONCE; i++) {
                usedGops++;
                if (!Objects.isNull(gops.get(i))) {
                    gops.get(i).clear();
                }
                gops.put(i, new ArrayList<>());
                for (int j = 0; j < Globals.GOP_SIZE; j++) {
                    if (frameIndex >= this.files.length) {
                        break;
                    }
                    gops.get(i).add(this.files[frameIndex]);
                    frameIndex++;
                }
                if (frameIndex >= this.files.length) {
                    break;
                }
            }

            // GOPs read, dispatch threads to process them
            for (int i = 0; i < usedGops; i++) {
                if (Objects.isNull(tempOutputStreams.get(i))) {
                    try {
                        tempOutputStreams.put(i, new BufferedOutputStream(
                                new FileOutputStream(outputFolder + "temp/temp" + i)
                        ));
                    } catch (Exception ex) {
                        logger.error("Error opening the output stream!");
                    }
                }
                if (Objects.isNull(internalFrameBuffers.get(i))) {
                    internalFrameBuffers.put(i, new InternalFrameBuffer());
                }
                gopService.execute(runnableProcessComboGop(i, gops.get(i)));
            }

            // Wait for threads to finish
            while(!gopService.isTerminated()) {
                gopService.shutdown();
            }

            while (!executorService.isTerminated()) {
                executorService.shutdown();
            }
        }

        // Writing
        stopwatch.stop();
        logger.benchmark(stopwatch);

        logger.log("Writing file header");

        if (this.compressToMjpeg) {
            aviWriter.writeAVIHeader(outputStream, this.files.length, videoWidth, videoHeight);
        } else {
            jVidWriter.writeJvidHeader(outputStream, this.files.length, videoWidth, videoHeight);
        }

        logger.log("File header written, writing frames");

        for (int i = 0; i < usedGops; i++) {
            File temp = new File(outputFolder + "temp/temp" + i);
            Files.copy(temp.toPath(), outputStream);
        }

        outputStream.flush();

        if (this.compressToMjpeg) {
            logger.log("Frames written, writing Idx1");
            aviWriter.writeIdx1(outputStream, this.files.length);
        }

        logger.log("Output file: " + outputFile);
    }

    private void processFrameMjpeg(YCbCrImage frame, String frameName, Integer gopIndex) throws IOException {
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

        BufferedOutputStream tempOutputStream = tempOutputStreams.get(gopIndex);

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

        BufferedOutputStream tempOutputStream = tempOutputStreams.get(0);

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
    private void processPFrameJvid(YCbCrImage frame, String frameName, Integer gopIndex, Integer frameIndex, boolean timeGop) throws IOException {
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

        BufferedOutputStream tempOutputStream1 = timeGop ? tempOutputStreams.get(gopIndex) : tempOutputStream;
        InternalFrameBuffer ifb = timeGop ? internalFrameBuffers.get(gopIndex) : internalFrameBuffer;

        // scale and subsample image
        logger.updateProgressStatus(2, "Subsampling Image...", frameName);
        frame.ScaleImage();
        frame.PerformSubsampling(YCbCrImage.YUV444Sampling);

        // split frame into blocks
        logger.updateProgressStatus(5, "Performing block splitting...", frameName);
        ArrayList<Block> blocks = frame.PerformBlockSplitting();
        RunLengthBlock rleBlock;
        stopwatch.lapFrame(timeGop ? gopIndex : frameIndex);

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
                ifb.write(new DWORD(pBlockCodeword).byteValue());
            }
            ifb.write(motionVector.byteValue());

            if (!block.isEmpty()) {
                // VLC encode block
                rleBlock = runLengthEncoder.encode(block);
                huffmanEncoder.encode(ifb, rleBlock);
                // EOB
                ifb.write(new WORD((byte) 255, (byte) 255).byteValue());
            }
        }

        stopwatch.lapFrame(timeGop ? gopIndex : frameIndex);
        logger.updateProgressStatus(90, "Finishing up...", frameName);

        // flush buffers
        huffmanEncoder.flushBuffer(ifb);

        // debug
        if (isDebug) {
            ifb.dumpBufferToStreamWithoutFlushing(frameOutputStream);
            jpgWriter.writeTrailerSection(frameOutputBuffer);
            frameOutputBuffer.dumpBufferToStream(frameOutputStream);
            frameOutputStream.flush();
        }

        jVidWriter.writeDataChunk(tempOutputStream1, ifb, 'P');
        tempOutputStream1.flush();

        logger.updateProgressStatus(100, "Finished!", frameName);
        stopwatch.lapFrame(timeGop ? gopIndex : frameIndex);
    }

    private void parallelProcessIFrameJvid(YCbCrImage frame, String frameName, Integer gopIndex) throws IOException {
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

        BufferedOutputStream tempOutputStream1 = tempOutputStreams.get(gopIndex);
        InternalFrameBuffer ifb = internalFrameBuffers.get(gopIndex);

        // scale and subsample image
        logger.updateProgressStatus(2, "Subsampling Image...", frameName);
        frame.ScaleImage();
        frame.PerformSubsampling(YCbCrImage.YUV444Sampling);

        // split frame into blocks
        logger.updateProgressStatus(5, "Performing block splitting...", frameName);
        ArrayList<Block> blocks = frame.PerformBlockSplitting();
        RunLengthBlock rleBlock;
        stopwatch.lapFrame(gopIndex);

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
            huffmanEncoder.encode(ifb, rleBlock);
        }

        stopwatch.lapFrame(gopIndex);
        logger.updateProgressStatus(90, "Finishing up...", frameName);

        // flush buffers
        huffmanEncoder.flushBuffer(ifb);

        // debug
        if (isDebug) {
            internalFrameBuffer.dumpBufferToStreamWithoutFlushing(frameOutputStream);
            jpgWriter.writeTrailerSection(frameOutputBuffer);
            frameOutputBuffer.dumpBufferToStream(frameOutputStream);
            frameOutputStream.flush();
        }

        logger.updateProgressStatus(100, "Finished!", frameName);
        jVidWriter.writeDataChunk(tempOutputStream1, ifb, 'I');
        tempOutputStream1.flush();
        stopwatch.lapFrame(gopIndex);
    }
    private void parallelProcessPFrameJvid(int frameIndex, YCbCrImage frame, String frameName) throws IOException {
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
    }
    private void parallelProcessGop(int gopIndex, List<File> gop) throws IOException {
        // Process I frame
        YCbCrImage frame = readImage(gop.get(0));
        String frameName = gop.get(0).getName();

        if (!stopwatch.isRunning()) {
            stopwatch.start();
        }
        stopwatch.startFrame(gopIndex);

        if (this.compressToMjpeg) {
            processFrameMjpeg(frame, frameName, gopIndex);
        } else {
            parallelProcessIFrameJvid(frame, frameName, gopIndex);
        }
        stopwatch.stopFrame(gopIndex);
        logger.benchmark(stopwatch, frameName, gopIndex);

        // Process P frames
        for (int i = 1; i < gop.size(); i++) {
            frame = readImage(gop.get(i));
            frameName = gop.get(i).getName();
            stopwatch.startFrame(gopIndex);
            if (this.compressToMjpeg) {
                processFrameMjpeg(frame, frameName, gopIndex);
            } else {
                processPFrameJvid(frame, frameName, gopIndex, i, true);
            }
            stopwatch.stopFrame(gopIndex);
            logger.benchmark(stopwatch, frameName, gopIndex);
        }
    }
    private void parallelProcessIFrameOperations(YCbCrImage frame, String frameName, int gopIndex) throws IOException {
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

        BufferedOutputStream tos = gopIndex == -1 ? tempOutputStream : tempOutputStreams.get(gopIndex);

        // scale and subsample image
        logger.updateProgressStatus(2, "Subsampling Image...", frameName);
        frame.ScaleImage();
        frame.PerformSubsampling(YCbCrImage.YUV444Sampling);

        // split frame into blocks
        logger.updateProgressStatus(5, "Performing block splitting...", frameName);
        ArrayList<Block> blocks = frame.PerformBlockSplitting();
        RunLengthBlock rleBlock;
        stopwatch.lapFrame(gopIndex == -1 ? 0 : Globals.GOP_SIZE * gopIndex);

        // process each block
        logger.updateProgressStatus(10, "Processing blocks...", frameName);

        // Create a CountDownLatch for the number of blocks
        CountDownLatch latch = new CountDownLatch(blocks.size());
        ConcurrentHashMap<Integer, Block> blockMap = new ConcurrentHashMap<>();

        int blockIndex = 0;
        for (Block block: blocks) {
//            logger.updateProgressStatus((int) (((double) blockIndex / blocks.size() * 100 * 0.9)), "Processing blocks...", frameName);

            // I frame => perform DCT, quantization, inverses, push to buffer, proceed to VLC

            // save a copy in the block buffer
            Block savedBlock = block.getCopy();
            blockBuffer.save(savedBlock);

            executorService.execute(runnableProcessBlockOps(blockIndex, block, blockMap, latch));
            blockIndex++;
        }

        try {
            latch.await();
        } catch (InterruptedException ex) {
            logger.error("Error waiting for threads to finish!");
            Thread.currentThread().interrupt();
        }

        for (int i = 0; i < blockMap.size(); i++) {
            // VLC encode block
            rleBlock = runLengthEncoder.encode(blockMap.get(i));
            huffmanEncoder.encode(internalFrameBuffer, rleBlock);
        }

        stopwatch.lapFrame(gopIndex == -1 ? 0 : Globals.GOP_SIZE * gopIndex);
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
        jVidWriter.writeDataChunk(tos, internalFrameBuffer, 'I');
        tos.flush();
        stopwatch.lapFrame(gopIndex == -1 ? 0 : Globals.GOP_SIZE * gopIndex);
    }
    private void parallelProcessPFrameOperations(int frameIndex, YCbCrImage frame, String frameName) throws IOException {
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

        // Create a CountDownLatch for the number of blocks
        CountDownLatch latch = new CountDownLatch(blocks.size());
        ConcurrentHashMap<Integer,FrameOpDataObject> frameOpData = new ConcurrentHashMap<>();

        int blockIndex = 0;
        for (Block block: blocks) {
//            logger.updateProgressStatus((int) (((double) blockIndex / blocks.size() * 100 * 0.9)), "Processing blocks...", frameName);

            executorService.execute(runnableProcessMotionOps(blockIndex, block, frameOpData, latch));
            blockIndex++;
        }

        try {
            latch.await();
        } catch (InterruptedException ex) {
            logger.error("Error waiting for threads to finish!");
            Thread.currentThread().interrupt();
        }

        // Retrieve computed frame data and write it to buffer
        for (int i = 0; i < frameOpData.size(); i++) {
            codewordChanged = !pBlockCodeword.equals(frameOpData.get(i).getCodeword());
            MotionVector motionVector = frameOpData.get(i).getMotionVector();
            Block block  = frameOpData.get(i).getError();
            pBlockCodeword = frameOpData.get(i).getCodeword();

            if (codewordChanged) {
                // Codeword changed, write it
                internalFrameBuffer.write(new DWORD(pBlockCodeword).byteValue());
            }
            internalFrameBuffer.write(motionVector.byteValue());

            if (!block.isEmpty()) {
                // VLC encode block
                rleBlock = runLengthEncoder.encode(block);
                huffmanEncoder.encode(internalFrameBuffer, rleBlock);
                // EOB
                internalFrameBuffer.write(new WORD((byte) 255, (byte) 255).byteValue());
            }
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

    private void parallelProcessComboPFrame(int frameIndex, YCbCrImage frame, String frameName, int gopIndex) throws IOException {
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

        internalFrameBuffers.put(frameIndex + Globals.GOP_SIZE * gopIndex, new InternalFrameBuffer());
        InternalFrameBuffer ifb = internalFrameBuffers.get(frameIndex + Globals.GOP_SIZE * gopIndex);

        // scale and subsample image
        logger.updateProgressStatus(2, "Subsampling Image...", frameName);
        frame.ScaleImage();
        frame.PerformSubsampling(YCbCrImage.YUV444Sampling);

        // split frame into blocks
        logger.updateProgressStatus(5, "Performing block splitting...", frameName);
        ArrayList<Block> blocks = frame.PerformBlockSplitting();
        RunLengthBlock rleBlock;
        stopwatch.lapFrame(frameIndex + Globals.GOP_SIZE * gopIndex);

        // process each block
        logger.updateProgressStatus(10, "Processing blocks...", frameName);

        // This is used to determine if we have to write a new codeword for the block
        // pBlockCodeword = mvec => only motion vector is written
        // pBlockCodeword = errb => motion vector and error block data is written
        // only write the codeword once per change
        String pBlockCodeword = "";
        boolean codewordChanged;

        // Create a CountDownLatch for the number of blocks
        CountDownLatch latch = new CountDownLatch(blocks.size());
        ConcurrentHashMap<Integer,FrameOpDataObject> frameOpData = new ConcurrentHashMap<>();

        int blockIndex = 0;
        for (Block block: blocks) {
//            logger.updateProgressStatus((int) (((double) blockIndex / blocks.size() * 100 * 0.9)), "Processing blocks...", frameName);

            executorService.execute(runnableProcessMotionOps(blockIndex, block, frameOpData, latch));
            blockIndex++;
        }

        try {
            latch.await();
        } catch (InterruptedException ex) {
            logger.error("Error waiting for threads to finish!");
            Thread.currentThread().interrupt();
        }

        // Retrieve computed frame data and write it to buffer
        for (int i = 0; i < frameOpData.size(); i++) {
            codewordChanged = !pBlockCodeword.equals(frameOpData.get(i).getCodeword());
            MotionVector motionVector = frameOpData.get(i).getMotionVector();
            Block block  = frameOpData.get(i).getError();
            pBlockCodeword = frameOpData.get(i).getCodeword();

            if (codewordChanged) {
                // Codeword changed, write it
                ifb.write(new DWORD(pBlockCodeword).byteValue());
            }
            ifb.write(motionVector.byteValue());

            if (!block.isEmpty()) {
                // VLC encode block
                rleBlock = runLengthEncoder.encode(block);
                huffmanEncoder.encode(ifb, rleBlock);
                // EOB
                ifb.write(new WORD((byte) 255, (byte) 255).byteValue());
            }
        }

        stopwatch.lapFrame(frameIndex + Globals.GOP_SIZE * gopIndex);
        logger.updateProgressStatus(90, "Finishing up...", frameName);

        // flush buffers
        huffmanEncoder.flushBuffer(ifb);
        stopwatch.lapFrame(frameIndex + Globals.GOP_SIZE * gopIndex);

        // debug
        if (isDebug) {
            ifb.dumpBufferToStreamWithoutFlushing(frameOutputStream);
            jpgWriter.writeTrailerSection(frameOutputBuffer);
            frameOutputBuffer.dumpBufferToStream(frameOutputStream);
            frameOutputStream.flush();
        }

    }
    private void parallelProcessComboGop(int gopIndex, List<File> gop) throws IOException {
        // Process I frame
        YCbCrImage frame = readImage(gop.get(0));
        String frameName = gop.get(0).getName();

        if (!stopwatch.isRunning()) {
            stopwatch.start();
        }
        stopwatch.startFrame(Globals.GOP_SIZE * gopIndex);

        parallelProcessIFrameOperations(frame, frameName, gopIndex);

        stopwatch.stopFrame(Globals.GOP_SIZE * gopIndex);
        logger.benchmark(stopwatch, frameName, Globals.GOP_SIZE * gopIndex);

        ExecutorService frameService = Executors.newFixedThreadPool(2);

        // Create a CountDownLatch for the number of P frames
        CountDownLatch latch = new CountDownLatch(gop.size() - 1);

        // Process P frames
        for (int i = 1; i < gop.size(); i++) {
            frame = readImage(gop.get(i));
            frameName = gop.get(i).getName();

            // Dispatch threads
            frameService.execute(runnableProcessComboPFrame(i, frame, frameName, gopIndex, latch));
        }
        try {
            latch.await();
        } catch (InterruptedException ex) {
            logger.error("Error waiting for threads to finish!");
            Thread.currentThread().interrupt();
        }
        for(InternalFrameBuffer buffer : internalFrameBuffers.values()) {
            jVidWriter.writeDataChunk(tempOutputStreams.get(gopIndex), buffer, 'P');
        }
        tempOutputStreams.get(gopIndex).flush();

        while (!frameService.isTerminated()) {
            frameService.shutdown();
        }
    }

    private Runnable runnableProcessPFrameJvid(int frameIndex, YCbCrImage frame, String frameName, CountDownLatch latch) {
        return () -> {
            try {
                stopwatch.startFrame(frameIndex);
                parallelProcessPFrameJvid(frameIndex, frame, frameName);
                stopwatch.stopFrame(frameIndex);
                logger.benchmark(stopwatch, frameName, frameIndex);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };
    }
    private Runnable runnableProcessGop(int gopIndex, List<File> gop) {
        return () -> {
          try {
              parallelProcessGop(gopIndex, gop);
          } catch (Exception e) {
              e.printStackTrace();
          }
        };
    }
    private Runnable runnableProcessBlockOps(int blockIndex, Block block, ConcurrentHashMap<Integer, Block> blockMap, CountDownLatch latch) {
        return () -> {
            try {
                // perform DCT
                DCT.forward(block);

                // quantize DCT coefficients
                quantizer.quantize(block);

                blockMap.put(blockIndex, block);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };
    }
    private Runnable runnableProcessMotionOps(int blockIndex, Block block, ConcurrentHashMap<Integer,FrameOpDataObject> frameOpData, CountDownLatch latch) {
        return () -> {
            try {
                // P frame => search in block buffer, estimate motion, subtract, perform DCT, quantization,
                //            proceed to VLC
                motionEstimator = new MotionEstimator();
                String pBlockCodeword;
                // look through block buffer for the most similar block
                Block similarBlock = blockBuffer.getSimilarBlock(block);

                // compute motion vector
                MotionVector motionVector = motionEstimator.computeMotionVector(block, similarBlock);

                // subtract found block from input block
                block.subtract(similarBlock);

                // if the error is 0, don't write it
                if (!block.isEmpty()) {
                    pBlockCodeword = "errb";

                    // perform DCT
                    DCT.forward(block);

                    // quantize DCT coefficients
                    quantizer.quantize(block);
                } else {
                    pBlockCodeword = "mvec";
                }
                frameOpData.put(blockIndex, new FrameOpDataObject(motionVector, block, pBlockCodeword));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        };
    }
    private Runnable runnableProcessComboGop(int gopIndex, List<File> gop) {
        return () -> {
            try {
                parallelProcessComboGop(gopIndex, gop);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
    private Runnable runnableProcessComboPFrame(int frameIndex, YCbCrImage frame, String frameName, int gopIndex, CountDownLatch latch) {
        return () -> {
            try {
                stopwatch.startFrame(frameIndex + Globals.GOP_SIZE * gopIndex);
                parallelProcessComboPFrame(frameIndex, frame, frameName, gopIndex);
                stopwatch.stopFrame(frameIndex + Globals.GOP_SIZE * gopIndex);
                logger.benchmark(stopwatch, frameName, frameIndex + Globals.GOP_SIZE * gopIndex);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
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

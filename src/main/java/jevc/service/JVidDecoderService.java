package jevc.service;

import jevc.entities.InternalFrameBuffer;
import jevc.operations.*;
import jevc.utils.JVidWriter;
import jevc.utils.ProgressStatus;

import java.io.BufferedOutputStream;
import java.io.File;

public class JVidDecoderService {
    private File file;
    private final InternalFrameBuffer internalFrameBuffer;
    private final DiscreteCosineTransform DCT;
    private final Quantizer quantizer;
    private RunLengthEncoder runLengthEncoder;
    private HuffmanEncoder huffmanEncoder;
    private final BlockBuffer blockBuffer;
    private MotionEstimator motionEstimator;
    private BufferedOutputStream outputStream;
    private final JVidWriter jVidWriter;
    private final String outputFolder;
    private String outputFile;
    private ProgressStatus progressStatus;
    public JVidDecoderService(File file, String outputFolder) {
        this.file = file;
        this.outputFolder = outputFolder;
        this.internalFrameBuffer = new InternalFrameBuffer();
        this.DCT = new DiscreteCosineTransform();
        this.quantizer = new Quantizer();
        this.jVidWriter = new JVidWriter();
        this.blockBuffer = new BlockBuffer();
        this.progressStatus = new ProgressStatus();
    }
    public void decompress() {

    }
}

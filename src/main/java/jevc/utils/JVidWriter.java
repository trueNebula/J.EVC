package jevc.utils;

import jevc.entities.DWORD;
import jevc.entities.Globals;
import jevc.entities.InternalFrameBuffer;
import jevc.entities.WORD;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class JVidWriter {
    public final JVidHeader jVidHeader;
    public final JVidStreamHeader jVidStreamHeader;
    public ArrayList<? extends StreamDataChunk> streamDataChunks;

    private final int fps = Globals.FRAMERATE;
    private final int bytesPerSec = 7000;
    private ArrayList<Integer> frameLengthBuffer;

    public JVidWriter() {
        this.jVidHeader = new JVidHeader(
                new DWORD("JVID"),
                new DWORD(-1)
        );
        this.jVidStreamHeader = new JVidStreamHeader(
                new DWORD("jvih"),
                new DWORD(28),
                new DWORD(40000),
                new DWORD(7000),
                new DWORD(-1),
                new DWORD(-1),
                new DWORD(-1)
        );

        frameLengthBuffer = new ArrayList<>();
    }

    public void writeJvidHeader(BufferedOutputStream outputStream, int frameCount, DWORD width, DWORD height) throws IOException {
        // get total length
        System.out.println("Lenghts: " + Arrays.toString(frameLengthBuffer.toArray()) + " Total: " + getJvidSize(frameCount).intValue());
        // fill in missing atoms
        jVidHeader.dwSize = getJvidSize(frameCount);
        jVidStreamHeader.dwTotalFrames = new DWORD(frameCount);
        jVidStreamHeader.dwWidth = width;
        jVidStreamHeader.dwHeight = height;

        // Write JVID Header
        outputStream.write(jVidHeader.dwFourCc.byteValue());
        outputStream.write(jVidHeader.dwSize.byteValue());

        // Write JVid Stream Header
        outputStream.write(jVidStreamHeader.dwFourCc.byteValue());
        outputStream.write(jVidStreamHeader.dwSize.byteValue());
        outputStream.write(jVidStreamHeader.dwMicroSecPerFrame.byteValue());
        outputStream.write(jVidStreamHeader.dwMaxBytesPerSec.byteValue());
        outputStream.write(jVidStreamHeader.dwTotalFrames.byteValue());
        outputStream.write(jVidStreamHeader.dwWidth.byteValue());
        outputStream.write(jVidStreamHeader.dwHeight.byteValue());
    }

    public void writeDataChunk(BufferedOutputStream outputStream, InternalFrameBuffer buffer, char frameType) throws IOException {
        DWORD type;

        if (frameType == 'I') {
            type = new DWORD("comp");
        } else {
            type = new DWORD("inte");
        }

        StreamDataChunk data = new StreamDataChunk(
                new DWORD("sdat"),
                new DWORD(buffer.size()),
                type
        );
        
        frameLengthBuffer.add(buffer.size() + 12);
        
        outputStream.write(data.dwFourCc.byteValue());
        outputStream.write(data.dwSize.byteValue());
        outputStream.write(data.dwType.byteValue());
        buffer.dumpBufferToStream(outputStream);
    }

    private DWORD getJvidSize(int frameCount) {
        // Jvid Header should have the full stream size (file size - 8)
        // jvih size = 7 * 4 = 28 Bytes
        // sdat size = 3 * 4 + frame size = 12 + frame size Bytes
        // frame sizes are stored in frameLengthBuffer
        // total size = 28 + (12 + frame size) * frameCount = 28 + size + 12 * frameCount

        int size = frameLengthBuffer.stream().reduce(0, Integer::sum);
        System.out.println(28 + size);
        return new DWORD(28 + size);
    }
}

class JVidHeader {
    public DWORD dwFourCc;
    public DWORD dwSize;

    public JVidHeader(DWORD dwFourCc, DWORD dwSize) {
        this.dwFourCc = dwFourCc;
        this.dwSize = dwSize;
    }
}

class JVidStreamHeader {
    public DWORD dwFourCc;
    public DWORD dwSize;
    public DWORD dwMicroSecPerFrame;
    public DWORD dwMaxBytesPerSec;
    public DWORD dwTotalFrames;
    public DWORD dwWidth;
    public DWORD dwHeight;

    public JVidStreamHeader(DWORD dwFourCc, DWORD dwSize, DWORD dwMicroSecPerFrame, DWORD dwMaxBytesPerSec, DWORD dwTotalFrames, DWORD dwWidth, DWORD dwHeight) {
        this.dwFourCc = dwFourCc;
        this.dwSize = dwSize;
        this.dwMicroSecPerFrame = dwMicroSecPerFrame;
        this.dwMaxBytesPerSec = dwMaxBytesPerSec;
        this.dwTotalFrames = dwTotalFrames;
        this.dwWidth = dwWidth;
        this.dwHeight = dwHeight;
    }
}

class StreamDataChunk {
    public DWORD dwFourCc;
    public DWORD dwSize;
    public DWORD dwType;

    public StreamDataChunk(DWORD dwFourCc, DWORD dwSize, DWORD dwType) {
        this.dwFourCc = dwFourCc;
        this.dwSize = dwSize;
        this.dwType = dwType;
    }
}

class CompressedDataChunk extends StreamDataChunk{
    public DWORD[] data;

    public CompressedDataChunk(DWORD dwFourCc, DWORD dwSize, DWORD dwType, DWORD[] data) {
        super(dwFourCc, dwSize, dwType);
        this.data = data;
    }
}

class MotionDataChunk extends StreamDataChunk {
    public DWORD dwMvecCC;
    public MotionVectorCOmponents MvecComponents;
    public DWORD dwErrCC;
    public DWORD[] errData;

    public MotionDataChunk(DWORD dwFourCc, DWORD dwSize, DWORD dwType, DWORD dwMvecCC, MotionVectorCOmponents mvecComponents, DWORD dwErrCC, DWORD[] errData) {
        super(dwFourCc, dwSize, dwType);
        this.dwMvecCC = dwMvecCC;
        MvecComponents = mvecComponents;
        this.dwErrCC = dwErrCC;
        this.errData = errData;
    }
}

class MotionVectorCOmponents {
    public WORD dwX;
    public WORD dwY;

    public MotionVectorCOmponents(WORD dwX, WORD dwY) {
        this.dwX = dwX;
        this.dwY = dwY;
    }
}
package jevc.utils;

import jevc.entities.DWORD;
import jevc.entities.Globals;
import jevc.entities.InternalFrameBuffer;
import jevc.entities.WORD;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

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
        // fill in missing atoms
        jVidHeader.dwSize = getJvidSize();
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

    public int[] readHeader(RandomAccessFile file) {
        byte[] buffer = new byte[4];
        int bitstreamSize = -1;
        int fps = -1, frameCount = -1, bitrate = -1, width = -1, height = -1;

        try {
            // Read JVID Header
            if (!ByteConverter.checkString(file, buffer, "JVID")) {
                throw new IOException("Invalid JVid Header");
            }

            bitstreamSize = ByteConverter.readInt(file, buffer);
            System.out.println("Bitstream Size: " + bitstreamSize);

            // Read JVid Stream Header
            if (!ByteConverter.checkString(file, buffer, "jvih")) {
                throw new IOException("Error: Invalid JVid Stream Header");
            }

            int jvihSize = ByteConverter.readInt(file, buffer);
            System.out.println("JVid Stream Header Size: " + jvihSize);

            if (jvihSize != 28) {
                throw new IOException("Error: Invalid JVid Stream Header Size");
            }

            fps = 1000000 / ByteConverter.readInt(file, buffer);
            System.out.println("Framerate: " + fps);

            bitrate = ByteConverter.readInt(file, buffer);
            System.out.println("Bitrate: " + bitrate);

            frameCount = ByteConverter.readInt(file, buffer);
            System.out.println("Frame Count: " + frameCount);

            width = ByteConverter.readInt(file, buffer);
            System.out.println("Width: " + width);

            height = ByteConverter.readInt(file, buffer);
            System.out.println("Height: " + height);

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return new int[]{bitstreamSize, fps, frameCount, bitrate, width, height};
    }

    private DWORD getJvidSize() {
        // Jvid Header should have the full stream size (file size - 8)
        // jvih size = 7 * 4 = 28 Bytes
        // sdat size = 3 * 4 + frame size = 12 + frame size Bytes
        // frame sizes are stored in frameLengthBuffer
        // total size = 28 + (12 + frame size) * frameCount = 28 + size + 12 * frameCount

        int size = frameLengthBuffer.stream().reduce(0, Integer::sum);
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

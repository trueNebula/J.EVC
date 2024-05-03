package jevc.utils;

import jevc.entities.DWORD;
import jevc.entities.InternalFrameBuffer;
import jevc.entities.WORD;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class AVIHeader {
    private final RIFF riff;
    private final LIST hdrl;
    private final MainAVIHeader avih;
    private final LIST strl;
    private final AVIStreamHeader strh;
    private final EXBMINFOHEADER strf;
    private final DWORD ddww;
    private final DWORD szs;
    private final DWORD totalFrames;
    private final LIST movi;

    private final int fps = 1;
    private final int bytesPerSec = 7000;
    private ArrayList<Integer> frameLengthBuffer;

    public AVIHeader(int frameCount, int length, DWORD width, DWORD height) {
        riff = new RIFF(
                new DWORD("RIFF"),
                getRiffSize(frameCount, length),
                new DWORD("AVI ")
        );
        hdrl = new LIST(
                new DWORD("LIST"),
                new DWORD(224),
                new DWORD("hdrl")
        );
        avih = new MainAVIHeader(
                new DWORD("avih"),
                new DWORD(56),
                new DWORD(1000000 / fps),
                new DWORD(bytesPerSec),
                new DWORD(0),
                new DWORD(16),
                new DWORD(frameCount),
                new DWORD(0),
                new DWORD(1),
                new DWORD(0),
                width,
                height,
                new DWORD[]{
                        new DWORD(0),
                        new DWORD(0),
                        new DWORD(0),
                        new DWORD(0)
                }
        );
        strl = new LIST(
                new DWORD("LIST"),
                new DWORD(148),
                new DWORD("strl")
        );
        strh = new AVIStreamHeader(
                new DWORD("strh"),
                new DWORD(64),
                new DWORD("vids"),
                new DWORD("MJPG"),
                new DWORD(0),
                new WORD((short) 0),
                new WORD((short) 0),
                new DWORD(0),
                new DWORD(1),
                new DWORD(fps),
                new DWORD(0),
                new DWORD(frameCount),
                new DWORD(0),
                new DWORD(0),
                new DWORD(0),
                new RECT(
                        new DWORD(0),
                        new DWORD(0),
                        width,
                        height
                )
        );
        strf = new EXBMINFOHEADER(
                new DWORD("strf"),
                new DWORD(40),
                new DWORD(40),
                width,
                height,
                new WORD((short) 1),
                new WORD((short) 24),
                new DWORD("MJPG"),
                new DWORD(getStrfImageSize(width, height, 24)),
                new DWORD(0),
                new DWORD(0),
                new DWORD(0),
                new DWORD(0)
        );
        movi = new LIST(
                new DWORD("LIST"),
                getMoviSize(frameCount, length),
                new DWORD("movi")
        );
        ddww = new DWORD(16);
        szs = new DWORD(4);
        totalFrames = new DWORD(frameCount);
        frameLengthBuffer = new ArrayList<>();
    }

    public void writeAVIHeader(BufferedOutputStream outputStream) throws IOException {
        // Write RIFF atom
        outputStream.write(riff.dwRIFF.byteValue());
        outputStream.write(riff.dwSize.byteValue());
        outputStream.write(riff.dwFourCc.byteValue());

        // Write HDRL atom
        outputStream.write(hdrl.dwList.byteValue());
        outputStream.write(hdrl.dwSize.byteValue());
        outputStream.write(hdrl.dwFourCc.byteValue());

        // Write AVIHeader atom
        outputStream.write(avih.dwFourCc.byteValue());
        outputStream.write(avih.dwSize.byteValue());
        outputStream.write(avih.dwMicroSecPerFrame.byteValue());
        outputStream.write(avih.dwMaxBytesPerSec.byteValue());
        outputStream.write(avih.dwPaddingGranularity.byteValue());
        outputStream.write(avih.dwFlags.byteValue());
        outputStream.write(avih.dwTotalFrames.byteValue());
        outputStream.write(avih.dwInitialFrames.byteValue());
        outputStream.write(avih.dwStreams.byteValue());
        outputStream.write(avih.dwSuggestedBufferSize.byteValue());
        outputStream.write(avih.dwWidth.byteValue());
        outputStream.write(avih.dwHeight.byteValue());
        outputStream.write(avih.dwReserved[0].byteValue());
        outputStream.write(avih.dwReserved[1].byteValue());
        outputStream.write(avih.dwReserved[2].byteValue());
        outputStream.write(avih.dwReserved[3].byteValue());

        // Write STRL atom
        outputStream.write(strl.dwList.byteValue());
        outputStream.write(strl.dwSize.byteValue());
        outputStream.write(strl.dwFourCc.byteValue());

        // Write AVIStreamHeader atom
        outputStream.write(strh.dwFourCc.byteValue());
        outputStream.write(strh.dwSize.byteValue());
        outputStream.write(strh.fccType.byteValue());
        outputStream.write(strh.fccHandler.byteValue());
        outputStream.write(strh.dwFlags.byteValue());
        outputStream.write(strh.wPriority.byteValue());
        outputStream.write(strh.wLanguage.byteValue());
        outputStream.write(strh.dwInitialFrames.byteValue());
        outputStream.write(strh.dwScale.byteValue());
        outputStream.write(strh.dwRate.byteValue());
        outputStream.write(strh.dwStart.byteValue());
        outputStream.write(strh.dwLength.byteValue());
        outputStream.write(strh.dwSuggestedBufferSize.byteValue());
        outputStream.write(strh.dwQuality.byteValue());
        outputStream.write(strh.dwSampleSize.byteValue());
        outputStream.write(strh.rcFrame.left.byteValue());
        outputStream.write(strh.rcFrame.top.byteValue());
        outputStream.write(strh.rcFrame.right.byteValue());
        outputStream.write(strh.rcFrame.bottom.byteValue());

        // Write ExbmInfoHeader atom
        outputStream.write(strf.dwFourCc.byteValue());
        outputStream.write(strf.dwSize.byteValue());
        outputStream.write(strf.biSize.byteValue());
        outputStream.write(strf.biWidth.byteValue());
        outputStream.write(strf.biHeight.byteValue());
        outputStream.write(strf.biPlanes.byteValue());
        outputStream.write(strf.biBitCount.byteValue());
        outputStream.write(strf.biCompression.byteValue());
        outputStream.write(strf.biSizeImage.byteValue());
        outputStream.write(strf.biXPelsPerMeter.byteValue());
        outputStream.write(strf.biYPelsPerMeter.byteValue());
        outputStream.write(strf.biClrUsed.byteValue());
        outputStream.write(strf.biClrImportant.byteValue());

        // Write DDWW
        outputStream.write("LIST".getBytes());
        outputStream.write(ddww.byteValue());
        outputStream.write("odml".getBytes());

        // Write SZS
        outputStream.write("dmlh".getBytes());
        outputStream.write(szs.byteValue());
        outputStream.write(totalFrames.byteValue());

        // Write MOVI atom
        outputStream.write(movi.dwList.byteValue());
        outputStream.write(movi.dwSize.byteValue());
        outputStream.write(movi.dwFourCc.byteValue());
    }

    public void writeDataChunk(BufferedOutputStream outputStream, InternalFrameBuffer buffer) throws IOException {
        CHUNK data = new CHUNK(
                new DWORD("00db"),
                new DWORD(buffer.size())
        );

        frameLengthBuffer.add(buffer.size());

        outputStream.write(data.dwFourCc.byteValue());
        outputStream.write(data.dwSize.byteValue());
        System.out.println(buffer.size());
        System.out.println(Arrays.toString(data.dwSize.byteValue()));
        buffer.dumpBufferToStream(outputStream);
    }

    public void writeIdx1(BufferedOutputStream outputStream, int frameCount) throws IOException {
        IDX1 idx1 = new IDX1(
                new DWORD("idx1"),
                new DWORD(4 * 4 * frameCount),
                new DWORD(16)
        );

        int offset = 4;

        outputStream.write(idx1.dwIdx1.byteValue());
        outputStream.write(idx1.dwIdxLen.byteValue());

        for (int i = 0; i < frameCount; i++) {
            outputStream.write(new DWORD("00dc").byteValue());
            outputStream.write(idx1.dwAviKeyframe.byteValue());
            outputStream.write(new DWORD(offset).byteValue());
            outputStream.write(new DWORD(frameLengthBuffer.get(i)).byteValue());

            offset += frameLengthBuffer.get(i) + 8;
        }

        outputStream.flush();
    }

    private DWORD getRiffSize(int frameCount, int length) {
        int len = frameCount * length;
        return new DWORD(256 + len + 8 * frameCount + 16 * frameCount);
    }

    private int getStrfImageSize(DWORD biWidth, DWORD biHeight, int biBitCount) {
        return ((biWidth.intValue() * biBitCount / 8 + 3) & 0xFFFFFFFC) * biHeight.intValue();
    }

    private DWORD getMoviSize(int frameCount, int length) {
        return new DWORD(length + 4 + 8 * frameCount);
    }
}

class RIFF {
    public DWORD dwRIFF;
    public DWORD dwSize;
    public DWORD dwFourCc;

    public RIFF(DWORD dwRIFF, DWORD dwSize, DWORD dwFourCc) {
        this.dwRIFF = dwRIFF;
        this.dwSize = dwSize;
        this.dwFourCc = dwFourCc;
    }
}

class CHUNK {
    public DWORD dwFourCc;
    public DWORD dwSize;

    public CHUNK(DWORD dwFourCc, DWORD dwSize) {
        this.dwFourCc = dwFourCc;
        this.dwSize = dwSize;
    }
}

class LIST {
    public DWORD dwList;
    public DWORD dwSize;
    public DWORD dwFourCc;

    public LIST(DWORD dwList, DWORD dwSize, DWORD dwFourCc) {
        this.dwList = dwList;
        this.dwSize = dwSize;
        this.dwFourCc = dwFourCc;
    }
}

class MainAVIHeader {
    public DWORD dwFourCc;
    public DWORD dwSize;

    public DWORD dwMicroSecPerFrame;
    public DWORD dwMaxBytesPerSec;
    public DWORD dwPaddingGranularity;

    public DWORD dwFlags;
    public DWORD dwTotalFrames;
    public DWORD dwInitialFrames;
    public DWORD dwStreams;
    public DWORD dwSuggestedBufferSize;

    public DWORD dwWidth;
    public DWORD dwHeight;

    public DWORD[] dwReserved;

    public MainAVIHeader(DWORD dwFourCc, DWORD dwSize, DWORD dwMicroSecPerFrame, DWORD dwMaxBytesPerSec, DWORD dwPaddingGranularity, DWORD dwFlags, DWORD dwTotalFrames, DWORD dwInitialFrames, DWORD dwStreams, DWORD dwSuggestedBufferSize, DWORD dwWidth, DWORD dwHeight, DWORD[] dwReserved) {
        this.dwFourCc = dwFourCc;
        this.dwSize = dwSize;
        this.dwMicroSecPerFrame = dwMicroSecPerFrame;
        this.dwMaxBytesPerSec = dwMaxBytesPerSec;
        this.dwPaddingGranularity = dwPaddingGranularity;
        this.dwFlags = dwFlags;
        this.dwTotalFrames = dwTotalFrames;
        this.dwInitialFrames = dwInitialFrames;
        this.dwStreams = dwStreams;
        this.dwSuggestedBufferSize = dwSuggestedBufferSize;
        this.dwWidth = dwWidth;
        this.dwHeight = dwHeight;
        this.dwReserved = dwReserved;
    }
}

class RECT {
    public DWORD left;
    public DWORD top;
    public DWORD right;
    public DWORD bottom;

    public RECT(DWORD left, DWORD top, DWORD right, DWORD bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }
}

class AVIStreamHeader {
    public DWORD dwFourCc;
    public DWORD dwSize;

    public DWORD fccType;
    public DWORD fccHandler;
    public DWORD dwFlags;
    public WORD wPriority;
    public WORD wLanguage;
    public DWORD dwInitialFrames;
    public DWORD dwScale;
    public DWORD dwRate;
    public DWORD dwStart;
    public DWORD dwLength;
    public DWORD dwSuggestedBufferSize;
    public DWORD dwQuality;
    public DWORD dwSampleSize;
    public RECT rcFrame;

    public AVIStreamHeader(DWORD dwFourCc, DWORD dwSize, DWORD fccType, DWORD fccHandler, DWORD dwFlags, WORD wPriority, WORD wLanguage, DWORD dwInitialFrames, DWORD dwScale, DWORD dwRate, DWORD dwStart, DWORD dwLength, DWORD dwSuggestedBufferSize, DWORD dwQuality, DWORD dwSampleSize, RECT rcFrame) {
        this.dwFourCc = dwFourCc;
        this.dwSize = dwSize;
        this.fccType = fccType;
        this.fccHandler = fccHandler;
        this.dwFlags = dwFlags;
        this.wPriority = wPriority;
        this.wLanguage = wLanguage;
        this.dwInitialFrames = dwInitialFrames;
        this.dwScale = dwScale;
        this.dwRate = dwRate;
        this.dwStart = dwStart;
        this.dwLength = dwLength;
        this.dwSuggestedBufferSize = dwSuggestedBufferSize;
        this.dwQuality = dwQuality;
        this.dwSampleSize = dwSampleSize;
        this.rcFrame = rcFrame;
    }
}

class EXBMINFOHEADER {
    public DWORD dwFourCc;
    public DWORD dwSize;
    public DWORD biSize;
    public DWORD biWidth;
    public DWORD biHeight;
    public WORD biPlanes;
    public WORD biBitCount;
    public DWORD biCompression;
    public DWORD biSizeImage;
    public DWORD biXPelsPerMeter;
    public DWORD biYPelsPerMeter;
    public DWORD biClrUsed;
    public DWORD biClrImportant;

    public EXBMINFOHEADER(DWORD dwFourCc, DWORD dwSize, DWORD biSize, DWORD biWidth, DWORD biHeight, WORD biPlanes, WORD biBitCount, DWORD biCompression, DWORD biSizeImage, DWORD biXPelsPerMeter, DWORD biYPelsPerMeter, DWORD biClrUsed, DWORD biClrImportant) {
        this.dwFourCc = dwFourCc;
        this.dwSize = dwSize;
        this.biSize = biSize;
        this.biWidth = biWidth;
        this.biHeight = biHeight;
        this.biPlanes = biPlanes;
        this.biBitCount = biBitCount;
        this.biCompression = biCompression;
        this.biSizeImage = biSizeImage;
        this.biXPelsPerMeter = biXPelsPerMeter;
        this.biYPelsPerMeter = biYPelsPerMeter;
        this.biClrUsed = biClrUsed;
        this.biClrImportant = biClrImportant;
    }
}

class IDX1 {
    public DWORD dwIdx1;
    public DWORD dwIdxLen;
    public DWORD dwAviKeyframe;

    public IDX1(DWORD dwIdx1, DWORD dwIdxLen, DWORD dwAviKeyframe) {
        this.dwIdx1 = dwIdx1;
        this.dwIdxLen = dwIdxLen;
        this.dwAviKeyframe = dwAviKeyframe;
    }
}

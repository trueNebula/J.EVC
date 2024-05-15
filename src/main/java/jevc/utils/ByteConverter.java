package jevc.utils;

import jevc.entities.DWORD;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteConverter {
    public static int readInt(RandomAccessFile file, byte[] buffer) throws IOException {
        file.readFully(buffer, 0, 4);
        return ByteBuffer.wrap(buffer, 0, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static String readString(RandomAccessFile file, byte[] buffer) throws IOException {
        file.readFully(buffer, 0, 4);
        return new String(buffer, 0, 4);
    }

    public static boolean checkString(RandomAccessFile file, byte[] buffer, String expected) throws IOException {
        file.readFully(buffer, 0, 4);
        return new String(buffer, 0, 4).equals(expected);
    }

    public static DWORD readDWORD(RandomAccessFile file, byte[] buffer) throws IOException {
        file.readFully(buffer, 0, 4);
        return new DWORD(buffer);
    }
}

package jevc.entities;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class InternalFrameBuffer {
    ByteArrayOutputStream buffer;

    public InternalFrameBuffer() {
        this.buffer = new ByteArrayOutputStream();
    }

    public void write(int value) {
        this.buffer.write(value);
    }

    public void write(byte value) {
        this.buffer.write(value);
    }

    public void write(byte[] value) {
        this.buffer.writeBytes(value);
    }

    public void dumpBufferToStreamWithoutFlushing(BufferedOutputStream outputStream) throws IOException {
        outputStream.write(this.buffer.toByteArray());
    }
    public int dumpBufferToStream(BufferedOutputStream outputStream) throws IOException {
        outputStream.write(this.buffer.toByteArray());
        int size = this.buffer.size();
        this.buffer.reset();

        return size;
    }

    public int size() {
        return this.buffer.size();
    }
}

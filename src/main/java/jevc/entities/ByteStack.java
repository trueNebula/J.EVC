package jevc.entities;

public class ByteStack {
    private final byte[] stack;
    private int top;

    public ByteStack(byte[] data, int size) {
        stack = data;
        top = 0;
    }

    public byte pop() {
        if (isEmpty()) {
            return 0;
        } else {
            return stack[top++];
        }
    }

    public byte[] pop(int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = pop();
        }
        return result;
    }

    public byte[] peek(int length) {
        byte[] result = new byte[length];
        if (top + length >= stack.length) {
            length = stack.length - top;
        }
        System.arraycopy(stack, top, result, 0, length);
        return result;
    }

    public boolean peekForEOB() {
        return stack[top] == (byte) 0xFF && stack[top + 1] == (byte) 0xFF;
    }

    public boolean isEmpty() {
        return top == stack.length - 1;
    }
}

package jevc.entities;

public class FrameOpDataObject {
    private final MotionVector motionVector;
    private final Block error;
    private final String codeword;

    public FrameOpDataObject(MotionVector motionVector, Block error, String codeword) {
        this.motionVector = motionVector;
        this.error = error;
        this.codeword = codeword;
    }

    public MotionVector getMotionVector() {
        return motionVector;
    }
    public Block getError() {
        return error;
    }
    public String getCodeword() {
        return codeword;
    }
}

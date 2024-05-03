package jevc.entities;

public class RunLength {
    // a DC coefficient would have the runlength = -1
    // an End of Block (EOB) will have: runlength = size = amplitude = 0
    private int runlength;
    private int size;
    private int amplitude;

    public RunLength(int runlength, int size, int amplitude) {
        this.runlength = runlength;
        this.size = size;
        this.amplitude = amplitude;
    }

    public void setRunlength(int rl) {
        this.runlength = rl;
    }
    public int getRunlength() {
        return runlength;
    }

    public void setSize(int size) {
        this.size = size;
    }
    public int getSize() {
        return size;
    }

    public void setAmplitude(int amplitude) {
        this.amplitude = amplitude;
    }
    public int getAmplitude() {
        return amplitude;
    }
}

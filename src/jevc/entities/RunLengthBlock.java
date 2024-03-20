package jevc.entities;

import java.util.ArrayList;


public class RunLengthBlock {
    private ArrayList<RunLength> data;
    private char type; // 'Y', 'U', 'V'   ('U'='Cb'; 'V'='Cr')

    public RunLengthBlock(ArrayList<RunLength> data, char type) {
        this.data = data;
        this.type = type;
    }

    public RunLengthBlock() {
        data = new ArrayList<RunLength>();
        type = '-';
    }

    public void print() {
        System.out.print("RunLengthBlock " + type + ": ");
        for (RunLength rl: data) {
            System.out.print("[" + rl.getRunlength() + ", " + rl.getSize() +", " + rl.getAmplitude() + "] ");
        }
        System.out.println();
    }

    public ArrayList<RunLength> getData() {
        return data;
    }
    public int getSize() { return data.size(); }
    public char getType() { return type; }

    public void setData(ArrayList<RunLength> data) {
        this.data = data;
    }
    public void setType(char type) {
        if ((type!='Y') && (type!='U') && (type!='V')) return;
        this.type = type;
    }
}

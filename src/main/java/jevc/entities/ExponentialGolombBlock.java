package jevc.entities;

import java.util.ArrayList;

public class ExponentialGolombBlock {
    private ArrayList<Integer> data;

    public ExponentialGolombBlock(ArrayList<Integer> data) {
        this.data = data;
    }

    public void print() {
        System.out.print("ExponentialGolombBlock: ");
        for (int i: data) {
            System.out.print(i + " ");
        }
        System.out.println();
    }
}

package jevc.entities;

public class Block {
    public static final int BLOCKSIZE = 8;
    private final char type; // 'Y', 'U', 'V'
    private int posX;
    private int posY;
    private int[][] data;

    public Block(int[][] data, char type, int posX, int posY) {
        this.type = type;
        this.posX = posX;
        this.posY = posY;
        this.data = data;
    }

    public Block(int[][] data, char type) {
        this.type = type;
        this.data = data;
        this.posX = -1;
        this.posY = -1;
    }

    public Block getCopy() {
        int[][] newData = new int[BLOCKSIZE][BLOCKSIZE];
        for (int i=0; i<BLOCKSIZE; i++) {
            for (int j = 0; j < BLOCKSIZE; j++) {
                newData[i][j] = data[i][j];
            }
        }
        return new Block(newData, type, posX, posY);
    }

    public void print() {
        System.out.println("++++++++++++++++++++++++++++++++++");
        System.out.println("Block type: " + type);
        for (int i=0; i<BLOCKSIZE; i++) {
            for (int j = 0; j < BLOCKSIZE; j++) {
                System.out.print(data[i][j] + " ");
            }
            System.out.println();
        }
        System.out.println("++++++++++++++++++++++++++++++++++");
    }

    public void setData(int[][] data) {
        this.data = data;
    }

    public int[][] getData() {
        return this.data;
    }

    public char getType() {
        return type;
    }

    public void setPos(int posX, int posY) { this.posX = posX; this.posY = posY; }

    public int getPosX() { return posX; }

    public int getPosY() { return posY; }

    public void add(Block block) {
        for (int i=0; i<BLOCKSIZE; i++) {
            for (int j = 0; j < BLOCKSIZE; j++) {
                data[i][j] += block.data[i][j];
            }
        }
    }

    public void subtract(Block block) {
        for (int i=0; i<BLOCKSIZE; i++) {
            for (int j = 0; j < BLOCKSIZE; j++) {
                data[i][j] -= block.data[i][j];
            }
        }
    }

    public boolean isEmpty() {
        for (int i=0; i<BLOCKSIZE; i++) {
            for (int j = 0; j < BLOCKSIZE; j++) {
                if (data[i][j] != 0) return false;
            }
        }
        return true;
    }
}

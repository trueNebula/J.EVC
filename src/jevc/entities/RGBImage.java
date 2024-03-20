package jevc.entities;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RGBImage {
    public int[][] R;
    public int[][] G;
    public int[][] B;
    public int width, height;

    public RGBImage(int[][] pixels, int width, int height) {
        this.width = width;
        this.height = height;
        R = new int[height][width];
        G = new int[height][width];
        B = new int[height][width];
        FromRGBPixels (pixels, width, height);
    }

    public RGBImage(int[][] Y, int[][] Cb, int[][] Cr, int width, int height) {
        this.width = width;
        this.height = height;
        R = new int[height][width];
        G = new int[height][width];
        B = new int[height][width];
        FromYCbCr(Y, Cb, Cr, width, height);
    }

    public void FromRGBPixels (int[][] pixels, int width, int height) {
        int red, green, blue;
        for (int i=0; i<height; i++)
            for (int j=0; j<width; j++) {
                red = (pixels[i][j] & 0x00ff0000) >> 16;
                green = (pixels[i][j] & 0x0000ff00) >> 8;
                blue = (pixels[i][j] & 0x000000ff);
                R[i][j] = red;
                G[i][j] = green;
                B[i][j] = blue;
            }
    }

    public void FromYCbCr(int[][] Y, int[][] Cb, int[][] Cr, int width, int height) {
        for (int i=0; i<height; i++)
            for (int j=0; j<width; j++) {
                R[i][j] = clamp((int) (Y[i][j] + 1.402*(Cr[i][j]-128)));
                G[i][j] = clamp((int) (Y[i][j] - 0.344*(Cb[i][j]-128) - 0.714*(Cr[i][j]-128)));
                B[i][j] = clamp((int) (Y[i][j] + 1.772*(Cb[i][j]-128)));
            }
    }

    private int clamp(int val) {
        if (val < 0) return 0;
        return Math.min(val, 255);
    }

    public void writePNGFile(String outfile) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int rgb;
        for (int i=0; i<height; i++) {
            for (int j = 0; j < width; j++) {
                rgb = 0;
                rgb = rgb | (R[i][j] << 16) | (G[i][j] << 8) | B[i][j];
                image.setRGB(j, i, rgb);
            }
        }
        ImageIO.write(image, "png", new File(outfile));
    }
}

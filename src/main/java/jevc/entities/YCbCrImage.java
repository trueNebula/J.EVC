package jevc.entities;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class YCbCrImage {
    public int[][] Y;
    public int[][] Cb;
    public int[][] Cr;
    private int width, height;
    private int sampling;

    /* We support the following subsampling rates:
     *    YUV444    3 bytes per pixel     (12 bytes per 4 pixels)
     *    YUV422    4 bytes per 2 pixels   (8 bytes per 4 pixels)
     *    YUV411    6 bytes per 4 pixels
     *    (NOT: YUV420p   6 bytes per 4 pixels, reordered)
     */
    public static final int YUV444Sampling = 0;
    public static final int YUV422HorizSampling = 1;
    public static final int YUV422VertSampling = 2;
    public static final int YUV411Sampling = 3; // equivalent to YUV420p

    public YCbCrImage(int height, int width, int sampling) {
        if ((sampling != YUV444Sampling) && (sampling != YUV422HorizSampling) &&
                (sampling != YUV422VertSampling) && (sampling != YUV411Sampling)) {
            return;
        }
        if ((height<=0) || (width<=0)) return;

        this.width = width;
        this.height = height;
        this.sampling = sampling;
        Y = new int[height][width];
        Cb = new int[height][width];
        Cr = new int[height][width];
    }

    public YCbCrImage(int[][] pixels, int width, int height) {
        this.width = width;
        this.height = height;
        this.sampling = YUV444Sampling;
        /* We always allocate more space for pixels so that rows and columns can
         * be padded in order to become a multiple of 16. We do this so that we
         * can apply 4:1:1 subsampling and then divide the picture into 8x8 pixel blocks.
         */
        int rows = height + ((height%16==0) ? 0 : 16-height%16);
        int cols = width + ((width%16==0) ? 0 : 16-width%16);
        Y = new int[rows][cols];
        Cb = new int[rows][cols];
        Cr = new int[rows][cols];
        FromRGBPixels (pixels, width, height);
    }

    public YCbCrImage(ArrayList<Block> blocks, int height, int width, int sampling) {
        if ((sampling != YUV444Sampling) && (sampling != YUV422HorizSampling) &&
                (sampling != YUV422VertSampling) && (sampling != YUV411Sampling)) {
            return;
        }
        this.width = width;
        this.height = height;
        this.sampling = sampling;
        Y = new int[height][width];
        Cb = new int[height][width];
        Cr = new int[height][width];
        int[][] data;

        for (int k = 0; k < blocks.size(); k++) {
            Block block = blocks.get(k);
//            if (k == 9363) {
//                System.out.println("this is bad!");
//
//            }
//
//            System.out.println("Adding block " + block.getType() + " with positions " +
//                    "[posY=" + block.getPosY() + ", posX=" + block.getPosX() + "] to the image ..");

            switch (block.getType()) {
                case 'Y' -> {
                    data = block.getData();
                    for (int i = 0; i < 8; i++) {
                        for (int j = 0; j < 8; j++) {
                            if (block.getPosY() + i == 1088) {
//                                System.out.println('a');
                            }
                            Y[block.getPosY() + i][block.getPosX() + j] = data[i][j];
                        }
                    }
                }
                case 'U' -> {
                    data = block.getData();
                    for (int i = 0; i < 8; i++) {
                        for (int j = 0; j < 8; j++) {
                            Cb[block.getPosY() + i][block.getPosX() + j] = data[i][j];
                        }
                    }
                }
                case 'V' -> {
                    data = block.getData();
                    for (int i = 0; i < 8; i++) {
                        for (int j = 0; j < 8; j++) {
                            Cr[block.getPosY() + i][block.getPosX() + j] = data[i][j];
                        }
                    }
                }
            }
        }
    }

    /* This code is not used anymore, the above constructor is better. */
    public void FromBlocks(ArrayList<Block> blocks, int sampling) {
        if ((sampling != YUV444Sampling) && (sampling != YUV422HorizSampling) &&
                (sampling != YUV422VertSampling) && (sampling != YUV411Sampling)) {
            return;
        }
        this.sampling = sampling;

        switch (sampling) {
            case YUV444Sampling -> FromYUV444SampledBlocks(blocks);
            case YUV422HorizSampling -> FromYUV422HorizSampledBlocks(blocks);
            case YUV422VertSampling -> FromYUV422VertSampledBlocks(blocks);
            case YUV411Sampling -> FromYUV411SampledBlocks(blocks);
        }
    }

    /* This function is not used anymore, it was used in the function:
     *      YCbCrImage(ArrayList<Block> blocks, int height, int width, int sampling)
     */
    public void FromYUV444SampledBlocks(ArrayList<Block> blocks) {
        int Yline, Cbline, Crline;
        int Ycol, Cbcol, Crcol;

        Yline = Cbline = Crline = 0;
        Ycol = Cbcol = Crcol = 0;

        System.out.println("height="+height+" width="+width);
        for (Block block: blocks) {
            if (block.getType()=='Y') {
                int[][] data = block.getData();
                for (int i=Yline; i<Yline+8; i++) {
                    for (int j=Ycol; j<Ycol+8; j++) {
                        Y[i][j] = data[i-Yline][j-Ycol];
                    }
                }
                Ycol += 8;
                if (Ycol >= width) { Yline += 8; Ycol = 0; }
            } else if (block.getType()=='U') {
                int[][] data = block.getData();
                for (int i=Cbline; i<Cbline+8; i++) {
                    for (int j=Cbcol; j<Cbcol+8; j++) {
                        Cb[i][j] = data[i-Cbline][j-Cbcol];
                    }
                }
                Cbcol += 8;
                if (Cbcol >= width) { Cbline += 8; Cbcol = 0; }
            } else if (block.getType()=='V') {
                int[][] data = block.getData();
                for (int i=Crline; i<Crline+8; i++) {
                    for (int j=Crcol; j<Crcol+8; j++) {
                        Cr[i][j] = data[i-Crline][j-Crcol];
                    }
                }
                Crcol += 8;
                if (Crcol >= width) { Crline += 8; Crcol = 0; }
            }
        }
    }

    /* This function is not used anymore, it was used in the function:
     *      YCbCrImage(ArrayList<Block> blocks, int height, int width, int sampling)
     */
    public void FromYUV422HorizSampledBlocks(ArrayList<Block> blocks) {
        int Yline, Cbline, Crline;
        int Ycol, Cbcol, Crcol;

        Yline = Cbline = Crline = 0;
        Ycol = Cbcol = Crcol = 0;

        System.out.println("height="+height+" width="+width);
        for (Block block: blocks) {
            if (block.getType()=='Y') {
                // In the following code, we need to place 4 consecutive Y blocks, Y1, Y2, Y3, Y4,
                // on the final image like this:
                //      Y1   Y2   ...
                //      Y3   Y4   ...
                //       .    .
                //       .    .
                //
                int[][] data = block.getData();
                for (int i=Yline; i<Yline+8; i++) {
                    for (int j=Ycol; j<Ycol+8; j++) {
                        Y[i][j] = data[i-Yline][j-Ycol];
                    }
                }
                Yline += 8;
                Ycol  += 8;
                // [0,0]->[8,8]   [0,8]->[8,16]
                // [8,0]->[16,8]  [8,8]->[16,16]
                if ((Yline%16==0) && (Ycol%16==0) && (Ycol>=width)) {
                    Ycol = 0;
                } else {
                    if (Ycol % 16 == 0) {
                        if (Yline % 16 == 0) {
                            Yline -= 16;
                        } else {
                            Ycol -= 16;
                        }
                    } else {
                        Yline -= 8;
                    }
                }
            } else if (block.getType()=='U') {
                // In the following code, we need to place 2 consecutive U blocks, U1, U2,
                // on the final image like this:
                //      U1   ...
                //      U2   ...
                //       .   ...
                //       .   ...
                //
                int[][] data = block.getData();
                for (int i=Cbline; i<Cbline+8; i++) {
                    for (int j=Cbcol; j<Cbcol+8; j++) {
                        Cb[i][j] = data[i-Cbline][j-Cbcol];
                    }
                }
                Cbline += 8;
                Cbcol  += 8;
                // [0,0]->[8,8]
                // [8,0]->[16,8]
                if ((Cbline%16==0) && (Cbcol>=width)) {
                    Cbcol = 0;
                } else {
                    if (Cbline%16==0) {
                        Cbline -= 16;
                    } else {
                        Cbcol -= 8;
                    }
                }
            } else if (block.getType()=='V') {
                // In the following code, we need to place 2 consecutive V blocks, V1, V2,
                // on the final image like this:
                //      V1   ...
                //      V2   ...
                //       .   ...
                //       .   ...
                //
                int[][] data = block.getData();
                for (int i=Crline; i<Crline+8; i++) {
                    for (int j=Crcol; j<Crcol+8; j++) {
                        Cr[i][j] = data[i-Crline][j-Crcol];
                    }
                }
                Crline += 8;
                Crcol  += 8;
                // [0,0]->[8,8]
                // [8,0]->[16,8]
                if ((Crline%16==0) && (Crcol>=width)) {
                    Crcol = 0;
                } else {
                    if (Crline%16==0) {
                        Crline -= 16;
                    } else {
                        Crcol -= 8;
                    }
                }
            }
        }
    }

    /* This function is not used anymore, it was used in the function:
     *      YCbCrImage(ArrayList<Block> blocks, int height, int width, int sampling)
     */
    public void FromYUV422VertSampledBlocks(ArrayList<Block> blocks) {
        int Yline, Cbline, Crline;
        int Ycol, Cbcol, Crcol;

        Yline = Cbline = Crline = 0;
        Ycol = Cbcol = Crcol = 0;

        System.out.println("height="+height+" width="+width);
        for (Block block: blocks) {
            if (block.getType()=='Y') {
                // In the following code, we need to place 4 consecutive Y blocks, Y1, Y2, Y3, Y4,
                // on the final image like this:
                //      Y1   Y2   ...
                //      Y3   Y4   ...
                //       .    .
                //       .    .
                //
                int[][] data = block.getData();
                for (int i=Yline; i<Yline+8; i++) {
                    for (int j=Ycol; j<Ycol+8; j++) {
                        Y[i][j] = data[i-Yline][j-Ycol];
                    }
                }
                Yline += 8;
                Ycol  += 8;
                // [0,0]->[8,8]   [0,8]->[8,16]
                // [8,0]->[16,8]  [8,8]->[16,16]
                if ((Yline%16==0) && (Ycol%16==0) && (Ycol>=width)) {
                    Ycol = 0;
                } else {
                    if (Ycol % 16 == 0) {
                        if (Yline % 16 == 0) {
                            Yline -= 16;
                        } else {
                            Ycol -= 16;
                        }
                    } else {
                        Yline -= 8;
                    }
                }
            } else if (block.getType()=='U') {
                // In the following code, we need to place 2 consecutive U blocks, U1, U2,
                // on the final image like this:
                //  U1   U2  ...
                //  .   .    ..
                //  .   .    ..
                //
                int[][] data = block.getData();
                for (int i=Cbline; i<Cbline+8; i++) {
                    for (int j=Cbcol; j<Cbcol+8; j++) {
                        Cb[i][j] = data[i-Cbline][j-Cbcol];
                    }
                }
                Cbcol += 8;
                if (Cbcol >= width) { Cbline += 8; Cbcol = 0; }
            } else if (block.getType()=='V') {
                // In the following code, we need to place 2 consecutive V blocks, V1, V2,
                // on the final image like this:
                // V1  V2  ...
                //  .   .  ..
                //  .   .  ..
                //
                int[][] data = block.getData();
                for (int i=Crline; i<Crline+8; i++) {
                    for (int j=Crcol; j<Crcol+8; j++) {
                        Cr[i][j] = data[i-Crline][j-Crcol];
                    }
                }
                Crcol += 8;
                if (Crcol >= width) { Crline += 8; Crcol = 0; }
            }
        }
    }

    /* This function is not used anymore, it was used in the function:
     *      YCbCrImage(ArrayList<Block> blocks, int height, int width, int sampling)
     */
    public void FromYUV411SampledBlocks(ArrayList<Block> blocks) {
        int Yline, Cbline, Crline;
        int Ycol, Cbcol, Crcol;

        Yline = Cbline = Crline = 0;
        Ycol = Cbcol = Crcol = 0;

        System.out.println("YUV411Sampling height=" + height + " width=" + width);
        for (Block block : blocks) {
            if (block.getType() == 'Y') {
                // In the following code, we need to place 4 consecutive Y blocks, Y1, Y2, Y3, Y4,
                // on the final image like this:
                //      Y1   Y2   ...
                //      Y3   Y4   ...
                //       .    .
                //       .    .
                //
                int[][] data = block.getData();
                for (int i = Yline; i < Yline + 8; i++) {
                    for (int j = Ycol; j < Ycol + 8; j++) {
                        Y[i][j] = data[i - Yline][j - Ycol];
                    }
                }
                Yline += 8;
                Ycol += 8;
                // [0,0]->[8,8]   [0,8]->[8,16]
                // [8,0]->[16,8]  [8,8]->[16,16]
                if ((Yline % 16 == 0) && (Ycol % 16 == 0) && (Ycol >= width)) {
                    Ycol = 0;
                } else {
                    if (Ycol % 16 == 0) {
                        if (Yline % 16 == 0) {
                            Yline -= 16;
                        } else {
                            Ycol -= 16;
                        }
                    } else {
                        Yline -= 8;
                    }
                }
            } else if (block.getType() == 'U') {
                int[][] data = block.getData();
                for (int i = Cbline; i < Cbline + 8; i++) {
                    for (int j = Cbcol; j < Cbcol + 8; j++) {
                        Cb[i][j] = data[i - Cbline][j - Cbcol];
                    }
                }
                Cbcol += 8;
                if (Cbcol >= width/2) { Cbline += 8; Cbcol = 0; }
            } else if (block.getType() == 'V') {
                int[][] data = block.getData();
                for (int i = Crline; i < Crline + 8; i++) {
                    for (int j = Crcol; j < Crcol + 8; j++) {
                        Cr[i][j] = data[i - Crline][j - Crcol];
                    }
                }
                Crcol += 8;
                if (Crcol >= width/2) { Crline += 8; Crcol = 0; }
            }
        }
    }

    private void FromRGBPixels (int[][] pixels, int width, int height) {
        /* a pixel is just a 32 bit integer in RGB format; the most significant 8 bits are not used */
        int red, green, blue, c;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                red = (pixels[i][j] & 0x00ff0000) >> 16;
                green = (pixels[i][j] & 0x0000ff00) >> 8;
                blue = (pixels[i][j] & 0x000000ff);
                c = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
                Y[i][j] = clamp(c);
                c = (int) (128 - 0.1687 * red - 0.3312 * green + 0.5 * blue);
                Cb[i][j] = clamp(c);
                c = (int) (128 + 0.5 * red - 0.4186 * green - 0.0813 * blue);
                Cr[i][j] = clamp(c);
                //System.out.print("(" + Y[i][j] + "," + Cb[i][j] + "," + Cr[i][j] + ") ");
            }
            //System.out.println();
        }
    }

    public void FromRGBPixels(int[][] R, int[][] G, int[][] B, int width, int height) {
        int c;
        for (int i=0; i<height; i++)
            for (int j=0; j<width; j++) {
                c = (int) (0.299*R[i][j] + 0.587*G[i][j] + 0.114*B[i][j]);
                Y[i][j] = clamp(c);
                c = (int) (128 - 0.1687*R[i][j] - 0.3312*G[i][j] + 0.5*B[i][j]);
                Cb[i][j] = clamp(c);
                c = (int) (128 + 0.5*R[i][j] - 0.4186*G[i][j] - 0.0813*B[i][j]);
                Cr[i][j] = clamp(c);
            }
    }

    public RGBImage convertToRGBImage() {
        return new RGBImage(Y, Cb, Cr, width, height);
    }

    public void ScaleImage() {
        int horizontalPadding = (width%16==0) ? 0 : 16 - width%16;
        int verticalPadding = (height%16==0) ? 0 : 16 - height%16;

        /* the padding space is already reserved in the constructor */
        if (height%16 > 0) {
            for (int i=height; i<height+verticalPadding; i++)
                for(int j=0; j<width; j++) {
                    Y[i][j] = Y[height-1][j];
                    Cb[i][j] = Cb[height-1][j];
                    Cr[i][j] = Cr[height-1][j];
                }
        }
        if (width%16 > 0) {
            for (int i=0; i<height; i++)
                for(int j=width; j<width+horizontalPadding; j++) {
                    Y[i][j] = Y[i][width-1];
                    Cb[i][j] = Cb[i][width-1];
                    Cr[i][j] = Cr[i][width-1];
                }
        }
        if ( (height%16>0) && (width%16>0) ) {
            for (int i=height; i<height+verticalPadding; i++)
                for (int j=width; j<width+horizontalPadding; j++) {
                    Y[i][j] = Y[height-1][width-1];
                    Cb[i][j] = Cb[height-1][width-1];
                    Cr[i][j] = Cr[height-1][width-1];
                }
        }
        height += verticalPadding;
        width += horizontalPadding;
    }

    public void PerformSubsampling(int samplingRatio) {
        if ((samplingRatio!= YUV422HorizSampling) && (samplingRatio!= YUV422VertSampling) &&
                (samplingRatio!= YUV411Sampling))
            return;

        if (samplingRatio == YUV422VertSampling) {
            /* 4 pixels are encoded into 4xY, 2xCb, 2xCr values */
            for (int i = 0; i < height / 2; i++)
                for (int j = 0; j < width; j++) {
                    Cb[i][j] = (Cb[2 * i][j] + Cb[2 * i + 1][j]) / 2;
                    Cr[i][j] = (Cr[2 * i][j] + Cr[2 * i + 1][j]) / 2;
                }
            // the width and height remain unchanged
        }
        if (samplingRatio == YUV422HorizSampling) {
            /* 4 pixels are encoded into 4xY, 2xCb, 2xCr values */
            for (int i = 0; i < height; i++)
                for (int j = 0; j < width/2; j++) {
                    Cb[i][j] = (Cb[i][2 * j] + Cb[i][2 * j + 1]) / 2;
                    Cr[i][j] = (Cr[i][2 * j] + Cr[i][2 * j + 1]) / 2;
                }
            // the width and height remain unchanged
        }
        if (samplingRatio == YUV411Sampling) {
            /* 4 pixels are encoded into 4xY, 1xCb, 1xCr values */
            for (int i = 0; i < height / 2; i++)
                for (int j = 0; j < width / 2; j++) {
                    Cb[i][j] = (Cb[2*i][2*j] + Cb[2*i][2*j+1] + Cb[2*i+1][2*j] + Cb[2*i+1][2*j+1]) / 4;
                    Cr[i][j] = (Cr[2*i][2*j] + Cr[2*i][2*j+1] + Cr[2*i+1][2*j] + Cr[2*i+1][2*j+1]) / 4;
                }
            // the width and height remain unchanged
        }

        this.sampling = samplingRatio;
    }

    public void PerformUpsampling() {
        if (sampling==YUV444Sampling)
            return; // nothing to do
        if (sampling==YUV422VertSampling) {
            System.out.println("Upsampling the image from YUV422VertSampling ..");
            int[][] newCbPixels = new int[height][width];
            int[][] newCrPixels = new int[height][width];
            for (int i = 0; i < height; i+=2) {
                for (int j = 0; j < width; j++) {
                    newCbPixels[i][j] = Cb[i/2][j];
                    newCbPixels[i+1][j] = Cb[i/2][j];
                    newCrPixels[i][j] = Cr[i/2][j];
                    newCrPixels[i+1][j] = Cr[i/2][j];
                }
            }
            Cb = newCbPixels;
            Cr = newCrPixels;
        }
        if (sampling==YUV422HorizSampling) {
            System.out.println("Upsampling the image from YUV422HorizSampling ..");
            int[][] newCbPixels = new int[height][width];
            int[][] newCrPixels = new int[height][width];
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j+=2) {
                    newCbPixels[i][j] = Cb[i][j/2];
                    newCbPixels[i][j+1] = Cb[i][j/2];
                    newCrPixels[i][j] = Cr[i][j/2];
                    newCrPixels[i][j+1] = Cr[i][j/2];
                }
            }
            Cb = newCbPixels;
            Cr = newCrPixels;
        }

        if (sampling==YUV411Sampling) {
            System.out.println("Upsampling the image YUV411Sampling ..");
            int[][] newCbPixels = new int[height][width];
            int[][] newCrPixels = new int[height][width];
            for (int i = 0; i < height; i+=2) {
                for (int j = 0; j < width; j+=2) {
                    newCbPixels[i][j] = Cb[i/2][j/2];
                    newCbPixels[i+1][j] = Cb[i/2][j/2];
                    newCbPixels[i][j+1] = Cb[i/2][j/2];
                    newCbPixels[i+1][j+1] = Cb[i/2][j/2];

                    newCrPixels[i][j] = Cr[i/2][j/2];
                    newCrPixels[i+1][j] = Cr[i/2][j/2];
                    newCrPixels[i][j+1] = Cr[i/2][j/2];
                    newCrPixels[i+1][j+1] = Cr[i/2][j/2];
                }
            }
            Cb = newCbPixels;
            Cr = newCrPixels;
        }
    }

    /* This method splits all color component matrixes (Y, Cb and Cr) into
     * 8x8 blocks and adds all these into one ArrayList in the correct encoding
     * order, the order in which blocks are expected to be written in the compressed
     * bitstream (as specified by the JPEG standard). The encoding order depends on the
     * sampling of this image and is specified by the JPEG standard. The encoding
     * order is detailed below:
     *
     * o If sampling == YUV444Sampling the blocks will appear in this order in the compressed bitstream:
     *      Y0, Cb0, Cr0, Y1, Cb1, Cr1, Y2, Cb2, Cr2, ...
     * o If sampling == YUV422HorizSampling/YUV422VertSampling the blocks will appear in this order
     *   in the compressed bitstream:
     *      Y0, Y1, Y2, Y3, Cb0, Cb1, Cr0, Cr1, Y4, Y5, Y6, Y7, Cb2, Cb3, Cr2, Cr3, ...
     * o If sampling == YUV111Sampling the blocks will appear in this order in the compressed bitstream:
     *      Y0, Y1, Y2, Y3, Cb0, Cr0, Y4, Y5, Y6, Y7, Cb1, Cr1, ...
     */
    public ArrayList<Block> PerformBlockSplitting() {
        ArrayList<Block> YBlocks = SplitColorBlocks('Y');
        ArrayList<Block> CbBlocks = SplitColorBlocks('U');
        ArrayList<Block> CrBlocks = SplitColorBlocks('V');
        
        ArrayList<Block> result = new ArrayList<>();
        int noOfBlocks = (height*width) / (8*8);
        int horizNoOfBlocks = width / 8;
        int vertNoOfBlocks = height / 8;
        switch (sampling) {
            case YUV444Sampling:
                // The first MCU is made from blocks: Y0, Cb0, Cr0
                for (int i = 0; i < noOfBlocks; i++) {
                    result.add(YBlocks.get(i));
                    result.add(CbBlocks.get(i));
                    result.add(CrBlocks.get(i));
                }
                break;
            case YUV422HorizSampling:
                /* The first 6 blocks in the encoded stream would be Y1, Y2, Y3, Y4, Cb0, Cb1, Cr0, Cr1
                 * (they form 1 MCU) where :
                 * Y matrix of blocks:    Y0  Y1 ....
                 *                        Y2  Y3 ....
                 *                         .    .
                 *                         .    .
                 *
                 * Cb matrix of blocks:   Cb0  .  ...
                 *                        Cb1  .  ...
                 *
                 * Cr matrix of blocks:   Cr0  .  ...
                 *                        Cr1  .  ...
                 *
                 */
                // TODO: The following code is dirty and it does not work properly! The corresponding
                // code from the JPEG Decoded is much better structured, but I was in a hurry to
                // wrap this thing up.
                int k = 0;
                for (int i = 0; i < vertNoOfBlocks; i += 2) {
                    for (int j = 0; j < horizNoOfBlocks; j += 2) {
                        result.add(YBlocks.get(i * horizNoOfBlocks + j));
                        result.add(YBlocks.get(i * horizNoOfBlocks + j + 1));
                        result.add(YBlocks.get((i + 1) * horizNoOfBlocks + j));
                        result.add(YBlocks.get((i + 1) * horizNoOfBlocks + j + 1));

                        result.add(CbBlocks.get(k));
                        result.add(CbBlocks.get(k + horizNoOfBlocks));
                        result.add(CrBlocks.get(k));
                        result.add(CrBlocks.get(k + horizNoOfBlocks));
                        k++;
                        if (k % horizNoOfBlocks ==0) {
                            k += horizNoOfBlocks;
                        }
                    }
                }
                break;
            case YUV422VertSampling:
                /* The first 6 blocks in the encoded stream would be Y1, Y2, Y3, Y4, Cb0, Cb1, Cr0, Cr1
                 * (they form 1 MCU) where :
                 * Y matrix of blocks:    Y0  Y1 ....
                 *                        Y2  Y3 ....
                 *                         .    .
                 *                         .    .
                 *
                 * Cb matrix of blocks:   Cb0  Cb1 ...
                 *                          .    .
                 *
                 * Cr matrix of blocks:   Cr0  Cr1 ...
                 *                          .    .
                 *
                 */
                // The following code is dirty. The corresponding code from the JPEG Decoded is much
                // better structured, but I was in a hurry to wrap this thing up.
                k = 0;
                for (int i = 0; i < vertNoOfBlocks; i += 2) {
                    for (int j = 0; j < horizNoOfBlocks; j += 2) {
                        result.add(YBlocks.get(i * horizNoOfBlocks + j));
                        result.add(YBlocks.get(i * horizNoOfBlocks + j + 1));
                        result.add(YBlocks.get((i + 1) * horizNoOfBlocks + j));
                        result.add(YBlocks.get((i + 1) * horizNoOfBlocks + j + 1));

                        result.add(CbBlocks.get(k));
                        result.add(CbBlocks.get(k+1));
                        result.add(CrBlocks.get(k));
                        result.add(CrBlocks.get(k+1));
                        k+=2;
                    }
                }
                break;
            case YUV411Sampling:
                System.out.println("height=" + height + " width=" + width);
                System.out.println("YBlocks.size= " + YBlocks.size());
                System.out.println("CbBlocks.size= " + CbBlocks.size());
                System.out.println("CrBlocks.size= " + CrBlocks.size());
                /* The first 6 blocks in the encoded stream would be Y1, Y2, Y3, Y4, Cb0, Cr0
                 * (they form 1 MCU) where :
                 * Y matrix of blocks:    Y0  Y1 ....
                 *                        Y2  Y3 ....
                 *                         .    .
                 *                         .    .
                 *
                 * Cb matrix of blocks:   Cb0  Cb1 ...
                 *                          .    .
                 *
                 * Cr matrix of blocks:   Cr0  Cr1 ...
                 *                          .    .
                 *
                 */
                // The following code is dirty. The corresponding code from the JPEG Decoded is much
                // better structured, but I was in a hurry to wrap this thing up.
                k = 0;
                for (int i = 0; i < vertNoOfBlocks; i += 2) {
                    for (int j = 0; j < horizNoOfBlocks; j += 2) {
                        result.add(YBlocks.get(i * horizNoOfBlocks + j));
                        result.add(YBlocks.get(i * horizNoOfBlocks + j + 1));
                        result.add(YBlocks.get((i + 1) * horizNoOfBlocks + j));
                        result.add(YBlocks.get((i + 1) * horizNoOfBlocks + j + 1));

                        result.add(CbBlocks.get(k));
                        result.add(CrBlocks.get(k));
                        k++;
                    }
                }
        }

        return result;
    }


    /* Split one color component matrix (Y, Cb or Cr) into 8x8 blocks */
    private ArrayList<Block> SplitColorBlocks(char type) {
        ArrayList<Block> result = new ArrayList<>();
        if ((type!='Y') && (type!='U') && (type!='V'))
            return result;
        int[][] colors;
        int localHeight = height;
        int localWidth = width;
        if (type=='Y') colors = this.Y;
        else if (type=='U') colors = this.Cb;
        else colors = this.Cr;

        if ((type!='Y') && (sampling==YUV422VertSampling)) {
            localHeight /= 2;
        } else if ((type!='Y') && (sampling==YUV422HorizSampling)) {
            localWidth /= 2;
        } else if ((type!='Y') && (sampling==YUV411Sampling)) {
            localHeight /= 2;
            localWidth /= 2;
        }

//        System.out.println("blocktype " + type + " localHeight=" + localHeight + " localWidth= " + localWidth);
        for(int i=0; i<localHeight; i+=Block.BLOCKSIZE) {
            for (int j = 0; j < localWidth; j += Block.BLOCKSIZE) {
                int[][] data = new int[Block.BLOCKSIZE][Block.BLOCKSIZE];
                for (int x = 0; x < Block.BLOCKSIZE; x++) {
                    for (int y = 0; y < Block.BLOCKSIZE; y++) {
                        data[x][y] = colors[i + x][j + y];
                    }
                }
                Block block = new Block(data, type, j, i);
                result.add(block);
            }
        }
        return result;
    }

    public static void computeSamplingFactors(int sampling, int[] horizSamplingFactors, int[] vertSamplingFactors) {
        // `sampling` can be: YUV444Sampling, YUV422HorizSampling, YUV422VertSampling, YUV411Sampling
        // `horizSamplingFactors` and `vertSamplingFactors` have each 3 elements, for Y, Cb, Cr
        /* For a specific component (Y, Cb or Cr), the horizontal resolution of the component is
         * Xc = X * (Hc/Hmax)  where X is the horizontal resolution of the original image,
         * Hc is the horizontal sampling factor and Hmax is the maximum horizontal sampling factor
         * out of all components (Y, Cb, Cr); similarly for the vertical sampling factor.
         */
        if ((sampling != YUV444Sampling) && (sampling != YUV422HorizSampling) &&
                (sampling != YUV422VertSampling) && (sampling != YUV411Sampling))
            return;

        horizSamplingFactors[0] = horizSamplingFactors[1] = horizSamplingFactors[2] = 1;
        vertSamplingFactors[0] = vertSamplingFactors[1] = vertSamplingFactors[2] = 1;
        if (sampling != YCbCrImage.YUV444Sampling) {
            horizSamplingFactors[0] = 2;
            vertSamplingFactors[0] = 2;
            if (sampling == YCbCrImage.YUV422HorizSampling) {
                vertSamplingFactors[1] = 2;
                vertSamplingFactors[2] = 2;
            }
            if (sampling == YCbCrImage.YUV422VertSampling) {
                horizSamplingFactors[1] = 2;
                horizSamplingFactors[2] = 2;
            }
        }
    }

    public static int computeSamplingFromFactors(int[] horizSamplingFactors, int[] vertSamplingFactors) {
        // `horizSamplingFactors` and `vertSamplingFactors` have each 3 elements, for Y, Cb, Cr
        // returns `sampling` which can be: YUV444Sampling, YUV422HorizSampling, YUV422VertSampling, YUV411Sampling
        /* For a specific component (Y, Cb or Cr), the horizontal resolution of the component is
         * Xc = X * (Hc/Hmax)  where X is the horizontal resolution of the original image,
         * Hc is the horizontal sampling factor and Hmax is the maximum horizontal sampling factor
         * out of all components (Y, Cb, Cr); similarly for the vertical sampling factor.
         */
        int maxHorizontalSampling = 1;
        int maxVerticalSampling = 1;
        for (int i=0; i<3; i++) {
            if (horizSamplingFactors[i]>maxHorizontalSampling)
                maxHorizontalSampling = horizSamplingFactors[i];
            if (vertSamplingFactors[i]>maxVerticalSampling)
                maxVerticalSampling = vertSamplingFactors[i];
        }

        float[] horiz_sampling = {1,1,1};
        float[] vert_sampling = {1,1,1};

        for (int i=0; i<3; i++) {
            horiz_sampling[i] = ((float) horizSamplingFactors[i]) / maxHorizontalSampling;
            vert_sampling[i] = ((float) vertSamplingFactors[i]) / maxVerticalSampling;
        }

        if ((horiz_sampling[0]==1) && (horiz_sampling[1]==0.5) && (horiz_sampling[2]==0.5) &&
                (vert_sampling[0]==1) && (vert_sampling[1]==0.5) && (vert_sampling[2]==0.5)) {
            return YUV411Sampling;
        }

        if ((horiz_sampling[0]==1) && (horiz_sampling[1]==0.5) && (horiz_sampling[2]==0.5) &&
                (vert_sampling[0]==1) && (vert_sampling[1]==1) && (vert_sampling[2]==1)) {
            return YUV422HorizSampling;
        }

        if ((horiz_sampling[0]==1) && (horiz_sampling[1]==1) && (horiz_sampling[2]==1) &&
                (vert_sampling[0]==1) && (vert_sampling[1]==0.5) && (vert_sampling[2]==0.5)) {
            return YUV422VertSampling;
        }

        return YUV444Sampling;
    }

    public static int[] adjustResolutionBasedonSampling(int width, int height, int sampling) {
        /* This function adjusts Width and Height based on sampling (YUV444Sampling,YUV422Sampling
         * or YUV11Sampling). In other words it increases width and height so that an appropriate
         * number of Minimum Coded Units (MCU) are contained by the image. This function is used
         * by the JPEG decoder.
         */
        int newWidth = width;
        int newHeight = height;

        if (sampling==YUV444Sampling) {
            // newWidth and newHeight must be multiple of 8
            newWidth = (width%8 == 0) ? width : width + (8 - width%8);
            newHeight = (height%8 == 0) ? height : height + (8 - height%8);
        } else if ((sampling==YUV422HorizSampling) || (sampling==YUV422VertSampling)) {
            // If the scaling is horizontal, newWidth must be multiple of 16 and newHeight must be multiple of 8.
            // If the scaling is vertical, newWidth must be multiple of 8 and newHeight must be multiple of 16.
            // Normally, here we should take into account the scaling factors from the JPEG file header,
            // but for simplicity reasons, we just make both newWidth and newHeight multiples of 16.
            newWidth = (width%16 == 0) ? width : width + (16 - width%16);
            newHeight = (height%16 == 0) ? height : height + (16 - height%16);
        } else if (sampling==YUV411Sampling) {
            // newWidth and newHeight must be multiple of 16
            newWidth = (width%16 == 0) ? width : width + (16 - width%16);
            newHeight = (height%16 == 0) ? height : height + (16 - height%16);
        }

        return new int[] {newWidth, newHeight};
    }

    private int clamp(int val) {
        if (val < 0) return 0;
        return Math.min(val, 255);
    }

    public void writePNGFile(String outfile) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        int pixel;
//        System.out.println(outfile+" Y: ---------------------");
        for (int i=0; i<height; i++) {
            for (int j = 0; j < width; j++) {
                if (outfile.equals("DebugDCTcoefficients")) {
                    // boost the color so that we can see something
                    Y[i][j] = (int) ((Y[i][j] + 128) *1.25);
                } else  if (outfile.equals("DebugQcoefficients")) {
                    // boost the color so that we can see something
                    Y[i][j] = (int) ((Y[i][j] + 128) *1.4);
                }
                pixel = clamp(Y[i][j]);
//                System.out.print(" " + Y[i][j]);
                Color color = new Color(pixel, pixel, pixel);
                image.setRGB(j, i, color.getRGB());
            }
//            System.out.println();
        }
//        System.out.println(outfile + " Cb: ---------------------");
        ImageIO.write(image, "png", new File(outfile));
        for (int i=0; i<height; i++) {
            for (int j = 0; j < width; j++) {
                if (outfile.equals("DebugDCTcoefficients")) {
                    // boost the color so that we can see something
                    Y[i][j] = (int) ((Y[i][j] + 128) *1.25);
                } else  if (outfile.equals("DebugQcoefficients")) {
                    // boost the color so that we can see something
                    Y[i][j] = (int) ((Y[i][j] + 128) *1.4);
                }
                pixel = clamp(Cb[i][j]);
//                System.out.print(" " + Cb[i][j]);
                Color color = new Color(pixel, pixel, pixel);
                image.setRGB(j, i, color.getRGB());
            }
//            System.out.println();
        }
//        System.out.println(outfile + " Cr: ---------------------");
        ImageIO.write(image, "png", new File(outfile+"-Cb.png"));
        for (int i=0; i<height; i++) {
            for (int j = 0; j < width; j++) {
                if (outfile.equals("DebugDCTcoefficients")) {
                    // boost the color so that we can see something
                    Y[i][j] = (int) ((Y[i][j] + 128) *1.25);
                } else  if (outfile.equals("DebugQcoefficients")) {
                    // boost the color so that we can see something
                    Y[i][j] = (int) ((Y[i][j] + 128) *1.4);
                }
                pixel = clamp(Cr[i][j]);
//                System.out.print(" " + Cr[i][j]);
                Color color = new Color(pixel, pixel, pixel);
                image.setRGB(j, i, color.getRGB());
            }
//            System.out.println();
        }
        ImageIO.write(image, "png", new File(outfile+"-Cr.png"));
    }

    public int getWidth() {
        return width;
    }
    public void setWidth(int width) {
        this.width = width;
    }
    public int getHeight() {
        return height;
    }
    public void setHeight(int height) {
        this.height = height;
    }
    public int getSampling() {
        return sampling;
    }
    public void setSampling(int sampling) {
        if ((sampling!=YUV444Sampling) && (sampling!=YUV422HorizSampling) &&
                (sampling!=YUV422VertSampling) && (sampling!=YUV411Sampling))
            return;
        this.sampling = sampling;
    }

}

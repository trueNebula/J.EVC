package jevc;

import jevc.entities.YCbCrImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class JPEGEncoder {

    public static void main(String[] args) throws IOException {
        if (args.length<1) {
            System.out.println("Run the program: java pdav.JPEGEncoder imageFile!");
        }
        BufferedImage img = null;
        String outputfile = args[0].substring(0, args[0].indexOf(".")) + ".jpg";

        try {
            File f = new File(args[0]);
            img = ImageIO.read(f);
        } catch(Exception ex) {
            System.out.println("Error opening image file!");
            System.exit(-1);
        }

        int[][] pixels = new int[img.getHeight()][img.getWidth()];

        for( int i = 0; i < img.getHeight(); i++ )
            for( int j = 0; j < img.getWidth(); j++ ) {
                pixels[i][j] = img.getRGB(j, i);
            }

        YCbCrImage image = new YCbCrImage(pixels, img.getWidth(), img.getHeight());
        Encoder encoder = new Encoder(image, outputfile);
        encoder.compress();

    }
}

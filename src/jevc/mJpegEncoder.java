package jevc;

import jevc.entities.YCbCrImage;
import jevc.service.mJpegEncoderService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class mJpegEncoder {
    public static void main(String[] args) throws IOException {
        if (args.length<1) {
            System.out.println("Run the program: java pdav.JPEGEncoder imageFile!");
        }

        String fileName = "output";
        String outputfile = args[0] + fileName + ".mjpg";
        String outputFolder = args[0] + "/compressed/";
        File[] files = null;

        try {
            files = new File(args[0]).listFiles(
                    file -> file.isFile() && file.getName().toLowerCase().endsWith(".png")
            );

        } catch(Exception ex) {
            System.out.println("Error opening image file! " + ex.getMessage());
            System.exit(-1);
        }

        if (!Objects.isNull(files)) {
            mJpegEncoderService encoder = new mJpegEncoderService(files, outputfile, outputFolder);
            encoder.compress();
        }
    }
}

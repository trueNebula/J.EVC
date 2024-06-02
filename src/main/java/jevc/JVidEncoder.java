package jevc;

import jevc.entities.Globals;
import jevc.service.JVidEncoderService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class JVidEncoder {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Run the program: java jevc.JVidEncoder folder fps");
        }

        String fileName = "output4";
        String outputFolder = args[0] + "/compressed/";
        String outputfile = outputFolder + fileName + ".jvd";
        File[] files = null;
        Globals.FRAMERATE = Integer.parseInt(args[1]);

        try {
            files = new File(args[0]).listFiles(
                    file -> file.isFile() && file.getName().toLowerCase().endsWith(".png")
            );

        } catch(Exception ex) {
            System.out.println("Error opening image file! " + ex.getMessage());
            System.exit(-1);
        }

        if (!Objects.isNull(files)) {
            System.out.println("Files: " + Arrays.toString(files));
            BufferedImage resolution = ImageIO.read(files[0]);
            Globals.MAX_HEIGHT = resolution.getHeight();
            Globals.MAX_WIDTH = resolution.getWidth();
            JVidEncoderService encoder = new JVidEncoderService(files, outputfile, outputFolder);
            encoder.compress();
        }
    }
}

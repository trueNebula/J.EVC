package jevc;

import jevc.entities.Globals;
import jevc.service.JVidDecoderService;

import java.io.File;

public class JVidDecoder {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Run the program: java jevc.JVidDecoder input fps");
        }

        Globals.FRAMERATE = Integer.parseInt(args[1]);
        File file = null;

        try {
            file = new File(args[0]);
        } catch (Exception ex) {
            System.out.println("Error opening file! " + ex.getMessage());
            System.exit(-1);
        }

        JVidDecoderService decoder = new JVidDecoderService(file);
        decoder.decompress();
    }
}

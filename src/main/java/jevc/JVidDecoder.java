package jevc;

import jevc.entities.Globals;
import jevc.service.JVidDecoderService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class JVidDecoder {
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            System.out.println("Run the program: java jevc.JVidDecoder input");
        }

        String outputFolder = getLastFolderPath(args[0]) + File.separator;
        System.out.println(outputFolder);

        File file = null;
        try {
            file = new File(args[0]);
        } catch (Exception ex) {
            System.out.println("Error opening file! " + ex.getMessage());
            System.exit(-1);
        }

        JVidDecoderService decoder = new JVidDecoderService(file, outputFolder);
        decoder.decompress();
    }

    private static String getLastFolderPath(String filePath) {
        String separator = File.separator;
        String[] parts = filePath.split("[/\\\\]");
        if (parts.length < 2) {
            // Handle cases where there is no folder in the path
            return filePath;
        }
        parts = Arrays.copyOf(parts, parts.length - 1);
        System.out.println(Arrays.toString(parts));
        return String.join(separator, parts);
    }
}

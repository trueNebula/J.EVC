package jevc;

import jevc.entities.Globals;
import jevc.service.JVidEncoderService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class JVidEncoder {
    public static void main(String[] args) throws IOException {
        // Flags
        String input = "";
        String output = "";
        int fps  = 30;
        String parralelization = "";
        boolean compressToMjpeg = false;
        boolean enableBenchmarking = false;
        String export = "";
        boolean isDebug = false;
        boolean isQuiet = false;

        // Set Flags
        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                switch (args[i].charAt(1)) {
                    case 'i' -> {
                        checkArgument(args[i], args[i+1]);
                        input = args[i+1];
                        i++;
                    }
                    case 'o' -> {
                        checkArgument(args[i], args[i+1]);
                        output = args[i+1];
                        i++;
                    }
                    case 'f' -> {
                        checkArgument(args[i], args[i+1]);
                        fps = Integer.parseInt(args[i+1]);
                        i++;
                    }
                    case 'p' -> {
                        checkArgument(args[i], args[i+1]);
                        parralelization = args[i+1];
                        i++;
                    }
                    case 'e' -> {
                        checkArgument(args[i], args[i+1]);
                        export = args[i+1];
                        i++;
                    }
                    case 'm' -> compressToMjpeg = true;
                    case 'b' -> enableBenchmarking = true;
                    case 'q' -> isQuiet = true;
                    case 'd' -> isDebug = true;
                    case 'h' -> printHelp();
                }
            }
        }

        if (input.isEmpty()) {
            System.out.println("Missing argument -i");
            System.exit(-1);
        }

        if (output.isEmpty()) {
            System.out.println("Missing argument -o");
            System.exit(-1);
        }

        File[] files = null;
        Globals.FRAMERATE = fps;

        try {
            files = new File(input).listFiles(
                    file -> file.isFile() && file.getName().toLowerCase().endsWith(".png")
            );

        } catch(Exception ex) {
            System.out.println("Error opening image file! " + ex.getMessage());
            System.exit(-1);
        }

        if (!Objects.isNull(files)) {
            BufferedImage resolution = ImageIO.read(files[0]);
            Globals.MAX_HEIGHT = resolution.getHeight();
            Globals.MAX_WIDTH = resolution.getWidth();

            JVidEncoderService encoder = new JVidEncoderService(
                    files,
                    output,
                    parralelization,
                    compressToMjpeg,
                    enableBenchmarking,
                    export,
                    isDebug,
                    isQuiet
            );
            encoder.compress();
        }
    }

    private static void printHelp() {
        System.out.println("JEVC - JPEG Based Video Codec");
        System.out.println("Flags:");
        System.out.println("  -i input: Input folder with PNG files");
        System.out.println("  -o output: Output folder and file name");
        System.out.println("  -f fps: Framerate");
        System.out.println("  -p types: Use parallelization");
        System.out.println("  Parallelization options:");
        System.out.println("    f: Per-Frame");
        System.out.println("    g: Per-GOP");
        System.out.println("    o: Frame Operation");
        System.out.println("    c: Combination");
        System.out.println("  -m: Compress to MJPEG");
        System.out.println("  -b: Enable benchmarking");
        System.out.println("  -e export: Export benchmark");
        System.out.println("  -d: Create debug frames");
        System.out.println("  -q: Quiet mode");
        System.out.println("  -h: Help");
        System.out.println("Example: jevc -i inputFolder -o outputFolder/output.jvd -f 30 -p c -b -e ./benchmark.txt -q");
        System.exit(0);
    }

    private static void checkArgument(String arg, String nextArg) {
        if (nextArg.charAt(0) == '-') {
            System.out.println("Missing argument " + arg);
            System.exit(-1);
        }
    }
}

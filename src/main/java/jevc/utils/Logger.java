package jevc.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class Logger {
    private final StringBuilder progressBar;
    private final boolean isQuiet;
    private final boolean isPrinting;
    private final String exportLocation;
    private FileOutputStream stream;

    public Logger(boolean isQuiet, boolean isPrinting, String exportLocation) {
        this.progressBar = new StringBuilder();
        this.isQuiet = isQuiet;
        this.isPrinting = isPrinting;
        this.exportLocation = exportLocation;

        File file = new File(exportLocation);
        try {
            stream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            this.error("File not found: " + exportLocation);
        }
    }

    public void log(String message) {
        if (this.isQuiet) {
            return;
        }

        System.out.println(message);
    }

    public void benchmark(LapStopwatch stopwatch, String frameName) throws IOException {
        // Benchmarking disabled
        if (!isPrinting && Objects.equals(exportLocation, "")) {
            return;
        }

        // Stopwatch laps:
        // lap 0: scaling, subsampling, block splitting
        // lap 1: block processing
        // lap 2: frame writing
        List<Long> laps = stopwatch.getLaps();
        List<String> times = List.of(
                laps.get(0) + "ms" + " ",
                (laps.get(1) - laps.get(0)) + "ms" + " ",
                (laps.get(2) - laps.get(1)) + "ms" + " ",
                laps.get(2) + "ms"
        );
        String table =
                "Pre Split ---------- Block Processing ---------- Frame Writing ---------- Total\n" +
                String.format("%-10s %20s %25s %20s\n", times.get(0), times.get(1), times.get(2), times.get(3));

        if (isPrinting) {
            if (isQuiet) {
                System.out.println("Frame: " + frameName);
            }
            System.out.print(table);
        }

        if (!Objects.equals(exportLocation, "")) {
            // Export to file
            stream.write((
                    frameName + " " +
                    times.get(0) +
                    times.get(1) +
                    times.get(2) +
                    times.get(3) + "\n"
            ).getBytes());
        }
    }

    public void error(String message) {
        System.err.println(message);
    }

    public void updateProgressStatus(int progress, String status, String frame) {
        if (this.isQuiet) {
            return;
        }

        int progressLength = 20;
        int completedLength = (int) ((double) progress / 100 * progressLength);

        this.progressBar.setLength(0);
        this.progressBar.append(frame).append(" [");
        for (int i = 0; i < progressLength; i++) {
            if (i < completedLength) {
                this.progressBar.append("-");
            } else {
                this.progressBar.append(" ");
            }
        }

        this.progressBar.append("] ").append(progress).append("% ").append(status);
        System.out.print("\r" + this.progressBar);

        if (progress == 100) {
            System.out.print("\r" + frame + " " + status);
            System.out.println();
        }
    }
}

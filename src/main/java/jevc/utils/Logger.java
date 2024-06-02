package jevc.utils;

public class Logger {
    private final StringBuilder progressBar;
    private boolean isQuiet = false;

    public Logger(boolean isQuiet) {
        this.progressBar = new StringBuilder();
        this.isQuiet = isQuiet;
    }

    public void log(String message) {
        if (this.isQuiet) {
            return;
        }

        System.out.println(message);
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
            System.out.println();
        }

    }
}

package jevc.utils;

public class ProgressStatus {
    private final StringBuilder progressBar;

    public ProgressStatus() {
        this.progressBar = new StringBuilder();
    }

    public void updateProgressStatus(int progress, String status, String frame) {
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

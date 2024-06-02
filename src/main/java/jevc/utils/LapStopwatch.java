package jevc.utils;


import com.google.common.base.Stopwatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LapStopwatch {
    private final Stopwatch stopwatch;
    private final List<Long> laps;

    public LapStopwatch() {
        this.stopwatch = Stopwatch.createUnstarted();
        this.laps = new ArrayList<>();
    }

    public void start() {
        this.stopwatch.start();
    }

    public void stop() {
        this.stopwatch.stop();
    }

    public void reset() {
        this.stopwatch.reset();
        this.laps.clear();
    }


    public void lap(TimeUnit desiredUnit) {
        this.laps.add(this.stopwatch.elapsed(desiredUnit));
    }

    public List<Long> getLaps() {
        return this.laps;
    }

}

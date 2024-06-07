package jevc.utils;


import com.google.common.base.Stopwatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class LapStopwatch {
    private final TimeUnit timeUnit;
    private final Stopwatch stopwatch;
    private final List<Long> laps;
    private final ConcurrentHashMap<Integer, Stopwatch> stopwatchMap;
    private final ConcurrentHashMap<Integer, List<Long>> lapsMap;

    public LapStopwatch(TimeUnit desiredUnit) {
        this.timeUnit = desiredUnit;
        this.stopwatch = Stopwatch.createUnstarted();
        this.laps = new ArrayList<>();
        this.lapsMap = new ConcurrentHashMap<>();
        this.stopwatchMap = new ConcurrentHashMap<>();
    }

    public void start() {
        this.stopwatch.start();
    }

    public void startFrame(Integer frameIndex) {
        this.stopwatchMap.put(frameIndex, Stopwatch.createStarted());
    }

    public void stop() {
        this.stopwatch.stop();
    }

    public void stopFrame(Integer frameIndex) {
        this.stopwatchMap.get(frameIndex).stop();
    }

    public void reset() {
        this.stopwatch.reset();
        this.laps.clear();
        this.lapsMap.clear();
    }

    public void lapFrame(Integer frameIndex) {
        if (!this.lapsMap.containsKey(frameIndex)) {
            this.lapsMap.put(frameIndex, new ArrayList<>());
        }
        this.lapsMap.get(frameIndex).add(this.stopwatchMap.get(frameIndex).elapsed(this.timeUnit));
    }

    public List<Long> getLaps() {
        return this.laps;
    }

    public List<Long> getLaps(Integer frameIndex) {
        return this.lapsMap.get(frameIndex);
    }

    public Long getTime() {
        return this.stopwatch.elapsed(this.timeUnit);
    }

    public boolean isRunning() {
        return this.stopwatch.isRunning();
    }

}

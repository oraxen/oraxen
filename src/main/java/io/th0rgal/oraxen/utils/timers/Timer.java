package io.th0rgal.oraxen.utils.timers;

import java.text.DecimalFormat;

public class Timer {

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.##");
    private long lastUsage = 0;
    private long delay;

    Timer(long delay) {
        this.delay = delay;
    }

    Timer(int delay) {
        this.delay = delay;
    }

    public void reset() {
        lastUsage = System.currentTimeMillis();
    }

    public boolean isFinished() {
        return System.currentTimeMillis() >= lastUsage + delay;
    }

    public long getRemainingTimeMillis() {
        return  lastUsage + delay - System.currentTimeMillis();
    }

}

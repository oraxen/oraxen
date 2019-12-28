package io.th0rgal.oraxen.utils.timers;

public class Timer {

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

}

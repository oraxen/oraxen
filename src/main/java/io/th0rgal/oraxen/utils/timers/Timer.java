package io.th0rgal.oraxen.utils.timers;

import io.th0rgal.oraxen.config.Message;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class Timer {

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.##");
    private long lastUsage = 0;
    private final long delay;
    private final TimeUnit timeUnit;

    Timer(long delay) {
        this(delay, TimeUnit.MILLISECONDS);
    }

    Timer(int delay) {
        this(delay, TimeUnit.MILLISECONDS);
    }

    Timer(long delay, TimeUnit timeUnit) {
        this.delay = delay;
        this.timeUnit = timeUnit;
    }

    Timer(int delay, TimeUnit timeUnit) {
        this.delay = delay;
        this.timeUnit = timeUnit;
    }


    public void reset() {
        lastUsage = timeUnit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public boolean isFinished() {
        return timeUnit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS) >= lastUsage + delay;
    }

    public long getRemainingTime() {
        return lastUsage + delay - timeUnit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public long getRemainingTime(TimeUnit timeUnit) {
        return timeUnit.convert(lastUsage, this.timeUnit) + timeUnit.convert(delay, this.timeUnit) - timeUnit.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public String getString() {
        return getRemainingTime() + " " + timeUnit.name();
    }

    public String getString(TimeUnit timeUnit) {
        return getRemainingTime(timeUnit) + " " + timeUnit.name();
    }

    public void sendToPlayer(Player player, TimeUnit timeUnit) {
        Message.COOLDOWN.send(player, "time", String.valueOf(getRemainingTime(timeUnit)),
                "unit", timeUnit.name().toLowerCase());
    }

}

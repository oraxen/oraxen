package io.th0rgal.oraxen.utils.timers;

import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.utils.AdventureUtils;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;

public class Timer {

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.##");
    private long lastUsage = 0;
    private final long delay;

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

    public long getRemainingTime() {
        return lastUsage + delay - System.currentTimeMillis();
    }

    public String getString() {
        return "%.2f".formatted(getRemainingTime() / 1000f);
    }

    public void sendToPlayer(Player player) {
        Message.COOLDOWN.send(player, AdventureUtils.tagResolver("time", getString()));
    }

}

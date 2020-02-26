package io.th0rgal.oraxen.utils.message;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import javax.xml.stream.events.Namespace;
import java.util.ArrayList;
import java.util.List;

public class BossbarMessager implements BossBar {

    private final BossbarManager manager;
    private final NamespacedKey key;
    private final BossBar bar;

    private int unused = 0;
    private Player current;

    public BossbarMessager(BossbarManager manager) {
        this.manager = manager;
        this.key = manager.generateNamespace();
        this.bar = Bukkit.createBossBar(key, "", BarColor.WHITE, BarStyle.SOLID);
    }

    public NamespacedKey getKey() {
        return key;
    }

    public BossbarManager getManager() {
        return manager;
    }

    public boolean free() {
        if(current == null)
            return true;
        if(unused == 10) {
            forceFree();
            return true;
        }
        unused++;
        return false;
    }

    public boolean isFree() {
        return current == null;
    }

    public void forceFree() {
        unused = 0;
        resetBar();
        bar.removePlayer(current);
        current = null;
    }

    public boolean apply(Player player) {
        if(player == null || current != null)
            return false;
        current = player;
        return true;
    }

    public void resetBar() {
        bar.setVisible(false);
        bar.setProgress(0);
        bar.setColor(BarColor.WHITE);
        bar.setStyle(BarStyle.SOLID);
        bar.setTitle("");
        for(BarFlag flag : BarFlag.values())
            if(bar.hasFlag(flag))
                bar.removeFlag(flag);
    }

    public void setVisible(boolean visible) {
        bar.setVisible(visible);
    }

    public boolean isVisible() {
        return bar.isVisible();
    }

    @Override
    public void show() {
        setVisible(true);
    }

    @Override
    public void hide() {
        setVisible(false);
    }

    public void setTitle(String title) {
        bar.setTitle(title);
    }

    public String getTitle() {
        return bar.getTitle();
    }

    public void setProgress(double progress) {
        bar.setProgress(progress);
    }

    public double getProgress() {
        return bar.getProgress();
    }

    @Override
    public void addPlayer(Player player) {
        if(player != current)
            bar.addPlayer(player);
    }

    @Override
    public void removePlayer(Player player) {
        if(player != current)
            bar.removePlayer(player);
    }

    @Override
    public void removeAll() {
    }

    @Override
    public List<Player> getPlayers() {
        return bar.getPlayers();
    }

    @Override
    public void setColor(BarColor color) {
        bar.setColor(color);
    }

    @Override
    public BarColor getColor() {
        return bar.getColor();
    }

    @Override
    public void setStyle(BarStyle style) {
        bar.setStyle(style);
    }

    @Override
    public BarStyle getStyle() {
        return bar.getStyle();
    }

    @Override
    public void addFlag(BarFlag flag) {
        bar.addFlag(flag);
    }

    @Override
    public boolean hasFlag(BarFlag flag) {
        return bar.hasFlag(flag);
    }

    @Override
    public void removeFlag(BarFlag flag) {
        bar.removeFlag(flag);
    }

}

package io.th0rgal.oraxen.utils.message;

import io.th0rgal.oraxen.utils.KeyGenerator;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BossbarManager {

    private final ArrayList<BossbarMessager> available = new ArrayList<>();
    private final Plugin plugin;

    public BossbarManager(Plugin plugin) {
        this.plugin = plugin;
    }

    protected NamespacedKey generateNamespace() {
        String randomKey = KeyGenerator.generateKey(5);
        while(doesExist(randomKey))
            randomKey = KeyGenerator.generateKey(5);
        return new NamespacedKey(plugin, randomKey);
    }

    private boolean doesExist(String key) {
        return available.stream().anyMatch(bossbar -> bossbar.getKey().getKey().equals(key));
    }

    public BossbarMessager getFreeMessager() {
        Optional<BossbarMessager> optional = available.stream().filter(BossbarMessager::isFree).findFirst();
        if(optional.isPresent())
            return optional.get();
        BossbarMessager messager = new BossbarMessager(this);
        available.add(messager);
        return messager;
    }

    public void freeUnused() {
        available.forEach(BossbarMessager::free);
    }

    public int size() {
        return available.size();
    }

    public int countFree() {
        return (int) available.stream().filter(BossbarMessager::free).count();
    }

}

package io.th0rgal.oraxen.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.*;

public class EventsManager {

    Plugin plugin;
    PluginManager pluginManager;
    static Set<Listener> customListeners = new HashSet<>();

    public EventsManager(Plugin plugin) {
        this.plugin = plugin;
        pluginManager = Bukkit.getPluginManager();
    }

    private void registerNativeEvents() {
        pluginManager.registerEvents(new PlayerListener(), plugin);
    }

    public void addEvents(Listener... listeners) {
        customListeners.addAll(Arrays.asList(listeners));
    }

    public void registerEvents() {
        registerNativeEvents();
        for (Listener listener : customListeners)
            pluginManager.registerEvents(listener, plugin);

    }

}
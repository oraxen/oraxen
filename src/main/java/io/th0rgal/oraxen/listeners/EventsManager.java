package io.th0rgal.oraxen.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.*;

public class EventsManager {

    private Plugin plugin;
    private PluginManager pluginManager;
    private static Set<Listener> customListeners = new HashSet<>();
    private static Set<Class<?>> customListenersClasses = new HashSet<>();

    public EventsManager(Plugin plugin) {
        this.plugin = plugin;
        pluginManager = Bukkit.getPluginManager();
    }

    private void registerNativeEvents() {
        pluginManager.registerEvents(new PlayerListener(), plugin);
    }

    public void addEvents(Listener... listeners) {
        for (Listener listener : listeners) {
            if (customListenersClasses.contains(listener.getClass()))
                return;

            customListenersClasses.add(listener.getClass());
            customListeners.add(listener);
        }
    }

    public void registerEvents() {
        registerNativeEvents();
        for (Listener listener : customListeners)
            pluginManager.registerEvents(listener, plugin);

    }

}
package io.th0rgal.oraxen.listeners;

import io.th0rgal.oraxen.recipes.RecipesBuilderEvents;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.*;

public class EventsManager {

    private Plugin plugin;
    private PluginManager pluginManager;

    public EventsManager(Plugin plugin) {
        this.plugin = plugin;
        pluginManager = Bukkit.getPluginManager();
    }

    public void registerNativeEvents() {
        pluginManager.registerEvents(new PlayerListener(), plugin);
        pluginManager.registerEvents(new RecipesBuilderEvents(), plugin);
    }
}
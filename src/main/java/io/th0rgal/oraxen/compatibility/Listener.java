package io.th0rgal.oraxen.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

public class Listener implements org.bukkit.event.Listener {

    protected final String pluginName;
    protected Plugin plugin;
    protected boolean isRegistered;

    public Listener(String pluginName) {
        this.pluginName = pluginName;
        this.isRegistered = false;
        this.plugin = null;
        CompatibilitiesManager.getListeners().add(this);
    }

    public void registerEvents(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        this.isRegistered = true;
        this.plugin = Bukkit.getPluginManager().getPlugin(pluginName);
    }

    public void unRegisterEvents() {
        HandlerList.unregisterAll(this);
        this.isRegistered = false;
    }

    public String getPluginName() {
        return pluginName;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}

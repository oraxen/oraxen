package io.th0rgal.oraxen.compatibilities;

import io.th0rgal.oraxen.OraxenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class CompatibilityProvider<T extends Plugin> implements Listener {

    protected String pluginName;
    protected T plugin;
    protected boolean isEnabled;

    public CompatibilityProvider() {
        this.isEnabled = false;
    }

    @SuppressWarnings("unchecked")
    public void enable(String pluginName) {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
        this.isEnabled = true;
        this.pluginName = pluginName;
        try {
            this.plugin = (T) Bukkit.getPluginManager().getPlugin(pluginName);
        } catch (ClassCastException ignored) {
        }
    }

    public void disable() {
        HandlerList.unregisterAll(this);
        this.isEnabled = false;
    }

    public String getPluginName() {
        return pluginName;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public Plugin getPlugin() {
        return plugin;
    }
}

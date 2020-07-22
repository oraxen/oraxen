package io.th0rgal.oraxen.compatibility;


import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibility.bossshoppro.BossShopProListener;
import io.th0rgal.oraxen.compatibility.cratereloaded.CrateReloadedListener;
import io.th0rgal.oraxen.compatibility.mythicmobs.MythicMobsListener;
import io.th0rgal.oraxen.settings.Message;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class CompatibilitiesManager implements org.bukkit.event.Listener {

    private static List<Listener> listeners;

    public CompatibilitiesManager() {
        Bukkit.getPluginManager().registerEvents(this, OraxenPlugin.get());
    }

    public static void enableCompatibilities(Plugin plugin) {
        listeners = new ArrayList<>();
        new MythicMobsListener();
        new CrateReloadedListener();
        new BossShopProListener();
        registerAllListeners(plugin);
    }

    public static void registerAllListeners(Plugin plugin) {
        for (Listener listener : listeners) {
            if (Bukkit.getPluginManager().isPluginEnabled(listener.getPluginName()) && !listener.isRegistered()) {
                Message.PLUGIN_HOOKS.log(listener.getPluginName());
                listener.registerEvents(plugin);
            }
        }
    }

    public static void unRegisterAllListeners() {
        for (Listener listener : listeners) {
            if (listener.isRegistered()) {
                listener.unRegisterEvents();
                Message.PLUGIN_UNHOOKS.log(listener.getPluginName());
            }
        }
    }

    public static List<Listener> getListeners() {
        return listeners;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        for (Listener listener : listeners) {
            if (event.getPlugin().getName().equals(listener.getPluginName()) && !listener.isRegistered()) {
                Message.PLUGIN_HOOKS.log(listener.getPluginName());
                listener.registerEvents(OraxenPlugin.get());
            }
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        for (Listener listener : listeners) {
            if (event.getPlugin().getName().equals(listener.getPluginName()) && listener.isRegistered()) {
                Message.PLUGIN_UNHOOKS.log(listener.getPluginName());
                listener.unRegisterEvents();
            }
        }
    }
}

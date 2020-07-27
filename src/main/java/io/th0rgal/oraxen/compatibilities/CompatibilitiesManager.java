package io.th0rgal.oraxen.compatibilities;

import io.th0rgal.oraxen.compatibilities.provided.bossshoppro.BossShopProCompatibility;
import io.th0rgal.oraxen.compatibilities.provided.cratereloaded.CrateReloadedCompatibility;
import io.th0rgal.oraxen.compatibilities.provided.mythicmobs.MythicMobsCompatibility;
import io.th0rgal.oraxen.settings.Message;
import org.bukkit.Bukkit;

import java.util.concurrent.ConcurrentHashMap;

public class CompatibilitiesManager {

    private static final ConcurrentHashMap<String, Class<? extends CompatibilityProvider<?>>> COMPATIBILITY_PROVIDERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CompatibilityProvider<?>> ACTIVE_COMPATIBILITY_PROVIDERS = new ConcurrentHashMap<>();

    public static void enableNativeCompatibilities() {
        new CompatibilityListener();
        addCompatibility("MythicMobs", MythicMobsCompatibility.class);
        addCompatibility("CrateReloaded", CrateReloadedCompatibility.class);
        addCompatibility("BossShopPro", BossShopProCompatibility.class);
    }

    public static void disableCompatibilities() {
        for (String pluginName : ACTIVE_COMPATIBILITY_PROVIDERS.keySet()) {
            disableCompatibility(pluginName);
        }
    }

    public static boolean enableCompatibility(String pluginName) {
        if (!ACTIVE_COMPATIBILITY_PROVIDERS.containsKey(pluginName) && COMPATIBILITY_PROVIDERS.containsKey(pluginName) && Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
            try {
                CompatibilityProvider<?> compatibilityProvider = COMPATIBILITY_PROVIDERS.get(pluginName).newInstance();
                ACTIVE_COMPATIBILITY_PROVIDERS.put(pluginName, compatibilityProvider);
                compatibilityProvider.enable(pluginName);
                Message.PLUGIN_HOOKS.log(pluginName);
                return true;
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public static boolean disableCompatibility(String pluginName) {
        if (ACTIVE_COMPATIBILITY_PROVIDERS.containsKey(pluginName) && ACTIVE_COMPATIBILITY_PROVIDERS.get(pluginName).isEnabled()) {
            ACTIVE_COMPATIBILITY_PROVIDERS.get(pluginName).disable();
            ACTIVE_COMPATIBILITY_PROVIDERS.remove(pluginName);
            Message.PLUGIN_UNHOOKS.log(pluginName);
            return true;
        }
        return false;
    }

    public static void addCompatibility(String compatibilityPluginName, Class<? extends CompatibilityProvider<?>> clazz) {
        if (compatibilityPluginName != null && clazz != null) {
            COMPATIBILITY_PROVIDERS.put(compatibilityPluginName, clazz);
            enableCompatibility(compatibilityPluginName);
        }
    }

    public static boolean isCompatibilityEnabled(String pluginName){
        return ACTIVE_COMPATIBILITY_PROVIDERS.containsKey(pluginName) && ACTIVE_COMPATIBILITY_PROVIDERS.get(pluginName).isEnabled();
    }

    public static ConcurrentHashMap<String, Class<? extends CompatibilityProvider<?>>> getCompatibilityProviders() {
        return COMPATIBILITY_PROVIDERS;
    }

    public static ConcurrentHashMap<String, CompatibilityProvider<?>> getActiveCompatibilityProviders() {
        return ACTIVE_COMPATIBILITY_PROVIDERS;
    }
}

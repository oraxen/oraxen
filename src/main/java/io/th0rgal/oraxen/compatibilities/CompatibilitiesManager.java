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
        addCompatibility("MythicMobs", MythicMobsCompatibility.class, true);
        addCompatibility("CrateReloaded", CrateReloadedCompatibility.class, true);
        addCompatibility("BossShopPro", BossShopProCompatibility.class, true);
    }

    public static void disableCompatibilities() {
        ACTIVE_COMPATIBILITY_PROVIDERS.forEach((pluginName, compatibilityProvider) -> disableCompatibility(pluginName));
    }

    public static boolean enableCompatibility(String pluginName) {
        try {
            if (!ACTIVE_COMPATIBILITY_PROVIDERS.containsKey(pluginName) && COMPATIBILITY_PROVIDERS.containsKey(pluginName) && Bukkit.getPluginManager().isPluginEnabled(pluginName)) {
                CompatibilityProvider<?> compatibilityProvider = COMPATIBILITY_PROVIDERS.get(pluginName).newInstance();
                compatibilityProvider.enable(pluginName);
                ACTIVE_COMPATIBILITY_PROVIDERS.put(pluginName, compatibilityProvider);
                Message.PLUGIN_HOOKS.log(pluginName);
                return true;
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static boolean disableCompatibility(String pluginName) {
        try {
            if (ACTIVE_COMPATIBILITY_PROVIDERS.get(pluginName).isEnabled())
                ACTIVE_COMPATIBILITY_PROVIDERS.get(pluginName).disable();
            ACTIVE_COMPATIBILITY_PROVIDERS.remove(pluginName);
            Message.PLUGIN_UNHOOKS.log(pluginName);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static boolean addCompatibility(String compatibilityPluginName, Class<? extends CompatibilityProvider<?>> clazz, boolean tryEnable) {
        try {
            if (compatibilityPluginName != null && clazz != null) {
                COMPATIBILITY_PROVIDERS.put(compatibilityPluginName, clazz);
                if(tryEnable)
                if(tryEnable)
                    return enableCompatibility(compatibilityPluginName);
                else
                    return true;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static boolean addCompatibility(String compatibilityPluginName, Class<? extends CompatibilityProvider<?>> clazz) {
        return addCompatibility(compatibilityPluginName, clazz, false);
    }

        public static CompatibilityProvider<?> getActiveCompatibility(String pluginName) {
        return ACTIVE_COMPATIBILITY_PROVIDERS.get(pluginName);
    }

    public static Class<? extends CompatibilityProvider<?>> getCompatibility(String pluginName) {
        return COMPATIBILITY_PROVIDERS.get(pluginName);
    }

    public static boolean isCompatibilityEnabled(String pluginName) {
        return ACTIVE_COMPATIBILITY_PROVIDERS.containsKey(pluginName) && ACTIVE_COMPATIBILITY_PROVIDERS.get(pluginName).isEnabled();
    }

    public static ConcurrentHashMap<String, Class<? extends CompatibilityProvider<?>>> getCompatibilityProviders() {
        return COMPATIBILITY_PROVIDERS;
    }

    public static ConcurrentHashMap<String, CompatibilityProvider<?>> getActiveCompatibilityProviders() {
        return ACTIVE_COMPATIBILITY_PROVIDERS;
    }
}

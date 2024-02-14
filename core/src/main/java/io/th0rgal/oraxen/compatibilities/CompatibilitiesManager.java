package io.th0rgal.oraxen.compatibilities;

import io.th0rgal.oraxen.compatibilities.provided.blocklocker.BlockLockerCompatibility;
import io.th0rgal.oraxen.compatibilities.provided.bossshoppro.BossShopProCompatibility;
import io.th0rgal.oraxen.compatibilities.provided.mythicmobs.MythicMobsCompatibility;
import io.th0rgal.oraxen.compatibilities.provided.placeholderapi.PlaceholderAPICompatibility;
import io.th0rgal.oraxen.compatibilities.provided.worldedit.WrappedWorldEdit;
import io.th0rgal.oraxen.config.Message;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.PluginUtils;
import io.th0rgal.oraxen.utils.logs.Logs;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

public class CompatibilitiesManager {

    private CompatibilitiesManager() {}

    private static final ConcurrentHashMap<String, Class<? extends CompatibilityProvider<?>>> COMPATIBILITY_PROVIDERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, CompatibilityProvider<?>> ACTIVE_COMPATIBILITY_PROVIDERS = new ConcurrentHashMap<>();

    public static void enableNativeCompatibilities() {
        WrappedWorldEdit.init();
        WrappedWorldEdit.registerParser();
        new CompatibilityListener();
        addCompatibility("PlaceholderAPI", PlaceholderAPICompatibility.class, true);
        addCompatibility("BossShopPro", BossShopProCompatibility.class, true);
        addCompatibility("MythicMobs", MythicMobsCompatibility.class, true);
        addCompatibility("BlockLocker", BlockLockerCompatibility.class, true);
    }

    public static void disableCompatibilities() {
        WrappedWorldEdit.unregister();

        ACTIVE_COMPATIBILITY_PROVIDERS.forEach((pluginName, compatibilityProvider) -> disableCompatibility(pluginName));
    }

    public static boolean enableCompatibility(final String pluginName) {
        try {
            if (!ACTIVE_COMPATIBILITY_PROVIDERS.containsKey(pluginName)
                    && COMPATIBILITY_PROVIDERS.containsKey(pluginName)
                    && hasPlugin(pluginName)) {
                final CompatibilityProvider<?> compatibilityProvider = COMPATIBILITY_PROVIDERS.
                        get(pluginName).getConstructor().newInstance();
                compatibilityProvider.enable(pluginName);
                ACTIVE_COMPATIBILITY_PROVIDERS.put(pluginName, compatibilityProvider);
                Message.PLUGIN_HOOKS.log(AdventureUtils.tagResolver("plugin", pluginName));
                return true;
            }
        } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static boolean disableCompatibility(final String pluginName) {
        try {
            if (!ACTIVE_COMPATIBILITY_PROVIDERS.containsKey(pluginName))
                return false;
            if (ACTIVE_COMPATIBILITY_PROVIDERS.get(pluginName).isEnabled())
                ACTIVE_COMPATIBILITY_PROVIDERS.get(pluginName).disable();
            ACTIVE_COMPATIBILITY_PROVIDERS.remove(pluginName);
            Message.PLUGIN_UNHOOKS.log(AdventureUtils.tagResolver("plugin", pluginName));
            return true;
        } catch (final Exception e) {
            Logs.logWarning(e.getMessage());
            return false;
        }
    }

    public static boolean addCompatibility(final String compatibilityPluginName,
                                           final Class<? extends CompatibilityProvider<?>> clazz, final boolean tryEnable) {
        try {
            if (compatibilityPluginName != null && clazz != null) {
                COMPATIBILITY_PROVIDERS.put(compatibilityPluginName, clazz);
                return !tryEnable || enableCompatibility(compatibilityPluginName);
            }
        } catch (final Exception e) {
            Logs.logWarning(e.getMessage());
            return false;
        }
        return false;
    }

    public static boolean addCompatibility(final String compatibilityPluginName,
                                           final Class<? extends CompatibilityProvider<?>> clazz) {
        return addCompatibility(compatibilityPluginName, clazz, false);
    }

    public static CompatibilityProvider<?> getActiveCompatibility(final String pluginName) {
        return ACTIVE_COMPATIBILITY_PROVIDERS.get(pluginName);
    }

    public static Class<? extends CompatibilityProvider<?>> getCompatibility(final String pluginName) {
        return COMPATIBILITY_PROVIDERS.get(pluginName);
    }

    public static boolean isCompatibilityEnabled(final String pluginName) {
        return ACTIVE_COMPATIBILITY_PROVIDERS.containsKey(pluginName)
                && ACTIVE_COMPATIBILITY_PROVIDERS.get(pluginName).isEnabled();
    }

    public static ConcurrentHashMap<String, Class<? extends CompatibilityProvider<?>>> getCompatibilityProviders() {
        return COMPATIBILITY_PROVIDERS;
    }

    public static ConcurrentHashMap<String, CompatibilityProvider<?>> getActiveCompatibilityProviders() {
        return ACTIVE_COMPATIBILITY_PROVIDERS;
    }

    public static boolean hasPlugin(String name) {
        return PluginUtils.isEnabled(name);
    }
}

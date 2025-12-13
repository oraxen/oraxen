package io.th0rgal.oraxen.utils.platform;

import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.Method;
import java.util.Collection;

public final class SpigotBukkitWrapper extends BukkitWrapper {

    @Override
    public boolean isFirstInstall(Key datapackKey) {
        try {
            Object dataPackManager = invokeStaticNoArgs(Bukkit.class, "getDataPackManager");
            if (dataPackManager == null)
                return false;

            Object dataPacksObj = invokeNoArgs(dataPackManager, "getDataPacks");
            if (!(dataPacksObj instanceof Collection<?> dataPacks))
                return false;

            for (Object pack : dataPacks) {
                Object key = invokeNoArgs(pack, "getKey");
                if (key == null)
                    continue;
                if (datapackKey.equals(Key.key(key.toString())))
                    return false;
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean isDatapackEnabled(Key datapackKey, World world) {
        try {
            Object dataPackManager = invokeStaticNoArgs(Bukkit.class, "getDataPackManager");
            if (dataPackManager == null)
                return false;

            // enabled packs
            Object enabledObj = invokeOneArg(dataPackManager, "getEnabledDataPacks", World.class, world);
            if (containsDatapackKey(enabledObj, datapackKey))
                return true;

            // disabled packs
            Object disabledObj = invokeOneArg(dataPackManager, "getDisabledDataPacks", World.class, world);
            return containsDatapackKey(disabledObj, datapackKey);
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public void setDatapackEnabled(String datapackName, boolean enabled) {
        // not available on spigot
    }

    private static boolean containsDatapackKey(Object packsObj, Key datapackKey) {
        if (!(packsObj instanceof Iterable<?> packs))
            return false;

        for (Object pack : packs) {
            Object key = invokeNoArgs(pack, "getKey");
            if (key == null)
                continue;
            if (key.equals(datapackKey) || datapackKey.asString().equals(key.toString()))
                return true;
        }
        return false;
    }

    private static Object invokeStaticNoArgs(Class<?> clazz, String methodName) {
        try {
            Method m = clazz.getMethod(methodName);
            return m.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeNoArgs(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeOneArg(Object target, String methodName, Class<?> argType, Object arg) {
        try {
            Method m = target.getClass().getMethod(methodName, argType);
            return m.invoke(target, arg);
        } catch (Throwable ignored) {
            return null;
        }
    }
}

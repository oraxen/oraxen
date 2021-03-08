package io.th0rgal.oraxen.settings;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

import org.bukkit.configuration.file.YamlConfiguration;

public abstract class ConfigUpdater {

    private static final TreeMap<Long, TreeSet<UpdateInfo>> UPDATES = new TreeMap<>();
    private static int amount = 0;

    public static boolean register(Object object) {
        return object instanceof Class ? registerStatic((Class<?>) object) : registerDeclared(object);
    }

    private static boolean registerDeclared(Object instance) {
        Class<?> reference = instance.getClass();
        int registered = 0;
        for (Method method : reference.getDeclaredMethods()) {
            Update update = method.getDeclaredAnnotation(Update.class);
            if (update == null)
                continue;
            add(new UpdateInfo(instance, update, method));
            registered++;
        }
        return registered != 0;
    }

    private static boolean registerStatic(Class<?> reference) {
        int registered = 0;
        for (Method method : reference.getMethods()) {
            Update update = method.getDeclaredAnnotation(Update.class);
            if (update == null)
                continue;
            add(new UpdateInfo(null, update, method));
            registered++;
        }
        return registered != 0;
    }

    private static void add(UpdateInfo info) {
        synchronized (UPDATES) {
            long key = info.getVersion();
            if (UPDATES.containsKey(key)) {
                UPDATES.get(key).add(info);
            } else {
                TreeSet<UpdateInfo> set = new TreeSet<>();
                set.add(info);
                UPDATES.put(key, set);
                amount++;
            }
        }
    }

    private static Entry<Long, TreeSet<UpdateInfo>> next(long key) {
        synchronized (UPDATES) {
            return Optional.ofNullable(UPDATES.higherEntry(key)).orElse(null);
        }
    }

    public static boolean update(File file, YamlConfiguration config) {
        if (!file.getPath().contains("Oraxen"))
            return false;
        String path = file.getPath().split("Oraxen", 2)[1].substring(1).split("\\.", 2)[0].replace('\\', '/');
        long version = Optional
            .ofNullable(config.get("version"))
            .filter(object -> object instanceof Number)
            .map(object -> ((Number) object).longValue())
            .orElse(0L);
        long oldVersion = version;
        int executed = 0;
        Entry<Long, TreeSet<UpdateInfo>> infos;
        while ((infos = next(version)) != null) {
            for (UpdateInfo info : infos.getValue()) {
                if (!info.getPathAsString().isEmpty() && !info.getPathAsString().equals(path))
                    continue;
                if (!info.isApplyable(version))
                    continue;
                if (info.apply(file, config))
                    version = infos.getKey().longValue();
            }
            if (++executed == amount)
                break;
        }
        config.set("version", version);
        return oldVersion != version;
    }

}

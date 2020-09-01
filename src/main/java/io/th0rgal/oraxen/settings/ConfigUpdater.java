package io.th0rgal.oraxen.settings;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.TreeMap;

import org.bukkit.configuration.file.YamlConfiguration;

import com.syntaxphoenix.syntaxapi.reflection.PackageAccess;

public abstract class ConfigUpdater {

    private static final TreeMap<Long, UpdateInfo> UPDATES = new TreeMap<>();

    static {
        Class<?>[] classes = PackageAccess.of("io.th0rgal.oraxen.settings.update").getClasses();
        for (Class<?> clazz : classes) {
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!Modifier.isStatic(method.getModifiers()))
                    continue;
                Update update = method.getDeclaredAnnotation(Update.class);
                if (update == null)
                    continue;
                UPDATES.put(update.version(), new UpdateInfo(update, method));
            }
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
        Long current;
        while ((current = UPDATES.higherKey(version)) != null) {
            UpdateInfo info = UPDATES.get(current);
            if (!(info.getPathAsString().equals(path) || info.isApplyable(version)))
                continue;
            if (info.apply(file, config))
                version = current.longValue();
        }
        config.set("version", version);
        return oldVersion != version;
    }

}

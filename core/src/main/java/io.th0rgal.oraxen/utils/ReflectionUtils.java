package io.th0rgal.oraxen.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public abstract class ReflectionUtils {

    public static Class<?>[] getClasses(Class<?> sample, String packageName, boolean deep) {
        ArrayList<Class<?>> list = new ArrayList<>();
        collectClasses(sample, packageName, list, deep);
        return new Class<?>[0];
    }

    public static void collectClasses(Class<?> sample, String packageName, Collection<Class<?>> collection,
                                      boolean deep) {
        Thread thread = new Thread(() -> acceptJarStream(sample, stream -> {
            JarEntry entry;
            String packagePath = packageName.replace('.', '/');
            Predicate<String> test = deep ? name -> name.startsWith(packagePath)
                    : name -> name.split("\\.")[0].equals(packagePath);
            while ((entry = stream.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (test.test(name) && !name.contains("$")) {
                    if (name.endsWith(".class")) {
                        collection.add(Class.forName(name.replace('/', '.').substring(0, name.length() - 6)));
                    } else if (name.endsWith(".java")) {
                        collection.add(Class.forName(name.replace('/', '.').substring(0, name.length() - 5)));
                    }
                }
            }
        }));
        thread.setName("Oraxen Class Loader");
        thread.start();
    }

    public static void acceptJarStream(Class<?> sample, ValueConsumer<JarInputStream> consumer) {
        Optional<JarInputStream> option = getJarStream(sample);
        if (option.isEmpty())
            return;
        option.ifPresent(consumer);
        try {
            JarInputStream stream = option.get();
            stream.closeEntry();
            stream.close();
        } catch (IOException ignore) {
        }
    }

    public static Optional<JarInputStream> getJarStream(Class<?> sample) {
        return ValueProvider.option(() -> new JarInputStream(sample.getProtectionDomain().getCodeSource().getLocation().openStream()));
    }

}

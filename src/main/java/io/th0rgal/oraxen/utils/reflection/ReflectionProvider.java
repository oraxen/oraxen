package io.th0rgal.oraxen.utils.reflection;

import com.syntaxphoenix.syntaxapi.reflection.ClassCache;
import com.syntaxphoenix.syntaxapi.reflection.Reflect;
import com.syntaxphoenix.syntaxapi.reflection.ReflectCache;

import io.th0rgal.oraxen.OraxenVersion;
import io.th0rgal.oraxen.utils.version.MinecraftVersion;
import io.th0rgal.oraxen.utils.version.ServerVersion;

import java.util.Optional;
import java.util.function.Consumer;

public class ReflectionProvider {

    public static final String CB_PATH_FORMAT = "org.bukkit.craftbukkit.%s.%s";
    public static final String NMS_PATH_FORMAT = "net.minecraft.server.%s.%s";

    public static final ReflectionProvider ORAXEN = new ReflectionProvider(Reflections::setup);

    protected final ReflectCache cache;

    protected final ServerVersion server;
    protected final MinecraftVersion minecraft;

    protected final String cbPath;
    protected final String nmsPath;

    public ReflectionProvider() {
        this((Consumer<ReflectionProvider>) null);
    }

    public ReflectionProvider(Consumer<ReflectionProvider> setup) {
        this(new ReflectCache(), setup);
    }

    public ReflectionProvider(ReflectCache cache) {
        this(cache, null);
    }

    public ReflectionProvider(ReflectCache cache, Consumer<ReflectionProvider> setup) {
        this.cache = cache;
        this.server = OraxenVersion.server();
        this.minecraft = OraxenVersion.minecraft();
        this.cbPath = String.format(CB_PATH_FORMAT, OraxenVersion.serverAsString(), "%s");
        this.nmsPath = String.format(NMS_PATH_FORMAT, OraxenVersion.serverAsString(), "%s");
        if (setup != null)
            setup.accept(this);
    }

    public ReflectCache getReflection() {
        return cache;
    }

    public String getNmsPath() {
        return nmsPath;
    }

    public String getCbPath() {
        return cbPath;
    }

    public ServerVersion getServerVersion() {
        return server;
    }

    public MinecraftVersion getMinecraftVersion() {
        return minecraft;
    }

    public Reflect createNMSReflect(String name, String path) {
        return cache.create(name, getNMSClass(path));
    }

    public Reflect createCBReflect(String name, String path) {
        return cache.create(name, getCBClass(path));
    }

    public Reflect createReflect(String name, String path) {
        return cache.create(name, getClass(path));
    }

    public Optional<Reflect> getOptionalReflect(String name) {
        return cache.get(name);
    }

    public Reflect getReflect(String name) {
        return cache.get(name).orElse(null);
    }

    public Class<?> getNMSClass(String path) {
        return getClass(String.format(nmsPath, path));
    }

    public Class<?> getCBClass(String path) {
        return getClass(String.format(cbPath, path));
    }

    public Class<?> getClass(String path) {
        return ClassCache.getClass(path);
    }

}

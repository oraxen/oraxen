package io.th0rgal.oraxen.utils.plugin;

import org.bukkit.plugin.Plugin;

import com.syntaxphoenix.syntaxapi.reflection.ReflectCache;

import io.th0rgal.oraxen.utils.reflection.ReflectionProvider;
import io.th0rgal.oraxen.utils.version.MinecraftVersion;

public class PluginPackage {

    private ReflectionProvider provider = new ReflectionProvider(new ReflectCache());

    private MinecraftVersion version;
    private String name;
    private Plugin plugin;

    PluginPackage(Plugin plugin) {
        update(plugin);
    }

    /*
     * 
     */

    final void delete() {
        version = null;
        plugin = null;
        name = null;
        provider.getReflection().clear();
        provider = null;
    }

    final void update(Plugin plugin) {
        this.plugin = plugin;
        this.name = plugin.getName();
        String rawVersion = plugin.getDescription().getVersion();
        this.version = MinecraftVersion.fromString(rawVersion.contains("[") ? rawVersion.split("\\[")[0] : rawVersion);
    }

    /*
     * 
     */

    public ReflectionProvider getProvider() {
        return provider;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public MinecraftVersion getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    /*
     * 
     */

    public boolean isFromPlugin(Plugin plugin) {
        return hasName(plugin.getName());
    }

    public boolean hasName(String name) {
        return this.name.equals(name);
    }

}

package org.playuniverse.snowypine.utils.plugin;

import org.bukkit.plugin.Plugin;

import com.syntaxphoenix.syntaxapi.reflection.ReflectCache;
import com.syntaxphoenix.syntaxapi.version.DefaultVersion;
import com.syntaxphoenix.syntaxapi.version.Version;
import com.syntaxphoenix.syntaxapi.version.VersionAnalyzer;

public class PluginPackage {

	public static final VersionAnalyzer ANALYZER = new DefaultVersion().getAnalyzer();

	private ReflectCache cache = new ReflectCache();

	private Version version;
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
		cache.clear();
		cache = null;
	}

	final void update(Plugin plugin) {
		this.plugin = plugin;
		this.name = plugin.getName();
		String rawVersion = plugin.getDescription().getVersion();
		this.version = ANALYZER.analyze(rawVersion.contains("[") ? rawVersion.split("\\[")[0] : rawVersion);
	}

	/*
	 * 
	 */

	public ReflectCache getCache() {
		return cache;
	}

	public Plugin getPlugin() {
		return plugin;
	}

	public Version getVersion() {
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

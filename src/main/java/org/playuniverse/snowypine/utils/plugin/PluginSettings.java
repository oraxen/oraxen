package org.playuniverse.snowypine.utils.plugin;

import java.util.ArrayList;
import java.util.Optional;

import org.bukkit.plugin.Plugin;

public class PluginSettings {

	private final ArrayList<PluginPackage> packages = new ArrayList<>();

	public void updatePlugin(Plugin plugin, boolean enabled) {
		Optional<PluginPackage> option = searchPackage(plugin);
		if (enabled) {
			if (option.isPresent())
				option.get().update(plugin);
			else
				packages.add(new PluginPackage(plugin));
			return;
		}
		if (!option.isPresent())
			return;
		PluginPackage pack = option.get();
		packages.remove(pack);
		pack.delete();
	}

	public Optional<PluginPackage> searchPackage(Plugin plugin) {
		return packages.stream().filter(pack -> pack.isFromPlugin(plugin)).findFirst();
	}

	public Optional<PluginPackage> searchPackage(String name) {
		return packages.stream().filter(pack -> pack.hasName(name)).findFirst();
	}

	public PluginPackage getPackage(Plugin plugin) {
		if (packages.isEmpty())
			return null;
		Optional<PluginPackage> option = searchPackage(plugin);
		return option.isPresent() ? option.get() : null;
	}

	public PluginPackage getPackage(String name) {
		if (packages.isEmpty())
			return null;
		Optional<PluginPackage> option = searchPackage(name);
		return option.isPresent() ? option.get() : null;
	}

}

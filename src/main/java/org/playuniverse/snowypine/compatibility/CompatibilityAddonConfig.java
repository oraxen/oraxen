package org.playuniverse.snowypine.compatibility;

import java.io.File;

import org.playuniverse.snowypine.config.Config;
import org.playuniverse.snowypine.config.Migration;
import org.playuniverse.snowypine.utils.plugin.PluginPackage;

public abstract class CompatibilityAddonConfig<A extends CompatibilityAddon> extends Config {

	private final A addon;
	private final String name;

	public CompatibilityAddonConfig(A addon, PluginPackage pluginPackage, Class<? extends Migration> clazz, int latestVersion) {
		super(new File("plugins/Snowypine/addons", pluginPackage.getName() + ".yml"), clazz, latestVersion);
		this.addon = addon;
		this.name = pluginPackage.getName();
	}

	public A getAddon() {
		return addon;
	}

	public final String getAddonName() {
		return name;
	}

}

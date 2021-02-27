package org.playuniverse.snowypine.compatibility;

import org.playuniverse.snowypine.Snowypine;
import org.playuniverse.snowypine.utils.plugin.PluginPackage;

public abstract class CompatibilityAddon {

	public abstract void onEnable(PluginPackage pluginPackage, Snowypine snowypine) throws Exception;

	public abstract void onDisable(Snowypine snowypine) throws Exception;

	public CompatibilityAddonConfig<?> getConfig() {
		return null;
	}

}

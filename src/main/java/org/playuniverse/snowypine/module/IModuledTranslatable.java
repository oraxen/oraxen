package org.playuniverse.snowypine.module;

import org.bukkit.plugin.Plugin;
import org.playuniverse.snowypine.Snowypine;
import org.playuniverse.snowypine.language.ITranslatable;

public interface IModuledTranslatable extends ITranslatable {

	@Override
	default Plugin getOwner() {
		return Snowypine.getPlugin();
	}

	@Override
	default String translationId() {
		return getOwner().getName().replace(" ", "_").replace(".", "_").toLowerCase() + ".module."
			+ getModule().getWrapper().getPluginId().replace(" ", "_").replace(".", "_").toLowerCase() + '.' + id();
	}

	Module getModule();

}

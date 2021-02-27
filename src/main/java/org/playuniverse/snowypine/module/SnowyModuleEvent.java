package org.playuniverse.snowypine.module;

import org.pf4j.PluginWrapper;

import com.syntaxphoenix.syntaxapi.event.Event;

public abstract class SnowyModuleEvent extends Event {

	private final SafeModuleManager manager;
	private final PluginWrapper wrapper;

	public SnowyModuleEvent(SafeModuleManager manager, PluginWrapper wrapper) {
		this.manager = manager;
		this.wrapper = wrapper;
	}

	public final SafeModuleManager getPluginManager() {
		return manager;
	}

	public final PluginWrapper getPlugin() {
		return wrapper;
	}

}

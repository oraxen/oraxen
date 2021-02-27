package org.playuniverse.snowypine.module;

import org.pf4j.PluginWrapper;

public class SnowyModuleDisableEvent extends SnowyModuleEvent {

	public SnowyModuleDisableEvent(SafeModuleManager manager, PluginWrapper wrapper) {
		super(manager, wrapper);
	}

}

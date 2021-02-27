package org.playuniverse.snowypine.language;

import java.util.function.Function;

import org.bukkit.plugin.Plugin;
import org.playuniverse.snowypine.Snowypine;

public enum Variable implements IVariable {

	//
	// General Variables
	//
	PREFIX("&8[&cSnowypine&8]&7"),

	// Config types
	CONFIG_TYPE_MODULE("Modules"),
	
	//
	// Command Variables
	//
	
	// Module
	COMMAND_MODULE_LIST_SPLIT("&f, "),

	//
	;

	private final String value;
	private final Function<String, String> function;

	Variable(String value) {
		this(value, null);
	}

	Variable(String value, Function<String, String> function) {
		this.value = value;
		this.function = function;
	}

	/*
	 *
	 */

	@Override
	public String id() {
		return id(name());
	}

	@Override
	public String value() {
		return value;
	}

	@Override
	public Function<String, String> modifier() {
		return function;
	}

	@Override
	public Plugin getOwner() {
		return Snowypine.getPlugin();
	}

}

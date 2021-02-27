package org.playuniverse.snowypine.language;

import org.bukkit.plugin.Plugin;
import org.playuniverse.snowypine.Snowypine;

public enum Message implements IMessage {

	//
	// General Messages
	//
	NO_PERMISSION("$prefix &7You're lacking the permission &4$permission &7to do this!"),
	WORK_IN_PROGRESS("$prefix &7This feature is &cwork in progress&7!"),
	NOT_PLAYER("$prefix &7This can &4only &7be done as a &4player&7!"),

	//
	// Command Messages
	//
	COMMAND_NOT_EXIST("$prefix &7The command '&c$name&7' doesn't exist!"),

	// Help
	COMMAND_HELP_INFO_PAGE("&8[&c$current &7/ &4$total&8]"),
	COMMAND_HELP_INFO_LINE("&8- &7$content"),
	COMMAND_HELP_INFO_CONTENT("&c$label $usage &8- &7$description"),
	COMMAND_HELP_INFO_SHORT("$prefix &4/snowypine $content"),
	COMMAND_HELP_INFO_HEADER("$prefix &7Info => &4$label $page"),
	COMMAND_HELP_INFO_DETAILED("$header", "", "$line1", "$line2", "$line3", "$line4", "$line5", "$line6", "", "$header"),

	// Language
	COMMAND_LANGUAGE_USAGE("$prefix &7Please provide the language you want to have!"),
	COMMAND_LANGUAGE_NON222EXISTENT("$prefix &7The language '&4$language&7' is non existent!"),
	COMMAND_LANGUAGE_CHANGED("$prefix &7Your language was updated to &c$language"),

	// Module
	COMMAND_MODULE_DOESNT_EXIST("$prefix &7The module '&c$name&7' doesn't exist!"),
	COMMAND_MODULE_LIST_START("&fModules(&a$enabled&f/&e$resolved&f/&c$disabled&f/&4$failed&f): "),
	COMMAND_MODULE_LIST_ITEM("$color$name"),
	COMMAND_MODULE_LIST_HOVER("&7PluginId: &c$plugin", "&7Author: &c$author", "&7Version: &c$version", "&7State:&c $color$state",
		"&7Description: &c$description"),
	COMMAND_MODULE_HAS_ALREADY("$prefix &7The module '&c$name&7' has already the state $color$state&7!"),
	COMMAND_MODULE_ENABLE_START("$prefix &7Trying to enable module '&c$name&7'..."),
	COMMAND_MODULE_ENABLE_END("$prefix &7Module was &cenabled &7successfully!"),
	COMMAND_MODULE_DISABLE_START("$prefix &7Trying to disable module '&c$name&7'..."),
	COMMAND_MODULE_DISABLE_END("$prefix &7Module was &cdisabled &7successfully!"),
	COMMAND_MODULE_DELETE_START("$prefix &7Trying to delete module '&c$name&7'..."),
	COMMAND_MODULE_DELETE_END("$prefix &7Module was &cdeleted &7successfully!"),
	COMMAND_MODULE_RELOAD_ONE_START("$prefix &7Trying to reload module '&c$name&7'..."),
	COMMAND_MODULE_RELOAD_ONE_END("$prefix &7Module was &creloaded &7successfully!"),
	COMMAND_MODULE_RELOAD_ALL_START("$prefix &7Trying to reload &call &7modules..."),
	COMMAND_MODULE_RELOAD_ALL_END("$prefix &7Reload was successful for &c$amount &7of &4$total &7modules!"),
	COMMAND_MODULE_UPDATE_START("$prefix &7Trying to update module '&c$name&7'..."),
	COMMAND_MODULE_UPDATE_NON222EXISTENT("$prefix &7The module has no updater!"),
	COMMAND_MODULE_UPDATE_NO222UPDATE("$prefix &7The module is already up2date!"),
	COMMAND_MODULE_UPDATE_END("$prefix &7Module was successfully &cupdated &7to '&c$version&7'!"),
	COMMAND_MODULE_DOWNLOAD_START("$prefix &7Trying to download module from '&c$url&7'..."),
	COMMAND_MODULE_DOWNLOAD_INVALID("$prefix &7The input '&c$input&7' is not an url!"),
	COMMAND_MODULE_DOWNLOAD_NON222EXISTENT("$prefix &7The file at '&c$url&7' is not a jar file or doesn't exist!"),
	COMMAND_MODULE_DOWNLOAD_ALREADY222EXISTENT("$prefix &7A module with the filename '&c$name&7' is already existent!"),
	COMMAND_MODULE_DOWNLOAD_END("$prefix &7Successfully downloaded module file '&c$name&7'!"),
	COMMAND_MODULE_WENT_WRONG("$prefix &7Something went wrong while executing the module command, look in the console for more detail."),

	//
	// Config Messages
	//
	CONFIG_RELOAD_NEEDED("$prefix &7Detected a change in config &c$name&7, reloading..."),
	CONFIG_RELOAD_DONE("$prefix &7Config &c$name &7was &4successfully&7 reloaded!"),

	//
	;

	private final String value;

	Message(String value) {
		this.value = value;
	}

	Message(String... values) {
		StringBuilder builder = new StringBuilder();
		int length = values.length - 1;
		for (int index = 0; index < values.length; index++) {
			builder.append(values[index]);
			if (index != length) {
				builder.append("\n");
			}
		}
		this.value = builder.toString();
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
	public Plugin getOwner() {
		return Snowypine.getPlugin();
	}

}

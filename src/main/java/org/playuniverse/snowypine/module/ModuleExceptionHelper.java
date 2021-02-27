package org.playuniverse.snowypine.module;

import org.pf4j.PluginWrapper;
import org.playuniverse.snowypine.Snowypine;

import com.syntaxphoenix.syntaxapi.logging.ILogger;
import com.syntaxphoenix.syntaxapi.logging.LogTypeId;

public class ModuleExceptionHelper {

	private ModuleExceptionHelper() {}

	public static void log(PluginWrapper wrapper, Throwable throwable) {
		ILogger logger = Snowypine.getCurrentLogger();
		logger.log(LogTypeId.ERROR, "===============================================");
		logger.log(LogTypeId.ERROR, "");
		logger.log(LogTypeId.ERROR, "Module '" + wrapper.getPluginId() + "' by " + wrapper.getDescriptor().getProvider());
		logger.log(LogTypeId.ERROR, "");
		logger.log(LogTypeId.ERROR, "-----------------------------------------------");
		logger.log(LogTypeId.ERROR, throwable);
		logger.log(LogTypeId.ERROR, "===============================================");
	}

	public static void log(PluginWrapper wrapper, Throwable throwable, String message) {
		ILogger logger = Snowypine.getCurrentLogger();
		logger.log(LogTypeId.ERROR, "===============================================");
		logger.log(LogTypeId.ERROR, "");
		logger.log(LogTypeId.ERROR, "Module '" + wrapper.getPluginId() + "' by " + wrapper.getDescriptor().getProvider());
		logger.log(LogTypeId.ERROR, "");
		logger.log(LogTypeId.ERROR, "-----------------------------------------------");
		logger.log(LogTypeId.ERROR, "");
		logger.log(LogTypeId.ERROR, message);
		logger.log(LogTypeId.ERROR, "");
		logger.log(LogTypeId.ERROR, "-----------------------------------------------");
		logger.log(LogTypeId.ERROR, throwable);
		logger.log(LogTypeId.ERROR, "===============================================");
	}

}

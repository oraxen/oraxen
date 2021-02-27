package org.playuniverse.snowypine;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.plugin.java.JavaPlugin;
import org.pf4j.PluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.playuniverse.snowypine.command.CommandProvider;
import org.playuniverse.snowypine.module.BukkitEventManager;
import org.playuniverse.snowypine.module.CommandHandler;
import org.playuniverse.snowypine.module.SafeModuleManager;
import org.playuniverse.snowypine.utils.reflection.ReflectionProvider;

import com.syntaxphoenix.syntaxapi.command.BaseArgument;
import com.syntaxphoenix.syntaxapi.event.EventManager;
import com.syntaxphoenix.syntaxapi.logging.AsyncLogger;
import com.syntaxphoenix.syntaxapi.logging.ILogger;
import com.syntaxphoenix.syntaxapi.logging.LogTypeId;
import com.syntaxphoenix.syntaxapi.logging.LoggerState;
import com.syntaxphoenix.syntaxapi.logging.SynLogger;
import com.syntaxphoenix.syntaxapi.random.Keys;
import com.syntaxphoenix.syntaxapi.service.ServiceManager;
import com.syntaxphoenix.syntaxapi.utils.java.tools.Container;

public abstract class ModuledPlugin extends JavaPlugin {

	public static final Keys KEYS = new Keys(73453345478693428L);

	/*
	 * 
	 */

	protected final Container<ReflectionProvider> provider = Container.of();

	private ServiceManager serviceManager;
	private SafeModuleManager pluginManager;

	private BukkitEventManager bukkitEventManager;
	private EventManager eventManager;

	private CommandProvider commandProvider;
	private CommandHandler commandHandler;

	private boolean init = false;

	private ILogger logger;

	protected final File directory;
	protected final File pluginDirectory;

	protected final Map<String, BaseArgument> arguments;

	/*
	 * 
	 */

	public ModuledPlugin() {
		this(Collections.unmodifiableMap(new HashMap<>()));
	}

	public ModuledPlugin(Map<String, BaseArgument> arguments) {

		//
		// Initializing variables
		//

		this.arguments = arguments;
		this.directory = new File("plugins", getName());
		this.pluginDirectory = new File(directory, "modules");

		//
		// Creating bot directory
		//

		if (!directory.exists()) {
			directory.mkdirs();
			return;
		}
		if (!directory.isDirectory() && !directory.delete()) {
			throw new RuntimeException("Can't delete 'directory' file -> directory has to be a folder");
		} else {
			directory.mkdirs();
		}

		if (!pluginDirectory.exists()) {
			pluginDirectory.mkdirs();
			return;
		}
		if (!pluginDirectory.isDirectory() && !pluginDirectory.delete()) {
			throw new RuntimeException("Can't delete 'directory' file -> directory has to be a folder");
		} else {
			pluginDirectory.mkdirs();
		}

	}

	/*
	 * 
	 */

	public final File getDirectory() {
		return directory;
	}

	public final File getPluginDirectory() {
		return pluginDirectory;
	}

	/*
	 * 
	 */

	public final ILogger getPluginLogger() {
		return logger;
	}

	public final EventManager getEventManager() {
		return eventManager;
	}

	public final BukkitEventManager getBukkitEventManager() {
		return bukkitEventManager;
	}

	public final CommandProvider getCommandProvider() {
		return commandProvider;
	}

	public final CommandHandler getCommandHandler() {
		return commandHandler;
	}

	public final ServiceManager getServiceManager() {
		return serviceManager;
	}

	public final ReflectionProvider getReflectionProvider() {
		return provider.get();
	}

	public final PluginManager getPluginManager() {
		return pluginManager;
	}

	/*
	 * Argument Handling
	 */

	public BaseArgument getArgument(String path) {
		return arguments.get(path);
	}

	public Object getArgumentValue(String path, Object value) {
		BaseArgument argument = arguments.get(path);
		return argument == null ? value : argument.asObject();
	}

	public String getArgumentString(String path, String value) {
		BaseArgument argument = arguments.get(path);
		return argument == null ? value : argument.toString();
	}

	/*
	 * 
	 */

	@Override
	public final void onEnable() {

		if (init) {
			return;
		}
		init = true;

		//
		// Creating logger
		//

		logger = createLogger();
		logger.setColored(false);

		//
		// Creating some services
		//

		provider.replace(ReflectionProvider.of(this));

		eventManager = createEventManager(logger);
		bukkitEventManager = createBukkitEventManager(logger);

		commandProvider = createCommandProvider(this);
		commandHandler = createCommandHandler(commandProvider);

		serviceManager = createServiceManager(logger);

		pluginManager = createPluginManager(pluginDirectory.toPath(), logger, provider, commandHandler, eventManager, bukkitEventManager, serviceManager);

		//
		// Running the startup of the actual bot logic
		//

		logger.log("Starting plugin base...");

		onStartup();

		logger.log("Plugin base started successfully");

		loadPlugins();

		logger.log("Plugin successfully started!");

	}

	/*
	 * 
	 */

	public final void loadPlugins() {
		//
		// Loading addons
		//

		logger.log("Loading functionality modules...");
		try {
			pluginManager.loadPlugins();
		} catch (Throwable throwable) {
			logger.log(throwable);
		}
		int size = pluginManager.getResolvedPlugins().size();
		logger.log("Loaded " + size + " modules to add functionality!");

		//
		// Enabling modules
		//

		logger.log("Enabling functionality modules...");
		pluginManager.startPlugins();
		size = pluginManager.getStartedPlugins().size();
		logger.log("Enabled " + size + " modules to add functionality!");

		PluginWrapper[] wrappers = pluginManager.getPlugins().stream().filter(wrapper -> wrapper.getPluginState() == PluginState.FAILED)
			.toArray(PluginWrapper[]::new);
		if (wrappers.length != 0) {
			logger.log(LogTypeId.ERROR, "Some plugins failed to load...");
			logger.log(LogTypeId.ERROR, "");
			for (int index = 0; index < wrappers.length; index++) {
				PluginWrapper wrapper = wrappers[index];
				logger.log(LogTypeId.ERROR, "===============================================");
				logger.log(LogTypeId.ERROR, "");
				logger.log(LogTypeId.ERROR, "Module '" + wrapper.getPluginId() + "' by " + wrapper.getDescriptor().getProvider());
				logger.log(LogTypeId.ERROR, "");
				logger.log(LogTypeId.ERROR, "-----------------------------------------------");
				logger.log(LogTypeId.ERROR, wrapper.getFailedException());
				logger.log(LogTypeId.ERROR, "===============================================");
				if (index + 1 != wrappers.length) {
					logger.log(LogTypeId.ERROR, "");
					logger.log(LogTypeId.ERROR, "");
				}
			}
			logger.log(LogTypeId.ERROR, "");
			logger.log(LogTypeId.ERROR, "Hope you can fix those soon!");
		}

		logger.log("Running post startup...");
		onPluginLoad();
		logger.log("Post startup executed successfully!");

	}

	/*
	 * 
	 */

	@Override
	public final void onDisable() {

		if (!init) {
			return;
		}
		init = false;

		//
		// Shutdown addons
		//

		pluginManager.stopPlugins();
		pluginManager.unloadPlugins();

		//
		// Shutdown the bot logic
		//

		onShutdown();

		//
		// Shutdown the actual bot
		//

		logger.log("Goodbye!");
		logger.close();

	}

	/*
	 * 
	 * 
	 * 
	 */

	protected ILogger createLogger() {
		return new AsyncLogger(new SynLogger(LoggerState.STREAM).setDefaultTypes()).setColored(true);
	}

	protected EventManager createEventManager(ILogger logger) {
		return new EventManager(logger);
	}

	protected BukkitEventManager createBukkitEventManager(ILogger logger) {
		return new BukkitEventManager(logger);
	}

	protected ServiceManager createServiceManager(ILogger logger) {
		return new ServiceManager(logger);
	}

	protected CommandProvider createCommandProvider(ModuledPlugin plugin) {
		return new CommandProvider(plugin);
	}

	protected CommandHandler createCommandHandler(CommandProvider provider) {
		return new CommandHandler(provider);
	}

	protected SafeModuleManager createPluginManager(Path path, ILogger logger, Container<ReflectionProvider> provider, CommandHandler commandHandler,
		EventManager eventManager, BukkitEventManager bukkitEventManager, ServiceManager serviceManager) {
		return new SafeModuleManager(path, logger, provider, commandHandler, eventManager, bukkitEventManager, serviceManager);
	}

	/*
	 * 
	 */

	protected abstract void onStartup();

	protected abstract void onPluginLoad();

	protected abstract void onShutdown();

}

package org.playuniverse.snowypine.module;

import java.io.File;
import java.util.Optional;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.playuniverse.snowypine.Snowypine;
import org.playuniverse.snowypine.config.Config;

import com.syntaxphoenix.syntaxapi.event.EventListener;
import com.syntaxphoenix.syntaxapi.logging.ILogger;
import com.syntaxphoenix.syntaxapi.utils.java.Files;

public abstract class Module extends Plugin {

	public static Optional<Module> getModuleAsOptional(String name) {
		return Optional.ofNullable(getModule(name));
	}

	public static Module getModule(String name) {
		PluginWrapper wrapper = Snowypine.getPlugin().getPluginManager().getPlugin(name);
		if (wrapper == null) {
			return null;
		}
		Plugin plugin = wrapper.getPlugin();
		if (plugin instanceof Module) {
			return (Module) plugin;
		}
		return null;
	}

	private final File dataLocation;

	public Module(PluginWrapper wrapper) {
		super(wrapper);
		this.dataLocation = new File(getPlugin().getPluginDirectory(), wrapper.getPluginId());
		Files.createFolder(dataLocation);
	}

	public final Snowypine getPlugin() {
		return Snowypine.getPlugin();
	}

	public final ILogger getLogger() {
		return getPlugin().getPluginLogger();
	}

	public final File getDataLocation() {
		return dataLocation;
	}

	@Override
	public final void start() {
		onStart();
		Config.ACCESS.load(wrapper);
	}

	@Override
	public final void stop() {
		Config.ACCESS.unload(wrapper);
		onStop();
	}

	@Override
	public final void delete() {
		onDelete();
	}

	protected void onStart() {}

	protected void onStop() {}

	protected void onDelete() {}

	public final void registerBukkitListener(EventListener listener) {
		getPlugin().getBukkitEventManager().registerEvents(listener);
	}

	public final void registerSyntaxListener(EventListener listener) {
		getPlugin().getEventManager().registerEvents(listener);
	}

	public final boolean registerCommand(ModuleCommand command) {
		return getPlugin().getCommandHandler().register(this, command);
	}

}

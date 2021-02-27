package org.playuniverse.snowypine;

import java.io.File;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.playuniverse.snowypine.bukkit.inventory.GuiListener;
import org.playuniverse.snowypine.command.CommandProvider;
import org.playuniverse.snowypine.command.commands.HelpCommand;
import org.playuniverse.snowypine.command.commands.LanguageCommand;
import org.playuniverse.snowypine.command.commands.ModuleCommand;
import org.playuniverse.snowypine.compatibility.CompatibilityHandler;
import org.playuniverse.snowypine.config.Config;
import org.playuniverse.snowypine.config.ConfigTimer;
import org.playuniverse.snowypine.helper.task.TaskHelper;
import org.playuniverse.snowypine.language.FallbackHandler;
import org.playuniverse.snowypine.language.Translations;
import org.playuniverse.snowypine.listener.*;
import org.playuniverse.snowypine.utils.plugin.PluginSettings;

import com.syntaxphoenix.syntaxapi.logging.ILogger;
import com.syntaxphoenix.syntaxapi.logging.LoggerState;
import com.syntaxphoenix.syntaxapi.logging.SynLogger;
import com.syntaxphoenix.syntaxapi.random.RandomNumberGenerator;
import com.syntaxphoenix.syntaxapi.utils.java.tools.Container;

public final class Snowypine extends ModuledPlugin {

	public static final Container<Snowypine> PLUGIN = Container.of();
	public static final PluginSettings SETTINGS = new PluginSettings();

	public static final Snowypine getPlugin() {
		return PLUGIN.get();
	}

	public static final ILogger getCurrentLogger() {
		return getPlugin().getPluginLogger();
	}

	public Snowypine() {
		PLUGIN.replace(this).lock();
	}

	private final RandomNumberGenerator random = NumberGeneratorType.MURMUR.create(8945349838935L);
	private final Container<DataDistributor<UUID>> dataDistributor = Container.of();
	private final Container<DataDistributor<Integer>> statsDistributor = Container.of();
	private final Container<WrappedContainer> globalData = Container.of();

	private BukkitTask configTask;

	@Override
	protected void onStartup() {

		TaskHelper.TASK.start();

		VersionControl control = VersionControl.get();

		dataDistributor.replace(control.getDataProvider().createDistributor(new File(getDirectory(), "data"))).lock();
		globalData.replace(createPersistentContainer()).lock();

		CompatibilityHandler.registerDefaults();

		registerEvents();
		setupConfigs();
		registerCommands();

	}

	@Override
	protected void onPluginLoad() {

		Translations.MANAGER.reloadCatch();

	}

	@Override
	protected void onShutdown() {

		configTask.cancel();

		dataDistributor.get().shutdown();

		TaskHelper.TASK.shutdown();

	}

	/*
	 * 
	 */

	private void registerEvents() {
		PluginManager pluginManager = Bukkit.getPluginManager();

		pluginManager.registerEvents(new FallbackHandler(), this);
		pluginManager.registerEvents(new PlayerListener(), this);
		pluginManager.registerEvents(new PluginLoadListener(), this);
		pluginManager.registerEvents(GuiListener.LISTENER, this);
	}

	private void setupConfigs() {
		BukkitScheduler scheduler = Bukkit.getScheduler();

		// Setup ConfigTimer -> 5s delay / 3s interval
		configTask = scheduler.runTaskTimerAsynchronously(this, ConfigTimer.TIMER, 0, 60);

		Config.ACCESS.getClass();

	}

	private void registerCommands() {
		CommandProvider provider = getCommandProvider();

		provider.register(HelpCommand.info());
		provider.register(LanguageCommand.info());
		provider.register(ModuleCommand.info());

	}

	/*
	 * 
	 */

	@Override
	protected ILogger createLogger() {
		return new SynLogger(LoggerState.STREAM).setDefaultTypes().setColored(true).setFormat("[%type%] => %message%");
	}

	/*
	 * 
	 */

	public final WrappedContainer getGlobalData() {
		return globalData.get();
	}

	public final DataDistributor<UUID> getDataDistributor() {
		return dataDistributor.get();
	}

	public final DataDistributor<Integer> getStatsDistributor() {
		return statsDistributor.get();
	}

	public final WrappedContainer createPersistentContainer() {
		return new SimpleSyntaxContainer<>(getDataDistributor().get(UUIDHelper.generateUniqueId(random)));
	}

}

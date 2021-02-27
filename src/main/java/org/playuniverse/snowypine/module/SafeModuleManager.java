package org.playuniverse.snowypine.module;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginState;
import org.pf4j.PluginStateEvent;
import org.pf4j.PluginStateListener;
import org.pf4j.PluginWrapper;
import org.playuniverse.snowypine.command.CommandInfo;
import org.playuniverse.snowypine.command.CommandProvider;
import org.playuniverse.snowypine.utils.reflection.JavaTools;
import org.playuniverse.snowypine.utils.reflection.ReflectionProvider;
import org.playuniverse.snowypine.utils.wait.Awaiter;

import com.syntaxphoenix.syntaxapi.command.CommandManager;
import com.syntaxphoenix.syntaxapi.event.Event;
import com.syntaxphoenix.syntaxapi.event.EventExecutor;
import com.syntaxphoenix.syntaxapi.event.EventListener;
import com.syntaxphoenix.syntaxapi.event.EventManager;
import com.syntaxphoenix.syntaxapi.logging.ILogger;
import com.syntaxphoenix.syntaxapi.service.ServiceManager;
import com.syntaxphoenix.syntaxapi.utils.java.Collect;
import com.syntaxphoenix.syntaxapi.utils.java.tools.Container;

public class SafeModuleManager extends DefaultPluginManager implements PluginStateListener {

	private final Container<ReflectionProvider> provider;

	private final ServiceManager service;

	private final CommandHandler command;

	private final BukkitEventManager bukkitEvent;
	private final EventManager event;

	private final ILogger logger;

	public SafeModuleManager(ILogger logger, Container<ReflectionProvider> provider, CommandHandler command, EventManager event, BukkitEventManager bukkitEvent,
		ServiceManager service) {
		super();
		this.provider = provider;
		this.event = event;
		this.logger = logger;
		this.service = service;
		this.command = command;
		this.bukkitEvent = bukkitEvent;
		super.addPluginStateListener(this);
	}

	public SafeModuleManager(Path pluginsRoot, ILogger logger, Container<ReflectionProvider> provider, CommandHandler command, EventManager event,
		BukkitEventManager bukkitEvent, ServiceManager service) {
		super(pluginsRoot);
		this.provider = provider;
		this.event = event;
		this.logger = logger;
		this.service = service;
		this.command = command;
		this.bukkitEvent = bukkitEvent;
		super.addPluginStateListener(this);
	}

	/*
	 * Getter
	 */

	public ReflectionProvider getProvider() {
		return provider.get();
	}

	public ServiceManager getServiceManager() {
		return service;
	}

	public BukkitEventManager getDiscordEventManager() {
		return bukkitEvent;
	}

	public EventManager getEventManager() {
		return event;
	}

	public ILogger getLogger() {
		return logger;
	}

	/*
	 * Plugin Listener
	 */

	@Override
	public synchronized void addPluginStateListener(PluginStateListener listener) {
		return;
	}

	@Override
	public synchronized void removePluginStateListener(PluginStateListener listener) {
		return;
	}

	@Override
	public void pluginStateChanged(PluginStateEvent event) {

		if (event.getPluginState() == PluginState.STARTED) {
			Awaiter.of(this.event.call(new SnowyModuleEnableEvent(this, event.getPlugin()))).await();
			return;
		}
		switch (event.getPluginState()) {
		case STOPPED:
		case DISABLED:
			break;
		default:
			return;
		}

		PluginWrapper wrapper = event.getPlugin();

		Awaiter.of(this.event.call(new SnowyModuleDisableEvent(this, wrapper))).await();

		List<Class<? extends EventListener>> owners = this.event.getOwnerClasses();
		int size = owners.size();
		for (int index = 0; index < size; index++) {
			if (isFromPlugin(wrapper, owners.get(index))) {
				continue;
			}
			owners.remove(index);
			index--;
			size--;
		}

		owners.stream().forEach(clazz -> this.event.unregisterEvents(clazz));
		List<Class<? extends Event>> events = this.event.getEvents().stream().filter(clazz -> isFromPlugin(wrapper, clazz)).collect(Collectors.toList());
		this.event.unregisterExecutors(events.stream().collect(collectExecutor()));
		events.forEach(clazz -> this.event.unregisterEvent(clazz));

		owners = this.bukkitEvent.getOwnerClasses();
		size = owners.size();
		for (int index = 0; index < size; index++) {
			if (isFromPlugin(wrapper, owners.get(index))) {
				continue;
			}
			owners.remove(index);
			index--;
			size--;
		}

		owners.stream().forEach(clazz -> this.bukkitEvent.unregisterEvents(clazz));
		List<Class<? extends org.bukkit.event.Event>> bukkitEvents = this.bukkitEvent.getEvents().stream().filter(clazz -> isFromPlugin(wrapper, clazz))
			.collect(Collectors.toList());
		this.bukkitEvent.unregisterExecutors(bukkitEvents.stream().collect(collectBukkitExecutor()));
		bukkitEvents.forEach(clazz -> this.bukkitEvent.unregisterEvent(clazz));

		service.getContainers().stream().filter(service -> isFromPlugin(wrapper, service.getOwner())).forEach(container -> service.unsubscribe(container));
		service.getServices().stream().filter(service -> isFromPlugin(wrapper, service.getOwner())).forEach(service -> this.service.unregister(service));

		CommandProvider commandProvider = command.getProvider();
		CommandManager manager = command.getManager();
		ModuleBoundCommand[] commands = command.getCommands(wrapper);
		for (int index = 0; index < commands.length; index++) {
			manager.unregister(commands[index]);
			Optional<CommandInfo> info = commandProvider.getOptionalInfo(commands[index]);
			if(info.isPresent()) {
				commandProvider.remove(info.get());
			}
		}

		ClassLoader loader = wrapper.getPluginClassLoader();
		Package[] packages = JavaTools.getPackages(loader);
		ReflectionProvider current = provider.get();
		for (int index = 0; index < packages.length; index++) {
			current.delete(packages[index].getName());
		}

	}

	/*
	 * Utilities
	 */

	private Collector<Class<? extends Event>, List<EventExecutor>, List<EventExecutor>> collectExecutor() {
		return Collect.collectList((output, clazz) -> this.event.getExecutorsForEvent(clazz, true).stream().forEach(executor -> {
			if (output.contains(executor)) {
				return;
			}
			output.add(executor);
		}));
	}

	private Collector<Class<? extends org.bukkit.event.Event>, List<BukkitEventExecutor>, List<BukkitEventExecutor>> collectBukkitExecutor() {
		return Collect.collectList((output, clazz) -> this.bukkitEvent.getExecutorsForEvent(clazz, true).stream().forEach(executor -> {
			if (output.contains(executor)) {
				return;
			}
			output.add(executor);
		}));
	}

	public boolean isFromPlugin(PluginWrapper wrapper, Object object) {
		return wrapper.equals(whichPlugin(!(object instanceof Class) ? object.getClass() : (Class<?>) object));
	}

}

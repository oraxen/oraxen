package org.playuniverse.snowypine.utils.reflection;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.playuniverse.snowypine.ModuledPlugin;
import org.playuniverse.snowypine.Snowypine;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;

import com.syntaxphoenix.syntaxapi.utils.java.Arrays;
import com.syntaxphoenix.syntaxapi.utils.java.tools.Container;

public final class ReflectionProvider {

	private static final HashMap<ModuledPlugin, ReflectionProvider> PROVIDERS = new HashMap<>();

	public static ReflectionProvider of(ModuledPlugin moduledPlugin) {
		if (PROVIDERS.containsKey(moduledPlugin)) {
			return PROVIDERS.get(moduledPlugin);
		}
		ReflectionProvider provider = new ReflectionProvider(moduledPlugin);
		PROVIDERS.put(moduledPlugin, provider);
		return provider;
	}

	private final HashMap<String, Reflections> reflections = new HashMap<>();
	private final ModuledPlugin moduledPlugin;

	private final Container<Reflections> global = Container.of();

	private ReflectionProvider(ModuledPlugin moduledPlugin) {
		this.moduledPlugin = moduledPlugin;
	}

	/*
	 * ClassLoaders
	 */

	private ClassLoader[] defaults;

	public ClassLoader[] classLoaders() {
		if (defaults != null) {
			return defaults;
		}
		return defaults = Arrays.merge(size -> new ClassLoader[size], ClasspathHelper.classLoaders(), getClass().getClassLoader(),
			ClassLoader.getSystemClassLoader(), Runtime.getRuntime().getClass().getClassLoader(), Snowypine.getPlugin().getClass().getClassLoader(),
			Bukkit.getServer().getClass().getClassLoader());
	}

	private Object[] buildParameters(String packageName, ClassLoader... loaders) {
		return Arrays.merge(new Object[] {
				packageName
		}, loaders.length == 0 ? classLoaders() : Arrays.merge(size -> new ClassLoader[size], classLoaders(), loaders));
	}

	/*
	 * Reflections
	 */

	public Reflections global() {
		if (global.isPresent()) {
			return global.get();
		}
		return global.replace(new Reflections((Object[]) classLoaders())).get();
	}

	public Reflections of(String packageName) {
		return of((PluginWrapper) null, packageName);
	}

	public Reflections of(Class<? extends Plugin> clazz, String packageName) {
		return of(clazz == null ? null : moduledPlugin.getPluginManager().whichPlugin(clazz), packageName);
	}

	public Reflections of(PluginWrapper wrapper, String packageName) {
		synchronized (reflections) {
			if (reflections.containsKey(packageName)) {
				return reflections.get(packageName);
			}
		}
		Reflections reflect = new Reflections(wrapper == null ? buildParameters(packageName) : buildParameters(packageName, wrapper.getPluginClassLoader()));
		synchronized (reflections) {
			reflections.put(packageName, reflect);
		}
		return reflect;
	}

	public boolean has(String packageName) {
		synchronized (reflections) {
			return reflections.containsKey(packageName);
		}
	}

	public boolean delete(String packageName) {
		synchronized (reflections) {
			return reflections.remove(packageName) != null;
		}
	}

	/*
	 * Data
	 */

	public ReflectionProvider flush() {
		synchronized (reflections) {
			reflections.clear();
		}
		return this;
	}

}

package org.playuniverse.snowypine.compatibility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.playuniverse.snowypine.Snowypine;
import org.playuniverse.snowypine.config.Config;
import org.playuniverse.snowypine.config.config.AddonConfig;
import org.playuniverse.snowypine.utils.plugin.PluginPackage;
import org.playuniverse.snowypine.utils.plugin.PluginSettings;
import com.syntaxphoenix.syntaxapi.reflection.Reflect;

public abstract class CompatibilityHandler {

	private static final Map<String, CompatAddon<?>> COMPAT = Collections.synchronizedMap(new HashMap<>());

	public static void registerDefaults() {
	}

	public static <E extends CompatibilityAddon> boolean register(String pluginName, Class<E> addonClass) {
		if (COMPAT.containsKey(pluginName) || isAddonRegistered(addonClass))
			return false;
		COMPAT.put(pluginName, new CompatAddon<E>(addonClass));
		return true;
	}

	public static String[] getCompatibilityNames() {
		return COMPAT.keySet().toArray(new String[0]);
	}

	public static <E extends CompatibilityAddon> boolean isAddonRegistered(Class<E> addonClass) {
		return COMPAT.values().stream().unordered().anyMatch(addon -> addon.getOwner().equals(addonClass));
	}

	@SuppressWarnings("unchecked")
	public static <E extends CompatibilityAddon> Optional<E> getAddon(Class<E> addonClass) {
		return COMPAT.values().stream().unordered().filter(addon -> addon.getOwner().equals(addonClass)).findFirst().flatMap(CompatAddon::getInstance)
			.map(addon -> (E) addon);
	}

	public static Optional<CompatibilityAddon> getAddon(String pluginName) {
		return Optional.ofNullable(COMPAT.get(pluginName)).flatMap(CompatAddon::getInstance);
	}

	public static void handleSettingsUpdate(PluginSettings settings) {
		Optional<PluginPackage> optional;
		AddonConfig addonConfig = Config.ACCESS.get(AddonConfig.class);
		for (String name : COMPAT.keySet()) {
			if (!(optional = settings.searchPackage(name)).isPresent()) {
				CompatAddon<?> addon = COMPAT.get(name);
				if (addon.getInstance().isPresent())
					addon.shutdown();
				continue;
			}
			CompatAddon<?> addon = COMPAT.get(name);
			PluginPackage pluginPackage = optional.get();
			if (pluginPackage.getPlugin().isEnabled()) {
				boolean disabled = addonConfig.isDisabled(pluginPackage.getName());
				if (!addon.getInstance().isPresent() && !disabled) {
					addon.start(pluginPackage);
				} else if (disabled) {
					addon.shutdown();
				}
				continue;
			}
			if (addon.getInstance().isPresent())
				addon.shutdown();
		}
	}

	public static class CompatAddon<E extends CompatibilityAddon> {

		private final Class<E> owner;
		private final Reflect reflect;

		private CompatibilityAddon instance;

		private CompatAddon(Class<E> owner) {
			this.owner = owner;
			this.reflect = new Reflect(owner);
		}

		public Class<E> getOwner() {
			return owner;
		}

		public Reflect getReflect() {
			return reflect;
		}

		public Optional<CompatibilityAddon> getInstance() {
			return Optional.ofNullable(instance);
		}

		void start(PluginPackage pluginPackage) {
			if (instance != null)
				return;
			instance = (CompatibilityAddon) reflect.init();
			try {
				instance.onEnable(pluginPackage, Snowypine.getPlugin());
			} catch (IncompatiblePluginException pluginException) {
				instance = null;
				Snowypine.getCurrentLogger().log(pluginException);
			} catch (Exception exception) {
				instance = null;
				Snowypine.getCurrentLogger().log("&bFailed to enable compatibility addon '&3" + owner.getSimpleName().split("\\.")[0] + "&7' for plugin '&3"
					+ pluginPackage.getName() + "&7'!");
			}
		}

		void shutdown() {
			if (instance == null)
				return;
			try {
				instance.onDisable(Snowypine.getPlugin());
			} catch (Exception exception) {
				Snowypine.getCurrentLogger().log("&bFailed to disable compatibility addon " + owner.getSimpleName().split("\\.")[0] + "&7'!");
			}
			instance = null;
		}

	}

}

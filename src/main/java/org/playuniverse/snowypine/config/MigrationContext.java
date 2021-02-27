package org.playuniverse.snowypine.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class MigrationContext {

	private final Map<String, Object> values;

	public MigrationContext(YamlConfiguration configuration) {
		this.values = mapRootSection(configuration);
	}

	public Map<String, Object> getValues() {
		return values;
	}

	public <E> MigrationContext map(String path, Class<E> sample, Function<E, Object> mapper) {
		if (values.containsKey(path)) {
			E value = safeCast(sample, values.remove(path));
			if (value == null)
				return this;
			values.put(path, mapper.apply(value));
		}
		return this;
	}

	public MigrationContext remove(String path) {
		values.remove(path);
		return this;
	}

	public MigrationContext move(String path, String newPath) {
		if (values.containsKey(path))
			values.put(newPath, values.remove(path));
		return this;
	}

	public MigrationContext stack(String stack, String path) {
		if (values.containsKey(path))
			values.put(stack + '.' + path, values.remove(path));
		return this;
	}

	private <E> E safeCast(Class<E> sample, Object value) {
		return sample.isInstance(value) ? sample.cast(value) : null;
	}

	/*
	 * Mapping
	 */

	public static final String KEY = "%s.%s";

	public static Map<String, Object> mapRootSection(ConfigurationSection section) {
		LinkedHashMap<String, Object> output = new LinkedHashMap<>();
		for (String key : section.getKeys(false)) {
			Object value = section.get(key);
			if (value instanceof ConfigurationSection) {
				mapSubSection(output, (ConfigurationSection) value);
				continue;
			}
			output.put(key, value);
		}
		return output;
	}

	public static void mapSubSection(Map<String, Object> output, ConfigurationSection section) {
		String path = section.getCurrentPath();
		for (String key : section.getKeys(false)) {
			Object value = section.get(key);
			if (value instanceof ConfigurationSection) {
				mapSubSection(output, (ConfigurationSection) value);
				continue;
			}
			output.put(String.format(KEY, path, key), value);
		}
	}

}
package org.playuniverse.snowypine.data.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class Properties implements IProperties {

	private final List<IProperty<?>> properties = Collections.synchronizedList(new ArrayList<>());

	@Override
	public IProperties set(IProperty<?>... properties) {
		for (IProperty<?> property : properties) {
			set(property);
		}
		return this;
	}

	@Override
	public IProperties set(IProperty<?> property) {
		remove(property.getKey());
		if (property.isPresent()) {
			properties.add(property);
		}
		return this;
	}

	@Override
	public IProperties add(IProperty<?>... properties) {
		for (IProperty<?> property : properties) {
			add(property);
		}
		return this;
	}

	@Override
	public IProperties add(IProperty<?> property) {
		if (has(property.getKey())) {
			return this;
		}
		if (property.isPresent()) {
			properties.add(property);
		}
		return this;
	}

	@Override
	public IProperties delete(String... keys) {
		for (String key : keys) {
			delete(key);
		}
		return this;
	}

	@Override
	public IProperties delete(String key) {
		IProperty<?> property = find(key);
		if (property.isPresent()) {
			properties.remove(property);
		}
		return this;
	}

	@Override
	public List<IProperty<?>> remove(String... keys) {
		ArrayList<IProperty<?>> list = new ArrayList<>();
		for (String key : keys) {
			IProperty<?> property = remove(key);
			if (property.isPresent()) {
				list.add(property);
			}
		}
		return list;
	}

	@Override
	public IProperty<?> remove(String key) {
		IProperty<?> property = find(key);
		if (property.isPresent()) {
			properties.remove(property);
		}
		return property;
	}

	@Override
	public List<IProperty<?>> find(String... keys) {
		ArrayList<IProperty<?>> list = new ArrayList<>();
		for (String key : keys) {
			IProperty<?> property = find(key);
			if (property.isPresent()) {
				list.add(property);
			}
		}
		return list;
	}

	@Override
	public IProperty<?> find(String key) {
		return properties.stream().filter(property -> property.getKey().equals(key)).findFirst().orElse(new VoidProperty(key));
	}

	@Override
	public IProperties clear() {
		properties.clear();
		return this;
	}

	@Override
	public boolean has(String key) {
		return properties.stream().anyMatch(property -> property.getKey().equals(key));
	}

	@Override
	public int count() {
		return properties.size();
	}

	@Override
	public boolean isEmpty() {
		return properties.isEmpty();
	}

	@Override
	public List<IProperty<?>> asList() {
		return new ArrayList<>(properties);
	}

	@Override
	public IProperty<?>[] asArray() {
		return properties.toArray(new IProperty<?>[0]);
	}

}

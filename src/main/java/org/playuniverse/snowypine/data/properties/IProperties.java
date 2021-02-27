package org.playuniverse.snowypine.data.properties;

import java.util.List;

public interface IProperties {

	IProperties set(IProperty<?>... properties);

	IProperties set(IProperty<?> property);

	IProperties add(IProperty<?>... properties);

	IProperties add(IProperty<?> property);

	IProperties delete(String... keys);

	IProperties delete(String key);

	List<IProperty<?>> remove(String... keys);

	IProperty<?> remove(String key);

	List<IProperty<?>> find(String... keys);

	IProperty<?> find(String key);

	IProperties clear();

	boolean has(String key);

	int count();

	boolean isEmpty();

	List<IProperty<?>> asList();

	IProperty<?>[] asArray();

	public static IProperties create() {
		return new Properties();
	}

}

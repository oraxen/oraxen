package org.playuniverse.snowypine.data.properties;

import java.util.function.Function;

@SuppressWarnings("unchecked")
class Property<E> implements IProperty<E> {

	private final String key;
	private final E value;

	public Property(String key, E value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public E getValue() {
		return value;
	}

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public Class<?> getOwner() {
		return value.getClass();
	}

	@Override
	public boolean isInstance(Class<?> sample) {
		return sample.isInstance(value);
	}

	@Override
	public <V> IProperty<V> cast(Class<V> sample) {
		if (isInstance(sample)) {
			return (IProperty<V>) this;
		}
		return new VoidProperty(key);
	}

	@Override
	public <V> IProperty<V> map(Function<E, V> mapper) {
		return IProperty.of(key, mapper.apply(value));
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

}

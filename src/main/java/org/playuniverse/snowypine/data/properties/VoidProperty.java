package org.playuniverse.snowypine.data.properties;

import java.util.function.Function;

@SuppressWarnings("rawtypes")
class VoidProperty implements IProperty {

	private final String key;

	public VoidProperty(String key) {
		this.key = key;
	}

	@Override
	public Object getValue() {
		return null;
	}

	@Override
	public String getKey() {
		return key;
	}
	
	@Override
	public Class getOwner() {
		return Void.class;
	}

	@Override
	public boolean isInstance(Class sample) {
		return false;
	}

	@Override
	public IProperty cast(Class sample) {
		return this;
	}

	@Override
	public IProperty map(Function mapper) {
		return this;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

}

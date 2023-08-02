package io.th0rgal.oraxen.protocol;

import java.util.HashMap;
import java.util.Map;

public class ProtocolMapping<T> {

    private final Object[] values = new Object[MinecraftVersion.values().length];
    private Map<MinecraftVersion, T> temp = new HashMap<>();

    public ProtocolMapping<T> add(MinecraftVersion version, T value) {
        temp.put(version, value);
        return this;
    }

    public ProtocolMapping<T> build() {
        T lastValue = null;
        for (MinecraftVersion version : MinecraftVersion.values()) {
            values[version.ordinal()] = lastValue = temp.getOrDefault(version, lastValue);
        }

        temp = null;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T get(MinecraftVersion version) {
        return (T) values[version.ordinal()];
    }
}
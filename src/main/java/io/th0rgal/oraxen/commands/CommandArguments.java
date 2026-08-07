package io.th0rgal.oraxen.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CommandArguments {

    private final Map<String, Object> byName = new HashMap<>();
    private final List<Object> byIndex = new ArrayList<>();

    public void put(String name, Object value) {
        byName.put(name, value);
        byIndex.add(value);
    }

    public Object get(String name) {
        return byName.get(name);
    }

    public Object get(int index) {
        return byIndex.get(index);
    }

    public Object[] args() {
        return byIndex.toArray();
    }

    public Object getOrDefault(String name, Object defaultValue) {
        return byName.getOrDefault(name, defaultValue);
    }

    public Optional<Object> getOptional(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public <T> Optional<T> getOptionalByClass(String name, Class<T> type) {
        Object value = byName.get(name);
        return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
    }
}

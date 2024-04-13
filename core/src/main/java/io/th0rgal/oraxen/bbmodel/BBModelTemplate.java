package io.th0rgal.oraxen.bbmodel;

import com.google.gson.JsonElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public enum BBModelTemplate {
    INSTANCE
    ;
    private final Map<String, OraxenBBModelGenerator> models = new HashMap<>();

    public void clear() {
        models.clear();
    }

    public void register(@NotNull String name, @NotNull JsonElement element) {
        models.put(name, new OraxenBBModelGenerator(element));
    }

    public @Nullable OraxenBBModelGenerator get(@NotNull String name) {
        return models.get(name);
    }
}

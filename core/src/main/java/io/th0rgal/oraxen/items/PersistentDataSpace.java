package io.th0rgal.oraxen.items;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public record PersistentDataSpace<T, Z>(NamespacedKey namespacedKey, PersistentDataType<T, Z> dataType) {

    @Deprecated(forRemoval = true)
    public NamespacedKey getNamespacedKey() {
        return namespacedKey();
    }

    @Deprecated(forRemoval = true)
    public PersistentDataType<T, Z> getDataType() {
        return dataType();
    }

}

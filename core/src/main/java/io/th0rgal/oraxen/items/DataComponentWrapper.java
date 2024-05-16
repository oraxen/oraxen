package io.th0rgal.oraxen.items;

import kr.toxicity.libraries.datacomponent.api.DataComponentType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DataComponentWrapper {
    @NotNull
    ItemStack apply(@NotNull ItemStack itemStack);
    @Nullable
    <T> T get(@NotNull DataComponentType<T> type);
    @NotNull default <T> T get(@NotNull DataComponentType<T> type, @NotNull T def) {
        T get = get(type);
        return get != null ? get : def;
    }
}

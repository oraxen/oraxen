package io.th0rgal.oraxen.items;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.th0rgal.oraxen.utils.VersionUtil;
import kr.toxicity.libraries.datacomponent.api.DataComponent;
import kr.toxicity.libraries.datacomponent.api.DataComponentAPI;
import kr.toxicity.libraries.datacomponent.api.DataComponentType;
import kr.toxicity.libraries.datacomponent.api.ItemAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class DataComponentAdapter {


    private record Support(@NotNull DataComponent component) implements DataComponentWrapper {
        @Override
        public @NotNull ItemStack apply(@NotNull ItemStack itemStack) {
            ItemAdapter adapter = DataComponentAPI.api().adapter(itemStack);
            component.set(adapter);
            return adapter.build();
        }
        @Override
        @Nullable public <T> T get(@NotNull DataComponentType<T> type) {
            return component.get(type);
        }
    }
    private static final DataComponentWrapper EMPTY = new DataComponentWrapper() {
        @NotNull
        @Override
        public ItemStack apply(@NotNull ItemStack itemStack) {
            return itemStack;
        }

        @Nullable
        @Override
        public <T> T get(@NotNull DataComponentType<T> type) {
            return null;
        }
    };


    private static @Nullable JsonElement convert(@NotNull Object object) {
        if (object instanceof String string) {
            return new JsonPrimitive(string);
        } else if (object instanceof Number number) {
            return new JsonPrimitive(number);
        } else if (object instanceof Character character) {
            return new JsonPrimitive(character);
        } else if (object instanceof Boolean bool) {
            return new JsonPrimitive(bool);
        } else if (object instanceof Component component) {
            return GsonComponentSerializer.gson().serializeToTree(component);
        } else if (object instanceof List<?> list) {
            JsonArray array = new JsonArray();
            for (Object o : list) {
                JsonElement converted = convert(o);
                if (converted != null) array.add(converted);
            }
            return array;
        } else if (object instanceof Map<?,?> map) {
            JsonObject jsonObject = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String s) {
                    JsonElement element = convert(entry.getValue());
                    if (element == null) continue;
                    jsonObject.add(s, element);
                }
            }
            return jsonObject;
        } else if (object instanceof ConfigurationSection section) {
            return fromYaml(section);
        } else return null;
    }

    private static JsonObject fromYaml(@NotNull ConfigurationSection section) {
        JsonObject object = new JsonObject();
        for (String key : section.getKeys(false)) {
            Object get = section.get(key);
            if (get == null) continue;
            JsonElement converted = convert(get);
            if (converted == null) continue;
            object.add(key, converted);
        }
        return object;
    }

    public static @NotNull DataComponentWrapper adapt(@NotNull ItemStack itemStack) {
        return adapt(DataComponentAPI.api().adapter(itemStack).serialize());
    }
    public static @NotNull DataComponentWrapper adapt(@NotNull JsonObject object) {
        return VersionUtil.atOrAbove("1.20.5") ? new Support(DataComponentAPI.api().deserializer().deserialize(object)) : EMPTY;
    }
    public static @NotNull DataComponentWrapper adapt(@NotNull ConfigurationSection section) {
        return adapt(fromYaml(section));
    }
}

package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class CustomBlockRegistry {
    private static final Map<String, CustomBlockType> registry = new HashMap<>();

    public static void register(CustomBlockType blockType) {
        registry.put(blockType.name(), blockType);
    }

    public static CustomBlockType get(String name) {
        return registry.get(name);
    }

    public static String[] names() {
        return registry.keySet().toArray(new String[0]);
    }

    @Nullable
    public static CustomBlockType fromMechanicSection(ConfigurationSection section) {
        String typeName = section.getString("type", "");
        CustomBlockType type = CustomBlockRegistry.get(typeName);
        if (type == null) {
            String itemId = section.getParent().getParent().getName();
            Logs.logError("No CustomBlock-type defined in " + itemId);
            Logs.logError("Valid types are: " + StringUtils.join(CustomBlockRegistry.names(), ", "));
        }
        return type;
    }
}

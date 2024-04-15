package io.th0rgal.oraxen.mechanics.provided.gameplay.custom_block;

import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public enum CustomBlockType {
    NOTEBLOCK, STRINGBLOCK;

    @Nullable
    public static CustomBlockType fromMechanicSection(ConfigurationSection section) {
        return Arrays.stream(CustomBlockType.values())
                .filter(e -> e.name().equals(section.getString("type", "")))
                .findFirst().orElseGet(() -> {
                    String itemId = section.getParent().getParent().getName();
                    Logs.logError("No CustomBlock-type defined in " + itemId);
                    Logs.logError("Valid types are: " + StringUtils.join(CustomBlockType.names(), ", "));
                    return null;
                });
    }

    public static String[] names() {
        return Arrays.stream(values()).map(Enum::name).toList().toArray(new String[0]);
    }
}

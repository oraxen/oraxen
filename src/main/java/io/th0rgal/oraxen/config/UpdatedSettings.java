package io.th0rgal.oraxen.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum UpdatedSettings {
    GUI_INVENTORY("gui_inventory", "oraxen_inventory.menu_layout"),
    ;

    private final String path;
    private final String newPath;

    UpdatedSettings(String path, String newPath) {
        this.path = path;
        this.newPath = newPath;
    }

    @Override
    public String toString() {
        return this.path + ", " + newPath;
    }

    public static List<String> toStringList() {
        return Arrays.stream(UpdatedSettings.values()).map(UpdatedSettings::toString).toList();
    }

    public static Map<String, String> toStringMap() {
        return Arrays.stream(UpdatedSettings.values()).map(u -> u.toString().split(", ", 2)).collect(Collectors.toMap(e -> e[0], e -> e[1]));
    }
}

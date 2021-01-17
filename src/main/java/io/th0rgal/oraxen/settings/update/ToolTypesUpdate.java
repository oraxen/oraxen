package io.th0rgal.oraxen.settings.update;

import io.th0rgal.oraxen.settings.Update;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ToolTypesUpdate {

    @Update(path = {"items", "blocks"}, version = 202101171304L)
    public static void updateItemTypes(YamlConfiguration config) {

        for (String sectionName : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(sectionName);

            if (!section.isConfigurationSection("Mechanics"))
                continue;
            ConfigurationSection mechanicSection = section.getConfigurationSection("Mechanics");

            if (!mechanicSection.isConfigurationSection("block"))
                continue;
            ConfigurationSection blockSection = mechanicSection.getConfigurationSection("block");

            if (!blockSection.isConfigurationSection("drop"))
                continue;
            ConfigurationSection dropSection = blockSection.getConfigurationSection("drop");

            if (!dropSection.isString("minimal_tool"))
                continue;

            dropSection.set("minimal_type", dropSection.getString("minimal_tool").replace("_PICKAXE", ""));
        }
    }
}

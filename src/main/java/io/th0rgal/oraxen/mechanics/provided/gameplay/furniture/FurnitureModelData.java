package io.th0rgal.oraxen.mechanics.provided.gameplay.furniture;

import io.th0rgal.oraxen.items.ModelData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/** Reserves render identifiers before inventory items can allocate them again. */
public final class FurnitureModelData {
    private static File file;
    private static YamlConfiguration assignments;

    private FurnitureModelData() {}

    public static void load(File storage) {
        synchronized (ModelData.DATAS) {
            YamlConfiguration loaded = new YamlConfiguration();
            loaded.options().pathSeparator('\0');
            if (storage.exists()) {
                try {
                    loaded.load(storage);
                } catch (IOException | org.bukkit.configuration.InvalidConfigurationException exception) {
                    throw new IllegalStateException("Cannot load furniture model data", exception);
                }
            }
            for (String materialName : loaded.getKeys(false)) {
                Material material = Material.valueOf(materialName);
                ConfigurationSection variants = loaded.getConfigurationSection(materialName);
                if (variants == null) continue;
                for (String variant : variants.getKeys(false)) {
                    int number = variants.getInt(variant);
                    var used = ModelData.DATAS.get(material);
                    if (used != null && used.entrySet().stream().anyMatch(entry ->
                            entry.getValue() == number && !entry.getKey().equals(variant))) {
                        throw new IllegalStateException("Furniture model data conflicts with an item on " + materialName);
                    }
                    new ModelData(material, variant, number);
                }
            }
            file = storage;
            assignments = loaded;
        }
    }

    public static int generateId(String variant, Material material) {
        synchronized (ModelData.DATAS) {
            int number = ModelData.generateId(variant, material);
            if (assignments != null) {
                String path = material.name() + "\0" + variant;
                if (!assignments.contains(path)) {
                    assignments.set(path, number);
                    try {
                        assignments.save(file);
                    } catch (IOException exception) {
                        assignments.set(path, null);
                        throw new IllegalStateException("Cannot save furniture model data", exception);
                    }
                }
            }
            return number;
        }
    }
}

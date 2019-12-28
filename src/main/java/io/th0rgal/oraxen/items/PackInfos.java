package io.th0rgal.oraxen.items;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class PackInfos {

    private int customModelData;
    private String modelName;
    private final List<String> layers;
    private final String parentModel;
    private final boolean generate_model;

    public PackInfos(ConfigurationSection configurationSection) {
        this.modelName = readModelName(configurationSection);
        this.layers = configurationSection.getStringList("layers");
        // can't be refactored with for each or stream because it'll throw ConcurrentModificationException
        for (int i = 0; i < layers.size(); i++) {
            String layer = layers.get(i);
            if (layer.endsWith(".png"))
                layers.set(i, layer.substring(0, layer.length() - 4));
        }
        this.generate_model = configurationSection.getBoolean("generate_model");
        this.parentModel = configurationSection.getString("parent_model");
    }

    // this is maybe not a really good function name
    private String readModelName(ConfigurationSection configurationSection) {
        String modelName = configurationSection.getString("model");
        if (modelName == null)
            return configurationSection.getParent().getName();
        if (modelName.endsWith(".json"))
            return modelName.substring(0, modelName.length() - 5);
        return modelName;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public int getCustomModelData() {
        return  customModelData;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    public boolean hasLayers() {
        return layers != null && !layers.isEmpty();
    }

    public List<String> getLayers() {
        return layers;
    }

    public String getParentModel() {
        return parentModel;
    }

    public boolean shouldGenerateModel() {
        return generate_model;
    }

}
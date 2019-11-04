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
        this.modelName = configurationSection.getString("model");
        if (modelName == null)
            this.modelName = configurationSection.getParent().getName();
        if (modelName.endsWith(".json"))
            this.modelName = modelName.substring(0, modelName.length() - 5);

        this.layers = configurationSection.getStringList("layers");
        for (int i = 0; i < layers.size(); i++) {
            String layer = layers.get(i);
            if (layer.endsWith(".png"))
                layers.set(i, layer.substring(0, layer.length() - 4));
        }

        this.generate_model = configurationSection.getBoolean("generate_model");
        this.parentModel = configurationSection.getString("parent_model");
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
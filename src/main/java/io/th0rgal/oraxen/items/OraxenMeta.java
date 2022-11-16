package io.th0rgal.oraxen.items;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class OraxenMeta {

    private int customModelData;
    private String modelName;
    private String blockingModel;
    private List<String> pullingModels;
    private String chargedModel;
    private String fireworkModel;
    private String castModel;
    private List<String> layers;
    private String parentModel;
    private boolean generate_model;
    private boolean hasPackInfos = false;
    private boolean excludedFromInventory = false;
    private boolean noUpdate = false;

    public void setExcludedFromInventory() {
        this.excludedFromInventory = true;
    }

    public boolean isExcludedFromInventory() {
        return excludedFromInventory;
    }

    public void setPackInfos(ConfigurationSection configurationSection) {
        this.hasPackInfos = true;
        this.modelName = readModelName(configurationSection, "model");
        this.blockingModel = readModelName(configurationSection, "blocking_model");
        this.castModel = readModelName(configurationSection, "cast_model");
        this.chargedModel = readModelName(configurationSection, "charged_model");
        this.fireworkModel = readModelName(configurationSection, "firework_model");
        this.pullingModels = configurationSection.isList("pulling_models")
                ? configurationSection.getStringList("pulling_models") : null;
        this.layers = configurationSection.getStringList("textures");
        // can't be refactored with for each or stream because it'll throw
        // ConcurrentModificationException
        for (int i = 0; i < layers.size(); i++) {
            String layer = layers.get(i);
            if (layer.endsWith(".png"))
                layers.set(i, layer.substring(0, layer.length() - 4));
        }
        this.generate_model = configurationSection.getBoolean("generate_model");
        this.parentModel = configurationSection.getString("parent_model", "item/generated");
    }

    // this might not be a really good function name
    private String readModelName(ConfigurationSection configurationSection, String configString) {
        String modelName = configurationSection.getString(configString);
        if (modelName == null && configString.equals("model"))
            return configurationSection.getParent().getName();
        if (modelName != null && modelName.endsWith(".json"))
            return modelName.substring(0, modelName.length() - 5);

        return modelName;
    }

    public boolean hasPackInfos() {
        return hasPackInfos;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void setNoUpdate(boolean noUpdate){
        this.noUpdate = noUpdate;
    }

    public String getModelName() {
        return modelName;
    }

    public boolean hasBlockingModel() {
        return blockingModel != null;
    }

    public String getBlockingModelName() {
        return blockingModel;
    }

    public boolean hasCastModel() {
        return castModel != null;
    }

    public String getCastModelName() {
        return castModel;
    }

    public boolean hasChargedModel() {
        return chargedModel != null;
    }

    public String getChargedModelName() {
        return chargedModel;
    }

    public boolean hasFireworkModel() {
        return fireworkModel != null;
    }

    public String getFireworkModelName() {
        return fireworkModel;
    }

    public boolean hasPullingModels() {
        return pullingModels != null && !pullingModels.isEmpty();
    }

    public List<String> getPullingModels() {
        return pullingModels;
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

    public boolean isNoUpdate(){
        return noUpdate;
    }

}

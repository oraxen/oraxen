package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.Utils;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OraxenMeta {

    private int customModelData;
    private String modelName;
    private String blockingModel;
    private List<String> pullingModels;
    private String chargedModel;
    private String fireworkModel;
    private String castModel;
    private List<String> layers;
    private Map<String, String> layersMap;
    private String parentModel;
    private String generatedModelPath;
    private boolean generate_model;
    private boolean hasPackInfos = false;
    private boolean excludedFromInventory = false;
    private boolean excludedFromCommands = false;
    private boolean noUpdate = false;
    private boolean disableEnchanting = false;

    public void setExcludedFromInventory(boolean excluded) {
        this.excludedFromInventory = excluded;
    }

    public boolean isExcludedFromInventory() {
        return excludedFromInventory;
    }

    public void setExcludedFromCommands(boolean excluded) {
        this.excludedFromCommands = excluded;
    }

    public boolean isExcludedFromCommands() {
        return excludedFromCommands;
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

        if (configurationSection.isList("textures")) {
            this.layers = configurationSection.getStringList("textures");
            List<String> layers = new ArrayList<>();
            this.layers.forEach(layer -> layers.add(this.layers.indexOf(layer), layer.replace(".png", "")));
            this.layers = layers;
        }
        else if (configurationSection.isConfigurationSection("textures")) {
            ConfigurationSection texturesSection = configurationSection.getConfigurationSection("textures");
            assert texturesSection != null;
            Map<String, String> layersMap = new HashMap<>();
            texturesSection.getKeys(false).forEach(key -> layersMap.put(key, texturesSection.getString(key).replace(".png", "")));
            this.layersMap = layersMap;
        }

        // If not specified, check if a model or texture is set
        this.generate_model = configurationSection.getBoolean("generate_model", getModelName().isEmpty());
        this.generatedModelPath = configurationSection.getString("generated_model_path", "");
        this.parentModel = configurationSection.getString("parent_model", "item/generated");
    }

    // this might not be a very good function name
    private String readModelName(ConfigurationSection configSection, String configString) {
        String modelName = configSection.getString(configString);
        List<String> textures = configSection.getStringList("textures");
        ConfigurationSection parent = configSection.getParent();
        modelName = modelName != null ? modelName : Settings.GENERATE_MODEL_BASED_ON_TEXTURE_PATH.toBool() && !textures.isEmpty() && parent != null
                ? Utils.getParentDirs(textures.stream().findFirst().get()) + parent.getName() : null;

        if (modelName == null && configString.equals("model") && parent != null)
            return parent.getName();
        else if (modelName != null)
            return modelName.replace(".json", "");
        else return null;
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

    public void setNoUpdate(boolean noUpdate) {
        this.noUpdate = noUpdate;
    }

    public void setDisableEnchanting(boolean disableEnchanting) { this.disableEnchanting = disableEnchanting; }

    public String getModelName() {
        return modelName;
    }

    public String getModelPath() {
        String[] pathElements = generatedModelPath.split(":");
        String path;
        if (pathElements.length > 1)
            path = "assets/" + pathElements[0] + "/models/" + pathElements[1];
        else
            path = "assets/minecraft/models/" + pathElements[0];
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path;
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

    public boolean hasLayersMap() {
        return layersMap != null && !layersMap.isEmpty();
    }

    public Map<String, String> getLayersMap() {
        return layersMap;
    }

    public String getParentModel() {
        return parentModel;
    }

    public String getGeneratedModelPath() {
        if (generatedModelPath.isEmpty())
            return generatedModelPath;
        return generatedModelPath + (generatedModelPath.endsWith("/") ? "" : "/");
    }

    public boolean shouldGenerateModel() {
        return generate_model;
    }

    public boolean isNoUpdate() {
        return noUpdate;
    }

    public boolean isDisableEnchanting() { return disableEnchanting; }

}

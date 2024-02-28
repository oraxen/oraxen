package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OraxenMeta {

    private int customModelData;
    private String modelName;
    private String blockingModel;
    private String blockingTexture;
    private List<String> pullingModels;
    private List<String> pullingTextures;
    private String chargedModel;
    private String chargedTexture;
    private String fireworkModel;
    private String fireworkTexture;
    private String castModel;
    private String castTexture;
    private List<String> damagedModels;
    private List<String> damagedTextures;
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

    public void setPackInfos(ConfigurationSection section) {
        this.hasPackInfos = true;
        this.modelName = readModelName(section, "model");
        this.blockingModel = readModelName(section, "blocking_model");
        this.castModel = readModelName(section, "cast_model");
        this.chargedModel = readModelName(section, "charged_model");
        this.fireworkModel = readModelName(section, "firework_model");
        this.pullingModels = section.isList("pulling_models")
                ? section.getStringList("pulling_models") : null;
        this.damagedModels = section.isList("damaged_models")
                ? section.getStringList("damaged_models") : null;

        // By adding the textures to pullingModels aswell,
        // we can use the same code for both pullingModels
        // and pullingTextures to add to the base-bow file predicates
        if (pullingModels == null && section.isList("pulling_textures")) {
            pullingTextures = section.getStringList("pulling_textures").stream().map(texture -> texture.replace(".png", "")).toList();
            pullingModels = pullingTextures;
        }

        if (chargedModel == null && section.isString("charged_texture")) {
            chargedTexture = section.getString("charged_texture").replace(".png", "");
            chargedModel = chargedTexture;
        }

        if (fireworkModel == null && section.isString("firework_texture")) {
            fireworkTexture = section.getString("firework_texture").replace(".png", "");
            fireworkModel = fireworkTexture;
        }

        if (castModel == null && section.isString("cast_texture")) {
            castTexture = section.getString("cast_texture").replace(".png", "");
            castModel = castTexture;
        }

        if (blockingModel == null && section.isString("blocking_texture")) {
            blockingTexture = section.getString("blocking_texture").replace(".png", "");
            blockingModel = blockingTexture;
        }

        if (damagedModels == null && section.isList("damaged_textures")) {
            damagedTextures = section.getStringList("damaged_textures").stream().map(texture -> texture.replace(".png", "")).toList();
            damagedModels = damagedTextures;
        }

        if (section.isList("textures")) {
            this.layers = section.getStringList("textures");
            List<String> layers = new ArrayList<>();
            this.layers.forEach(layer -> layers.add(this.layers.indexOf(layer), layer.replace(".png", "")));
            this.layers = layers;
        }
        else if (section.isConfigurationSection("textures")) {
            ConfigurationSection texturesSection = section.getConfigurationSection("textures");
            assert texturesSection != null;
            Map<String, String> layersMap = new HashMap<>();
            texturesSection.getKeys(false).forEach(key -> layersMap.put(key, texturesSection.getString(key).replace(".png", "")));
            this.layersMap = layersMap;
        }

        // If not specified, check if a model or texture is set
        this.generate_model = section.getBoolean("generate_model", modelName.isEmpty());
        this.generatedModelPath = section.getString("generated_model_path", "");
        this.parentModel = section.getString("parent_model", "item/generated");

        if (generate_model && !modelName.matches("^[a-z0-9-_/]+$")) {
            Logs.logWarning("Item " + section.getParent().getName() + " is set to generate a model, but ItemID does not adhere to [a-z0-9-_]!");
            Logs.logWarning("This will generate a malformed model!");
        }

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

    public OraxenMeta setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
        return this;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public OraxenMeta setModelName(String modelName) {
        this.modelName = modelName;
        return this;
    }

    public OraxenMeta setNoUpdate(boolean noUpdate) {
        this.noUpdate = noUpdate;
        return this;
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

    public static String getModelPath(String model) {
        String[] pathElements = model.split(":");
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

    public String getBlockingModel() {
        return blockingModel;
    }

    public boolean hasBlockingTexture() {
        return blockingTexture != null;
    }

    public String getBlockingTexture() {
        return blockingTexture;
    }

    public boolean hasCastModel() {
        return castModel != null;
    }

    public String getCastModel() {
        return castModel;
    }

    public boolean hasCastTexture() {
        return castTexture != null;
    }

    public String getCastTexture() {
        return castTexture;
    }

    public boolean hasChargedModel() {
        return chargedModel != null;
    }

    public String getChargedModel() {
        return chargedModel;
    }

    public boolean hasChargedTexture() {
        return chargedTexture != null;
    }

    public String getChargedTexture() {
        return chargedTexture;
    }

    public boolean hasFireworkModel() {
        return fireworkModel != null;
    }

    public String getFireworkModel() {
        return fireworkModel;
    }

    public boolean hasFireworkTexture() {
        return fireworkTexture != null;
    }

    public String getFireworkTexture() {
        return fireworkTexture;
    }

    public boolean hasPullingModels() {
        return pullingModels != null && !pullingModels.isEmpty();
    }

    public List<String> getPullingModels() {
        return pullingModels;
    }

    public boolean hasPullingTextures() {
        return pullingTextures != null && !pullingTextures.isEmpty();
    }

    public List<String> getPullingTextures() {
        return pullingTextures;
    }

    public boolean hasDamagedModels() {
        return damagedModels != null && !damagedModels.isEmpty();
    }

    public List<String> getDamagedModels() {
        return damagedModels;
    }

    public boolean hasDamagedTextures() {
        return damagedTextures != null && !damagedTextures.isEmpty();
    }

    public List<String> getDamagedTextures() {
        return damagedTextures;
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


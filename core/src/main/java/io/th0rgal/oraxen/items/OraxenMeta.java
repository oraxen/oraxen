package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.new_pack.ModelGenerator;
import io.th0rgal.oraxen.utils.Utils;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import team.unnamed.creative.model.Model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OraxenMeta {

    private int customModelData;
    private Key modelKey;
    private Key blockingModel;
    private List<Key> pullingModels;
    private Key chargedModel;
    private Key fireworkModel;
    private Key castModel;
    private List<Key> damagedModels;
    private List<Key> layers;
    private Map<String, Key> layersMap;
    private Key parentModel;
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
        this.modelKey = readModelName(section, "model");
        this.blockingModel = readModelName(section, "blocking_model");
        this.castModel = readModelName(section, "cast_model");
        this.chargedModel = readModelName(section, "charged_model");
        this.fireworkModel = readModelName(section, "firework_model");
        this.pullingModels = section.getStringList("pulling_models").stream().map(Key::key).toList();
        this.damagedModels = section.getStringList("damaged_models").stream().map(Key::key).toList();

        // By adding the textures to pullingModels aswell,
        // we can use the same code for both pullingModels
        // and pullingTextures to add to the base-bow file predicates
        if (pullingModels.isEmpty()) pullingModels = section.getStringList("pulling_textures").stream().map(texture -> texture.replace(".png", "")).map(Key::key).toList();

        if (chargedModel == null) chargedModel = Key.key(section.getString("charged_texture", "").replace(".png", ""));
        if (fireworkModel == null) fireworkModel = Key.key(section.getString("firework_texture", "").replace(".png", ""));
        if (castModel == null) castModel = Key.key(section.getString("cast_texture", "").replace(".png", ""));
        if (blockingModel == null) blockingModel = Key.key(section.getString("blocking_texture", "").replace(".png", ""));
        if (damagedModels == null) damagedModels = section.getStringList("damaged_textures").stream().map(texture -> texture.replace(".png", "")).map(Key::key).toList();

        ConfigurationSection textureSection = section.getConfigurationSection("textures");
        if (textureSection != null) {
            ConfigurationSection texturesSection = section.getConfigurationSection("textures");
            assert texturesSection != null;
            Map<String, Key> layersMap = new HashMap<>();
            texturesSection.getKeys(false).forEach(key -> layersMap.put(key, Key.key(texturesSection.getString(key))));
            this.layersMap = layersMap;
        }
        else if (section.isList("textures")) this.layers = section.getStringList("textures").stream().map(Key::key).toList();
        else if (section.isString("textures")) this.layers = List.of(Key.key(section.getString("textures")));
        else if (section.isString("texture")) this.layers = List.of(Key.key(section.getString("texture")));
        else {
            this.layers = new ArrayList<>();
            this.layersMap = new HashMap<>();
        }

        // If not specified, check if a model or texture is set
        this.generate_model = modelKey == null;
        this.parentModel = Key.key(section.getString("parent_model", "item/generated"));
    }

    // this might not be a very good function name
    private Key readModelName(ConfigurationSection configSection, String configString) {
        String modelName = configSection.getString(configString);
        List<String> textures = configSection.getStringList("textures");
        ConfigurationSection parent = configSection.getParent();
        modelName = modelName != null ? modelName : Settings.GENERATE_MODEL_BASED_ON_TEXTURE_PATH.toBool() && !textures.isEmpty() && parent != null
                ? Utils.getParentDirs(textures.stream().findFirst().get()) + parent.getName() : null;

        if (modelName == null && configString.equals("model") && parent != null)
            return Key.key(parent.getName());
        else if (modelName != null)
            return Key.key(modelName.replace(".json", ""));
        else return null;
    }

    public boolean hasPackInfos() {
        return hasPackInfos;
    }

    public int customModelData() {
        return customModelData;
    }

    public void customModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public void modelKey(Key modelKey) {
        this.modelKey = modelKey;
    }

    public Model model() {
        return ModelGenerator.generateModelBuilder(this).build();
    }

    public Key modelKey() {
        return modelKey;
    }

    public boolean hasBlockingModel() {
        return blockingModel != null;
    }

    public Key blockingModel() {
        return blockingModel;
    }

    public boolean hasCastModel() {
        return castModel != null;
    }

    public Key castModel() {
        return castModel;
    }

    public boolean hasChargedModel() {
        return chargedModel != null;
    }

    public Key chargedModel() {
        return chargedModel;
    }

    public boolean hasFireworkModel() {
        return fireworkModel != null;
    }

    public Key fireworkModel() {
        return fireworkModel;
    }

    public List<Key> pullingModels() {
        return pullingModels;
    }

    public List<Key> damagedModels() {
        return damagedModels;
    }

    public Map<String, Key> getLayersMap() {
        return layersMap;
    }

    public Key parentModelKey() {
        return parentModel;
    }

    public boolean shouldGenerateModel() {
        return generate_model;
    }

    public boolean noUpdate() {
        return noUpdate;
    }

    public void noUpdate(boolean noUpdate) {
        this.noUpdate = noUpdate;
    }


    public boolean disableEnchanting() { return disableEnchanting; }

    public void disableEnchanting(boolean disableEnchanting) { this.disableEnchanting = disableEnchanting; }


}


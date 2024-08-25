package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.utils.KeyUtils;
import net.kyori.adventure.key.Key;
import org.bukkit.configuration.ConfigurationSection;
import team.unnamed.creative.model.ModelTexture;
import team.unnamed.creative.model.ModelTextures;

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
    private List<ModelTexture> textureLayers;
    private Map<String, ModelTexture> textureVariables;
    private ModelTextures modelTextures;
    private Key parentModel;
    private boolean hasPackInfos = false;
    private boolean excludedFromInventory = false;
    private boolean excludedFromCommands = false;
    private boolean noUpdate = false;
    private boolean disableEnchanting = false;
    private boolean generateModel = false;

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
        this.pullingModels = section.getStringList("pulling_models").stream().map(KeyUtils::dropExtension).toList();
        this.damagedModels = section.getStringList("damaged_models").stream().map(KeyUtils::dropExtension).toList();

        // By adding the textures to pullingModels aswell,
        // we can use the same code for both pullingModels
        // and pullingTextures to add to the base-bow file predicates
        if (pullingModels.isEmpty()) pullingModels = section.getStringList("pulling_textures").stream().map(KeyUtils::dropExtension).toList();
        if (damagedModels == null) damagedModels = section.getStringList("damaged_textures").stream().map(KeyUtils::dropExtension).toList();

        if (chargedModel == null) chargedModel = KeyUtils.dropExtension(section.getString("charged_texture", ""));
        if (fireworkModel == null) fireworkModel = KeyUtils.dropExtension(section.getString("firework_texture", ""));
        if (castModel == null) castModel = KeyUtils.dropExtension(section.getString("cast_texture", ""));
        if (blockingModel == null) blockingModel = KeyUtils.dropExtension(section.getString("blocking_texture", ""));

        ConfigurationSection textureSection = section.getConfigurationSection("textures");
        if (textureSection != null) {
            ConfigurationSection texturesSection = section.getConfigurationSection("textures");
            assert texturesSection != null;
            Map<String, ModelTexture> variables = new HashMap<>();
            texturesSection.getKeys(false).forEach(key -> variables.put(key, ModelTexture.ofKey(KeyUtils.dropExtension(texturesSection.getString(key)))));
            this.textureVariables = variables;
        }
        else if (section.isList("textures")) this.textureLayers = section.getStringList("textures").stream().map(KeyUtils::dropExtension).map(ModelTexture::ofKey).toList();
        else if (section.isString("textures")) this.textureLayers = List.of(ModelTexture.ofKey(KeyUtils.dropExtension(section.getString("textures", ""))));
        else if (section.isString("texture")) this.textureLayers = List.of(ModelTexture.ofKey(KeyUtils.dropExtension(section.getString("texture", ""))));

        this.textureVariables = textureVariables != null ? textureVariables : new HashMap<>();
        this.textureLayers = textureLayers != null ? textureLayers : new ArrayList<>();

        this.modelTextures = ModelTextures.builder()
                .particle(textureVariables.get("particle"))
                .variables(textureVariables)
                .layers(textureLayers)
                .build();

        this.parentModel = Key.key(section.getString("parent_model", "item/generated"));
        this.generateModel = section.getString("model") == null;
    }

    // this might not be a very good function name
    private Key readModelName(ConfigurationSection configSection, String configString) {
        String modelName = configSection.getString(configString);
        String parent = configSection.getParent().getName();

        if (modelName == null && configString.equals("model") && Key.parseable(parent)) return Key.key(parent);
        else if (Key.parseable(modelName)) return KeyUtils.dropExtension(modelName);
        else return KeyUtils.MALFORMED_KEY_PLACEHOLDER;
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

    public Key modelKey() {
        return modelKey;
    }

    public boolean hasBlockingModel() {
        return blockingModel != null && !blockingModel.value().isEmpty();
    }

    public Key blockingModel() {
        return blockingModel;
    }

    public boolean hasCastModel() {
        return castModel != null && !castModel.value().isEmpty();
    }

    public Key castModel() {
        return castModel;
    }

    public boolean hasChargedModel() {
        return chargedModel != null && !chargedModel.value().isEmpty();
    }

    public Key chargedModel() {
        return chargedModel;
    }

    public boolean hasFireworkModel() {
        return fireworkModel != null && !fireworkModel.value().isEmpty();
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

    public ModelTextures modelTextures() {
        return modelTextures;
    }

    public Key parentModelKey() {
        return parentModel;
    }

    public boolean shouldGenerateModel() {
        return generateModel;
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


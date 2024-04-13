package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.bbmodel.BBModelTemplate;
import io.th0rgal.oraxen.bbmodel.OraxenBBModelGenerator;
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

    private OraxenTexturesMeta texturesMeta;

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
        this.pullingModels = section.getStringList("pulling_models").stream().map(s -> Key.key(s.replace(".png", ""))).toList();
        this.damagedModels = section.getStringList("damaged_models").stream().map(s -> Key.key(s.replace(".png", ""))).toList();

        // By adding the textures to pullingModels aswell,
        // we can use the same code for both pullingModels
        // and pullingTextures to add to the base-bow file predicates
        if (pullingModels.isEmpty()) pullingModels = section.getStringList("pulling_textures").stream().map(t -> Key.key(t.replace(".png", ""))).toList();
        if (damagedModels == null) damagedModels = section.getStringList("damaged_textures").stream().map(t -> Key.key(t.replace(".png", ""))).toList();

        if (chargedModel == null) chargedModel = Key.key(section.getString("charged_texture", "").replace(".png", ""));
        if (fireworkModel == null) fireworkModel = Key.key(section.getString("firework_texture", "").replace(".png", ""));
        if (castModel == null) castModel = Key.key(section.getString("cast_texture", "").replace(".png", ""));
        if (blockingModel == null) blockingModel = Key.key(section.getString("blocking_texture", "").replace(".png", ""));


        this.parentModel = Key.key(section.getString("parent_model", "item/generated"));

        String bbmodel = section.getString("bbmodel");
        OraxenBBModelGenerator generator = bbmodel != null ? BBModelTemplate.INSTANCE.get(bbmodel) : null;

        if (generator != null) {
            texturesMeta = new OraxenBBModelTexturesMeta(this, generator, section.getIntegerList("animation").stream().mapToInt(i -> i).toArray());
            generateModel = true;
        } else {
            texturesMeta = new OraxenKeyTexturesMeta(this, section);
            this.generateModel = section.getString("model") == null;
        }
    }

    // this might not be a very good function name
    private Key readModelName(ConfigurationSection configSection, String configString) {
        String modelName = configSection.getString(configString);
        ConfigurationSection parent = configSection.getParent();

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

    public OraxenTexturesMeta texturesMeta() {
        return texturesMeta;
    }

    public boolean disableEnchanting() { return disableEnchanting; }

    public void disableEnchanting(boolean disableEnchanting) { this.disableEnchanting = disableEnchanting; }


}


package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PredicatesGenerator {

    private final JsonObject json = new JsonObject();
    // not static here because only instanciated once I think
    private final String[] tools = new String[]{"PICKAXE", "SWORD", "HOE", "AXE", "SHOVEL"};

    public PredicatesGenerator(final Material material, final List<ItemBuilder> items) {

        // parent
        json.addProperty("parent", getParent(material));

        String vanillaTextureName = getVanillaTextureName(material, false);

        // textures
        final ItemMeta exampleMeta = new ItemStack(material).getItemMeta();
        final JsonObject textures = new JsonObject();

        // potions use the overlay as the base layer
        if (exampleMeta instanceof PotionMeta) {
            textures.addProperty("layer0", vanillaTextureName + "_overlay");
            textures.addProperty("layer1", vanillaTextureName);
        }
        // to support colored leather armor
        else if (exampleMeta instanceof LeatherArmorMeta && material != Material.LEATHER_HORSE_ARMOR) {
            textures.addProperty("layer0", vanillaTextureName);
            textures.addProperty("layer1", vanillaTextureName + "_overlay");
        }
        else textures.addProperty("layer0", vanillaTextureName);

        json.add("textures", textures);

        // overrides
        final JsonArray overrides = new JsonArray();

        // specific items
        switch (material) {
            case SHIELD -> {
                overrides.add(getOverride("blocking", 1, "item/shield_blocking"));
                json.addProperty("gui_light", "front");
                json.add("display", JsonParser.parseString(Settings.SHIELD_DISPLAY.toString()).getAsJsonObject());
            }
            case BOW -> {
                JsonObject pullingPredicate = new JsonObject();
                pullingPredicate.addProperty("pulling", 1);
                overrides.add(getOverride(JsonParser.parseString(pullingPredicate.toString()).getAsJsonObject(), "item/bow_pulling_0"));
                pullingPredicate.addProperty("pull", 0.65);
                overrides.add(getOverride(JsonParser.parseString(pullingPredicate.toString()).getAsJsonObject(), "item/bow_pulling_1"));
                pullingPredicate.addProperty("pull", 0.9);
                overrides.add(getOverride(pullingPredicate, "item/bow_pulling_2"));
                json.add("display", JsonParser.parseString(Settings.BOW_DISPLAY.toString()).getAsJsonObject());
            }
            case CROSSBOW -> {
                JsonObject pullingPredicate = new JsonObject();
                pullingPredicate.addProperty("pulling", 1);
                overrides.add(getOverride(JsonParser.parseString(pullingPredicate.toString()).getAsJsonObject(), "item/crossbow_pulling_0"));
                pullingPredicate.addProperty("pull", 0.65);
                overrides.add(getOverride(JsonParser.parseString(pullingPredicate.toString()).getAsJsonObject(), "item/crossbow_pulling_1"));
                pullingPredicate.addProperty("pull", 0.9);
                overrides.add(getOverride(pullingPredicate, "item/crossbow_pulling_2"));

                JsonObject chargedPredicate = new JsonObject();
                chargedPredicate.addProperty("charged", 1);
                overrides.add(getOverride(JsonParser.parseString(chargedPredicate.toString()).getAsJsonObject(), "item/crossbow_arrow"));
                chargedPredicate.addProperty("firework", 1);
                overrides.add(getOverride(JsonParser.parseString(chargedPredicate.toString()).getAsJsonObject(), "item/crossbow_firework"));

                json.add("display", JsonParser.parseString(Settings.CROSSBOW_DISPLAY.toString()).getAsJsonObject());
            }
        }

        // custom items
        for (final ItemBuilder item : items) {
            OraxenMeta oraxenMeta = item.getOraxenMeta();
            int customModelData = oraxenMeta.getCustomModelData();

            // Skip duplicate
            if (overrides.contains(getOverride("custom_model_data", customModelData, oraxenMeta.getGeneratedModelPath() + oraxenMeta.getModelName())))
                continue;

            overrides.add(getOverride("custom_model_data", customModelData, oraxenMeta.getGeneratedModelPath() + oraxenMeta.getModelName()));
            if (oraxenMeta.hasBlockingModel()) {
                final JsonObject predicate = new JsonObject();
                predicate.addProperty("blocking", 1);
                overrides.add(getOverride(predicate, "custom_model_data", customModelData, oraxenMeta.getBlockingModel()));
            }
            if (oraxenMeta.hasChargedModel()) {
                final JsonObject predicate = new JsonObject();
                predicate.addProperty("charged", 1);
                overrides.add(getOverride(predicate, "custom_model_data", customModelData, oraxenMeta.getChargedModel()));
            }
            if (oraxenMeta.hasFireworkModel()) {
                final JsonObject predicate = new JsonObject();
                predicate.addProperty("charged", 1);
                predicate.addProperty("firework", 1);
                overrides.add(getOverride(predicate, "custom_model_data", customModelData, oraxenMeta.getFireworkModel()));
            }
            if (oraxenMeta.hasPullingModels()) {
                final List<String> pullingModels = oraxenMeta.getPullingModels();
                for (int i = 0; i < pullingModels.size(); i++) {
                    String pullingModel = pullingModels.get(i);
                    final JsonObject predicate = new JsonObject();
                    predicate.addProperty("pulling", 1);
                    // Round to nearest 0.X5 (0.0667 -> 0.65, 0.677 -> 0.7)
                    float pull = Math.min(Utils.customRound((((float) (i + 1) / pullingModels.size())), 0.05f), 0.9f);
                    // First pullingModel should always be used immediatly, thus pull: 0f
                    if (i != 0) predicate.addProperty("pull", pull);
                    overrides.add(getOverride(predicate, "custom_model_data", customModelData, pullingModel));
                }
            }
            if (oraxenMeta.hasCastModel()) {
                final JsonObject predicate = new JsonObject();
                predicate.addProperty("cast", 1);
                overrides.add(getOverride(predicate, "custom_model_data", customModelData, oraxenMeta.getCastModel()));
            }
            if (oraxenMeta.hasDamagedModels()) {
                final List<String> damagedModels = oraxenMeta.getDamagedModels();
                for (int i = 0; i <= damagedModels.size(); i++) {
                    if (i == 0) continue;
                    final JsonObject predicate = new JsonObject();
                    predicate.addProperty("damaged", 1);
                    predicate.addProperty("damage", Math.min((float) i / damagedModels.size(), 0.99f));
                    overrides.add(getOverride(predicate, "custom_model_data", customModelData, damagedModels.get(i - 1)));
                }
            }

        }
        json.add("overrides", overrides);
    }

    public static void generatePullingModels(OraxenMeta oraxenMeta) {
        if (!oraxenMeta.hasPullingTextures()) return;
        for (String texture : oraxenMeta.getPullingTextures()) {
            final JsonObject json = new JsonObject();
            json.addProperty("parent", oraxenMeta.getParentModel());
            final JsonObject textureJson = new JsonObject();
            textureJson.addProperty("layer0", texture);
            json.add("textures", textureJson);
            ResourcePack.writeStringToVirtual(OraxenMeta.getModelPath(Utils.getParentDirs(texture)),
                    Utils.getFileNameOnly(texture) + ".json", json.toString());
        }
    }

    public static void generateChargedModels(OraxenMeta oraxenMeta) {
        if (!oraxenMeta.hasChargedTexture()) return;
        final JsonObject json = new JsonObject();
        json.addProperty("parent", oraxenMeta.getParentModel());
        final JsonObject textureJson = new JsonObject();
        textureJson.addProperty("layer0", oraxenMeta.getChargedTexture());
        json.add("textures", textureJson);
        ResourcePack.writeStringToVirtual(OraxenMeta.getModelPath(Utils.getParentDirs(oraxenMeta.getChargedTexture())),
                Utils.getFileNameOnly(oraxenMeta.getChargedTexture()) + ".json", json.toString());
    }

    public static void generateBlockingModels(OraxenMeta oraxenMeta) {
        if (!oraxenMeta.hasBlockingTexture()) return;
        final JsonObject json = new JsonObject();
        json.addProperty("parent", oraxenMeta.getParentModel());
        final JsonObject textureJson = new JsonObject();
        textureJson.addProperty("layer0", oraxenMeta.getBlockingTexture());
        json.add("textures", textureJson);
        ResourcePack.writeStringToVirtual(OraxenMeta.getModelPath(Utils.getParentDirs(oraxenMeta.getBlockingTexture())),
                Utils.getFileNameOnly(oraxenMeta.getBlockingTexture()) + ".json", json.toString());
    }

    public static void generateFireworkModels(OraxenMeta oraxenMeta) {
        if (!oraxenMeta.hasFireworkTexture()) return;
        final JsonObject json = new JsonObject();
        json.addProperty("parent", oraxenMeta.getParentModel());
        final JsonObject textureJson = new JsonObject();
        textureJson.addProperty("layer0", oraxenMeta.getFireworkTexture());
        json.add("textures", textureJson);
        ResourcePack.writeStringToVirtual(OraxenMeta.getModelPath(Utils.getParentDirs(oraxenMeta.getFireworkTexture())),
                Utils.getFileNameOnly(oraxenMeta.getFireworkTexture()) + ".json", json.toString());
    }

    public static void generateCastModels(OraxenMeta oraxenMeta) {
        if (!oraxenMeta.hasCastTexture()) return;
        final JsonObject json = new JsonObject();
        json.addProperty("parent", oraxenMeta.getParentModel());
        final JsonObject textureJson = new JsonObject();
        textureJson.addProperty("layer0", oraxenMeta.getCastTexture());
        json.add("textures", textureJson);
        ResourcePack.writeStringToVirtual(OraxenMeta.getModelPath(Utils.getParentDirs(oraxenMeta.getCastTexture())),
                Utils.getFileNameOnly(oraxenMeta.getCastTexture()) + ".json", json.toString());
    }

    public static void generateDamageModels(OraxenMeta oraxenMeta) {
        if (!oraxenMeta.hasDamagedTextures()) return;
        for (String texture : oraxenMeta.getDamagedTextures()) {
            final JsonObject json = new JsonObject();
            json.addProperty("parent", oraxenMeta.getParentModel());
            final JsonObject textureJson = new JsonObject();
            textureJson.addProperty("layer0", texture);
            json.add("textures", textureJson);
            ResourcePack.writeStringToVirtual(OraxenMeta.getModelPath(Utils.getParentDirs(texture)),
                    Utils.getFileNameOnly(texture) + ".json", json.toString());
        }
    }

    private JsonObject getOverride(final String property, final int propertyValue, final String model) {
        return getOverride(new JsonObject(), property, propertyValue, model);
    }

    private JsonObject getOverride(final JsonObject predicate, final String property, final int propertyValue, final String model) {
        predicate.addProperty(property, propertyValue);
        return getOverride(predicate, model);
    }

    private JsonObject getOverride(final JsonObject predicate, final String model) {
        final JsonObject override = new JsonObject();
        override.add("predicate", predicate);
        override.addProperty("model", model);
        return override;
    }

    public String getVanillaModelName(final Material material) {
        return getVanillaTextureName(material, true);
    }

    public String getVanillaTextureName(final Material material, final boolean model) {
        if (!model)
            if (material.isBlock()) return "block/" + material.toString().toLowerCase(Locale.ENGLISH);
            else if (material == Material.CROSSBOW) return "item/crossbow_standby";
        return "item/" + material.toString().toLowerCase(Locale.ENGLISH);
    }

    private String getParent(final Material material) {
        if (material.isBlock())
            return "block/cube_all";
        if (Arrays.stream(tools).anyMatch(tool -> material.toString().contains(tool)))
            return "item/handheld";
        if (material == Material.FISHING_ROD)
            return "item/handheld_rod";
        if (material == Material.SHIELD)
            return "builtin/entity";
        return "item/generated";
    }

    public JsonObject toJSON() {
        return json;
    }

}

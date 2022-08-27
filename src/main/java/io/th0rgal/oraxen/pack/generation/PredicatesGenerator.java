package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
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

        // textures
        final ItemMeta exampleMeta = new ItemStack(material).getItemMeta();
        final JsonObject textures = new JsonObject();

        // potions use the overlay as the base layer
        if (exampleMeta instanceof PotionMeta) {
            textures.addProperty("layer0", getVanillaTextureName(material, false) + "_overlay");
        } else
            textures.addProperty("layer0", getVanillaTextureName(material, false));

        // to support colored leather armor
        if (exampleMeta instanceof LeatherArmorMeta)
            textures.addProperty("layer1", getVanillaTextureName(material, false) + "_overlay");
        // to support colored potions
        if (exampleMeta instanceof PotionMeta) {
            textures.addProperty("layer1", getVanillaTextureName(material, false));
        }

        json.add("textures", textures);

        // overrides
        final JsonArray overrides = new JsonArray();

        // specific items
        switch (material) {
            case SHIELD:
                overrides.add(getOverride("blocking", 1, "item/shield_blocking"));
                json.addProperty("gui_light", "front");
                json.add("display", JsonParser.parseString(Settings.SHIELD_DISPLAY.toString()).getAsJsonObject());
                break;

            case BOW:
                JsonObject pullingPredicate = new JsonObject();
                pullingPredicate.addProperty("pulling", 1);
                /*
                    JsonParser.parseString(pullingPredicate.toString()).getAsJsonObject()
                    This is the easiest (but incredibly slow and inefficient) way to clone a JsonObject
                 */
                overrides.add(getOverride(JsonParser.parseString(pullingPredicate.toString()).getAsJsonObject(), "item/bow_pulling_0"));
                pullingPredicate.addProperty("pull", 0.65);
                overrides.add(getOverride(JsonParser.parseString(pullingPredicate.toString()).getAsJsonObject(), "item/bow_pulling_1"));
                pullingPredicate.addProperty("pull", 0.9);
                overrides.add(getOverride(pullingPredicate, "item/bow_pulling_2"));
                json.add("display", JsonParser.parseString(Settings.BOW_DISPLAY.toString()).getAsJsonObject());
                break;


            case CROSSBOW:
                pullingPredicate = new JsonObject();
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
                break;

            default:
                break;
        }

        // custom items
        for (final ItemBuilder item : items) {
            OraxenMeta oraxenMeta = item.getOraxenMeta();
            int customModelData = oraxenMeta.getCustomModelData();

            // Skip duplicate
            if (overrides.contains(getOverride("custom_model_data", customModelData, oraxenMeta.getModelName()))) continue;

            overrides.add(getOverride("custom_model_data", customModelData, oraxenMeta.getModelName()));
            if (oraxenMeta.hasBlockingModel()) {
                final JsonObject predicate = new JsonObject();
                predicate.addProperty("blocking", 1);
                overrides.add(getOverride(predicate, "custom_model_data", customModelData, oraxenMeta.getBlockingModelName()));
            }
            if (oraxenMeta.hasChargedModel()) {
                final JsonObject predicate = new JsonObject();
                predicate.addProperty("charged", 1);
                overrides.add(getOverride(predicate, "custom_model_data", customModelData, oraxenMeta.getChargedModelName()));
            }
            if (oraxenMeta.hasFireworkModel()) {
                final JsonObject predicate = new JsonObject();
                predicate.addProperty("charged", 1);
                predicate.addProperty("firework", 1);
                overrides.add(getOverride(predicate, "custom_model_data", customModelData, oraxenMeta.getFireworkModelName()));
            }
            if (oraxenMeta.hasPullingModels()) {
                final List<String> pullingModels = oraxenMeta.getPullingModels();
                for (float i = 0; i < pullingModels.size(); i++) {
                    final JsonObject predicate = new JsonObject();
                    predicate.addProperty("pulling", 1);
                    if (i != 0)
                        predicate.addProperty("pull", i / pullingModels.size());
                    overrides.add(getOverride(predicate, "custom_model_data", customModelData, pullingModels.get((int) i)));
                }
            }

        }
        json.add("overrides", overrides);
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
        if (material.isBlock())
            return "block/" + material.toString().toLowerCase(Locale.ENGLISH);
        else if (!model && material == Material.CROSSBOW)
            return "item/crossbow_standby";
        else
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

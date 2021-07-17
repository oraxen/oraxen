package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.Arrays;
import java.util.List;

public class PredicatesGenerator {

    private final JsonObject json = new JsonObject();

    public PredicatesGenerator(Material material, List<ItemBuilder> items) {

        // parent
        json.addProperty("parent", getParent(material));

        // textures
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", getVanillaTextureName(material, false));

        // to support colored leather armors + potions
        ItemMeta exampleMeta = new ItemStack(material).getItemMeta();
        if (exampleMeta instanceof LeatherArmorMeta || exampleMeta instanceof PotionMeta)
            textures.addProperty("layer1", getVanillaTextureName(material, false) + "_overlay");

        json.add("textures", textures);

        // overrides
        JsonArray overrides = new JsonArray();

        // specific items
        switch (material) {
            case SHIELD:
                overrides.add(getOverride("blocking", 1, "item/shield_blocking"));
                json.addProperty("gui_light", "front");
                json.add("display", new JsonParser().parse(Settings.SHIELD_DISPLAY.toString()).getAsJsonObject());
                break;

            case BOW:
                JsonParser parser = new JsonParser();
                JsonObject pullingPredicate = new JsonObject();
                pullingPredicate.addProperty("pulling", 1);
                /*
                    parser.parse(pullingPredicate.toString()).getAsJsonObject()
                    This is the easiest (but incredibly slow and inefficient) way to clone a JsonObject
                 */
                overrides.add(getOverride(parser.parse(pullingPredicate.toString()).getAsJsonObject(), "item/bow_pulling_0"));
                pullingPredicate.addProperty("pull", 0.65);
                overrides.add(getOverride(parser.parse(pullingPredicate.toString()).getAsJsonObject(), "item/bow_pulling_1"));
                pullingPredicate.addProperty("pull", 0.9);
                overrides.add(getOverride(pullingPredicate, "item/bow_pulling_2"));
                json.add("display", parser.parse(Settings.BOW_DISPLAY.toString()).getAsJsonObject());
                break;


            case CROSSBOW:
                parser = new JsonParser();
                pullingPredicate = new JsonObject();
                pullingPredicate.addProperty("pulling", 1);
                /*
                    parser.parse(pullingPredicate.toString()).getAsJsonObject()
                    This is the easiest (but incredibly slow and inefficient) way to clone a JsonObject
                 */
                overrides.add(getOverride(parser.parse(pullingPredicate.toString()).getAsJsonObject(), "item/crossbow_pulling_0"));
                pullingPredicate.addProperty("pull", 0.65);
                overrides.add(getOverride(parser.parse(pullingPredicate.toString()).getAsJsonObject(), "item/crossbow_pulling_1"));
                pullingPredicate.addProperty("pull", 0.9);
                overrides.add(getOverride(pullingPredicate, "item/crossbow_pulling_2"));
                json.add("display", parser.parse(Settings.CROSSBOW_DISPLAY.toString()).getAsJsonObject());
                break;
        }

        // custom items
        for (ItemBuilder item : items) {
            overrides
                    .add(getOverride("custom_model_data", item.getOraxenMeta().getCustomModelData(),
                            item.getOraxenMeta().getModelName()));
            if (item.getOraxenMeta().hasBlockingModel()) {
                JsonObject predicate = new JsonObject();
                predicate.addProperty("blocking", 1);
                overrides
                        .add(getOverride(predicate, "custom_model_data", item.getOraxenMeta().getCustomModelData(),
                                item.getOraxenMeta().getBlockingModelName()));
            }
            if (item.getOraxenMeta().hasPullingModels()) {
                List<String> pullingModels = item.getOraxenMeta().getPullingModels();
                for (float i = 0; i < pullingModels.size(); i++) {
                    JsonObject predicate = new JsonObject();
                    predicate.addProperty("pulling", 1);
                    if (i != 0)
                        predicate.addProperty("pull", i / pullingModels.size());
                    overrides
                            .add(getOverride(predicate, "custom_model_data", item.getOraxenMeta().getCustomModelData(),
                                    pullingModels.get((int) i)));
                }
            }

        }
        json.add("overrides", overrides);
    }

    private JsonObject getOverride(String property, int propertyValue, String model) {
        return getOverride(new JsonObject(), property, propertyValue, model);
    }

    private JsonObject getOverride(JsonObject predicate, String property, int propertyValue, String model) {
        predicate.addProperty(property, propertyValue);
        return getOverride(predicate, model);
    }

    private JsonObject getOverride(JsonObject predicate, String model) {
        JsonObject override = new JsonObject();
        override.add("predicate", predicate);
        override.addProperty("model", model);
        return override;
    }

    public String getVanillaModelName(Material material) {
        return getVanillaTextureName(material, true);
    }

    public String getVanillaTextureName(Material material, boolean model) {
        if (material.isBlock())
            return "block/" + material.toString().toLowerCase();
        else if (!model && material == Material.CROSSBOW)
            return "item/crossbow_standby";
        else
            return "item/" + material.toString().toLowerCase();

    }

    // not static here because only instanciated once I think
    private final String[] tools = new String[]{"PICKAXE", "SWORD", "HOE", "AXE", "SHOVEL"};

    private String getParent(Material material) {
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
        return this.json;
    }

}

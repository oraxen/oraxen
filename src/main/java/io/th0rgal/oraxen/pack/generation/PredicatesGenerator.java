package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

        //parent
        json.addProperty("parent", getParent(material));

        //textures
        JsonObject textures = new JsonObject();
        textures.addProperty("layer0", getVanillaTextureName(material));

        //to support colored leather armors + potions
        ItemMeta exampleMeta = new ItemStack(material).getItemMeta();
        if (exampleMeta instanceof LeatherArmorMeta || exampleMeta instanceof PotionMeta)
            textures.addProperty("layer1", getVanillaTextureName(material) + "_overlay");

        json.add("textures", textures);

        //overrides
        JsonArray overrides = new JsonArray();

        //custom items
        for (ItemBuilder item : items) {
            JsonObject override = new JsonObject();
            JsonObject predicate = new JsonObject();
            predicate.addProperty("custom_model_data", item.getPackInfos().getCustomModelData());
            override.add("predicate", predicate);
            override.addProperty("model", item.getPackInfos().getModelName());
            overrides.add(override);
        }

        json.add("overrides", overrides);

    }

    public String getVanillaModelName(Material material) {
        return getVanillaTextureName(material);
    }

    public String getVanillaTextureName(Material material) {
        return "item/" + material
                .toString()
                .toLowerCase();
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
        return "item/generated";
    }

    public JsonObject toJSON() {
        return this.json;
    }

}

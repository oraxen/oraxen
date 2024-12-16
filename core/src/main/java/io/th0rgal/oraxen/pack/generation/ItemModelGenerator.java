package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Material;

public class ItemModelGenerator {

    public static JsonObject generateModel(String parentModel, String texturePath, Material material) {
        JsonObject modelJson = new JsonObject();
        modelJson.addProperty("parent", parentModel);

        JsonObject textureJson = new JsonObject();
        textureJson.addProperty("layer0", texturePath);
        modelJson.add("textures", textureJson);

        // Add tints if needed
        if (needsTinting(material)) {
            modelJson.add("tints", createTintArray(material));
        }

        return modelJson;
    }

    private static JsonArray createTintArray(Material material) {
        JsonArray tints = new JsonArray();

        if (material.name().startsWith("LEATHER_")) {
            // Add dye tint for leather armor
            JsonObject dyeTint = new JsonObject();
            dyeTint.addProperty("type", "minecraft:dye");
            dyeTint.addProperty("default", 10511680); // Default leather color
            tints.add(dyeTint);
        } else if (material == Material.POTION || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION) {
            // Add potion tint
            JsonObject potionTint = new JsonObject();
            potionTint.addProperty("type", "minecraft:potion");
            potionTint.addProperty("default", 16253176); // Default water color
            tints.add(potionTint);
        }

        return tints;
    }

    private static boolean needsTinting(Material material) {
        return material.name().startsWith("LEATHER_")
                || material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }
}
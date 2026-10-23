package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Opaque ARGB defaults for leather item tint sources. */
final class LeatherTintDefaults {
    private static final int VANILLA_LEATHER_COLOR = 0xFFA06540;
    // Keep the existing custom-model RGB, but include fully opaque alpha.
    private static final int CUSTOM_LEATHER_COLOR = 0xFFFCF8F8;

    private LeatherTintDefaults() {
    }

    static int defaultColor(JsonObject model) {
        JsonElement reference = model.get("model");
        if (reference != null && reference.isJsonPrimitive()) {
            String modelPath = reference.getAsString();
            // Minecraft resolves unqualified model references in its own namespace.
            if (!modelPath.contains(":")) {
                modelPath = "minecraft:" + modelPath;
            }
            return switch (modelPath) {
                case "minecraft:item/leather_helmet", "minecraft:item/leather_chestplate",
                     "minecraft:item/leather_leggings", "minecraft:item/leather_boots",
                     "minecraft:item/leather_horse_armor" -> VANILLA_LEATHER_COLOR;
                default -> CUSTOM_LEATHER_COLOR;
            };
        }
        return CUSTOM_LEATHER_COLOR;
    }
}

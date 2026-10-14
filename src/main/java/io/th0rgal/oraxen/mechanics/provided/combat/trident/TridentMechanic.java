package io.th0rgal.oraxen.mechanics.provided.combat.trident;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.mechanics.Mechanic;
import io.th0rgal.oraxen.mechanics.MechanicFactory;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ItemDisplay;

import java.util.Locale;

public class TridentMechanic extends Mechanic {
    private final String model;
    private final String chargingModel;
    private final String thrownModel;
    private final ItemDisplay.ItemDisplayTransform transform;
    private final String throwSound;
    private final String hitSound;
    private final String groundSound;
    private final String returnSound;

    public TridentMechanic(MechanicFactory factory, ConfigurationSection section) {
        super(factory, section, item -> item.setType(Material.TRIDENT)
                .setItemModel(new NamespacedKey("oraxen_tridents", section.getParent().getParent().getName() + "_held")));
        model = modelPath(section, "appearance.model");
        chargingModel = section.contains("appearance.charging-model")
                ? modelPath(section, "appearance.charging-model") : model;
        thrownModel = modelPath(section, "appearance.thrown-model");
        transform = ItemDisplay.ItemDisplayTransform.valueOf(
                section.getString("appearance.transform", "NONE").toUpperCase(Locale.ROOT));
        throwSound = soundKey(section, "sounds.throw", "minecraft:item.trident.throw");
        hitSound = soundKey(section, "sounds.hit", "minecraft:item.trident.hit");
        groundSound = soundKey(section, "sounds.hit-ground", hitSound);
        returnSound = soundKey(section, "sounds.return", throwSound);
    }

    private static String soundKey(ConfigurationSection section, String property, String fallback) {
        String value = section.getString(property, fallback);
        if (NamespacedKey.fromString(value) == null) throw new IllegalArgumentException("Invalid sound " + value);
        return value;
    }

    private static String modelPath(ConfigurationSection section, String property) {
        String value = section.getString(property);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(property + " is required");
        if (value.endsWith(".json")) value = value.substring(0, value.length() - 5);
        NamespacedKey key = NamespacedKey.fromString(value);
        if (key == null) throw new IllegalArgumentException("Invalid model path " + value);
        return key.toString();
    }

    public String getModel() { return model; }
    public String getChargingModel() { return chargingModel; }
    public String getThrownModel() { return thrownModel; }
    public ItemDisplay.ItemDisplayTransform getTransform() { return transform; }
    public NamespacedKey getThrownItemModel() { return new NamespacedKey("oraxen_tridents", getItemID()); }
    public String getThrowSound() { return throwSound; }
    public String getHitSound() { return hitSound; }
    public String getGroundSound() { return groundSound; }
    public String getReturnSound() { return returnSound; }

    public String soundFor(String vanillaSound) {
        return switch (vanillaSound) {
            case "item.trident.throw" -> throwSound;
            case "item.trident.hit" -> hitSound;
            case "item.trident.hit_ground" -> groundSound;
            case "item.trident.return" -> returnSound;
            default -> null;
        };
    }

    public JsonObject modelDefinition(boolean thrown) {
        JsonObject root = new JsonObject();
        if (!thrown && !chargingModel.equals(model)) {
            JsonObject condition = new JsonObject();
            condition.addProperty("type", "minecraft:condition");
            condition.addProperty("property", "minecraft:using_item");
            condition.add("on_true", modelObject(chargingModel));
            condition.add("on_false", modelObject(model));
            root.add("model", condition);
        } else {
            root.add("model", modelObject(thrown ? thrownModel : model));
        }
        return root;
    }

    private static JsonObject modelObject(String path) {
        JsonObject modelObject = new JsonObject();
        modelObject.addProperty("type", "minecraft:model");
        modelObject.addProperty("model", path);
        return modelObject;
    }
}

package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.jetbrains.annotations.Nullable;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class ComponentArmorModels {
    public void generatePackFiles(List<VirtualFile> output) {
        Set<String> armorPrefixes = armorPrefixes(output);
        writeArmorModels(output, armorPrefixes);
        copyArmorLayerTextures(output);
        checkOraxenArmorItems(armorPrefixes);
    }

    private void writeArmorModels(List<VirtualFile> output, Set<String> armorPrefixes) {
        for (String armorprefix : armorPrefixes) {
            JsonObject armorModel = Json.createObjectBuilder().add("texture", "oraxen:" + armorprefix).build();
            JsonArray armorModelArray = Json.createArrayBuilder().add(armorModel).build();
            JsonObject equipmentModel = Json.createObjectBuilder().add("layers", Json.createObjectBuilder()
                    .add("humanoid", armorModelArray)
                    .add("humanoid_leggings", armorModelArray).build()
            ).build();

            InputStream equipmentStream = new ByteArrayInputStream(equipmentModel.toString().getBytes());
            output.add(new VirtualFile("assets/oraxen/models/equipment", armorprefix + ".json", equipmentStream));
        }
    }

    private void copyArmorLayerTextures(List<VirtualFile> output) {
        for (VirtualFile virtualFile : output) {
            String path = virtualFile.getPath();
            String armorFolder = path.endsWith("_armor_layer_1.png") ? "humanoid" : "humanoid_leggings";
            String armorPrefix = armorPrefix(virtualFile);
            if (armorPrefix.isEmpty()) continue;

            String armorPath = "assets/oraxen/textures/entity/equipment/%s/%s.png".formatted(armorFolder, armorPrefix);
            virtualFile.setPath(armorPath);
        }
    }

    private void checkOraxenArmorItems(Set<String> armorPrefixes) {
        // No need to log for all 4 armor pieces, so skip to minimise log spam
        List<String> skippedArmorType = new ArrayList<>();
        for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
            String itemId = entry.getKey();
            ItemBuilder itemBuilder = entry.getValue();
            ItemStack itemStack = itemBuilder.getReferenceClone();
            String armorPrefix = StringUtils.substringBeforeLast(itemId, "_");
            EquipmentSlot slot = slotFromItem(itemId);

            if (!armorPrefixes.contains(armorPrefix) || skippedArmorType.contains(armorPrefix) || slot == null) continue;
            if (itemStack == null || !itemStack.hasItemMeta()) continue;

            if (!itemBuilder.hasEquippableComponent() || itemBuilder.getEquippableComponent().getModel() == null) {
                if (!Settings.CUSTOM_ARMOR_COMPONENT_ASSIGN.toBool()) {
                    Logs.logWarning("Item " + itemId + " does not have an equippable-component configured properly.");
                    Logs.logWarning("Oraxen has been configured to use Components for custom-armor due to " + Settings.CUSTOM_ARMOR_TYPE.getPath() + " setting");
                    Logs.logWarning("Custom Armor will not work unless an equippable-component is set.", true);
                    skippedArmorType.add(armorPrefix);
                } else {
                    EquippableComponent component = Optional.ofNullable(itemBuilder.getEquippableComponent()).orElse(new ItemStack(Material.PAPER).getItemMeta().getEquippable());
                    NamespacedKey modelKey = NamespacedKey.fromString("oraxen:" + armorPrefix);
                    if (component.getModel() == null) component.setModel(modelKey);
                    component.setSlot(slotFromItem(itemId));
                    itemBuilder.setEquippableComponent(component);

                    itemBuilder.save();
                    Logs.logWarning("Item " + itemId + " does not have an equippable-component set.");
                    Logs.logInfo("Configured Components.equippable.model to %s for %s".formatted(modelKey.toString(), itemId), true);
                }
            }
        }
    }

    @Nullable
    private EquipmentSlot slotFromItem(String itemId) {
        return switch (StringUtils.substringAfterLast(itemId, "_").toUpperCase(Locale.ENGLISH)) {
            case "HELMET" -> EquipmentSlot.HEAD;
            case "CHESTPLATE" -> EquipmentSlot.CHEST;
            case "LEGGINGS" -> EquipmentSlot.LEGS;
            case "BOOTS" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private Set<String> armorPrefixes(List<VirtualFile> output) {
        return output.stream().map(this::armorPrefix).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }

    private String armorPrefix(VirtualFile virtualFile) {
        return virtualFile.getPath().endsWith("_armor_layer_1.png")
                ? StringUtils.substringAfterLast(StringUtils.substringBefore(virtualFile.getPath(), "_armor_layer_1.png"), "/")
                : virtualFile.getPath().endsWith("_armor_layer_2.png")
                ? StringUtils.substringAfterLast(StringUtils.substringBefore(virtualFile.getPath(), "_armor_layer_2.png"), "/")
                : "";
    }
}

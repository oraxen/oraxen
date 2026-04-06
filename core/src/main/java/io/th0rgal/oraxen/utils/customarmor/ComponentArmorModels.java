package io.th0rgal.oraxen.utils.customarmor;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.utils.VersionUtil;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ComponentArmorModels {
    public void generatePackFiles(List<VirtualFile> output) {
        Set<String> armorPrefixes = armorPrefixes(output);
        Set<String> elytraPrefixes = elytraPrefixes(output);
        writeArmorModels(output, armorPrefixes, elytraPrefixes);
        copyArmorLayerTextures(output);
        checkOraxenArmorItems(armorPrefixes, elytraPrefixes);
    }

    private void writeArmorModels(List<VirtualFile> output, Set<String> armorPrefixes, Set<String> elytraPrefixes) {
        for (String armorprefix : armorPrefixes) {
            JsonObject armorModel = Json.createObjectBuilder().add("texture", "oraxen:" + armorprefix).build();
            JsonArray armorModelArray = Json.createArrayBuilder().add(armorModel).build();
            JsonObject equipmentModel = Json.createObjectBuilder()
                    .add("layers", Json.createObjectBuilder()
                            .add("humanoid", armorModelArray)
                            .add("humanoid_leggings", armorModelArray)
                            .build())
                    .build();

            InputStream equipmentStream = new ByteArrayInputStream(
                    equipmentModel.toString().getBytes(StandardCharsets.UTF_8));
            output.add(new VirtualFile(
                    VersionUtil.atOrAbove("1.21.4") ? "assets/oraxen/equipment" : "assets/oraxen/models/equipment",
                    armorprefix + ".json", equipmentStream));

            if (!elytraPrefixes.contains(armorprefix))
                continue;

            JsonObject elytraModel = Json.createObjectBuilder()
                    .add("layers", Json.createObjectBuilder()
                            .add("wings", armorModelArray)
                            .build())
                    .build();
            InputStream elytraStream = new ByteArrayInputStream(
                    elytraModel.toString().getBytes(StandardCharsets.UTF_8));
            output.add(new VirtualFile(
                    VersionUtil.atOrAbove("1.21.4") ? "assets/oraxen/equipment" : "assets/oraxen/models/equipment",
                    armorprefix + "_elytra.json", elytraStream));
        }
    }

    private void copyArmorLayerTextures(List<VirtualFile> output) {
        for (VirtualFile virtualFile : output) {
            String path = virtualFile.getPath();
            String armorFolder = path.endsWith("_armor_layer_1.png") ? "humanoid"
                    : path.endsWith("_armor_layer_2.png") ? "humanoid_leggings"
                            : path.endsWith("_elytra.png") ? "wings" : "";
            String armorPrefix = armorPrefix(virtualFile);
            if (armorPrefix.isEmpty() || armorFolder.isEmpty())
                continue;

            String armorPath = "assets/oraxen/textures/entity/equipment/%s/%s.png".formatted(armorFolder, armorPrefix);
            virtualFile.setPath(armorPath);
        }
    }

    private void checkOraxenArmorItems(Set<String> armorPrefixes, Set<String> elytraPrefixes) {
        // No need to log for all 4 armor pieces, so skip to minimise log spam
        List<String> skippedArmorType = new ArrayList<>();
        for (Map.Entry<String, ItemBuilder> entry : OraxenItems.getEntries()) {
            String itemId = entry.getKey();
            ItemBuilder itemBuilder = entry.getValue();
            ItemStack itemStack = itemBuilder.getReferenceClone();
            String armorPrefix = StringUtils.substringBeforeLast(itemId, "_");

            if (itemStack == null || !itemStack.hasItemMeta())
                continue;

            EquipmentSlot slot = slotFromItem(itemId, itemStack);
            boolean isElytra = isElytraItem(itemId, itemStack);

            if (!armorPrefixes.contains(armorPrefix) || skippedArmorType.contains(armorPrefix) || slot == null)
                continue;

            if (!itemBuilder.hasEquippableComponent() || itemBuilder.getEquippableComponent().getModel() == null) {
                if (!Settings.CUSTOM_ARMOR_COMPONENT_ASSIGN.toBool()) {
                    Logs.logWarning("Item " + itemId + " does not have an equippable-component configured properly.");
                    Logs.logWarning("Oraxen has been configured to use Components for custom-armor due to "
                            + Settings.CUSTOM_ARMOR_TYPE.getPath() + " setting");
                    Logs.logWarning("Custom Armor will not work unless an equippable-component is set.", true);
                    skippedArmorType.add(armorPrefix);
                } else {
                    EquippableComponent component = Optional.ofNullable(itemBuilder.getEquippableComponent())
                            .orElse(new ItemStack(Material.PAPER).getItemMeta().getEquippable());
                    String modelId = isElytra && elytraPrefixes.contains(armorPrefix) ? armorPrefix + "_elytra"
                            : armorPrefix;
                    NamespacedKey modelKey = NamespacedKey.fromString("oraxen:" + modelId);
                    if (component.getModel() == null)
                        component.setModel(modelKey);
                    component.setSlot(slot);
                    itemBuilder.setEquippableComponent(component);

                    itemBuilder.save();
                    Logs.logWarning("Item " + itemId + " does not have an equippable-component set.");
                    Logs.logInfo("Configured Components.equippable.model to %s for %s".formatted(modelKey.toString(),
                            itemId), true);
                }
            }
        }
    }

    @Nullable
    private EquipmentSlot slotFromItem(String itemId, @Nullable ItemStack itemStack) {
        if (isElytraItem(itemId, itemStack))
            return EquipmentSlot.CHEST;
        return switch (StringUtils.substringAfterLast(itemId, "_").toUpperCase(Locale.ENGLISH)) {
            case "HELMET" -> EquipmentSlot.HEAD;
            case "CHESTPLATE" -> EquipmentSlot.CHEST;
            case "LEGGINGS" -> EquipmentSlot.LEGS;
            case "BOOTS" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private boolean isElytraItem(String itemId, @Nullable ItemStack itemStack) {
        if (itemId.toLowerCase(Locale.ENGLISH).endsWith("_elytra"))
            return true;
        if (itemStack == null || !itemStack.hasItemMeta())
            return false;
        return itemStack.getType() == Material.ELYTRA || itemStack.getItemMeta().isGlider();
    }

    private Set<String> armorPrefixes(List<VirtualFile> output) {
        return output.stream().map(this::armorPrefix).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }

    private Set<String> elytraPrefixes(List<VirtualFile> output) {
        return output.stream()
                .filter(file -> file.getPath().endsWith("_elytra.png"))
                .map(this::armorPrefix)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    private String armorPrefix(VirtualFile virtualFile) {
        return virtualFile.getPath().endsWith("_armor_layer_1.png")
                ? StringUtils.substringAfterLast(
                        StringUtils.substringBefore(virtualFile.getPath(), "_armor_layer_1.png"), "/")
                : virtualFile.getPath().endsWith("_armor_layer_2.png")
                        ? StringUtils.substringAfterLast(
                                StringUtils.substringBefore(virtualFile.getPath(), "_armor_layer_2.png"), "/")
                        : virtualFile.getPath().endsWith("_elytra.png")
                                ? StringUtils.substringAfterLast(
                                        StringUtils.substringBefore(virtualFile.getPath(), "_elytra.png"), "/")
                        : "";
    }
}

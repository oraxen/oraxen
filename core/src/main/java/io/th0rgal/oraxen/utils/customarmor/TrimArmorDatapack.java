package io.th0rgal.oraxen.utils.customarmor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.packs.DataPack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class TrimArmorDatapack {
    private static final World defaultWorld = Bukkit.getWorlds().get(0);
    public static final Key datapackKey = Key.key("minecraft:file/oraxen_custom_armor");
    private static final File customArmorDatapack = defaultWorld.getWorldFolder().toPath()
            .resolve("datapacks/oraxen_custom_armor").toFile();
    private final boolean isFirstInstall;
    private final boolean datapackEnabled;
    private final JsonObject datapackMeta = new JsonObject();
    private final JsonObject sourcesObject = new JsonObject();

    public TrimArmorDatapack() {
        JsonObject data = new JsonObject();
        data.addProperty("description", "Datapack for Oraxens Custom Armor trims");
        data.addProperty("pack_format", 26);
        datapackMeta.add("pack", data);
        trimSourcesObject();

        this.isFirstInstall = isFirstInstall();
        this.datapackEnabled = isDatapackEnabled();
    }

    public static void clearOldDataPacks() {
        try {
            FileUtils.deleteDirectory(customArmorDatapack);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateTrimAssets(List<VirtualFile> output) {
        Set<String> armorPrefixes = armorPrefixes(output);
        customArmorDatapack.toPath().resolve("data").toFile().mkdirs();
        writeMCMeta(customArmorDatapack);
        writeVanillaTrimPattern(customArmorDatapack);
        writeCustomTrimPatterns(customArmorDatapack, armorPrefixes);
        writeTrimAtlas(output, armorPrefixes);
        copyArmorLayerTextures(output);

        if (isFirstInstall) {
            Logs.logError("Oraxen's Custom-Armor datapack could not be found...");
            Logs.logWarning("The first time CustomArmor.armor_type is set to TRIMS in settings.yml");
            Logs.logWarning("you need to restart your server so that the DataPack is enabled...");
            Logs.logWarning("Custom-Armor will not work, please restart your server once!", true);
        } else if (!datapackEnabled) {
            Logs.logError("Oraxen's Custom-Armor datapack is not enabled...");
            Logs.logWarning("Custom-Armor will not work, please restart your server!", true);
        } else
            checkOraxenArmorItems();
    }

    private void writeVanillaTrimPattern(File datapack) {
        File vanillaArmorJson = datapack.toPath().resolve("data/minecraft/trim_pattern/"
                + Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase(Locale.ROOT) + ".json").toFile();
        vanillaArmorJson.getParentFile().mkdirs();
        JsonObject vanillaTrimPattern = new JsonObject();
        JsonObject description = new JsonObject();
        description.addProperty("translate",
                "trim_pattern.minecraft." + Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase(Locale.ROOT));
        vanillaTrimPattern.add("description", description);
        vanillaTrimPattern.addProperty("asset_id",
                "minecraft:" + Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase(Locale.ROOT));
        vanillaTrimPattern.addProperty("template_item", "minecraft:debug_stick");

        try {
            vanillaArmorJson.createNewFile();
            FileUtils.writeStringToFile(vanillaArmorJson, vanillaTrimPattern.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeCustomTrimPatterns(File datapack, Set<String> armorPrefixes) {
        for (String armorPrefix : armorPrefixes) {
            File armorJson = datapack.toPath().resolve("data/oraxen/trim_pattern/" + armorPrefix + ".json").toFile();
            armorJson.getParentFile().mkdirs();
            JsonObject trimPattern = new JsonObject();
            JsonObject description = new JsonObject();
            description.addProperty("translate", "trim_pattern.oraxen." + armorPrefix);
            trimPattern.add("description", description);
            trimPattern.addProperty("asset_id", "oraxen:" + armorPrefix);
            trimPattern.addProperty("template_item", "minecraft:debug_stick");
            try {
                armorJson.createNewFile();
                FileUtils.writeStringToFile(armorJson, trimPattern.toString(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeTrimAtlas(List<VirtualFile> output, Set<String> armorPrefixes) {
        Optional<VirtualFile> trimsAtlas = output.stream()
                .filter(v -> v.getPath().equals("assets/minecraft/atlases/armor_trims.json")).findFirst();

        // If for some reason the atlas exists already, we append to it
        if (trimsAtlas.isPresent()) {
            try {
                String trimsAtlasContent = IOUtils.toString(trimsAtlas.get().getInputStream(), StandardCharsets.UTF_8);
                JsonObject atlasJson = (JsonObject) JsonParser.parseString(trimsAtlasContent);
                JsonArray sourcesArray = atlasJson.getAsJsonArray("sources");
                for (JsonElement element : sourcesArray) {
                    JsonObject sourceObject = element.getAsJsonObject();

                    // Get the textures array
                    JsonArray texturesArray = sourceObject.getAsJsonArray("textures");
                    for (String armorPrefix : armorPrefixes) {
                        texturesArray.add("oraxen:trims/models/armor/" + armorPrefix);
                        texturesArray.add("oraxen:trims/models/armor/" + armorPrefix + "_leggings");
                    }
                    texturesArray.add("minecraft:trims/models/armor/"
                            + Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase(Locale.ROOT));
                    texturesArray.add("minecraft:trims/models/armor/"
                            + Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase(Locale.ROOT) + "_leggings");

                    sourceObject.remove("textures");
                    sourceObject.add("textures", texturesArray);
                }

                atlasJson.remove("sources");
                atlasJson.add("sources", sourcesArray);
                trimsAtlas.get().setInputStream(IOUtils.toInputStream(atlasJson.toString(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            JsonObject atlasJson = new JsonObject();
            JsonArray sourcesArray = new JsonArray();
            JsonArray texturesArray = new JsonArray();

            for (String armorPrefix : armorPrefixes) {
                texturesArray.add("oraxen:trims/models/armor/" + armorPrefix);
                texturesArray.add("oraxen:trims/models/armor/" + armorPrefix + "_leggings");
            }
            texturesArray.add("minecraft:trims/models/armor/"
                    + Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase(Locale.ROOT));
            texturesArray.add("minecraft:trims/models/armor/"
                    + Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase(Locale.ROOT) + "_leggings");

            sourcesObject.add("textures", texturesArray);
            sourcesArray.add(sourcesObject);
            atlasJson.add("sources", sourcesArray);

            output.add(new VirtualFile("assets/minecraft/atlases", "armor_trims.json",
                    IOUtils.toInputStream(atlasJson.toString(), StandardCharsets.UTF_8)));
        }
    }

    private void copyArmorLayerTextures(List<VirtualFile> output) {
        String armorPath = "assets/minecraft/textures/models/armor/";
        String vanillaTrimPath = "assets/minecraft/textures/trims/models/armor/";
        String oraxenTrimPath = "assets/oraxen/textures/trims/models/armor/";
        String material = Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase(Locale.ROOT);

        for (VirtualFile virtualFile : output) {
            String path = virtualFile.getPath();
            String armorPrefix = armorPrefix(virtualFile);
            if (path.endsWith("_armor_layer_1.png")) {
                virtualFile.setPath(oraxenTrimPath + armorPrefix + ".png");
            } else if (path.endsWith("_armor_layer_2.png")) {
                virtualFile.setPath(oraxenTrimPath + armorPrefix + "_leggings.png");
            }

            if (path.equals(armorPath + material + "_layer_1.png")) {
                virtualFile.setPath(vanillaTrimPath + material + ".png");
            } else if (path.equals(armorPath + material + "_layer_2.png")) {
                virtualFile.setPath(vanillaTrimPath + material + "_leggings.png");
            }
        }

        String resourcePath = "pack/textures/models/armor/";
        if (output.stream().noneMatch(v -> v.getPath().equals(vanillaTrimPath + material + ".png")))
            output.add(new VirtualFile(vanillaTrimPath, material + ".png",
                    OraxenPlugin.get().getResource(resourcePath + material + "_layer_1.png")));
        if (output.stream().noneMatch(v -> v.getPath().equals(vanillaTrimPath + material + "_leggings.png")))
            output.add(new VirtualFile(vanillaTrimPath, material + "_leggings.png",
                    OraxenPlugin.get().getResource(resourcePath + material + "_layer_2.png")));

        output.add(new VirtualFile(armorPath, material + "_layer_1.png",
                OraxenPlugin.get().getResource(resourcePath + "transparent_layer_1.png")));
        output.add(new VirtualFile(armorPath, material + "_layer_2.png",
                OraxenPlugin.get().getResource(resourcePath + "transparent_layer_2.png")));
    }

    private void writeMCMeta(File datapack) {
        try {
            File packMeta = datapack.toPath().resolve("pack.mcmeta").toFile();
            packMeta.createNewFile();
            FileUtils.writeStringToFile(packMeta, datapackMeta.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkOraxenArmorItems() {
        // No need to log for all 4 armor pieces, so skip to minimise log spam
        List<String> skippedArmorType = new ArrayList<>();
        for (ItemBuilder itemBuilder : OraxenItems.getItems()) {
            String itemID = OraxenItems.getIdByItem(itemBuilder);
            ItemStack itemStack = itemBuilder.build();
            String armorPrefix = StringUtils.substringBeforeLast(itemID, "_");
            boolean changed = false;

            if (skippedArmorType.contains(armorPrefix))
                continue;
            if (itemStack == null || !Tag.ITEMS_TRIMMABLE_ARMOR.isTagged(itemBuilder.getType()))
                continue;
            if (!itemStack.hasItemMeta() || !(itemStack.getItemMeta() instanceof ArmorMeta))
                continue;
            if (!itemStack.getType().name().toUpperCase()
                    .startsWith(Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toUpperCase()))
                continue;

            if (!itemBuilder.getItemFlags().contains(ItemFlag.HIDE_ARMOR_TRIM)) {
                Logs.logWarning("Item " + itemID + " does not have the HIDE_ARMOR_TRIM flag set.");

                if (Settings.CUSTOM_ARMOR_TRIMS_ASSIGN.toBool()) {
                    itemBuilder.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
                    changed = true;
                    if (Settings.DEBUG.toBool())
                        Logs.logInfo("Assigned HIDE_ARMOR_TRIM flag to " + itemID, true);
                } else
                    Logs.logWarning("Custom Armors are recommended to have the HIDE_ARMOR_TRIM flag set.", true);
            }
            if (!itemBuilder.hasTrimPattern() && CustomArmorType.getSetting() == CustomArmorType.TRIMS) {

                TrimPattern trimPattern = Registry.TRIM_PATTERN.get(NamespacedKey.fromString("oraxen:" + armorPrefix));
                if (trimPattern == null) {
                    Logs.logError("Could not get trim-pattern for " + itemID + ": oraxen:" + armorPrefix);
                    Logs.logWarning("Ensure that the  DataPack is enabled `/datapack list` and restart your server");
                    skippedArmorType.add(armorPrefix);
                } else if (!Settings.CUSTOM_ARMOR_TRIMS_ASSIGN.toBool()) {
                    Logs.logWarning("Item " + itemID + " does not have a trim pattern set.");
                    Logs.logWarning("Oraxen has been configured to use Trims for custom-armor due to "
                            + Settings.CUSTOM_ARMOR_TYPE.getPath() + " setting");
                    Logs.logWarning("Custom Armor will not work unless a trim pattern is set.", true);
                    skippedArmorType.add(armorPrefix);
                } else {
                    itemBuilder.setTrimPattern(trimPattern.key());
                    changed = true;
                    Logs.logWarning("Item " + itemID + " does not have a trim pattern set.");
                    Logs.logInfo("Assigned trim pattern " + trimPattern.key().asString() + " to " + itemID, true);
                }
            }

            if (changed)
                itemBuilder.save();
        }
    }

    private void trimSourcesObject() {
        JsonObject permutationObject = new JsonObject();
        permutationObject.addProperty("quartz", "trims/color_palettes/quartz");
        permutationObject.addProperty("iron", "trims/color_palettes/iron");
        permutationObject.addProperty("gold", "trims/color_palettes/gold");
        permutationObject.addProperty("diamond", "trims/color_palettes/diamond");
        permutationObject.addProperty("netherite", "trims/color_palettes/netherite");
        permutationObject.addProperty("redstone", "trims/color_palettes/redstone");
        permutationObject.addProperty("copper", "trims/color_palettes/copper");
        permutationObject.addProperty("emerald", "trims/color_palettes/emerald");
        permutationObject.addProperty("lapis", "trims/color_palettes/lapis");
        permutationObject.addProperty("amethyst", "trims/color_palettes/amethyst");
        sourcesObject.addProperty("type", "minecraft:paletted_permutations");
        sourcesObject.addProperty("palette_key", "trims/color_palettes/trim_palette");
        sourcesObject.add("permutations", permutationObject);
    }

    private Set<String> armorPrefixes(List<VirtualFile> output) {
        return output.stream().map(this::armorPrefix).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }

    private String armorPrefix(VirtualFile virtualFile) {
        return virtualFile.getPath().endsWith("_armor_layer_1.png")
                ? StringUtils.substringAfterLast(
                        StringUtils.substringBefore(virtualFile.getPath(), "_armor_layer_1.png"), "/")
                : virtualFile.getPath().endsWith("_armor_layer_2.png")
                        ? StringUtils.substringAfterLast(
                                StringUtils.substringBefore(virtualFile.getPath(), "_armor_layer_2.png"), "/")
                        : "";
    }

    private boolean isFirstInstall() {
        return Bukkit.getDataPackManager().getDataPacks().stream().filter(d -> d.getKey() != null)
                .noneMatch(d -> datapackKey.equals(Key.key(d.getKey().toString())));
    }

    private boolean isDatapackEnabled() {
        for (DataPack dataPack : Bukkit.getDataPackManager().getEnabledDataPacks(defaultWorld)) {
            if (dataPack.getKey() == null)
                continue;
            if (dataPack.getKey().equals(datapackKey))
                return true;
        }
        for (DataPack dataPack : Bukkit.getDataPackManager().getDisabledDataPacks(defaultWorld)) {
            if (dataPack.getKey() == null)
                continue;
            if (dataPack.getKey().equals(datapackKey))
                return true;
        }

        return false;
    }
}

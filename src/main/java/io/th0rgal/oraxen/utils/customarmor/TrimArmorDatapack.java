package io.th0rgal.oraxen.utils.customarmor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.configs.Message;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.pack.generation.OraxenDatapack;
import io.th0rgal.oraxen.utils.VirtualFile;
import io.th0rgal.oraxen.utils.logs.Logs;
import io.th0rgal.oraxen.utils.VersionUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class TrimArmorDatapack extends OraxenDatapack {
    public static final Key DATAPACK_KEY = Key.key("minecraft:file/oraxen_trim_armor");
    private final JsonObject sourcesObject = new JsonObject();

    public TrimArmorDatapack() {
        super("oraxen_trim_armor",
                "Datapack for Oraxens Custom Armor trims",
                26);
        trimSourcesObject();
    }

    @Override
    protected Key getDatapackKey() {
        return DATAPACK_KEY;
    }

    @Override
    public void generateAssets(List<VirtualFile> output) {
        Set<String> armorPrefixes = armorPrefixes(output);
        datapackFolder.toPath().resolve("data").toFile().mkdirs();
        writeMCMeta();
        writeVanillaTrimPattern();
        writeCustomTrimPatterns(armorPrefixes);
        writeTrimAtlas(output, armorPrefixes);
        copyArmorLayerTextures(output);

        if (isFirstInstall || !datapackEnabled) {
            Message.DATAPACK_GENERATED.send(Bukkit.getConsoleSender(),
                    TagResolver.resolver(Placeholder.parsed("datapack_name", "Custom-Armor")));
        } else {
            checkOraxenArmorItems();
        }

        enableDatapack(CustomArmorType.getSetting() == CustomArmorType.TRIMS);
    }

    private void writeVanillaTrimPattern() {
        File vanillaArmorJson = datapackFolder.toPath().resolve("data/minecraft/trim_pattern/"
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

    private void writeCustomTrimPatterns(Set<String> armorPrefixes) {
        for (String armorPrefix : armorPrefixes) {
            File armorJson = datapackFolder.toPath().resolve("data/oraxen/trim_pattern/" + armorPrefix + ".json")
                    .toFile();
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

    private void checkOraxenArmorItems() {
        // No need to log for all 4 armor pieces, so skip to minimise log spam
        List<String> skippedArmorType = new ArrayList<>();
        for (ItemBuilder itemBuilder : OraxenItems.getItems()) {
            String itemID = OraxenItems.getIdByItem(itemBuilder);
            ItemStack itemStack = itemBuilder.build();
            String armorPrefix = StringUtils.substringBeforeLast(itemID, "_");

            if (skippedArmorType.contains(armorPrefix) || !isCustomTrimArmor(itemBuilder, itemStack))
                continue;

            boolean changed = ensureHideArmorTrimFlag(itemID, itemBuilder);
            changed |= ensureTrimPattern(itemID, armorPrefix, itemBuilder, skippedArmorType);

            if (changed)
                itemBuilder.save();
        }
    }

    private boolean isCustomTrimArmor(ItemBuilder itemBuilder, ItemStack itemStack) {
        if (itemStack == null || !Tag.ITEMS_TRIMMABLE_ARMOR.isTagged(itemBuilder.getType()))
            return false;
        if (!itemStack.hasItemMeta() || !(itemStack.getItemMeta() instanceof ArmorMeta))
            return false;
        return itemStack.getType().name().toUpperCase()
                .startsWith(Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toUpperCase());
    }

    private boolean ensureHideArmorTrimFlag(String itemID, ItemBuilder itemBuilder) {
        if (itemBuilder.getItemFlags().contains(ItemFlag.HIDE_ARMOR_TRIM))
            return false;

        Logs.logWarning("Item " + itemID + " does not have the HIDE_ARMOR_TRIM flag set.");
        if (!Settings.CUSTOM_ARMOR_TRIMS_ASSIGN.toBool()) {
            Logs.logWarning("Custom Armors are recommended to have the HIDE_ARMOR_TRIM flag set.", true);
            return false;
        }

        itemBuilder.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
        if (Settings.DEBUG.toBool())
            Logs.logInfo("Assigned HIDE_ARMOR_TRIM flag to " + itemID, true);
        return true;
    }

    private boolean ensureTrimPattern(String itemID, String armorPrefix, ItemBuilder itemBuilder, List<String> skippedArmorType) {
        if (itemBuilder.hasTrimPattern() || CustomArmorType.getSetting() != CustomArmorType.TRIMS)
            return false;

        TrimPattern trimPattern = getTrimPattern(armorPrefix, skippedArmorType);
        if (skippedArmorType.contains(armorPrefix))
            return false;
        if (trimPattern == null) {
            Logs.logError("Could not get trim-pattern for " + itemID + ": oraxen:" + armorPrefix);
            Logs.logWarning("Ensure that the DataPack is enabled `/datapack list` and restart your server");
            skippedArmorType.add(armorPrefix);
            return false;
        }

        if (!Settings.CUSTOM_ARMOR_TRIMS_ASSIGN.toBool()) {
            warnMissingTrimPattern(itemID, armorPrefix, skippedArmorType);
            return false;
        }

        itemBuilder.setTrimPattern(trimPattern.key());
        Logs.logWarning("Item " + itemID + " does not have a trim pattern set.");
        Logs.logInfo("Assigned trim pattern " + trimPattern.key().asString() + " to " + itemID, true);
        return true;
    }

    private TrimPattern getTrimPattern(String armorPrefix, List<String> skippedArmorType) {
        try {
            return VersionUtil.isPaperServer()
                    ? Registry.TRIM_PATTERN.get(NamespacedKey.fromString("oraxen:" + armorPrefix))
                    : null;
        } catch (NoSuchMethodError e) {
            Logs.logWarning("Registry.TRIM_PATTERN.get is not available in your server version.");
            Logs.logWarning("Custom armor with trim patterns requires PaperMC or compatible fork.");
            skippedArmorType.add(armorPrefix);
            return null;
        }
    }

    private void warnMissingTrimPattern(String itemID, String armorPrefix, List<String> skippedArmorType) {
        Logs.logWarning("Item " + itemID + " does not have a trim pattern set.");
        Logs.logWarning("Oraxen has been configured to use Trims for custom-armor due to "
                + Settings.CUSTOM_ARMOR_TYPE.getPath() + " setting");
        Logs.logWarning("Custom Armor will not work unless a trim pattern is set.", true);
        skippedArmorType.add(armorPrefix);
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
}

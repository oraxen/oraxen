package io.th0rgal.oraxen.utils.customarmor;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.TrimPattern;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.atlas.Atlas;
import team.unnamed.creative.atlas.AtlasSource;
import team.unnamed.creative.atlas.PalettedPermutationsAtlasSource;
import team.unnamed.creative.base.Writable;
import team.unnamed.creative.texture.Texture;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class TrimArmorDatapack implements CustomArmor {
    private static final File customArmorDatapack = Bukkit.getWorldContainer().toPath().resolve("world/datapacks/oraxen_custom_armor").toFile();

    private final JsonObject datapackMeta = new JsonObject();
    private final Key palleteKey;
    private final Map<String, Key> permutations = new HashMap<>();
    public TrimArmorDatapack() {
        clearOldDataPacks();
        JsonObject data = new JsonObject();
        data.addProperty("description", "Datapack for Oraxens Custom Armor trims");
        data.addProperty("pack_format", 26);
        datapackMeta.add("pack", data);

        palleteKey = Key.key("trims/color_palettes/trim_palette");
        permutations.put("quartz", Key.key("trims/color_palettes/quartz"));
        permutations.put("iron", Key.key("trims/color_palettes/iron"));
        permutations.put("gold", Key.key("trims/color_palettes/gold"));
        permutations.put("diamond", Key.key("trims/color_palettes/diamond"));
        permutations.put("netherite", Key.key("trims/color_palettes/netherite"));
        permutations.put("redstone", Key.key("trims/color_palettes/redstone"));
        permutations.put("copper", Key.key("trims/color_palettes/copper"));
        permutations.put("emerald", Key.key("trims/color_palettes/emerald"));
        permutations.put("lapis", Key.key("trims/color_palettes/lapis"));
        permutations.put("amethyst", Key.key("trims/color_palettes/amethyst"));
    }

    @Override
    public void generateNeededFiles() {
        generateTrimAssets();
    }

    public static void clearOldDataPacks() {
        try {
            FileUtils.deleteDirectory(customArmorDatapack);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void generateTrimAssets() {
        ResourcePack resourcePack = OraxenPlugin.get().resourcePack();
        Set<String> armorPrefixes = armorPrefixes(resourcePack);
        customArmorDatapack.toPath().resolve("data").toFile().mkdirs();
        writeMCMeta(customArmorDatapack);
        writeVanillaTrimPattern(customArmorDatapack);
        writeCustomTrimPatterns(customArmorDatapack, armorPrefixes);
        writeTrimAtlas(resourcePack, armorPrefixes);
        try {
            copyArmorLayerTextures(resourcePack);
        } catch (IOException e) {
            Logs.logError("Failed to copy armor-layer textures");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
        }
        checkOraxenArmorItems();
    }

    private static void writeVanillaTrimPattern(File datapack) {
        File vanillaArmorJson = datapack.toPath().resolve("data/minecraft/trim_pattern/" + Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase() + ".json").toFile();
        vanillaArmorJson.getParentFile().mkdirs();
        JsonObject vanillaTrimPattern = new JsonObject();
        JsonObject description = new JsonObject();
        description.addProperty("translate", "trim_pattern.minecraft." + Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase());
        vanillaTrimPattern.add("description", description);
        vanillaTrimPattern.addProperty("asset_id", "minecraft:" + Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase());
        vanillaTrimPattern.addProperty("template_item", "minecraft:debug_stick");

        try {
            vanillaArmorJson.createNewFile();
            FileUtils.writeStringToFile(vanillaArmorJson, vanillaTrimPattern.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeCustomTrimPatterns(File datapack, Set<String> armorPrefixes) {
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

    private void writeTrimAtlas(ResourcePack resourcePack, Set<String> armorPrefixes) {
        Atlas trimsAtlas = resourcePack.atlas(Key.key("armor_trims"));

        // If for some reason the atlas exists already, we append to it
        if (trimsAtlas != null) {
            List<AtlasSource> sources = trimsAtlas.sources();
            for (AtlasSource source : new ArrayList<>(sources)) {
                if (!(source instanceof PalettedPermutationsAtlasSource palletedSource)) continue;

                List<Key> textures = palletedSource.textures();
                for (String armorPrefix : armorPrefixes) {
                    textures.add(Key.key("oraxen:trims/models/armor/" + armorPrefix));
                    textures.add(Key.key("oraxen:trims/models/armor/" + armorPrefix + "_leggings"));
                }

                String trimMat = Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase();
                textures.add(Key.key("minecraft:trims/models/armor/" + trimMat));
                textures.add(Key.key("minecraft:trims/models/armor/" +  trimMat + "_leggings"));

                sources.remove(palletedSource);
                sources.add(AtlasSource.palettedPermutations(textures, palletedSource.paletteKey(), palletedSource.permutations()));
            }

            resourcePack.atlas(trimsAtlas.toBuilder().sources(sources).build());
        } else {
            List<Key> textures = new ArrayList<>();
            for (String armorPrefix : armorPrefixes) {
                textures.add(Key.key("oraxen:trims/models/armor/" + armorPrefix));
                textures.add(Key.key("oraxen:trims/models/armor/" + armorPrefix + "_leggings"));
            }

            String trimMat = Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase();
            textures.add(Key.key("minecraft:trims/models/armor/" + trimMat));
            textures.add(Key.key("minecraft:trims/models/armor/" +  trimMat + "_leggings"));

            resourcePack.atlas(Atlas.atlas().key(Key.key("armor_trims")).addSource(AtlasSource.palettedPermutations(textures, palleteKey, permutations)).build());
        }
    }

    private void copyArmorLayerTextures(ResourcePack resourcePack) throws IOException {
        String armorPath = "assets/minecraft/textures/models/armor/";
        String vanillaTrimPath = "assets/minecraft/textures/trims/models/armor/";
        String oraxenTrimPath = "assets/oraxen/textures/trims/models/armor/";
        String material = Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toLowerCase();

        resourcePack.textures().stream().filter(t ->
                t.key().asString().endsWith("_armor_layer_1.png") || t.key().asString().endsWith("_armor_layer_2.png")
        ).toList().forEach( t -> {
            String armorPrefix = armorPrefix(t);
            if (t.key().asString().endsWith("_armor_layer_1.png"))
                resourcePack.texture(t.toBuilder().key(Key.key(oraxenTrimPath + armorPrefix + ".png")).build());
            else if (t.key().asString().endsWith("_armor_layer_2.png"))
                resourcePack.texture(t.toBuilder().key(Key.key(oraxenTrimPath + armorPrefix + "_leggings.png")).build());

            if (t.key().asString().equals(armorPath + material + "_layer_1.png"))
                resourcePack.texture(t.toBuilder().key(Key.key(vanillaTrimPath + material + ".png")).build());
            else if (t.key().asString().equals(armorPath + material + "_layer_2.png"))
                resourcePack.texture(t.toBuilder().key(Key.key(vanillaTrimPath + material + "_leggings.png")).build());
        });

        String resourcePath = "assets/minecraft/textures/models/armor/";
        if (resourcePack.textures().stream().noneMatch(t -> t.key().asString().equals(oraxenTrimPath + material + ".png")))
            resourcePack.texture(Texture.texture(Key.key(vanillaTrimPath + material + ".png"), Writable.copyInputStream(OraxenPlugin.get().getResource(resourcePath + material + "_layer_1.png"))));
        if (resourcePack.textures().stream().noneMatch(t -> t.key().asString().equals(oraxenTrimPath + material + "_leggings.png")))
            resourcePack.texture(Texture.texture(Key.key(vanillaTrimPath + material + "_leggings.png"), Writable.copyInputStream(OraxenPlugin.get().getResource(resourcePath + material + "_layer_2.png"))));

        resourcePack.texture(Texture.texture(Key.key(armorPath + material + "_layer_1.png"), Writable.copyInputStream(OraxenPlugin.get().getResource(resourcePath + "transparent_layer_1.png"))));
        resourcePack.texture(Texture.texture(Key.key(armorPath + material + "_layer_2.png"), Writable.copyInputStream(OraxenPlugin.get().getResource(resourcePath + "transparent_layer_2.png"))));
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
        for (ItemBuilder itemBuilder : OraxenItems.getItems()) {
            String itemID = OraxenItems.getIdByItem(itemBuilder);
            ItemStack itemStack = itemBuilder.build();
            boolean changed = false;

            if (itemStack == null || !Tag.ITEMS_TRIMMABLE_ARMOR.isTagged(itemBuilder.getType())) continue;
            if (!itemStack.hasItemMeta() || !(itemStack.getItemMeta() instanceof ArmorMeta)) continue;
            if (!itemStack.getType().name().toUpperCase().startsWith(Settings.CUSTOM_ARMOR_TRIMS_MATERIAL.toString().toUpperCase())) continue;

            if (!itemBuilder.getItemFlags().contains(ItemFlag.HIDE_ARMOR_TRIM)) {
                Logs.logWarning("Item " + itemID + " does not have the HIDE_ARMOR_TRIM flag set.");

                if (Settings.CUSTOM_ARMOR_TRIMS_ASSIGN.toBool()) {
                    itemBuilder.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
                    changed = true;
                    if (Settings.DEBUG.toBool()) Logs.logInfo("Assigned HIDE_ARMOR_TRIM flag to " + itemID, true);
                } else Logs.logWarning("Custom Armors are recommended to have the HIDE_ARMOR_TRIM flag set.", true);
            }
            if (!itemBuilder.hasTrimPattern() && CustomArmorType.getSetting() == CustomArmorType.TRIMS) {
                String armorPrefix = StringUtils.substringBeforeLast(itemID,"_");
                Logs.logWarning("Item " + itemID + " does not have a trim pattern set.");
                Logs.logWarning("Oraxen has been configured to use Trims for custom-armor due to " + Settings.CUSTOM_ARMOR_TYPE.getPath() + " setting");

                TrimPattern trimPattern = Registry.TRIM_PATTERN.get(NamespacedKey.fromString("oraxen:" + armorPrefix));
                if (Settings.CUSTOM_ARMOR_TRIMS_ASSIGN.toBool() && trimPattern != null) {
                    itemBuilder.setTrimPattern(trimPattern.key());
                    changed = true;
                    if (Settings.DEBUG.toBool()) Logs.logInfo("Assigned trim pattern " + trimPattern.key().asString() + " to " + itemID, true);
                } else Logs.logWarning("Custom Armor will not work unless a trim pattern is set.", true);
            }

            if (changed) itemBuilder.save();
        }
    }

    private Set<String> armorPrefixes(ResourcePack resourcePack) {
        return resourcePack.textures().stream().map(this::armorPrefix).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
    }

    private String armorPrefix(Texture texture) {
        String textureKey = texture.key().asString();
        return textureKey.endsWith("_armor_layer_1")
                ? StringUtils.substringAfterLast(StringUtils.substringBefore(textureKey, "_armor_layer_1"), "/")
                : textureKey.endsWith("_armor_layer_2")
                ? StringUtils.substringAfterLast(StringUtils.substringBefore(textureKey, "_armor_layer_2"), "/")
                : "";
    }
}

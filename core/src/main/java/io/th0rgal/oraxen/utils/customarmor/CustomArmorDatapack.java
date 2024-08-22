package io.th0rgal.oraxen.utils.customarmor;

import com.google.gson.JsonObject;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.pack.DefaultResourcePackExtractor;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.kyori.adventure.key.Key;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.packs.DataPack;
import team.unnamed.creative.ResourcePack;
import team.unnamed.creative.atlas.Atlas;
import team.unnamed.creative.atlas.AtlasSource;
import team.unnamed.creative.atlas.PalettedPermutationsAtlasSource;
import team.unnamed.creative.texture.Texture;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings({"deprecation", "CallToPrintStackTrace", "ResultOfMethodCallIgnored", "UnstableApiUsage"})
public class CustomArmorDatapack {

    public enum TrimArmorMaterial {
        CHAINMAIL, LEATHER, IRON, GOLD, DIAMOND, NETHERITE;

        public String id() {
            return toString().toLowerCase(Locale.ENGLISH);
        }
    }

    private static final World defaultWorld = Bukkit.getWorlds().get(0);
    public static final Key datapackKey = Key.key("minecraft:file/oraxen_custom_armor");
    private static final File customArmorDatapack = defaultWorld.getWorldFolder().toPath().resolve("datapacks/oraxen_custom_armor").toFile();
    private final boolean isFirstInstall;
    private final boolean datapackEnabled;
    private final JsonObject datapackMeta = new JsonObject();
    private final Key palleteKey;
    private final Map<String, Key> permutations = new LinkedHashMap<>();

    public CustomArmorDatapack() {
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

        this.isFirstInstall = isFirstInstall();
        this.datapackEnabled = isDatapackEnabled();
    }

    public void generateTrimAssets() {
        ResourcePack resourcePack = OraxenPlugin.get().packGenerator().resourcePack();
        LinkedHashSet<String> armorPrefixes = armorPrefixes(resourcePack);
        customArmorDatapack.toPath().resolve("data").toFile().mkdirs();
        writeMCMeta();
        writeVanillaTrimPattern();
        writeCustomTrimPatterns(armorPrefixes);
        writeTrimAtlas(resourcePack, armorPrefixes);
        try {
            copyArmorLayerTextures(resourcePack);
        } catch (IOException e) {
            Logs.logError("Failed to copy armor-layer textures");
            if (Settings.DEBUG.toBool()) e.printStackTrace();
        }

        if (isFirstInstall) {
            Logs.logError("Oraxen's Custom-Armor datapack could not be found...");
            Logs.logWarning("The first time CustomArmor is enabled in settings.yml");
            Logs.logWarning("you need to restart your server so that the DataPack is enabled...");
            Logs.logWarning("Custom-Armor will not work, please restart your server once!", true);
        } else if (!datapackEnabled) {
            Logs.logError("Oraxen's Custom-Armor datapack is not enabled...");
            Logs.logWarning("Custom-Armor will not work, please restart your server!", true);
        } else checkOraxenArmorItems();
    }

    private static void writeVanillaTrimPattern() {
        File vanillaArmorJson = CustomArmorDatapack.customArmorDatapack.toPath().resolve("data/minecraft/trim_pattern/chainmail.json").toFile();
        vanillaArmorJson.getParentFile().mkdirs();
        JsonObject vanillaTrimPattern = new JsonObject();
        JsonObject description = new JsonObject();
        description.addProperty("translate", "trim_pattern.minecraft.chainmail");
        vanillaTrimPattern.add("description", description);
        vanillaTrimPattern.addProperty("asset_id", "minecraft:chainmail");
        vanillaTrimPattern.addProperty("template_item", "minecraft:debug_stick");

        try {
            vanillaArmorJson.createNewFile();
            FileUtils.writeStringToFile(vanillaArmorJson, vanillaTrimPattern.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeCustomTrimPatterns(LinkedHashSet<String> armorPrefixes) {
        for (String armorPrefix : armorPrefixes) {
            File armorJson = CustomArmorDatapack.customArmorDatapack.toPath().resolve("data/oraxen/trim_pattern/" + armorPrefix + ".json").toFile();
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

    private void writeTrimAtlas(ResourcePack resourcePack, LinkedHashSet<String> armorPrefixes) {
        Atlas trimsAtlas = resourcePack.atlas(Key.key("armor_trims"));

        // If for some reason the atlas exists already, we append to it
        if (trimsAtlas != null) {
            List<AtlasSource> sources = new ArrayList<>(trimsAtlas.sources());
            for (AtlasSource source : trimsAtlas.sources()) {
                if (!(source instanceof PalettedPermutationsAtlasSource palletedSource)) continue;

                List<Key> textures = new ArrayList<>(palletedSource.textures());
                for (String armorPrefix : armorPrefixes) {
                    textures.add(Key.key("oraxen:trims/models/armor/" + armorPrefix));
                    textures.add(Key.key("oraxen:trims/models/armor/" + armorPrefix + "_leggings"));
                }

                textures.add(Key.key("minecraft:trims/models/armor/chainmail"));
                textures.add(Key.key("minecraft:trims/models/armor/chainmail_leggings"));

                sources.remove(palletedSource);
                sources.add(AtlasSource.palettedPermutations(new LinkedHashSet<>(textures).stream().toList(), palletedSource.paletteKey(), palletedSource.permutations()));
            }

            resourcePack.atlas(trimsAtlas.toBuilder().sources(sources).build());
        } else {
            List<Key> textures = new ArrayList<>();
            for (String armorPrefix : armorPrefixes) {
                textures.add(Key.key("oraxen:trims/models/armor/" + armorPrefix));
                textures.add(Key.key("oraxen:trims/models/armor/" + armorPrefix + "_leggings"));
            }

            textures.add(Key.key("minecraft:trims/models/armor/chainmail"));
            textures.add(Key.key("minecraft:trims/models/armor/chainmail_leggings"));

            resourcePack.atlas(Atlas.atlas().key(Key.key("armor_trims")).addSource(AtlasSource.palettedPermutations(new LinkedHashSet<>(textures).stream().toList(), palleteKey, permutations)).build());
        }
    }

    private void copyArmorLayerTextures(ResourcePack resourcePack) throws IOException {
        String armorPath = "minecraft:models/armor/";
        String vanillaTrimPath = "minecraft:trims/models/armor/";
        String oraxenTrimPath = "oraxen:trims/models/armor/";
        Key chainmailKey = Key.key(armorPath + "chainmail_layer_1.png"), chainmailKey2 = Key.key(armorPath + "chainmail_layer_2.png");

        new LinkedHashSet<>(resourcePack.textures()).forEach(armorTexture -> {
            String armorPrefix = armorPrefix(armorTexture);
            if (armorTexture.key().asString().endsWith("_armor_layer_1.png"))
                resourcePack.texture(Key.key(oraxenTrimPath + armorPrefix + ".png"), armorTexture.data());
            else if (armorTexture.key().asString().endsWith("_armor_layer_2.png"))
                resourcePack.texture(Key.key(oraxenTrimPath + armorPrefix + "_leggings.png"), armorTexture.data());
        });

        Texture chainmail = Optional.ofNullable(resourcePack.texture(chainmailKey)).orElse(DefaultResourcePackExtractor.vanillaResourcePack.texture(chainmailKey));
        Texture chainmail2 = Optional.ofNullable(resourcePack.texture(chainmailKey2)).orElse(DefaultResourcePackExtractor.vanillaResourcePack.texture(chainmailKey2));

        if (chainmail != null) resourcePack.texture(Key.key(vanillaTrimPath + "chainmail.png"), chainmail.data());
        else Logs.logWarning(String.format("Could not find %s in ResourcePack or VanillaPack", chainmailKey.asString()));
        if (chainmail2 != null) resourcePack.texture(Key.key(vanillaTrimPath + "chainmail_leggings.png"), chainmail2.data());
        else Logs.logWarning(String.format("Could not find %s in ResourcePack or VanillaPack", chainmailKey2.asString()));

        Optional.ofNullable(resourcePack.texture(Key.key(armorPath + "transparent_layer.png"))).ifPresent(transparent -> {
            resourcePack.texture(chainmailKey, transparent.data());
            resourcePack.texture(chainmailKey2, transparent.data());
        });
    }

    private void writeMCMeta() {
        try {
            File packMeta = CustomArmorDatapack.customArmorDatapack.toPath().resolve("pack.mcmeta").toFile();
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
            String armorPrefix = StringUtils.substringBeforeLast(itemID,"_");
            boolean changed = false;

            if (skippedArmorType.contains(armorPrefix)) continue;
            if (itemStack == null || !Tag.ITEMS_TRIMMABLE_ARMOR.isTagged(itemBuilder.getType())) continue;
            if (!itemStack.hasItemMeta() || !(itemStack.getItemMeta() instanceof ArmorMeta)) continue;
            if (!itemStack.getType().name().startsWith("CHAINMAIL")) continue;

            if (!itemBuilder.getItemFlags().contains(ItemFlag.HIDE_ARMOR_TRIM)) {
                Logs.logWarning("Item " + itemID + " does not have the HIDE_ARMOR_TRIM flag set.");

                if (Settings.CUSTOM_ARMOR_TRIMS_ASSIGN.toBool()) {
                    itemBuilder.addItemFlags(ItemFlag.HIDE_ARMOR_TRIM);
                    changed = true;
                    if (Settings.DEBUG.toBool()) Logs.logInfo("Assigned HIDE_ARMOR_TRIM flag to " + itemID, true);
                } else Logs.logWarning("Custom Armors are recommended to have the HIDE_ARMOR_TRIM flag set.", true);
            }
            if (!itemBuilder.hasTrimPattern()) {

                TrimPattern trimPattern = Registry.TRIM_PATTERN.get(NamespacedKey.fromString("oraxen:" + armorPrefix));
                if (trimPattern == null) {
                    Logs.logError("Could not get trim-pattern for " + itemID + ": oraxen:" + armorPrefix);
                    Logs.logWarning("Ensure that the  DataPack is enabled `/datapack list` and restart your server");
                    skippedArmorType.add(armorPrefix);
                } else if (!Settings.CUSTOM_ARMOR_TRIMS_ASSIGN.toBool()) {
                    Logs.logWarning("Item " + itemID + " does not have a trim pattern set.");
                    Logs.logWarning("Custom Armor will not work unless a trim pattern is set.", true);
                    skippedArmorType.add(armorPrefix);
                } else {
                    itemBuilder.setTrimPattern(trimPattern.key());
                    changed = true;
                    Logs.logWarning("Item " + itemID + " does not have a trim pattern set.");
                    Logs.logInfo("Assigned trim pattern " + trimPattern.key().asString() + " to " + itemID, true);
                }
            }

            if (changed) itemBuilder.save();
        }
    }

    private LinkedHashSet<String> armorPrefixes(ResourcePack resourcePack) {
        return resourcePack.textures().stream().map(this::armorPrefix).filter(StringUtils::isNotBlank).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String armorPrefix(Texture texture) {
        String textureKey = texture.key().asString();
        return textureKey.endsWith("_armor_layer_1.png")
                ? StringUtils.substringAfterLast(StringUtils.substringBefore(textureKey, "_armor_layer_1.png"), "/")
                : textureKey.endsWith("_armor_layer_2.png")
                ? StringUtils.substringAfterLast(StringUtils.substringBefore(textureKey, "_armor_layer_2.png"), "/")
                : textureKey.endsWith("_layer_1.png")
                ? StringUtils.substringAfterLast(StringUtils.substringBefore(textureKey, "_layer_1.png"), "/")
                : textureKey.endsWith("_layer_2.png")
                ? StringUtils.substringAfterLast(StringUtils.substringBefore(textureKey, "_layer_2.png"), "/")
                : "";
    }

    private boolean isFirstInstall() {
        return Bukkit.getDataPackManager().getDataPacks().stream().filter(d -> d.getKey() != null).noneMatch(d -> datapackKey.equals(Key.key(d.getKey().asString())));
    }

    private boolean isDatapackEnabled() {
        for (DataPack dataPack : Bukkit.getDataPackManager().getEnabledDataPacks(defaultWorld)) {
            if (dataPack.getKey() == null) continue;
            if (datapackKey.equals(dataPack.getKey())) return true;
        }
        for (DataPack dataPack : Bukkit.getDataPackManager().getDisabledDataPacks(defaultWorld)) {
            if (dataPack.getKey() == null) continue;
            if (datapackKey.equals(dataPack.getKey())) return true;
        }

        return false;
    }
}

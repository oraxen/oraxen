package io.th0rgal.oraxen.pack.generation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.items.ItemBuilder;
import io.th0rgal.oraxen.items.OraxenMeta;
import io.th0rgal.oraxen.utils.ItemUtils;
import io.th0rgal.oraxen.utils.Utils;
import io.th0rgal.oraxen.utils.VersionUtil;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PredicatesGenerator {

    private final JsonObject json = new JsonObject();
    // not static here because only instanciated once I think
    private final String[] tools = new String[]{"PICKAXE", "SWORD", "HOE", "AXE", "SHOVEL"};

    public PredicatesGenerator(final Material material, final List<ItemBuilder> items) {

        // parent
        json.addProperty("parent", getParent(material));

        String vanillaTextureName = getVanillaTextureName(material, false);

        // textures
        final ItemMeta exampleMeta = new ItemStack(material).getItemMeta();
        JsonObject textures = new JsonObject();

        if (material == Material.TIPPED_ARROW) {
            textures.addProperty("layer0", "item/tipped_arrow_head");
            textures.addProperty("layer1", "item/tipped_arrow_base");
        } else if (material == Material.FIREWORK_STAR) {
            textures.addProperty("layer0", vanillaTextureName);
            textures.addProperty("layer1", vanillaTextureName + "_overlay");
        } else if (exampleMeta instanceof PotionMeta) {
            textures.addProperty("layer0", "item/potion_overlay");
            textures.addProperty("layer1", vanillaTextureName);
        } else if (exampleMeta instanceof LeatherArmorMeta && material != Material.LEATHER_HORSE_ARMOR) {
            textures.addProperty("layer0", vanillaTextureName);
            textures.addProperty("layer1", vanillaTextureName + "_overlay");
        } else if (exampleMeta instanceof SpawnEggMeta) textures = new JsonObject();
        else textures.addProperty("layer0", vanillaTextureName);

        if (!textures.entrySet().isEmpty()) json.add("textures", textures);

        // overrides
        final JsonArray overrides = new JsonArray();

        // specific items
        switch (material) {
            case FISHING_ROD -> overrides.add(getOverride("cast", 1, "item/fishing_rod_cast"));
            case SHIELD -> {
                overrides.add(getOverride("blocking", 1, "item/shield_blocking"));
                json.addProperty("gui_light", "front");
                json.add("display", JsonParser.parseString(Settings.SHIELD_DISPLAY.toString()).getAsJsonObject());
            }
            case BOW -> {
                JsonObject pullingPredicate = new JsonObject();
                pullingPredicate.addProperty("pulling", 1);
                overrides.add(getOverride(JsonParser.parseString(pullingPredicate.toString()).getAsJsonObject(), "item/bow_pulling_0"));
                pullingPredicate.addProperty("pull", 0.65);
                overrides.add(getOverride(JsonParser.parseString(pullingPredicate.toString()).getAsJsonObject(), "item/bow_pulling_1"));
                pullingPredicate.addProperty("pull", 0.9);
                overrides.add(getOverride(pullingPredicate, "item/bow_pulling_2"));
                json.add("display", JsonParser.parseString(Settings.BOW_DISPLAY.toString()).getAsJsonObject());
            }
            case CROSSBOW -> {
                JsonObject pullingPredicate = new JsonObject();
                pullingPredicate.addProperty("pulling", 1);
                overrides.add(getOverride(JsonParser.parseString(pullingPredicate.toString()).getAsJsonObject(), "item/crossbow_pulling_0"));
                pullingPredicate.addProperty("pull", 0.65);
                overrides.add(getOverride(JsonParser.parseString(pullingPredicate.toString()).getAsJsonObject(), "item/crossbow_pulling_1"));
                pullingPredicate.addProperty("pull", 0.9);
                overrides.add(getOverride(pullingPredicate, "item/crossbow_pulling_2"));

                JsonObject chargedPredicate = new JsonObject();
                chargedPredicate.addProperty("charged", 1);
                overrides.add(getOverride(JsonParser.parseString(chargedPredicate.toString()).getAsJsonObject(), "item/crossbow_arrow"));
                chargedPredicate.addProperty("firework", 1);
                overrides.add(getOverride(JsonParser.parseString(chargedPredicate.toString()).getAsJsonObject(), "item/crossbow_firework"));

                json.add("display", JsonParser.parseString(Settings.CROSSBOW_DISPLAY.toString()).getAsJsonObject());
            }
            case LIGHT -> {
                JsonObject lightPredicate = new JsonObject();
                for (int i = 0; i < 16; i++) {
                    lightPredicate.addProperty("level", (float) i / 16);
                    overrides.add(getOverride(JsonParser.parseString(lightPredicate.toString()).getAsJsonObject(), "item/light_" + (i >= 10 ? i : "0" + i)));
                }
            }
            case CHEST, ENDER_CHEST, TRAPPED_CHEST -> json.add("display", JsonParser.parseString(CHEST_DISPLAY).getAsJsonObject());
            case LARGE_AMETHYST_BUD -> json.add("display", JsonParser.parseString(LARGE_AMETHYST_BUD_DISPLAY).getAsJsonObject());
            case SMALL_DRIPLEAF -> json.add("display", JsonParser.parseString(SMALL_DRIPLEAF_DISPLAY).getAsJsonObject());
            case BIG_DRIPLEAF -> json.add("display", JsonParser.parseString(BIG_DRIPLEAF_DISPLAY).getAsJsonObject());
            case HANGING_ROOTS -> json.add("display", JsonParser.parseString(HANGING_ROOTS_DISPLAY).getAsJsonObject());
            case DRAGON_HEAD -> json.add("display", JsonParser.parseString(DRAGON_HEAD_DISPLAY).getAsJsonObject());
            case CONDUIT -> json.add("display", JsonParser.parseString(CONDUIT_DISPLAY).getAsJsonObject());
        }

        if (VersionUtil.atOrAbove("1.20") && material == Material.DECORATED_POT) {
            textures.addProperty("particle", "entity/decorated_pot/decorated_pot_side");
            json.add("display", JsonParser.parseString(DECORATED_POT_DISPLAY).getAsJsonObject());
        }
        if (material == Material.COMPASS || material == Material.CLOCK || (VersionUtil.atOrAbove("1.19") && material == Material.RECOVERY_COMPASS)) {
            String override = material == Material.CLOCK ? CLOCK_OVERRIDES : COMPASS_OVERRIDES;
            JsonArray jsonArray = JsonParser.parseString(override.replace("X", material.name().toLowerCase(Locale.ROOT))).getAsJsonArray();
            for (int i = 0; i < jsonArray.size(); i++) {
                overrides.add(jsonArray.get(i).getAsJsonObject());
            }
        }

        // custom items
        for (final ItemBuilder item : items) {
            OraxenMeta oraxenMeta = item.getOraxenMeta();
            int customModelData = oraxenMeta.getCustomModelData();

            // Skip duplicate
            if (overrides.contains(getOverride("custom_model_data", customModelData, oraxenMeta.getGeneratedModelPath() + oraxenMeta.getModelName())))
                continue;

            overrides.add(getOverride("custom_model_data", customModelData, oraxenMeta.getGeneratedModelPath() + oraxenMeta.getModelName()));
            if (oraxenMeta.hasBlockingModel()) {
                final JsonObject predicate = new JsonObject();
                predicate.addProperty("blocking", 1);
                overrides.add(getOverride(predicate, "custom_model_data", customModelData, oraxenMeta.getBlockingModel()));
            }
            if (oraxenMeta.hasChargedModel()) {
                final JsonObject predicate = new JsonObject();
                predicate.addProperty("charged", 1);
                overrides.add(getOverride(predicate, "custom_model_data", customModelData, oraxenMeta.getChargedModel()));
            }
            if (oraxenMeta.hasFireworkModel()) {
                final JsonObject predicate = new JsonObject();
                predicate.addProperty("charged", 1);
                predicate.addProperty("firework", 1);
                overrides.add(getOverride(predicate, "custom_model_data", customModelData, oraxenMeta.getFireworkModel()));
            }
            if (oraxenMeta.hasPullingModels()) {
                final List<String> pullingModels = oraxenMeta.getPullingModels();
                for (int i = 0; i < pullingModels.size(); i++) {
                    String pullingModel = pullingModels.get(i);
                    final JsonObject predicate = new JsonObject();
                    predicate.addProperty("pulling", 1);
                    // Round to nearest 0.X5 (0.0667 -> 0.65, 0.677 -> 0.7)
                    float pull = Math.min(Utils.customRound((((float) (i + 1) / pullingModels.size())), 0.05f), 0.9f);
                    // First pullingModel should always be used immediatly, thus pull: 0f
                    if (i != 0) predicate.addProperty("pull", pull);
                    overrides.add(getOverride(predicate, "custom_model_data", customModelData, pullingModel));
                }
            }
            if (oraxenMeta.hasCastModel()) {
                final JsonObject predicate = new JsonObject();
                predicate.addProperty("cast", 1);
                overrides.add(getOverride(predicate, "custom_model_data", customModelData, oraxenMeta.getCastModel()));
            }
            if (oraxenMeta.hasDamagedModels()) {
                final List<String> damagedModels = oraxenMeta.getDamagedModels();
                for (int i = 0; i <= damagedModels.size(); i++) {
                    if (i == 0) continue;
                    final JsonObject predicate = new JsonObject();
                    predicate.addProperty("damaged", 1);
                    predicate.addProperty("damage", Math.min((float) i / damagedModels.size(), 0.99f));
                    overrides.add(getOverride(predicate, "custom_model_data", customModelData, damagedModels.get(i - 1)));
                }
            }

        }
        json.add("overrides", overrides);
    }

    public static void generatePullingModels(OraxenMeta oraxenMeta) {
        if (!oraxenMeta.hasPullingTextures()) return;
        for (String texture : oraxenMeta.getPullingTextures()) {
            final JsonObject json = new JsonObject();
            json.addProperty("parent", oraxenMeta.getParentModel());
            final JsonObject textureJson = new JsonObject();
            textureJson.addProperty("layer0", texture);
            json.add("textures", textureJson);
            ResourcePack.writeStringToVirtual(OraxenMeta.getModelPath(Utils.getParentDirs(texture)),
                    Utils.getFileNameOnly(texture) + ".json", json.toString());
        }
    }

    public static void generateChargedModels(OraxenMeta oraxenMeta) {
        if (!oraxenMeta.hasChargedTexture()) return;
        final JsonObject json = new JsonObject();
        json.addProperty("parent", oraxenMeta.getParentModel());
        final JsonObject textureJson = new JsonObject();
        textureJson.addProperty("layer0", oraxenMeta.getChargedTexture());
        json.add("textures", textureJson);
        ResourcePack.writeStringToVirtual(OraxenMeta.getModelPath(Utils.getParentDirs(oraxenMeta.getChargedTexture())),
                Utils.getFileNameOnly(oraxenMeta.getChargedTexture()) + ".json", json.toString());
    }

    public static void generateBlockingModels(OraxenMeta oraxenMeta) {
        if (!oraxenMeta.hasBlockingTexture()) return;
        final JsonObject json = new JsonObject();
        json.addProperty("parent", oraxenMeta.getParentModel());
        final JsonObject textureJson = new JsonObject();
        textureJson.addProperty("layer0", oraxenMeta.getBlockingTexture());
        json.add("textures", textureJson);
        ResourcePack.writeStringToVirtual(OraxenMeta.getModelPath(Utils.getParentDirs(oraxenMeta.getBlockingTexture())),
                Utils.getFileNameOnly(oraxenMeta.getBlockingTexture()) + ".json", json.toString());
    }

    public static void generateFireworkModels(OraxenMeta oraxenMeta) {
        if (!oraxenMeta.hasFireworkTexture()) return;
        final JsonObject json = new JsonObject();
        json.addProperty("parent", oraxenMeta.getParentModel());
        final JsonObject textureJson = new JsonObject();
        textureJson.addProperty("layer0", oraxenMeta.getFireworkTexture());
        json.add("textures", textureJson);
        ResourcePack.writeStringToVirtual(OraxenMeta.getModelPath(Utils.getParentDirs(oraxenMeta.getFireworkTexture())),
                Utils.getFileNameOnly(oraxenMeta.getFireworkTexture()) + ".json", json.toString());
    }

    public static void generateCastModels(OraxenMeta oraxenMeta) {
        if (!oraxenMeta.hasCastTexture()) return;
        final JsonObject json = new JsonObject();
        json.addProperty("parent", oraxenMeta.getParentModel());
        final JsonObject textureJson = new JsonObject();
        textureJson.addProperty("layer0", oraxenMeta.getCastTexture());
        json.add("textures", textureJson);
        ResourcePack.writeStringToVirtual(OraxenMeta.getModelPath(Utils.getParentDirs(oraxenMeta.getCastTexture())),
                Utils.getFileNameOnly(oraxenMeta.getCastTexture()) + ".json", json.toString());
    }

    public static void generateDamageModels(OraxenMeta oraxenMeta) {
        if (!oraxenMeta.hasDamagedTextures()) return;
        for (String texture : oraxenMeta.getDamagedTextures()) {
            final JsonObject json = new JsonObject();
            json.addProperty("parent", oraxenMeta.getParentModel());
            final JsonObject textureJson = new JsonObject();
            textureJson.addProperty("layer0", texture);
            json.add("textures", textureJson);
            ResourcePack.writeStringToVirtual(OraxenMeta.getModelPath(Utils.getParentDirs(texture)),
                    Utils.getFileNameOnly(texture) + ".json", json.toString());
        }
    }

    private JsonObject getOverride(final String property, final int propertyValue, final String model) {
        return getOverride(new JsonObject(), property, propertyValue, model);
    }

    private JsonObject getOverride(final JsonObject predicate, final String property, final int propertyValue, final String model) {
        predicate.addProperty(property, propertyValue);
        return getOverride(predicate, model);
    }

    private JsonObject getOverride(final JsonObject predicate, final String model) {
        final JsonObject override = new JsonObject();
        override.add("predicate", predicate);
        override.addProperty("model", model);
        return override;
    }

    public String getVanillaModelName(final Material material) {
        return getVanillaTextureName(material, true);
    }

    public String getVanillaTextureName(final Material material, final boolean model) {
        String materialName = material.toString().toLowerCase(Locale.ROOT);
        if (!model) {
            if (material == Material.COMPASS) return "item/compass_16";
            if (VersionUtil.atOrAbove("1.19") && material == Material.RECOVERY_COMPASS) return "item/recovery_compass_16";
            if (material == Material.DEBUG_STICK) return "item/stick";
            if (material == Material.ENCHANTED_GOLDEN_APPLE) return "item/golden_apple";
            if (material == Material.SUNFLOWER) return "block/sunflower_front";
            if (Tag.TALL_FLOWERS.isTagged(material) && !material.name().equals("PITCHER_PLANT"))
                return "block/" + materialName + "_top";
            if (material == Material.LARGE_FERN || material == Material.TALL_GRASS)
                return "block/" + materialName + "_top";
            if (material.name().contains("GLASS_PANE"))
                return "block/" + materialName.replace("_pane", "");
            if (material == Material.TWISTING_VINES || material == Material.WEEPING_VINES)
                return "block/" + materialName + "_plant";
            if (material == Material.IRON_BARS || (material.isBlock() && !has2DBlockIcon(material)))
                return "block/" + materialName;
            if (material == Material.CROSSBOW) return "item/crossbow_standby";
        }
        return "item/" + materialName;
    }

    private String getParent(final Material material) {
        String materialName = material.name().toLowerCase(Locale.ROOT);
        if (material == Material.SNOW)
            return "block/snow_height2";
        if (material == Material.FISHING_ROD || material == Material.WARPED_FUNGUS_ON_A_STICK || material == Material.CARROT_ON_A_STICK)
            return "item/handheld_rod";
        if (material == Material.SCAFFOLDING)
            return "block/scaffolding_stable";
        if (material == Material.RESPAWN_ANCHOR)
            return "block/respawn_anchor_0";
        if (VersionUtil.atOrAbove("1.20") && (material == Material.SUSPICIOUS_GRAVEL || material == Material.SUSPICIOUS_SAND))
            return "block/" + materialName + "_0";
        if (material == Material.CONDUIT || material == Material.SHIELD || material == Material.CHEST || material == Material.TRAPPED_CHEST || material == Material.ENDER_CHEST)
            return "builtin/entity";
        if (VersionUtil.atOrAbove("1.20") && material == Material.DECORATED_POT)
            return "builtin/entity";
        if (material == Material.SMALL_DRIPLEAF)
            return "block/small_dripleaf_top";
        if (material == Material.SPORE_BLOSSOM || material == Material.BIG_DRIPLEAF || material == Material.AZALEA || material == Material.FLOWERING_AZALEA)
            return "block/" + materialName;
        if (material == Material.CHORUS_FLOWER || material == Material.CHORUS_PLANT || material == Material.END_ROD)
            return "block/" + materialName;
        if (material == Material.SMALL_AMETHYST_BUD || material == Material.MEDIUM_AMETHYST_BUD || material == Material.LARGE_AMETHYST_BUD)
            return "item/amethyst_bud";
        if (material == Material.AMETHYST_CLUSTER)
            return "item/generated";
        if (VersionUtil.atOrAbove("1.19") && material == Material.SCULK_VEIN)
            return "item/generated";
        if (VersionUtil.atOrAbove("1.20") && material == Material.CALIBRATED_SCULK_SENSOR)
            return "block/calibrated_sculk_sensor_inactive";
        if (materialName.contains("infested"))
            return "block/" + StringUtils.substringAfter(materialName, "infested_");

        if (ItemUtils.isSkull(material))
            return "item/template_skull";
        if (Tag.SHULKER_BOXES.isTagged(material))
            return "item/template_shulker_box";
        if (Tag.CORAL_PLANTS.isTagged(material) || materialName.contains("coral_fan"))
            return "item/generated";
        if (material.name().contains("GLASS_PANE"))
            return "item/generated";
        if (materialName.startsWith("waxed") && materialName.contains("copper"))
            return "block/" + StringUtils.substringAfter(materialName, "waxed_");
        if (Tag.BEDS.isTagged(material))
            return "item/template_bed";
        if (Tag.TRAPDOORS.isTagged(material))
            return "block/" + materialName + "_bottom";
        if (materialName.endsWith("_spawn_egg"))
            return "item/template_spawn_egg";
        if (Tag.BANNERS.isTagged(material))
            return "item/template_banner";
        if (Tag.CARPETS.isTagged(material) || material == Material.MOSS_CARPET)
            return "block/" + materialName;

        if (Arrays.stream(tools).anyMatch(tool -> material.toString().contains(tool)))
            return "item/handheld";
        if (ItemUtils.hasInventoryParent(material))
            return "block/" + materialName + "_inventory";
        if (has2DBlockIcon(material))
            return "item/generated";
        if ((material.isBlock() && material.isSolid()))
            return "block/" + materialName;
        return "item/generated";
    }

    private static boolean has2DBlockIcon(Material material) {
        if (material == Material.BARRIER || material == Material.STRUCTURE_VOID || material == Material.LIGHT) return true;
        if (material == Material.IRON_BARS || material == Material.CHAIN) return true;
        if (material == Material.SEA_PICKLE || material == Material.POINTED_DRIPSTONE || material == Material.BELL) return true;
        if (material == Material.COMPARATOR || material == Material.REPEATER) return true;
        if (material == Material.FLOWER_POT || material == Material.BREWING_STAND) return true;
        if (material == Material.CAULDRON || material == Material.HOPPER) return true;
        if (material == Material.SUGAR_CANE || material == Material.BAMBOO) return true;
        if (material == Material.NETHER_WART || material == Material.WHEAT || material == Material.CAKE) return true;
        if (material == Material.LANTERN || material == Material.SOUL_LANTERN) return true;
        if (material == Material.CAMPFIRE || material == Material.SOUL_CAMPFIRE) return true;
        if (VersionUtil.atOrAbove("1.20") && (material == Material.PITCHER_PLANT || material == Material.PINK_PETALS)) return true;
        if (Tag.DOORS.isTagged(material)) return true;
        if (Tag.CANDLES.isTagged(material)) return true;
        if (Tag.SIGNS.isTagged(material)) return true;
        if (VersionUtil.atOrAbove("1.20") && (material == Material.SNIFFER_EGG || Tag.ITEMS_HANGING_SIGNS.isTagged(material))) return true;

        return false;
    }

    private static final String HANGING_ROOTS_DISPLAY = """
            {"thirdperson_righthand": {"rotation": [0,0,0],"translation": [0,0,1],"scale": [0.55,0.55,0.55]}, "firstperson_righthand": {"rotation": [0,-90,25],"translation": [1.13,0,1.13],"scale": [0.68,0.68,0.68]}}""".trim();
    private static final String CHEST_DISPLAY = """
            {"gui": {"rotation": [30,45,0],"translation": [0,0,0],"scale": [0.625,0.625,0.625]}, "ground": {"rotation": [0,0,0],"translation": [0,3,0],"scale": [0.25,0.25,0.25]}, "head": {"rotation": [0,180,0],"translation": [0,0,0],"scale": [1,1,1]}, "fixed": {"rotation": [0,180,0],"translation": [0,0,0],"scale": [0.5,0.5,0.5]}, "thirdperson_righthand": {"rotation": [75,315,0],"translation": [0,2.5,0],"scale": [0.375,0.375,0.375]}, "firstperson_righthand": {"rotation": [0,315,0],"translation": [0,0,0],"scale": [0.4,0.4,0.4]}}""".trim();
    private static final String LARGE_AMETHYST_BUD_DISPLAY = """
            {"fixed": {"translation": [0,4,0]}}""".trim();

    private static final String SMALL_DRIPLEAF_DISPLAY = """
            {"thirdperson_righthand": {"rotation": [0,0,0],"translation": [0,4,1],"scale": [0.55,0.55,0.55]}, "firstperson_righthand": {"rotation": [0,45,0],"translation": [0,3.2,0],"scale": [0.4,0.4,0.4]}}""".trim();
    private static final String BIG_DRIPLEAF_DISPLAY = """
            {"gui": {"rotation": [30,225,0],"translation": [0,-2,0],"scale": [0.625,0.625,0.625]}, "fixed": {"rotation": [0,0,0],"translation": [0,0,-1],"scale": [0.5,0.5,0.5]}, "thirdperson_righthand": {"rotation": [0,0,0],"translation": [0,1,0],"scale": [0.55,0.55,0.55]}, "firstperson_righthand": {"rotation": [0,0,0],"translation": [1.13,0,1.13],"scale": [0.68,0.68,0.68]}}""".trim();
    private static final String DRAGON_HEAD_DISPLAY = """
            {"gui": {"translation": [-2,2,0],"rotation": [30,45,0],"scale": [0.6,0.6,0.6]}, "thirdperson_righthand": {"rotation": [0,180,0],"translation": [0,-1,2],"scale": [0.5,0.5,0.5]}}""".trim();
    private static final String CONDUIT_DISPLAY = """
            {"gui":{"rotation":[30,45,0],"translation":[0,0,0],"scale":[1,1,1]},"ground":{"rotation":[0,0,0],"translation":[0,3,0],"scale":[0.5,0.5,0.5]},"head":{"rotation":[0,180,0],"translation":[0,0,0],"scale":[1,1,1]},"fixed":{"rotation":[0,180,0],"translation":[0,0,0],"scale":[1,1,1]},"thirdperson_righthand":{"rotation":[75,315,0],"translation":[0,2.5,0],"scale":[0.5,0.5,0.5]},"firstperson_righthand":{"rotation":[0,315,0],"translation":[0,0,0],"scale":[0.8,0.8,0.8]}}""".trim();
    private static final String DECORATED_POT_DISPLAY = """
            {"thirdperson_righthand":{"rotation":[0,90,0],"translation":[0,2,0.5],"scale":[0.375,0.375,0.375]},"firstperson_righthand":{"rotation":[0,90,0],"translation":[0,0,0],"scale":[0.375,0.375,0.375]},"gui":{"rotation":[30,45,0],"translation":[0,0,0],"scale":[0.6,0.6,0.6]},"ground":{"rotation":[0,0,0],"translation":[0,1,0],"scale":[0.25,0.25,0.25]},"head":{"rotation":[0,180,0],"translation":[0,16,0],"scale":[1.5,1.5,1.5]},"fixed":{"rotation":[0,180,0],"translation":[0,0,0],"scale":[0.5,0.5,0.5]}}""".trim();
    private static final String CLOCK_OVERRIDES = """
            [{"predicate": {"time": 0}, "model": "item/X"},{"predicate": {"time": 0.0078125}, "model": "item/X_01"},{"predicate": {"time": 0.0234375}, "model": "item/X_02"},{"predicate": {"time": 0.0390625}, "model": "item/X_03"},{"predicate": {"time": 0.0546875}, "model": "item/X_04"},{"predicate": {"time": 0.0703125}, "model": "item/X_05"},{"predicate": {"time": 0.0859375}, "model": "item/X_06"},{"predicate": {"time": 0.1015625}, "model": "item/X_07"},{"predicate": {"time": 0.1171875}, "model": "item/X_08"},{"predicate": {"time": 0.1328125}, "model": "item/X_09"},{"predicate": {"time": 0.1484375}, "model": "item/X_10"},{"predicate": {"time": 0.1640625}, "model": "item/X_11"},{"predicate": {"time": 0.1796875}, "model": "item/X_12"},{"predicate": {"time": 0.1953125}, "model": "item/X_13"},{"predicate": {"time": 0.2109375}, "model": "item/X_14"},{"predicate": {"time": 0.2265625}, "model": "item/X_15"},{"predicate": {"time": 0.2421875}, "model": "item/X_16"},{"predicate": {"time": 0.2578125}, "model": "item/X_17"},{"predicate": {"time": 0.2734375}, "model": "item/X_18"},{"predicate": {"time": 0.2890625}, "model": "item/X_19"},{"predicate": {"time": 0.3046875}, "model": "item/X_20"},{"predicate": {"time": 0.3203125}, "model": "item/X_21"},{"predicate": {"time": 0.3359375}, "model": "item/X_22"},{"predicate": {"time": 0.3515625}, "model": "item/X_23"},{"predicate": {"time": 0.3671875}, "model": "item/X_24"},{"predicate": {"time": 0.3828125}, "model": "item/X_25"},{"predicate": {"time": 0.3984375}, "model": "item/X_26"},{"predicate": {"time": 0.4140625}, "model": "item/X_27"},{"predicate": {"time": 0.4296875}, "model": "item/X_28"},{"predicate": {"time": 0.4453125}, "model": "item/X_29"},{"predicate": {"time": 0.4609375}, "model": "item/X_30"},{"predicate": {"time": 0.4765625}, "model": "item/X_31"},{"predicate": {"time": 0.4921875}, "model": "item/X_32"},{"predicate": {"time": 0.5078125}, "model": "item/X_33"},{"predicate": {"time": 0.5234375}, "model": "item/X_34"},{"predicate": {"time": 0.5390625}, "model": "item/X_35"},{"predicate": {"time": 0.5546875}, "model": "item/X_36"},{"predicate": {"time": 0.5703125}, "model": "item/X_37"},{"predicate": {"time": 0.5859375}, "model": "item/X_38"},{"predicate": {"time": 0.6015625}, "model": "item/X_39"},{"predicate": {"time": 0.6171875}, "model": "item/X_40"},{"predicate": {"time": 0.6328125}, "model": "item/X_41"},{"predicate": {"time": 0.6484375}, "model": "item/X_42"},{"predicate": {"time": 0.6640625}, "model": "item/X_43"},{"predicate": {"time": 0.6796875}, "model": "item/X_44"},{"predicate": {"time": 0.6953125}, "model": "item/X_45"},{"predicate": {"time": 0.7109375}, "model": "item/X_46"},{"predicate": {"time": 0.7265625}, "model": "item/X_47"},{"predicate": {"time": 0.7421875}, "model": "item/X_48"},{"predicate": {"time": 0.7578125}, "model": "item/X_49"},{"predicate": {"time": 0.7734375}, "model": "item/X_50"},{"predicate": {"time": 0.7890625}, "model": "item/X_51"},{"predicate": {"time": 0.8046875}, "model": "item/X_52"},{"predicate": {"time": 0.8203125}, "model": "item/X_53"},{"predicate": {"time": 0.8359375}, "model": "item/X_54"},{"predicate": {"time": 0.8515625}, "model": "item/X_55"},{"predicate": {"time": 0.8671875}, "model": "item/X_56"},{"predicate": {"time": 0.8828125}, "model": "item/X_57"},{"predicate": {"time": 0.8984375}, "model": "item/X_58"},{"predicate": {"time": 0.9140625}, "model": "item/X_59"},{"predicate": {"time": 0.9296875}, "model": "item/X_60"},{"predicate": {"time": 0.9453125}, "model": "item/X_61"},{"predicate": {"time": 0.9609375}, "model": "item/X_62"},{"predicate": {"time": 0.9765625}, "model": "item/X_63"},{"predicate": {"time": 0.9921875}, "model": "item/X"}]
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            """.trim();
    private static final String COMPASS_OVERRIDES = """
            [{"predicate": {"angle": 0}, "model": "item/X"},{"predicate": {"angle": 0.015625}, "model": "item/X_17"},{"predicate": {"angle": 0.046875}, "model": "item/X_18"},{"predicate": {"angle": 0.078125}, "model": "item/X_19"},{"predicate": {"angle": 0.109375}, "model": "item/X_20"},{"predicate": {"angle": 0.140625}, "model": "item/X_21"},{"predicate": {"angle": 0.171875}, "model": "item/X_22"},{"predicate": {"angle": 0.203125}, "model": "item/X_23"},{"predicate": {"angle": 0.234375}, "model": "item/X_24"},{"predicate": {"angle": 0.265625}, "model": "item/X_25"},{"predicate": {"angle": 0.296875}, "model": "item/X_26"},{"predicate": {"angle": 0.328125}, "model": "item/X_27"},{"predicate": {"angle": 0.359375}, "model": "item/X_28"},{"predicate": {"angle": 0.390625}, "model": "item/X_29"},{"predicate": {"angle": 0.421875}, "model": "item/X_30"},{"predicate": {"angle": 0.453125}, "model": "item/X_31"},{"predicate": {"angle": 0.484375}, "model": "item/X_00"},{"predicate": {"angle": 0.515625}, "model": "item/X_01"},{"predicate": {"angle": 0.546875}, "model": "item/X_02"},{"predicate": {"angle": 0.578125}, "model": "item/X_03"},{"predicate": {"angle": 0.609375}, "model": "item/X_04"},{"predicate": {"angle": 0.640625}, "model": "item/X_05"},{"predicate": {"angle": 0.671875}, "model": "item/X_06"},{"predicate": {"angle": 0.703125}, "model": "item/X_07"},{"predicate": {"angle": 0.734375}, "model": "item/X_08"},{"predicate": {"angle": 0.765625}, "model": "item/X_09"},{"predicate": {"angle": 0.796875}, "model": "item/X_10"},{"predicate": {"angle": 0.828125}, "model": "item/X_11"},{"predicate": {"angle": 0.859375}, "model": "item/X_12"},{"predicate": {"angle": 0.890625}, "model": "item/X_13"},{"predicate": {"angle": 0.921875}, "model": "item/X_14"},{"predicate": {"angle": 0.953125}, "model": "item/X_15"},{"predicate": {"angle": 0.984375}, "model": "item/X"}]""".trim();

    public JsonObject toJSON() {
        return json;
    }

}

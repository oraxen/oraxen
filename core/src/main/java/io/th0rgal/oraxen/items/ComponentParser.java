package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.ParseUtils;
import io.th0rgal.oraxen.utils.PotionUtils;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.inventory.meta.components.JukeboxPlayableComponent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ComponentParser {

    private final ConfigurationSection section;
    @Nullable private final ConfigurationSection componentSection;
    private final String itemId;
    private final ItemBuilder itemBuilder;

    public ComponentParser(final ConfigurationSection itemSection, final ItemBuilder itemBuilder) {
        this.section = itemSection;
        this.componentSection = section.getConfigurationSection("Components");
        this.itemId = section.getName();
        this.itemBuilder = itemBuilder;
    }

    public void parseComponents() {
        String itemName = section.getString("itemname", section.getString("displayname"));
        if (itemName != null && VersionUtil.atOrAbove("1.20.5")) {
            if (VersionUtil.isPaperServer()) itemBuilder.itemName(AdventureUtils.MINI_MESSAGE.deserialize(itemName));
            else itemBuilder.setItemName(itemName);
        }

        if (componentSection == null || VersionUtil.below("1.20.5")) return;

        if (componentSection.contains("max_stack_size")) itemBuilder.setMaxStackSize(Math.clamp(componentSection.getInt("max_stack_size"), 1, 99));

        if (componentSection.contains("enchantment_glint_override")) itemBuilder.setEnchantmentGlindOverride(componentSection.getBoolean("enchantment_glint_override"));
        if (componentSection.contains("durability")) {
            itemBuilder.setDamagedOnBlockBreak(componentSection.getBoolean("durability.damage_block_break"));
            itemBuilder.setDamagedOnEntityHit(componentSection.getBoolean("durability.damage_entity_hit"));
            itemBuilder.setDurability(Math.max(componentSection.getInt("durability.value"), componentSection.getInt("durability", 1)));
        }
        if (componentSection.contains("rarity")) itemBuilder.setRarity(ItemRarity.valueOf(componentSection.getString("rarity")));
        if (componentSection.contains("fire_resistant")) itemBuilder.setFireResistant(componentSection.getBoolean("fire_resistant"));
        if (componentSection.contains("hide_tooltip")) itemBuilder.setHideToolTip(componentSection.getBoolean("hide_tooltip"));

        parseFoodComponent();
        parseToolComponent();

        if (VersionUtil.below("1.21")) return;

        ConfigurationSection jukeboxSection = componentSection.getConfigurationSection("jukebox_playable");
        if (jukeboxSection != null) {
            JukeboxPlayableComponent jukeboxPlayable = new ItemStack(Material.MUSIC_DISC_CREATOR).getItemMeta().getJukeboxPlayable();
            jukeboxPlayable.setShowInTooltip(jukeboxSection.getBoolean("show_in_tooltip"));
            jukeboxPlayable.setSongKey(NamespacedKey.fromString(jukeboxSection.getString("song_key")));
            itemBuilder.setJukeboxPlayable(jukeboxPlayable);
        }
    }

    @SuppressWarnings({"UnstableApiUsage", "unchecked"})
    private void parseToolComponent() {
        ConfigurationSection toolSection = componentSection.getConfigurationSection("tool");
        if (toolSection == null) return;

        ToolComponent toolComponent = new ItemStack(Material.PAPER).getItemMeta().getTool();
        toolComponent.setDamagePerBlock(Math.max(toolSection.getInt("damage_per_block", 1), 0));
        toolComponent.setDefaultMiningSpeed(Math.max((float) toolSection.getDouble("default_mining_speed", 1.0), 0f));

        for (Map<?, ?> ruleEntry : toolSection.getMapList("rules")) {
            float speed = ParseUtils.parseFloat(String.valueOf(ruleEntry.get("speed")), 1.0f);
            boolean correctForDrops = Boolean.parseBoolean(String.valueOf(ruleEntry.get("correct_for_drops")));
            Set<Material> materials = new HashSet<>();
            Set<Tag<Material>> tags = new HashSet<>();

            if (ruleEntry.containsKey("material")) {
                try {
                    Material material = Material.valueOf(String.valueOf(ruleEntry.get("material")));
                    if (material.isBlock()) materials.add(material);
                } catch (Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + itemId);
                    Logs.logWarning("Malformed \"material\"-section");
                    if (Settings.DEBUG.toBool()) e.printStackTrace();
                }
            }

            if (ruleEntry.containsKey("materials")) {
                try {
                    List<String> materialIds = (List<String>) ruleEntry.get("materials");
                    for (String materialId : materialIds) {
                        Material material = Material.valueOf(materialId);
                        if (material.isBlock()) materials.add(material);
                    }
                } catch (Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + itemId);
                    Logs.logWarning("Malformed \"materials\"-section");
                    if (Settings.DEBUG.toBool()) e.printStackTrace();
                }
            }

            if (ruleEntry.containsKey("tag")) {
                try {
                    NamespacedKey tagKey = NamespacedKey.fromString(String.valueOf(ruleEntry.get("tag")));
                    if (tagKey != null) tags.add(Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class));
                } catch (Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + itemId);
                    Logs.logWarning("Malformed \"tag\"-section");
                    if (Settings.DEBUG.toBool()) e.printStackTrace();
                }
            }

            if (ruleEntry.containsKey("tags")) {
                try {
                    for (String tagString : (List<String>) ruleEntry.get("tags")) {
                        NamespacedKey tagKey = NamespacedKey.fromString(tagString);
                        if (tagKey != null) tags.add(Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class));
                    }
                } catch (Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + itemId);
                    Logs.logWarning("Malformed \"material\"-section");
                    if (Settings.DEBUG.toBool()) e.printStackTrace();
                }
            }

            if (!materials.isEmpty()) toolComponent.addRule(materials, speed, correctForDrops);
            for (Tag<Material> tag : tags) toolComponent.addRule(tag, speed, correctForDrops);
        }

        itemBuilder.setToolComponent(toolComponent);
    }

    @SuppressWarnings("UnstableApiUsage")
    private void parseFoodComponent() {
        ConfigurationSection foodSection = componentSection.getConfigurationSection("food");
        if (foodSection == null) return;

        FoodComponent foodComponent = new ItemStack(Material.PAPER).getItemMeta().getFood();
        foodComponent.setNutrition(foodSection.getInt("nutrition"));
        foodComponent.setSaturation((float) foodSection.getDouble("saturation", 0.0));
        foodComponent.setCanAlwaysEat(foodSection.getBoolean("can_always_eat"));
        foodComponent.setEatSeconds((float) foodSection.getDouble("eat_seconds", 1.6));

        ConfigurationSection effectsSection = foodSection.getConfigurationSection("effects");
        if (effectsSection != null) for (String effect : effectsSection.getKeys(false)) {
            ConfigurationSection effectSection = effectsSection.getConfigurationSection(effect);
            if (effectSection == null) continue;
            PotionEffectType effectType = PotionUtils.getEffectType(effect);
            if (effectType == null)
                Logs.logError("Invalid potion effect: " + effect + ", in " + StringUtils.substringBefore(effectsSection.getCurrentPath(), ".") + " food-property!");
            else {
                foodComponent.addEffect(
                        new PotionEffect(effectType,
                                Math.max(effectSection.getInt("duration", 1) * 20, 0),
                                Math.max(effectSection.getInt("amplifier", 0), 0),
                                effectSection.getBoolean("ambient", true),
                                effectSection.getBoolean("show_particles", true),
                                effectSection.getBoolean("show_icon", true)),
                        (float) Math.clamp(effectSection.getDouble("probability", 1.0), 0, 1)
                );
            }
        }
        itemBuilder.setFoodComponent(foodComponent);
    }
}

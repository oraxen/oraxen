package io.th0rgal.oraxen.items;

import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.configs.Settings;
import io.th0rgal.oraxen.nms.NMSHandler;
import io.th0rgal.oraxen.nms.NMSHandlers;
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.OraxenYaml;
import io.th0rgal.oraxen.utils.VersionUtil;
import io.th0rgal.oraxen.utils.logs.Logs;
import net.Indyuce.mmoitems.MMOItems;
import net.kyori.adventure.key.Key;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.inventory.meta.components.JukeboxPlayableComponent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.inventory.meta.components.UseCooldownComponent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class ItemComponents {

    private final ConfigurationSection section;
    private final Material type;

    public ItemComponents(final ConfigurationSection section, final Material type) {
        this.section = section;
        this.type = type;
    }

    public void apply(final ItemBuilder item, final ConfigurationSection mergedSection) {
        parseDataComponents(item, mergedSection);
    }

    private void parseDataComponents(final ItemBuilder item, final ConfigurationSection section) {
        if (section.contains("itemname") && VersionUtil.atOrAbove("1.20.5"))
            item.setItemName(AdventureUtils.parseMiniMessage(section.getString("itemname")));
        else if (section.contains("displayname"))
            item.setItemName(AdventureUtils.parseMiniMessage(section.getString("displayname")));

        final ConfigurationSection components = OraxenYaml.getConfigurationSection(section, "Components");
        if (components == null || !VersionUtil.atOrAbove("1.20.5"))
            return;

        // Handle legacy components for backward compatibility
        handleLegacyComponents(item, components);

        // Handle generic components
        if (VersionUtil.atOrAbove("1.21.3")) {
            for (final String key : components.getKeys(false)) {
                // Skip legacy components that are handled separately
                if (isLegacyComponent(key))
                    continue;

                final Object value = components.get(key);
                if (value instanceof ConfigurationSection || value instanceof Map) {
                    NMSHandlers.getHandler().setComponent(item, key, value);
                }
            }
        }
    }

    private void handleLegacyComponents(final ItemBuilder item, final ConfigurationSection components) {

        if (OraxenYaml.contains(components, "durability")) {
            item.setDamagedOnBlockBreak(OraxenYaml.getBoolean(components, "durability.damage_block_break"));
            item.setDamagedOnEntityHit(OraxenYaml.getBoolean(components, "durability.damage_entity_hit"));
            item.setDurability(Math.max(OraxenYaml.getInt(components, "durability.value"),
                    OraxenYaml.getInt(components, "durability", 1)));
        }
        if (OraxenYaml.contains(components, "fire_resistant"))
            item.setFireResistant(OraxenYaml.getBoolean(components, "fire_resistant"));
        if (OraxenYaml.contains(components, "hide_tooltip"))
            item.setHideToolTip(OraxenYaml.getBoolean(components, "hide_tooltip"));
        if (OraxenYaml.contains(components, "max_stack_size"))
            item.setMaxStackSize(OraxenYaml.getInt(components, "max_stack_size"));

        final NMSHandler nmsHandler = NMSHandlers.getHandler();
        if (nmsHandler == null) {
            Logs.logWarning("NMSHandler is null - some components won't work properly");
            if (Settings.DEBUG.toBool()) {
                Logs.logError("Item parsing: " + (section != null ? section.getName() : "unknown section"));
            }
        } else {
            Optional.ofNullable(OraxenYaml.getConfigurationSection(components, "food"))
                    .ifPresent(food -> nmsHandler.foodComponent(item, food));
        }

        Optional.ofNullable(OraxenYaml.getConfigurationSection(components, "tool"))
                .ifPresent(toolSection -> parseToolComponent(item, toolSection));

        if (!VersionUtil.atOrAbove("1.21"))
            return;

        Optional.ofNullable(OraxenYaml.getString(components, "painting_variant"))
                .ifPresent(item::setPaintingVariant);

        final ConfigurationSection jukeboxSection = OraxenYaml.getConfigurationSection(components, "jukebox_playable");
        if (jukeboxSection != null && VersionUtil.isPaperServer()) {
            try {
                final JukeboxPlayableComponent jukeboxPlayable = new ItemStack(Material.MUSIC_DISC_CREATOR)
                        .getItemMeta()
                        .getJukeboxPlayable();

                try {
                    jukeboxPlayable.setShowInTooltip(jukeboxSection.getBoolean("show_in_tooltip"));
                } catch (final NoSuchMethodError e) {
                    Logs.logWarning(
                            "Error setting jukebox show_in_tooltip: This method is not available in your server version");
                    Logs.debug(e);
                }

                try {
                    jukeboxPlayable.setSongKey(NamespacedKey.fromString(jukeboxSection.getString("song_key", "")));
                } catch (final NoSuchMethodError e) {
                    Logs.logWarning(
                            "Error setting jukebox song_key: This method is not available in your server version");
                    Logs.debug(e);
                }

                item.setJukeboxPlayable(jukeboxPlayable);
            } catch (final Exception e) {
                Logs.logWarning("Failed to create JukeboxPlayableComponent for item: " + section.getName());
                Logs.debug(e);
            }
        } else if (jukeboxSection != null) {
            Logs.logInfo("JukeboxPlayableComponent is only supported on Paper servers. Skipping this component.");
        }

        if (!VersionUtil.atOrAbove("1.21.2"))
            return;
        Optional.ofNullable(OraxenYaml.getConfigurationSection(components, "equippable"))
                .ifPresent(equippable -> parseEquippableComponent(item, equippable));

        Optional.ofNullable(OraxenYaml.getConfigurationSection(components, "use_cooldown")).ifPresent((cooldownSection) -> {
            try {
                final UseCooldownComponent useCooldownComponent = new ItemStack(Material.PAPER).getItemMeta()
                        .getUseCooldown();
                final String group = Optional.ofNullable(cooldownSection.getString("group"))
                        .orElse("oraxen:" + OraxenItems.getIdByItem(item));
                if (!group.isEmpty())
                    useCooldownComponent.setCooldownGroup(NamespacedKey.fromString(group));
                useCooldownComponent
                        .setCooldownSeconds((float) Math.max(cooldownSection.getDouble("seconds", 1.0), 0f));
                item.setUseCooldownComponent(useCooldownComponent);
            } catch (final NoSuchMethodError | Exception e) {
                Logs.logWarning(
                        "Error setting UseCooldownComponent: This component is not available in your server version");
                Logs.debug(e);
            }
        });

        Optional.ofNullable(OraxenYaml.getConfigurationSection(components, "use_remainder"))
                .ifPresent(useRemainder -> parseUseRemainderComponent(item, useRemainder));

        Optional.ofNullable(OraxenYaml.getString(components, "tooltip_style")).map(NamespacedKey::fromString)
                .ifPresent(item::setTooltipStyle);
        Optional.ofNullable(OraxenYaml.getString(components, "item_model")).map(NamespacedKey::fromString)
                .ifPresent(item::setItemModel);

        if (nmsHandler != null) {
            Optional.ofNullable(OraxenYaml.getConfigurationSection(components, "consumable"))
                    .ifPresent(consumableSection -> nmsHandler.consumableComponent(item, consumableSection));
        }
    }

    private boolean isLegacyComponent(final String key) {
        final String normalizedKey = key.toLowerCase(Locale.ROOT);
        return normalizedKey.equals("durability") ||
                normalizedKey.equals("fire_resistant") ||
                normalizedKey.equals("hide_tooltip") ||
                normalizedKey.equals("max_stack_size") ||
                normalizedKey.equals("food") ||
                normalizedKey.equals("tool") ||
                normalizedKey.equals("painting_variant") ||
                normalizedKey.equals("jukebox_playable") ||
                normalizedKey.equals("equippable") ||
                normalizedKey.equals("use_cooldown") ||
                normalizedKey.equals("use_remainder") ||
                normalizedKey.equals("tooltip_style") ||
                normalizedKey.equals("item_model") ||
                normalizedKey.equals("consumable");
    }

    private void parseUseRemainderComponent(final ItemBuilder item,
            @NotNull final ConfigurationSection useRemainderSection) {
        final ItemStack result;
        final int amount = useRemainderSection.getInt("amount", 1);

        if (useRemainderSection.contains("oraxen_item"))
            result = ItemUpdater
                    .updateItem(OraxenItems.getItemById(useRemainderSection.getString("oraxen_item")).build());
        else if (useRemainderSection.contains("crucible_item"))
            result = new WrappedCrucibleItem(useRemainderSection.getString("crucible_item")).build();
        else if (useRemainderSection.contains("mmoitems_id") && useRemainderSection.isString("mmoitems_type"))
            result = MMOItems.plugin.getItem(useRemainderSection.getString("mmoitems_type"),
                    useRemainderSection.getString("mmoitems_id"));
        else if (useRemainderSection.contains("ecoitem_id"))
            result = new WrappedEcoItem(useRemainderSection.getString("ecoitem_id")).build();
        else if (useRemainderSection.contains("minecraft_type")) {
            final Material material = OraxenYaml.getMaterial(useRemainderSection.getString("minecraft_type", "AIR"));
            if (material == null || material.isAir())
                return;
            result = new ItemStack(material);
        } else
            result = useRemainderSection.getItemStack("minecraft_item");

        if (result != null)
            result.setAmount(amount);
        item.setUseRemainder(result);
    }

    @SuppressWarnings({ "UnstableApiUsage", "unchecked" })
    private void parseToolComponent(final ItemBuilder item, @NotNull final ConfigurationSection toolSection) {
        final ToolComponent toolComponent = new ItemStack(type).getItemMeta().getTool();
        toolComponent.setDamagePerBlock(Math.max(toolSection.getInt("damage_per_block", 1), 0));
        toolComponent.setDefaultMiningSpeed(Math.max((float) toolSection.getDouble("default_mining_speed", 1.0), 0f));

        for (final Map<?, ?> ruleEntry : toolSection.getMapList("rules")) {
            final float speed = NumberUtils.toFloat(String.valueOf(ruleEntry.get("speed")), 1f);
            final boolean correctForDrops = Boolean.parseBoolean(String.valueOf(ruleEntry.get("correct_for_drops")));
            final Set<Material> materials = new HashSet<>();
            final Set<Tag<Material>> tags = new HashSet<>();

            if (ruleEntry.containsKey("material")) {
                try {
                    final Material material = OraxenYaml.getMaterial(String.valueOf(ruleEntry.get("material")));
                    if (material == null)
                        throw new IllegalArgumentException("Unknown material");
                    if (material.isBlock())
                        materials.add(material);
                } catch (final Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + section.getName());
                    Logs.logWarning("Malformed \"material\"-section");
                    Logs.debug(e);
                }
            }

            if (ruleEntry.containsKey("materials")) {
                try {
                    final List<String> materialIds = (List<String>) ruleEntry.get("materials");
                    for (final String materialId : materialIds) {
                        final Material material = OraxenYaml.getMaterial(materialId);
                        if (material == null)
                            throw new IllegalArgumentException("Unknown material");
                        if (material.isBlock())
                            materials.add(material);
                    }
                } catch (final Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + section.getName());
                    Logs.logWarning("Malformed \"materials\"-section");
                    Logs.debug(e);
                }
            }

            if (ruleEntry.containsKey("tag")) {
                try {
                    final NamespacedKey tagKey = NamespacedKey.fromString(String.valueOf(ruleEntry.get("tag")));
                    if (tagKey != null)
                        tags.add(Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class));
                } catch (final Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + section.getName());
                    Logs.logWarning("Malformed \"tag\"-section");
                    Logs.debug(e);
                }
            }

            if (ruleEntry.containsKey("tags")) {
                try {
                    for (final String tagString : (List<String>) ruleEntry.get("tags")) {
                        final NamespacedKey tagKey = NamespacedKey.fromString(tagString);
                        if (tagKey != null)
                            tags.add(Bukkit.getTag(Tag.REGISTRY_BLOCKS, tagKey, Material.class));
                    }
                } catch (final Exception e) {
                    Logs.logWarning("Error parsing rule-entry in " + section.getName());
                    Logs.logWarning("Malformed \"tags\"-section");
                    Logs.debug(e);
                }
            }

            if (!materials.isEmpty())
                toolComponent.addRule(materials, speed, correctForDrops);
            for (final Tag<Material> tag : tags)
                toolComponent.addRule(tag, speed, correctForDrops);
        }

        item.setToolComponent(toolComponent);
    }

    private void parseEquippableComponent(final ItemBuilder item, final ConfigurationSection equippableSection) {
        final EquippableComponent equippableComponent = new ItemStack(type).getItemMeta().getEquippable();

        final String slot = equippableSection.getString("slot");
        try {
            equippableComponent.setSlot(EquipmentSlot.valueOf(slot));
        } catch (final Exception e) {
            Logs.logWarning("Error parsing equippable-component in %s...".formatted(section.getName()));
            Logs.logWarning("Invalid \"slot\"-value %s".formatted(slot));
            Logs.logWarning("Valid values are: %s".formatted(StringUtils.join(EquipmentSlot.values())));
            return;
        }

        final List<EntityType> entityTypes = equippableSection.getStringList("allowed_entity_types").stream()
                .map(e -> EnumUtils.getEnum(EntityType.class, e)).toList();
        if (equippableSection.contains("allowed_entity_types"))
            equippableComponent.setAllowedEntities(entityTypes.isEmpty() ? null : entityTypes);
        if (equippableSection.contains("damage_on_hurt"))
            equippableComponent.setDamageOnHurt(equippableSection.getBoolean("damage_on_hurt", true));
        if (equippableSection.contains("dispensable"))
            equippableComponent.setDispensable(equippableSection.getBoolean("dispensable", true));
        if (equippableSection.contains("swappable"))
            equippableComponent.setSwappable(equippableSection.getBoolean("swappable", true));

        Optional.ofNullable(equippableSection.getString("model", null)).map(NamespacedKey::fromString)
                .ifPresent(equippableComponent::setModel);
        Optional.ofNullable(equippableSection.getString("camera_overlay")).map(NamespacedKey::fromString)
                .ifPresent(equippableComponent::setCameraOverlay);

        // Only use Registry.SOUNDS::get if we're running on Paper
        if (VersionUtil.isPaperServer() && equippableSection.contains("equip_sound")) {
            try {
                Optional.ofNullable(equippableSection.getString("equip_sound"))
                        .map(Key::key)
                        .map(key -> org.bukkit.Registry.SOUNDS.get(key))
                        .ifPresent(equippableComponent::setEquipSound);
            } catch (final NoSuchMethodError e) {
                // This will catch errors on older server versions
                Logs.logWarning("Error setting equip_sound: Your server version doesn't support this feature.");
            }
        }

        item.setEquippableComponent(equippableComponent);
    }
}

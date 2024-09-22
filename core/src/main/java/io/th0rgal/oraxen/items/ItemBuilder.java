package io.th0rgal.oraxen.items;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.jeff_media.morepersistentdatatypes.DataType;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.compatibilities.provided.ecoitems.WrappedEcoItem;
import io.th0rgal.oraxen.compatibilities.provided.mmoitems.WrappedMMOItem;
import io.th0rgal.oraxen.compatibilities.provided.mythiccrucible.WrappedCrucibleItem;
import io.th0rgal.oraxen.config.Settings;
import io.th0rgal.oraxen.utils.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.inventory.meta.components.JukeboxPlayableComponent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

@SuppressWarnings("ALL")
public class ItemBuilder {

    public static final NamespacedKey UNSTACKABLE_KEY = new NamespacedKey(OraxenPlugin.get(), "unstackable");
    public static final NamespacedKey ORIGINAL_NAME_KEY = new NamespacedKey(OraxenPlugin.get(), "original_name");

    private final ItemStack itemStack;
    private final Map<PersistentDataSpace, Object> persistentDataMap = new HashMap<>();
    private final PersistentDataContainer persistentDataContainer;
    private final Map<Enchantment, Integer> enchantments;
    private OraxenMeta oraxenMeta;
    private Material type;
    private int amount;
    private Color color; // LeatherArmorMeta, PotionMeta, MapMeta & FireWorkEffectMeta
    private Key trimPattern; // TrimPattern
    private PotionType potionType;
    private List<PotionEffect> potionEffects;
    private OfflinePlayer owningPlayer; // SkullMeta
    private DyeColor bodyColor; // TropicalFishBucketMeta
    private TropicalFish.Pattern pattern;
    private DyeColor patternColor;
    private String displayName;
    private boolean unbreakable;
    private boolean unstackable;
    private Set<ItemFlag> itemFlags;
    private boolean hasAttributeModifiers;
    private Multimap<Attribute, AttributeModifier> attributeModifiers;
    private boolean hasCustomModelData;
    private int customModelData;
    private List<String> lore;
    private ItemStack finalItemStack;

    // 1.20.5+ properties
    @Nullable
    private FoodComponent foodComponent;
    @Nullable
    private ToolComponent toolComponent;
    @Nullable
    private Boolean enchantmentGlintOverride;
    @Nullable
    private Integer maxStackSize;
    @Nullable
    private String itemName;
    @Nullable
    private Boolean fireResistant;
    @Nullable
    private Boolean hideToolTip;
    @Nullable
    private ItemRarity rarity;
    @Nullable
    private Integer durability;
    private boolean damagedOnBlockBreak;
    private boolean damagedOnEntityHit;

    // 1.21+ properties
    @Nullable
    private JukeboxPlayableComponent jukeboxPlayable;


    public ItemBuilder(final Material material) {
        this(new ItemStack(material));
    }

    public ItemBuilder(WrappedMMOItem wrapped) {
        this(wrapped.build());
    }

    public ItemBuilder(WrappedCrucibleItem wrapped) {
        this(wrapped.build());
    }

    public ItemBuilder(WrappedEcoItem wrapped) {
        this(wrapped.build());
    }

    public ItemBuilder(@NotNull ItemStack itemStack) {

        this.itemStack = itemStack;

        type = itemStack.getType();

        amount = itemStack.getAmount();

        final ItemMeta itemMeta = itemStack.getItemMeta();
        assert itemMeta != null;

        if (itemMeta instanceof LeatherArmorMeta leatherArmorMeta)
            color = leatherArmorMeta.getColor();

        if (itemMeta instanceof PotionMeta potionMeta) {
            color = potionMeta.getColor();
            potionType = PotionUtils.getPotionType(potionMeta);
            potionEffects = new ArrayList<>(potionMeta.getCustomEffects());
        }

        if (itemMeta instanceof MapMeta mapMeta)
            color = mapMeta.getColor();

        if (itemMeta instanceof FireworkEffectMeta effectMeta)
            color = effectMeta.hasEffect() ? Utils.getOrDefault(effectMeta.getEffect().getColors(), 0, Color.WHITE) : Color.WHITE;

        if (VersionUtil.atOrAbove("1.20") && itemMeta instanceof ArmorMeta armorMeta && armorMeta.hasTrim())
            trimPattern = armorMeta.getTrim().getMaterial().key();

        if (itemMeta instanceof SkullMeta skullMeta)
            owningPlayer = skullMeta.getOwningPlayer();

        if (itemMeta instanceof TropicalFishBucketMeta tropicalFishBucketMeta && tropicalFishBucketMeta.hasVariant()) {
            bodyColor = tropicalFishBucketMeta.getBodyColor();
            pattern = tropicalFishBucketMeta.getPattern();
            patternColor = tropicalFishBucketMeta.getPatternColor();
        }

        if (itemMeta.hasDisplayName()) {
            if (VersionUtil.isPaperServer()) displayName = AdventureUtils.MINI_MESSAGE.serialize(itemMeta.displayName());
            else displayName = itemMeta.getDisplayName();
        }

        unbreakable = itemMeta.isUnbreakable();
        unstackable = itemMeta.getPersistentDataContainer().has(UNSTACKABLE_KEY, DataType.UUID);

        if (!itemMeta.getItemFlags().isEmpty())
            itemFlags = itemMeta.getItemFlags();

        hasAttributeModifiers = itemMeta.hasAttributeModifiers();
        if (hasAttributeModifiers)
            attributeModifiers = itemMeta.getAttributeModifiers();

        hasCustomModelData = itemMeta.hasCustomModelData();
        if (hasCustomModelData)
            customModelData = itemMeta.getCustomModelData();

        if (itemMeta.hasLore()) {
            if (VersionUtil.isPaperServer()) lore = itemMeta.lore().stream().map(AdventureUtils.MINI_MESSAGE::serialize).toList();
            else lore = itemMeta.getLore();
        }

        persistentDataContainer = itemMeta.getPersistentDataContainer();

        enchantments = new HashMap<>();

        if (VersionUtil.atOrAbove("1.20.5")) {
            if (itemMeta.hasItemName()) {
                if (VersionUtil.isPaperServer())
                    itemName = AdventureUtils.MINI_MESSAGE.serialize(itemMeta.itemName());
                else itemName = itemMeta.getItemName();
            } else itemName = null;

            durability = (itemMeta instanceof Damageable damageable) && damageable.hasMaxDamage() ? damageable.getMaxDamage() : null;
            fireResistant = itemMeta.isFireResistant() ? true : null;
            hideToolTip = itemMeta.isHideTooltip() ? true : null;
            foodComponent = itemMeta.hasFood() ? itemMeta.getFood() : null;
            toolComponent = itemMeta.hasTool() ? itemMeta.getTool() : null;
            enchantmentGlintOverride = itemMeta.hasEnchantmentGlintOverride() ? itemMeta.getEnchantmentGlintOverride() : null;
            rarity = itemMeta.hasRarity() ? itemMeta.getRarity() : null;
            maxStackSize = itemMeta.hasMaxStackSize() ? itemMeta.getMaxStackSize() : null;
            if (maxStackSize != null && maxStackSize == 1) unstackable = true;
        }

        if (VersionUtil.atOrAbove("1.21")) {
            jukeboxPlayable = itemMeta.hasJukeboxPlayable() ? itemMeta.getJukeboxPlayable() : null;
        }

    }

    public Material getType() {
        return type;
    }

    public ItemBuilder setType(final Material type) {
        this.type = type;
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        if (amount > type.getMaxStackSize())
            amount = type.getMaxStackSize();
        this.amount = amount;
        return this;
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    public ItemBuilder setDisplayName(final String displayName) {
        this.displayName = displayName;
        return this;
    }

    public boolean hasItemName() {
        return itemName != null;
    }

    @Nullable
    public String getItemName() {
        return itemName;
    }

    public ItemBuilder setItemName(String itemName) {
        this.itemName = itemName;
        return this;
    }

    public boolean hasLores() {
        return lore != null && !lore.isEmpty();
    }

    public List<String> getLore() {
        return lore != null ? lore : new ArrayList<>();
    }

    public ItemBuilder setLore(final List<String> lore) {
        this.lore = lore;
        return this;
    }

    public ItemBuilder setUnbreakable(final boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    public boolean isUnstackable() {
        return unstackable;
    }

    public ItemBuilder setUnstackable(final boolean unstackable) {
        this.unstackable = unstackable;
        if (unstackable && VersionUtil.atOrAbove("1.20.5")) maxStackSize = 1;
        return this;
    }

    @Nullable
    public Integer getDurability() {
        return durability;
    }

    public ItemBuilder setDurability(@Nullable Integer durability) {
        this.durability = durability;
        return this;
    }

    public boolean isDamagedOnBlockBreak() {
        return damagedOnBlockBreak;
    }

    public void setDamagedOnBlockBreak(boolean damagedOnBlockBreak) {
        this.damagedOnBlockBreak = damagedOnBlockBreak;
    }

    public boolean isDamagedOnEntityHit() {
        return damagedOnEntityHit;
    }

    public void setDamagedOnEntityHit(boolean damagedOnEntityHit) {
        this.damagedOnEntityHit = damagedOnEntityHit;
    }

    /**
     * Check if the ItemBuilder has color.
     *
     * @return true if the ItemBuilder has color that is not default LeatherMetaColor
     */
    public boolean hasColor() {
        return color != null && !color.equals(Bukkit.getItemFactory().getDefaultLeatherColor());
    }

    public Color getColor() {
        return color;
    }

    public ItemBuilder setColor(final Color color) {
        this.color = color;
        return this;
    }

    public boolean hasTrimPattern() {
        return VersionUtil.atOrAbove("1.20") && trimPattern != null && getTrimPattern() != null;
    }

    @Nullable
    public Key getTrimPatternKey() {
        if (!VersionUtil.atOrAbove("1.20")) return null;
        if (!Tag.ITEMS_TRIMMABLE_ARMOR.isTagged(type)) return null;
        return trimPattern;
    }

    @Nullable
    public TrimPattern getTrimPattern() {
        if (!VersionUtil.atOrAbove("1.20")) return null;
        if (!Tag.ITEMS_TRIMMABLE_ARMOR.isTagged(type)) return null;
        if (trimPattern == null) return null;
        NamespacedKey key = NamespacedKey.fromString(trimPattern.asString());
        if (key == null) return null;
        return Registry.TRIM_PATTERN.get(key);
    }

    public ItemBuilder setTrimPattern(final Key trimKey) {
        if (!VersionUtil.atOrAbove("1.20")) return this;
        if (!Tag.ITEMS_TRIMMABLE_ARMOR.isTagged(type)) return this;
        this.trimPattern = trimKey;
        return this;
    }

    public boolean hasFoodComponent() {
        return VersionUtil.atOrAbove("1.20.5") && foodComponent != null;
    }

    @Nullable
    public FoodComponent getFoodComponent() {
        return foodComponent;
    }

    public ItemBuilder setFoodComponent(FoodComponent foodComponent) {
        this.foodComponent = foodComponent;
        return this;
    }

    public boolean hasToolComponent() {
        return VersionUtil.atOrAbove("1.20.5") && toolComponent != null;
    }

    @Nullable
    public ToolComponent getToolComponent() {
        return toolComponent;
    }

    public ItemBuilder setToolComponent(ToolComponent toolComponent) {
        this.toolComponent = toolComponent;
        return this;
    }

    public boolean hasJukeboxPlayable() {
        return VersionUtil.atOrAbove("1.21") && jukeboxPlayable != null;
    }

    @Nullable
    public JukeboxPlayableComponent getJukeboxPlayable() {
        return jukeboxPlayable;
    }

    public ItemBuilder setJukeboxPlayable(JukeboxPlayableComponent jukeboxPlayable) {
        this.jukeboxPlayable = jukeboxPlayable;
        return this;
    }

    public boolean hasEnchantmentGlindOverride() {
        return VersionUtil.atOrAbove("1.20.5") && enchantmentGlintOverride != null;
    }

    @Nullable
    public Boolean getEnchantmentGlindOverride() {
        return enchantmentGlintOverride;
    }

    public ItemBuilder setEnchantmentGlindOverride(@Nullable Boolean enchantmentGlintOverride) {
        this.enchantmentGlintOverride = enchantmentGlintOverride;
        return this;
    }

    public boolean hasRarity() {
        return VersionUtil.atOrAbove("1.20.5") && rarity != null;
    }

    @Nullable
    public ItemRarity getRarity() {
        return rarity;
    }

    public ItemBuilder setRarity(@Nullable ItemRarity rarity) {
        this.rarity = rarity;
        return this;
    }

    public ItemBuilder setFireResistant(boolean fireResistant) {
        this.fireResistant = fireResistant;
        return this;
    }

    public ItemBuilder setHideToolTip(boolean hideToolTip) {
        this.hideToolTip = hideToolTip;
        return this;
    }

    public boolean hasMaxStackSize() {
        return VersionUtil.atOrAbove("1.20.5") && maxStackSize != null;
    }

    @Nullable
    public Integer getMaxStackSize() {
        return maxStackSize;
    }


    public ItemBuilder setMaxStackSize(@Nullable Integer maxStackSize) {
        this.maxStackSize = maxStackSize;
        this.setUnstackable(maxStackSize != null && maxStackSize == 1);
        return this;
    }

    public ItemBuilder setBasePotionType(final PotionType potionType) {
        this.potionType = potionType;
        return this;
    }

    public ItemBuilder addPotionEffect(final PotionEffect potionEffect) {
        if (potionEffects == null) potionEffects = new ArrayList<>();
        potionEffects.add(potionEffect);
        return this;
    }

    public ItemBuilder setOwningPlayer(final OfflinePlayer owningPlayer) {
        this.owningPlayer = owningPlayer;
        return this;
    }

    public <T, Z> ItemBuilder setCustomTag(final NamespacedKey namespacedKey, final PersistentDataType<T, Z> dataType, final Z data) {
        persistentDataMap.put(new PersistentDataSpace(namespacedKey, dataType), data);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T, Z> Z getCustomTag(final NamespacedKey namespacedKey, final PersistentDataType<T, Z> dataType) {
        for (final Map.Entry<PersistentDataSpace, Object> dataSpace : persistentDataMap.entrySet())
            if (dataSpace.getKey().namespacedKey().equals(namespacedKey)
                    && dataSpace.getKey().dataType().equals(dataType))
                return (Z) dataSpace.getValue();
        return null;
    }

    public boolean hasCustomTag() {
        return !persistentDataContainer.isEmpty();
    }


    public <T, Z> void addCustomTag(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        persistentDataMap.put(new PersistentDataSpace(key, type), value);
    }

    public ItemBuilder removeCustomTag(NamespacedKey key) {
        persistentDataContainer.remove(key);
        return this;
    }

    public ItemBuilder setCustomModelData(final int customModelData) {
        if (!hasCustomModelData)
            hasCustomModelData = true;
        this.customModelData = customModelData;
        return this;
    }

    public ItemBuilder addItemFlags(final ItemFlag... itemFlags) {
        if (this.itemFlags == null)
            this.itemFlags = new HashSet<>();
        this.itemFlags.addAll(Arrays.asList(itemFlags));
        return this;
    }

    public List<ItemFlag> getItemFlags() {
        return itemFlags != null ? new ArrayList<>(itemFlags) : new ArrayList<>();
    }

    public ItemBuilder addAttributeModifiers(final Attribute attribute, final AttributeModifier attributeModifier) {
        if (!hasAttributeModifiers) {
            hasAttributeModifiers = true;
            attributeModifiers = HashMultimap.create();
        }
        attributeModifiers.put(attribute, attributeModifier);
        return this;
    }

    public ItemBuilder addAllAttributeModifiers(final Multimap<Attribute, AttributeModifier> attributeModifiers) {
        if (!hasAttributeModifiers)
            hasAttributeModifiers = true;
        this.attributeModifiers.putAll(attributeModifiers);
        return this;
    }

    public ItemBuilder setTropicalFishBucketBodyColor(final DyeColor bodyColor) {
        this.bodyColor = bodyColor;
        return this;
    }

    public ItemBuilder setTropicalFishBucketPattern(final TropicalFish.Pattern pattern) {
        this.pattern = pattern;
        return this;
    }

    public ItemBuilder setTropicalFishBucketPatternColor(final DyeColor patternColor) {
        this.patternColor = patternColor;
        return this;
    }

    public ItemBuilder addEnchant(final Enchantment enchant, final int level) {
        enchantments.put(enchant, level);
        return this;
    }

    public ItemBuilder addEnchants(final Map<Enchantment, Integer> enchants) {
        for (final Map.Entry<Enchantment, Integer> enchant : enchants.entrySet())
            addEnchant(enchant.getKey(), enchant.getValue());
        return this;
    }

    public boolean hasOraxenMeta() {
        return oraxenMeta != null;
    }

    public OraxenMeta getOraxenMeta() {
        return oraxenMeta;
    }

    public ItemBuilder setOraxenMeta(final OraxenMeta itemResources) {
        oraxenMeta = itemResources;
        return this;
    }

    public ItemStack getReferenceClone() {
        return itemStack.clone();
    }

    public ItemBuilder clone() {
        return new ItemBuilder(itemStack.clone());
    }

    @SuppressWarnings("unchecked")
    public ItemBuilder regen() {
        final ItemStack itemStack = this.itemStack;
        if (type != null)
            itemStack.setType(type);
        if (amount != itemStack.getAmount())
            itemStack.setAmount(amount);

        ItemMeta itemMeta = itemStack.getItemMeta();

        // 1.20.5+ properties
        if (VersionUtil.atOrAbove("1.20.5")) {
            if (itemMeta instanceof Damageable damageable) damageable.setMaxDamage(durability);
            if (hasItemName()) {
                if (VersionUtil.isPaperServer()) itemMeta.itemName(AdventureUtils.MINI_MESSAGE.deserialize(itemName));
                else itemMeta.setItemName(itemName);
            }
            if (hasMaxStackSize()) itemMeta.setMaxStackSize(maxStackSize);
            if (hasEnchantmentGlindOverride()) itemMeta.setEnchantmentGlintOverride(enchantmentGlintOverride);
            if (hasRarity()) itemMeta.setRarity(rarity);
            if (hasFoodComponent()) itemMeta.setFood(foodComponent);
            if (hasToolComponent()) itemMeta.setTool(toolComponent);
            if (fireResistant != null) itemMeta.setFireResistant(fireResistant);
            if (hideToolTip != null) itemMeta.setHideTooltip(hideToolTip);
        }

        if (VersionUtil.atOrAbove("1.21")) {
            if (hasJukeboxPlayable()) itemMeta.setJukeboxPlayable(jukeboxPlayable);
        }

        handleVariousMeta(itemMeta);
        itemMeta.setUnbreakable(unbreakable);

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        if (displayName != null) {
            if (!VersionUtil.atOrAbove("1.20.5")) pdc.set(ORIGINAL_NAME_KEY, DataType.STRING, displayName);
            if (VersionUtil.isPaperServer()) {
                Component displayName = AdventureUtils.MINI_MESSAGE.deserialize(this.displayName);
                displayName = displayName.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE).colorIfAbsent(NamedTextColor.WHITE);
                itemMeta.displayName(displayName);
            } else itemMeta.setDisplayName(displayName);
        }

        if (itemFlags != null)
            itemMeta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));

        if (enchantments.size() > 0) {
            for (final Map.Entry<Enchantment, Integer> enchant : enchantments.entrySet()) {
                if (enchant.getKey() == null) continue;
                int lvl = enchant.getValue() != null ? enchant.getValue() : 1;
                itemMeta.addEnchant(enchant.getKey(), lvl, true);
            }
        }

        if (hasAttributeModifiers)
            itemMeta.setAttributeModifiers(attributeModifiers);

        if (hasCustomModelData)
            itemMeta.setCustomModelData(customModelData);

        if (!persistentDataMap.isEmpty())
            for (final Map.Entry<PersistentDataSpace, Object> dataSpace : persistentDataMap.entrySet())
                pdc.set(dataSpace.getKey().namespacedKey(), (PersistentDataType<?, Object>) dataSpace.getKey().dataType(), dataSpace.getValue());

        if (VersionUtil.isPaperServer()) {
            @Nullable List<Component> loreLines = lore != null? lore.stream().map(AdventureUtils.MINI_MESSAGE::deserialize).toList() : new ArrayList<>();
            loreLines = loreLines.stream().map(c -> c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)).toList();
            itemMeta.lore(lore != null ? loreLines : null);
        }
        else itemMeta.setLore(lore);

        itemStack.setItemMeta(itemMeta);
        finalItemStack = itemStack;

        return this;
    }

    public void save() {
        regen();
        OraxenItems.getMap().entrySet().stream().filter(entry -> entry.getValue().containsValue(this)).findFirst().ifPresent(entry -> {
            YamlConfiguration yamlConfiguration = OraxenYaml.loadConfiguration(entry.getKey());
            if (this.hasColor()) {
                String color = this.color.getRed() + "," + this.color.getGreen() + "," + this.color.getBlue();
                yamlConfiguration.set(OraxenItems.getIdByItem(this.build()) + ".color", color);
            }
            if (this.hasTrimPattern()) {
                String trimPattern = this.getTrimPatternKey().asString();
                yamlConfiguration.set(OraxenItems.getIdByItem(this.build()) + ".trim_pattern", trimPattern);
            }
            if (!getItemFlags().isEmpty()) {
                yamlConfiguration.set(OraxenItems.getIdByItem(this.build()) + ".ItemFlags", this.itemFlags.stream().map(ItemFlag::name).toList());
            }
            try {
                yamlConfiguration.save(entry.getKey());
            } catch (IOException e) {
                if (Settings.DEBUG.toBool()) e.printStackTrace();
            }
        });
    }

    private void handleVariousMeta(ItemMeta itemMeta) {
        if (itemMeta instanceof LeatherArmorMeta leatherArmorMeta && color != null && !color.equals(leatherArmorMeta.getColor())) {
            leatherArmorMeta.setColor(color);
        } else if (itemMeta instanceof PotionMeta potionMeta) {
            handlePotionMeta(potionMeta);
        } else if (itemMeta instanceof MapMeta mapMeta && color != null && !color.equals(mapMeta.getColor())) {
            mapMeta.setColor(color);
        } else if (itemMeta instanceof FireworkEffectMeta effectMeta) {
            FireworkEffect.Builder fireWorkBuilder = effectMeta.clone().hasEffect() ? effectMeta.getEffect().builder() : FireworkEffect.builder();
            if (color != null) fireWorkBuilder.withColor(color);

            // If both above fail, the below will throw an exception as builder needs atleast one color
            // If so return the base meta
            try {
                effectMeta.setEffect(fireWorkBuilder.build());
            } catch (IllegalStateException ignored) {
            }
        } else if (VersionUtil.atOrAbove("1.20") && itemMeta instanceof ArmorMeta armorMeta && hasTrimPattern()) {
            armorMeta.setTrim(new ArmorTrim(TrimMaterial.REDSTONE, getTrimPattern()));
        } else if (itemMeta instanceof SkullMeta skullMeta) {
            final OfflinePlayer defaultOwningPlayer = skullMeta.getOwningPlayer();
            if (!Objects.equals(owningPlayer, defaultOwningPlayer)) {
                skullMeta.setOwningPlayer(owningPlayer);
            }
        } else if (itemMeta instanceof TropicalFishBucketMeta tropicalFishBucketMeta && tropicalFishBucketMeta.hasVariant())
            handleTropicalFishBucketMeta(tropicalFishBucketMeta);
    }

    private ItemMeta handlePotionMeta(PotionMeta potionMeta) {
        if (color != null && !color.equals(potionMeta.getColor()))
            potionMeta.setColor(color);

        if (potionType != null && !potionType.equals(PotionUtils.getPotionType(potionMeta)))
            PotionUtils.setPotionType(potionMeta, potionType);

        if (!potionEffects.equals(potionMeta.getCustomEffects()))
            for (final PotionEffect potionEffect : potionEffects)
                potionMeta.addCustomEffect(potionEffect, true);

        return potionMeta;
    }

    private ItemMeta handleTropicalFishBucketMeta(TropicalFishBucketMeta tropicalFishBucketMeta) {

        final DyeColor defaultColor = tropicalFishBucketMeta.getBodyColor();
        if (!bodyColor.equals(defaultColor))
            tropicalFishBucketMeta.setBodyColor(bodyColor);

        final TropicalFish.Pattern defaultPattern = tropicalFishBucketMeta.getPattern();
        if (!pattern.equals(defaultPattern))
            tropicalFishBucketMeta.setPattern(pattern);

        final DyeColor defaultPatternColor = tropicalFishBucketMeta.getPatternColor();
        if (!patternColor.equals(defaultPatternColor))
            tropicalFishBucketMeta.setPatternColor(patternColor);

        return tropicalFishBucketMeta;
    }

    public ItemStack[] buildArray(final int amount) {
        final ItemStack built = build();
        final int max = hasMaxStackSize() ? maxStackSize : type != null ? type.getMaxStackSize() : itemStack.getType().getMaxStackSize();
        final int rest = max == amount ? amount : amount % max;
        final int iterations = amount > max ? (amount - rest) / max : 0;
        final ItemStack[] output = new ItemStack[iterations + (rest > 0 ? 1 : 0)];
        for (int index = 0; index < iterations; index++) {
            ItemStack clone = built.clone();
            clone.setAmount(max);
            if (unstackable) clone = handleUnstackable(clone);
            output[index] = ItemUpdater.updateItem(clone);
        }
        if (rest != 0) {
            ItemStack clone = built.clone();
            clone.setAmount(rest);
            if (unstackable) clone = handleUnstackable(clone);
            output[iterations] = ItemUpdater.updateItem(clone);
        }
        return output;
    }

    public ItemStack build() {
        if (finalItemStack == null) regen();
        if (unstackable) return handleUnstackable(finalItemStack);
        else return finalItemStack.clone();
    }

    private ItemStack handleUnstackable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || VersionUtil.atOrAbove("1.20.5")) return item;
        meta.getPersistentDataContainer().set(UNSTACKABLE_KEY, DataType.UUID, UUID.randomUUID());
        item.setItemMeta(meta);
        item.setAmount(1);
        return item;
    }

    @Override
    public String toString() {
        // todo
        return super.toString();
    }

}

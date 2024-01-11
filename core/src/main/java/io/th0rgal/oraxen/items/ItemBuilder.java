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
import io.th0rgal.oraxen.utils.AdventureUtils;
import io.th0rgal.oraxen.utils.OraxenYaml;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

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
    private int durability; // Damageable
    private Color color; // LeatherArmorMeta, PotionMeta, MapMeta & FireWorkEffectMeta
    private PotionData potionData;
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

        if (itemMeta instanceof Damageable damageable)
            durability = damageable.getDamage();

        if (itemMeta instanceof LeatherArmorMeta leatherArmorMeta)
            color = leatherArmorMeta.getColor();

        if (itemMeta instanceof PotionMeta potionMeta) {
            color = potionMeta.getColor();
            potionData = potionMeta.getBasePotionData();
            potionEffects = new ArrayList<>(potionMeta.getCustomEffects());
        }

        if (itemMeta instanceof MapMeta mapMeta)
            color = mapMeta.getColor();

        if (itemMeta instanceof FireworkEffectMeta effectMeta)
            color = effectMeta.hasEffect() ? effectMeta.getEffect().getColors().get(0) : Color.WHITE;

        if (itemMeta instanceof SkullMeta skullMeta)
            owningPlayer = skullMeta.getOwningPlayer();

        if (itemMeta instanceof TropicalFishBucketMeta tropicalFishBucketMeta && tropicalFishBucketMeta.hasVariant()) {
            bodyColor = tropicalFishBucketMeta.getBodyColor();
            pattern = tropicalFishBucketMeta.getPattern();
            patternColor = tropicalFishBucketMeta.getPatternColor();
        }

        if (itemMeta.hasDisplayName())
            displayName = itemMeta.getDisplayName();

        unbreakable = itemMeta.isUnbreakable();
        unstackable = itemMeta.getPersistentDataContainer().has(UNSTACKABLE_KEY, DataType.UUID);

        if (!itemMeta.getItemFlags().isEmpty())
            itemFlags = itemMeta.getItemFlags();

        hasAttributeModifiers = itemMeta.hasAttributeModifiers();
        if (hasAttributeModifiers)
            attributeModifiers = itemMeta.getAttributeModifiers();

        hasCustomModelData = itemMeta.hasCustomModelData();
        if (itemMeta.hasCustomModelData())
            customModelData = itemMeta.getCustomModelData();

        if (itemMeta.hasLore())
            lore = itemMeta.getLore();

        persistentDataContainer = itemMeta.getPersistentDataContainer();

        enchantments = new HashMap<>();

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

    public String getDisplayName() {
        return displayName != null ? displayName : AdventureUtils.MINI_MESSAGE.serialize(itemStack.displayName());
    }

    public ItemBuilder setDisplayName(final String displayName) {
        this.displayName = displayName;
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
        return this;
    }

    public ItemBuilder setDurability(final int durability) {
        this.durability = durability;
        return this;
    }

    /**
     * Check if the ItemBuilder has color.
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

    public ItemBuilder setBasePotionData(final PotionData potionData) {
        this.potionData = potionData;
        return this;
    }

    public ItemBuilder addPotionEffect(final PotionEffect potionEffect) {
        if (potionEffects == null)
            potionEffects = new ArrayList<>();
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
        persistentDataContainer.set(key, type, value);
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

    @SuppressWarnings("unchecked")
    public ItemBuilder regen() {
        final ItemStack itemStack = this.itemStack;

        /*
         * CHANGING ITEM
         */
        if (type != null)
            itemStack.setType(type);
        if (amount != itemStack.getAmount())
            itemStack.setAmount(amount);

        /*
         * CHANGING ItemBuilder META
         */
        ItemMeta itemMeta = handleVariousMeta(itemStack.getItemMeta());
        assert itemMeta != null;
        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        if (displayName != null) {
            pdc.set(ORIGINAL_NAME_KEY, DataType.STRING, displayName);
            itemMeta.setDisplayName(displayName);
        }

        itemMeta.setUnbreakable(unbreakable);

        /*if (isUnstackable()) {
            // This needs to be set in the final build() and buildArray() methods
            // Otherwise it will set the tag for the entire stack and you can have stacks
            // This should prevent that
        }*/

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

        itemMeta.setLore(lore);

        itemStack.setItemMeta(itemMeta);
        finalItemStack = itemStack;

        return this;
    }

    public void save() {
        regen();
        OraxenItems.getMap().entrySet().stream().filter(entry -> entry.getValue().containsValue(this)).findFirst().ifPresent(entry -> {
            YamlConfiguration yamlConfiguration = OraxenYaml.loadConfiguration(entry.getKey());
            String color = this.color.getRed() + "," + this.color.getGreen() + "," + this.color.getBlue();
            yamlConfiguration.set(OraxenItems.getIdByItem(this.build()) + ".color", color);
            try {
                yamlConfiguration.save(entry.getKey());
            } catch (IOException e) {
                if (Settings.DEBUG.toBool()) e.printStackTrace();
            }
        });
    }

    private ItemMeta handleVariousMeta(ItemMeta itemMeta) {
        // durability
        if (itemMeta instanceof Damageable damageable && durability != damageable.getDamage()) {
            damageable.setDamage(durability);
            return damageable;
        }

        if (itemMeta instanceof LeatherArmorMeta leatherArmorMeta && color != null && !color.equals(leatherArmorMeta.getColor())) {
            leatherArmorMeta.setColor(color);
            return leatherArmorMeta;
        }

        if (itemMeta instanceof PotionMeta potionMeta)
            return handlePotionMeta(potionMeta);

        if (itemMeta instanceof MapMeta mapMeta && color != null && !color.equals(mapMeta.getColor())) {
            mapMeta.setColor(color);
            return mapMeta;
        }

        if (itemMeta instanceof FireworkEffectMeta effectMeta) {
            FireworkEffect.Builder fireWorkBuilder = effectMeta.clone().hasEffect() ? effectMeta.getEffect().builder() : FireworkEffect.builder();
            if (color != null) fireWorkBuilder.withColor(color);

            // If both above fail, the below will throw an exception as builder needs atleast one color
            // If so return the base meta
            try {
                effectMeta.setEffect(fireWorkBuilder.build());
            } catch (IllegalStateException ignored) {
            }
            return effectMeta;
        }

        if (itemMeta instanceof SkullMeta skullMeta) {
            final OfflinePlayer defaultOwningPlayer = skullMeta.getOwningPlayer();
            if (!Objects.equals(owningPlayer, defaultOwningPlayer)) {
                skullMeta.setOwningPlayer(owningPlayer);
                return skullMeta;
            }
        }

        if (itemMeta instanceof TropicalFishBucketMeta tropicalFishBucketMeta && tropicalFishBucketMeta.hasVariant())
            return handleTropicalFishBucketMeta(tropicalFishBucketMeta);

        return itemMeta;
    }

    private ItemMeta handlePotionMeta(PotionMeta potionMeta) {
        if (color != null && !color.equals(potionMeta.getColor()))
            potionMeta.setColor(color);

        if (!potionData.equals(potionMeta.getBasePotionData()))
            potionMeta.setBasePotionData(potionData);

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

    public int getMaxStackSize() {
        return unstackable ? 1 : type != null ? type.getMaxStackSize() : itemStack.getType().getMaxStackSize();
    }

    public ItemStack[] buildArray(final int amount) {
        final ItemStack built = build();
        final int max = getMaxStackSize();
        final int rest = max == amount ? amount : amount % max;
        final int iterations = amount > max ? (amount - rest) / max : 0;
        final ItemStack[] output = new ItemStack[iterations + (rest > 0 ? 1 : 0)];
        for (int index = 0; index < iterations; index++) {
            final ItemStack clone = built.clone();
            clone.setAmount(max);
            if (unstackable) handleUnstackable(clone);
            output[index] = ItemUpdater.updateItem(clone);
        }
        if (rest != 0) {
            ItemStack clone = built.clone();
            clone.setAmount(rest);
            if (unstackable) handleUnstackable(clone);
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
        if (meta == null) return item;
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

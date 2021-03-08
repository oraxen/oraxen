package io.th0rgal.oraxen.items;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.syntaxphoenix.syntaxapi.nbt.NbtCompound;
import com.syntaxphoenix.syntaxapi.nbt.NbtTag;
import com.syntaxphoenix.syntaxapi.nbt.NbtType;

import io.th0rgal.oraxen.utils.reflection.ItemTools;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class ItemBuilder {

    private final ItemStack itemStack;
    private OraxenMeta oraxenMeta;

    private Material type;
    private int amount;

    private int durability; // Damageable
    private Color color; // LeatherArmorMeta & PotionMeta
    private PotionData potionData;
    private List<PotionEffect> potionEffects;
    private OfflinePlayer owningPlayer; // SkullMeta

    private DyeColor bodyColor; // TropicalFishBucketMeta
    private TropicalFish.Pattern pattern;
    private DyeColor patternColor;

    private String displayName;
    private boolean unbreakable;
    private Set<ItemFlag> itemFlags;
    private boolean hasAttributeModifiers;
    private Multimap<Attribute, AttributeModifier> attributeModifiers;
    private final Map<PersistentDataSpace, Object> persistentDataMap = new HashMap<>();
    private boolean hasCustomModelData;
    private int customModelData;
    private List<String> lore;
    private final PersistentDataContainer persistentDataContainer;
    private final Map<Enchantment, Integer> enchantments;

    private final NbtCompound overrideData;
    private final NbtCompound customItemData;

    public ItemBuilder(Material material) {
        this(new ItemStack(material));
    }

    public ItemBuilder(ItemStack itemStack) {

        this.itemStack = itemStack;

        this.type = itemStack.getType();

        this.amount = itemStack.getAmount();

        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta instanceof Damageable)
            this.durability = ((Damageable) itemMeta).getDamage();

        if (itemMeta instanceof LeatherArmorMeta)
            this.color = ((LeatherArmorMeta) itemMeta).getColor();

        if (itemMeta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) itemMeta;
            this.color = potionMeta.getColor();
            this.potionData = potionMeta.getBasePotionData();
            this.potionEffects = new ArrayList<>(potionMeta.getCustomEffects());
        }

        if (itemMeta instanceof SkullMeta)
            this.owningPlayer = ((SkullMeta) itemMeta).getOwningPlayer();

        if (itemMeta instanceof TropicalFishBucketMeta) {
            TropicalFishBucketMeta tropicalFishBucketMeta = (TropicalFishBucketMeta) itemMeta;
            this.bodyColor = tropicalFishBucketMeta.getBodyColor();
            this.pattern = tropicalFishBucketMeta.getPattern();
            this.patternColor = tropicalFishBucketMeta.getPatternColor();
        }

        if (itemMeta.hasDisplayName())
            this.displayName = itemMeta.getDisplayName();

        this.unbreakable = itemMeta.isUnbreakable();

        if (!itemMeta.getItemFlags().isEmpty())
            this.itemFlags = itemMeta.getItemFlags();

        this.hasAttributeModifiers = itemMeta.hasAttributeModifiers();
        if (hasAttributeModifiers)
            this.attributeModifiers = itemMeta.getAttributeModifiers();

        this.hasCustomModelData = itemMeta.hasCustomModelData();
        if (itemMeta.hasCustomModelData())
            this.customModelData = itemMeta.getCustomModelData();

        if (itemMeta.hasLore())
            this.lore = itemMeta.getLore();

        this.persistentDataContainer = itemMeta.getPersistentDataContainer();

        this.overrideData = ItemTools.toNbtCompound(itemStack);
        this.customItemData = overrideData.hasKey("oraxenData", NbtType.COMPOUND) ? overrideData.getCompound("oraxenData") : new NbtCompound();

        this.enchantments = new HashMap<>();

    }

    public ItemBuilder setType(Material type) {
        this.type = type;
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        if (amount > type.getMaxStackSize())
            amount = type.getMaxStackSize();
        this.amount = amount;
        return this;
    }

    public ItemBuilder setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        this.lore = lore;
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    public ItemBuilder setDurability(int durability) {
        this.durability = durability;
        return this;
    }

    public ItemBuilder setColor(Color color) {
        this.color = color;
        return this;
    }

    public ItemBuilder setBasePotionData(PotionData potionData) {
        this.potionData = potionData;
        return this;
    }

    public ItemBuilder addPotionEffect(PotionEffect potionEffect) {
        if (potionEffects == null)
            potionEffects = new ArrayList<>();
        potionEffects.add(potionEffect);
        return this;
    }

    public ItemBuilder setOwningPlayer(OfflinePlayer owningPlayer) {
        this.owningPlayer = owningPlayer;
        return this;
    }

    public <T, Z> ItemBuilder setCustomTag(NamespacedKey namespacedKey, PersistentDataType<T, Z> dataType, Z data) {
        persistentDataMap.put(new PersistentDataSpace(namespacedKey, dataType), data);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <T, Z> Z getCustomTag(NamespacedKey namespacedKey, PersistentDataType<T, Z> dataType) {
        for (Map.Entry<PersistentDataSpace, Object> dataSpace : persistentDataMap.entrySet())
            if (dataSpace.getKey().getNamespacedKey().equals(namespacedKey) && dataSpace.getKey().getDataType().equals(dataType))
                return (Z) dataSpace.getValue();
        return null;
    }

    public boolean hasCustomTag() {
        return !persistentDataContainer.isEmpty();
    }

    public ItemBuilder setCustomModelData(int customModelData) {
        if (!hasCustomModelData)
            hasCustomModelData = true;
        this.customModelData = customModelData;
        return this;
    }

    public ItemBuilder setOverrideTag(String name, NbtTag tag) {
        overrideData.set(name, tag);
        return this;
    }

    public ItemBuilder setOverrideTags(NbtCompound compound) {
        overrideData.getValue().putAll(compound.getValue());
        return this;
    }

    public NbtTag getOverrideTag(String name) {
        return overrideData.get(name);
    }

    public ItemBuilder setNbtTag(String name, NbtTag tag) {
        customItemData.set(name, tag);
        return this;
    }

    public NbtCompound getNbtData(boolean override) {
        return override ? overrideData : customItemData;
    }

    public ItemBuilder setNbtTags(NbtCompound compound) {
        customItemData.getValue().putAll(compound.getValue());
        return this;
    }

    public NbtTag getNbtTag(String name) {
        return customItemData.get(name);
    }

    public ItemBuilder addItemFlags(ItemFlag... itemFlags) {
        if (this.itemFlags == null)
            this.itemFlags = new HashSet<>();
        this.itemFlags.addAll(Arrays.asList(itemFlags));
        return this;
    }

    public ItemBuilder addAttributeModifiers(Attribute attribute, AttributeModifier attributeModifier) {
        if (!hasAttributeModifiers) {
            hasAttributeModifiers = true;
            attributeModifiers = HashMultimap.create();
        }
        attributeModifiers.put(attribute, attributeModifier);
        return this;
    }

    public ItemBuilder addAllAttributeModifiers(Multimap<Attribute, AttributeModifier> attributeModifiers) {
        if (!hasAttributeModifiers)
            hasAttributeModifiers = true;
        this.attributeModifiers.putAll(attributeModifiers);
        return this;
    }

    public ItemBuilder setTropicalFishBucketBodyColor(DyeColor bodyColor) {
        this.bodyColor = bodyColor;
        return this;
    }

    public ItemBuilder setTropicalFishBucketPattern(TropicalFish.Pattern pattern) {
        this.pattern = pattern;
        return this;
    }

    public ItemBuilder setTropicalFishBucketPatternColor(DyeColor patternColor) {
        this.patternColor = patternColor;
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchant, int level) {
        enchantments.put(enchant, level);
        return this;
    }

    public ItemBuilder addEnchants(Map<Enchantment, Integer> enchants) {
        for (Map.Entry<Enchantment, Integer> enchant : enchants.entrySet())
            addEnchant(enchant.getKey(), enchant.getValue());
        return this;
    }

    public void setOraxenMeta(OraxenMeta itemResources) {
        this.oraxenMeta = itemResources;
    }

    public boolean hasOraxenMeta() {
        return oraxenMeta != null;
    }

    public OraxenMeta getOraxenMeta() {
        return oraxenMeta;
    }

    public ItemStack getReferenceClone() {
        return itemStack.clone();
    }

    private ItemStack finalItemStack;

    @SuppressWarnings("unchecked")
    public ItemBuilder regen() {

        ItemStack itemStack = ItemTools.fromNbtCompound(overrideData);

        overrideData.remove("oraxenData");

        if (!customItemData.isEmpty())
            overrideData.set("oraxenData", customItemData);

        /*
         * CHANGING ITEM
         */
        if (type != null)
            itemStack.setType(this.type);
        if (amount != itemStack.getAmount())
            itemStack.setAmount(this.amount);

        /*
         * CHANGING ItemBuilder META
         */
        ItemMeta itemMeta = itemStack.getItemMeta();

        // durability
        if (itemMeta instanceof Damageable) {
            Damageable damageable = (Damageable) itemMeta;
            if (durability != damageable.getDamage()) {
                damageable.setDamage(durability);
                itemMeta = (ItemMeta) damageable;
            }
        }

        if (itemMeta instanceof LeatherArmorMeta) {
            LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) itemMeta;
            if (color != null && !color.equals(leatherArmorMeta.getColor())) {
                leatherArmorMeta.setColor(color);
                itemMeta = leatherArmorMeta;
            }
        }

        if (itemMeta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) itemMeta;

            if (color != null && !color.equals(potionMeta.getColor()))
                potionMeta.setColor(color);

            if (!potionData.equals(potionMeta.getBasePotionData()))
                potionMeta.setBasePotionData(potionData);

            if (!potionEffects.equals(potionMeta.getCustomEffects()))
                for (PotionEffect potionEffect : potionEffects)
                    potionMeta.addCustomEffect(potionEffect, true);

            itemMeta = potionMeta;
        }

        if (itemMeta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) itemMeta;
            OfflinePlayer defaultOwningPlayer = skullMeta.getOwningPlayer();
            if (!owningPlayer.equals(defaultOwningPlayer)) {
                skullMeta.setOwningPlayer(owningPlayer);
                itemMeta = skullMeta;
            }
        }

        if (itemMeta instanceof TropicalFishBucketMeta) {
            TropicalFishBucketMeta tropicalFishBucketMeta = (TropicalFishBucketMeta) itemMeta;

            DyeColor defaultColor = tropicalFishBucketMeta.getBodyColor();
            if (!bodyColor.equals(defaultColor))
                tropicalFishBucketMeta.setBodyColor(bodyColor);

            TropicalFish.Pattern defaultPattern = tropicalFishBucketMeta.getPattern();
            if (!pattern.equals(defaultPattern))
                tropicalFishBucketMeta.setPattern(pattern);

            DyeColor defaultPatternColor = tropicalFishBucketMeta.getPatternColor();
            if (!patternColor.equals(defaultPatternColor))
                tropicalFishBucketMeta.setPatternColor(patternColor);

            itemMeta = tropicalFishBucketMeta;
        }

        if (displayName != null)
            itemMeta.setDisplayName(displayName);

        itemMeta.setUnbreakable(unbreakable);
        if (itemFlags != null)
            itemMeta.addItemFlags(itemFlags.toArray(new ItemFlag[0]));

        if (enchantments.size() > 0)
            for (Map.Entry<Enchantment, Integer> enchant : enchantments.entrySet())
                itemMeta.addEnchant(enchant.getKey(), enchant.getValue(), true);

        if (hasAttributeModifiers)
            itemMeta.setAttributeModifiers(attributeModifiers);

        if (hasCustomModelData)
            itemMeta.setCustomModelData(customModelData);

        if (!persistentDataMap.isEmpty())
            for (Map.Entry<PersistentDataSpace, Object> dataSpace : persistentDataMap.entrySet())
                itemMeta
                    .getPersistentDataContainer()
                    .set(dataSpace.getKey().getNamespacedKey(), (PersistentDataType<?, Object>) dataSpace.getKey().getDataType(), dataSpace.getValue());

        itemMeta.setLore(lore);

        itemStack.setItemMeta(itemMeta);
        finalItemStack = itemStack;

        return this;
    }

    public int getMaxStackSize() {
        return type != null ? type.getMaxStackSize() : itemStack.getType().getMaxStackSize();
    }

    public ItemStack[] buildArray(int amount) {
        ItemStack built = build();
        int max = getMaxStackSize();
        int rest = max == amount ? amount : amount % max;
        int iterations = amount > max ? (amount - rest) / max : 0;
        ItemStack[] output = new ItemStack[iterations + (rest > 0 ? 1 : 0)];
        System.out.println(max + "/" + rest + "/" + iterations);
        for (int index = 0; index < iterations; index++) {
            ItemStack clone = built.clone();
            clone.setAmount(max);
            output[index] = clone;
        }
        if (rest != 0) {
            ItemStack clone = built.clone();
            clone.setAmount(rest);
            output[iterations] = clone;
        }
        return output;
    }

    public ItemStack build() {
        if (finalItemStack == null)
            regen();
        return finalItemStack.clone();
    }

    @Override
    public String toString() {
        // todo
        return super.toString();
    }

}

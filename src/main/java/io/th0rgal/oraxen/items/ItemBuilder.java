package io.th0rgal.oraxen.items;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.th0rgal.oraxen.utils.ItemUtils;

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

    private ItemStack itemStack;
    private PackInfos packInfos;

    private Material type;
    private int amount;

    private int durability; //Damageable
    private Color color; //LeatherArmorMeta & PotionMeta
    private PotionData potionData;
    private List<PotionEffect> potionEffects;
    private OfflinePlayer owningPlayer; //SkullMeta

    private DyeColor bodyColor; //TropicalFishBucketMeta
    private TropicalFish.Pattern pattern;
    private DyeColor patternColor;

    private String displayName;
    private boolean unbreakable;
    private Set<ItemFlag> itemFlags;
    private boolean hasAttributeModifiers;
    private Multimap<Attribute, AttributeModifier> attributeModifiers;
    private Map<PersistentDataSpace, Object> persistentDataMap = new HashMap<>();
    private Map<String, Object> nbtTags = new HashMap<>();
    private boolean hasCustomModelData;
    private int customModelData;
    private List<String> lore;
    private PersistentDataContainer persistentDataContainer;
    private Map<Enchantment, Integer> enchantments;

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
            this.potionEffects = potionMeta.getCustomEffects();
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

        this.enchantments = new HashMap<>();

    }

    public ItemBuilder setType(Material type) {
        this.type = type;
        return this;
    }

    public ItemBuilder setAmount(int amount) {
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
        if (this.potionEffects == null)
            this.potionEffects = new ArrayList<>();
        this.potionEffects.add(potionEffect);
        return this;
    }

    public ItemBuilder setOwningPlayer(OfflinePlayer owningPlayer) {
        this.owningPlayer = owningPlayer;
        return this;
    }

    public <T, Z> ItemBuilder setCustomTag(NamespacedKey namespacedKey, PersistentDataType<T, Z> dataType, Z data) {
        this.persistentDataMap.put(new PersistentDataSpace(namespacedKey, dataType), data);
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
        return !this.persistentDataContainer.isEmpty();
    }

    public ItemBuilder setCustomModelData(int customModelData) {
        if (!this.hasCustomModelData)
            this.hasCustomModelData = true;
        this.customModelData = customModelData;
        return this;
    }

    public ItemBuilder addCustomNBTTags(Map<String, Object> nbtTags) {
        this.nbtTags.putAll(nbtTags);
        return this;
    }

    public ItemBuilder setNBTBase(String field, Object nbtBase) {
        this.nbtTags.put(field, nbtBase);
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... itemFlags) {
        if (this.itemFlags == null)
            this.itemFlags = new HashSet<>();
        this.itemFlags.addAll(Arrays.asList(itemFlags));
        return this;
    }

    public ItemBuilder addAttributeModifiers(Attribute attribute, AttributeModifier attributeModifier) {
        if (!this.hasAttributeModifiers) {
            this.hasAttributeModifiers = true;
            this.attributeModifiers = HashMultimap.create();
        }
        this.attributeModifiers.put(attribute, attributeModifier);
        return this;
    }

    public ItemBuilder addAllAttributeModifiers(Multimap<Attribute, AttributeModifier> attributeModifiers) {
        if (!this.hasAttributeModifiers)
            this.hasAttributeModifiers = true;
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
        this.enchantments.put(enchant, level);
        return this;
    }

    public ItemBuilder addEnchants(Map<Enchantment, Integer> enchants) {
        for (Map.Entry<Enchantment, Integer> enchant : enchants.entrySet())
            addEnchant(enchant.getKey(), enchant.getValue());
        return this;
    }

    public void setPackInfos(PackInfos itemResources) {
        this.packInfos = itemResources;
    }

    public boolean hasPackInfos() {
        return this.packInfos != null;
    }

    public PackInfos getPackInfos() {
        return this.packInfos;
    }

    private ItemStack finalItemStack;

    @SuppressWarnings("unchecked")
    public ItemBuilder regen() {

        /*
         CHANGING ITEM
         */
        if (type != null)
            this.itemStack.setType(this.type);
        if (amount != itemStack.getAmount())
            this.itemStack.setAmount(this.amount);

        /*
         CHANGING ItemBuilder META
         */
        ItemMeta itemMeta = this.itemStack.getItemMeta();

        //durability
        if (itemMeta instanceof Damageable) {
            Damageable damageable = (Damageable) itemMeta;
            if (this.durability != damageable.getDamage()) {
                damageable.setDamage(durability);
                itemMeta = (ItemMeta) damageable;
            }
        }

        if (itemMeta instanceof LeatherArmorMeta) {
            LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) itemMeta;
            if (!this.color.equals(leatherArmorMeta.getColor())) {
                leatherArmorMeta.setColor(this.color);
                itemMeta = leatherArmorMeta;
            }
        }

        if (itemMeta instanceof PotionMeta) {
            PotionMeta potionMeta = (PotionMeta) itemMeta;

            if (!this.color.equals(potionMeta.getColor()))
                potionMeta.setColor(this.color);

            if (!this.potionData.equals(potionMeta.getBasePotionData()))
                potionMeta.setBasePotionData(potionData);

            if (!this.potionEffects.equals(potionMeta.getCustomEffects()))
                for (PotionEffect potionEffect : this.potionEffects)
                    potionMeta.addCustomEffect(potionEffect, true);

            itemMeta = potionMeta;
        }

        if (itemMeta instanceof SkullMeta) {
            SkullMeta skullMeta = (SkullMeta) itemMeta;
            if (!this.owningPlayer.equals(skullMeta.getOwningPlayer())) {
                skullMeta.setOwningPlayer(this.owningPlayer);
                itemMeta = skullMeta;
            }
        }

        if (itemMeta instanceof TropicalFishBucketMeta) {
            TropicalFishBucketMeta tropicalFishBucketMeta = (TropicalFishBucketMeta) itemMeta;

            if (!this.bodyColor.equals(tropicalFishBucketMeta.getBodyColor()))
                tropicalFishBucketMeta.setBodyColor(this.bodyColor);

            if (!this.pattern.equals(tropicalFishBucketMeta.getPattern()))
                tropicalFishBucketMeta.setPattern(this.pattern);

            if (!this.patternColor.equals(tropicalFishBucketMeta.getPatternColor()))
                tropicalFishBucketMeta.setPatternColor(this.patternColor);

            itemMeta = tropicalFishBucketMeta;
        }


        if (this.displayName != null)
            itemMeta.setDisplayName(this.displayName);

        itemMeta.setUnbreakable(this.unbreakable);
        if (this.itemFlags != null)
            itemMeta.addItemFlags(this.itemFlags.toArray(new ItemFlag[0]));

        if (this.enchantments.size() > 0)
            for (Map.Entry<Enchantment, Integer> enchant : this.enchantments.entrySet())
                itemMeta.addEnchant(enchant.getKey(), enchant.getValue(), true);

        if (this.hasAttributeModifiers)
            itemMeta.setAttributeModifiers(this.attributeModifiers);

        if (this.hasCustomModelData)
            itemMeta.setCustomModelData(this.customModelData);

        if (!this.persistentDataMap.isEmpty())
            for (Map.Entry<PersistentDataSpace, Object> dataSpace : persistentDataMap.entrySet())
                itemMeta.getPersistentDataContainer().set(dataSpace.getKey().getNamespacedKey(),
                        (PersistentDataType<?, Object>) dataSpace.getKey().getDataType(),
                        dataSpace.getValue());
        itemMeta.setLore(this.lore);

        this.itemStack.setItemMeta(itemMeta);
        if (!this.nbtTags.isEmpty()) {
            Object nmsItem = ItemUtils.getNMSCopy(itemStack);
            Object nbtTagCompound = ItemUtils.getNBTTagCompound(nmsItem);
            for (Map.Entry<String, Object> nbtTagByPath : this.nbtTags.entrySet())
                ItemUtils.setNBTBase(nbtTagCompound, nbtTagByPath.getKey(), nbtTagByPath.getValue());
            ItemUtils.setNBTTagCompound(nmsItem, nbtTagCompound);
            this.finalItemStack = ItemUtils.fromNMS(nmsItem);
        } else
            this.finalItemStack = this.itemStack;

        return this;
    }

    public ItemStack build() {
        if (finalItemStack == null)
            regen();
        return this.finalItemStack;
    }

    @Override
    public String toString() {
        //todo
        return super.toString();
    }

}

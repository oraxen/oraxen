package io.th0rgal.oraxen.items;

import com.google.common.collect.Multimap;
import io.th0rgal.oraxen.utils.ItemUtils;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.TropicalFish;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class Item {

    private ItemStack itemStack;
    private PackInfos packInfos;
    private Object itemTag;

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
    private boolean hasCustomModelData;
    private int customModelData;
    private List<String> lore;
    private Map<Enchantment, Integer> enchantments;

    public Item(Material material) {
        this(new ItemStack(material));
    }

    public Item(ItemStack itemStack) {

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

        this.enchantments = new HashMap<>();

        this.itemTag = ItemUtils.getNBTTagCompound(ItemUtils.getNMSCopy(itemStack));

    }

    public Item setType(Material type) {
        this.type = type;
        return this;
    }

    public Item setAmount(int amount) {
        this.amount = amount;
        return this;
    }

    public Item setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public Item setLore(List<String> lore) {
        this.lore = lore;
        return this;
    }

    public Item setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
        return this;
    }

    public Item setDurability(int durability) {
        this.durability = durability;
        return this;
    }

    public Item setColor(Color color) {
        this.color = color;
        return this;
    }

    public Item setBasePotionData(PotionData potionData) {
        this.potionData = potionData;
        return this;
    }

    public Item addPotionEffect(PotionEffect potionEffect) {
        if (this.potionEffects == null)
            this.potionEffects = new ArrayList<>();
        this.potionEffects.add(potionEffect);
        return this;
    }

    public Item setOwningPlayer(OfflinePlayer owningPlayer) {
        this.owningPlayer = owningPlayer;
        return this;
    }

    public Item setCustomModelData(int customModelData) {
        if (!this.hasCustomModelData)
            this.hasCustomModelData = true;
        this.customModelData = customModelData;
        return this;
    }

    public Item addItemFlags(ItemFlag... itemFlags) {
        if (this.itemFlags == null)
            this.itemFlags = new HashSet<ItemFlag>();
        this.itemFlags.addAll(Arrays.asList(itemFlags));
        return this;
    }

    public Item addAttributeModifiers(Attribute attribute, AttributeModifier attributeModifier) {
        if (!this.hasAttributeModifiers)
            this.hasAttributeModifiers = true;
        this.attributeModifiers.put(attribute, attributeModifier);
        return this;
    }

    public Item addAllAttributeModifiers(Multimap<Attribute, AttributeModifier> attributeModifiers) {
        if (!this.hasAttributeModifiers)
            this.hasAttributeModifiers = true;
        this.attributeModifiers.putAll(attributeModifiers);
        return this;
    }

    public Item setTropicalFishBucketBodyColor(DyeColor bodyColor) {
        this.bodyColor = bodyColor;
        return this;
    }

    public Item setTropicalFishBucketPattern(TropicalFish.Pattern pattern) {
        this.pattern = pattern;
        return this;
    }

    public Item setTropicalFishBucketPatternColor(DyeColor patternColor) {
        this.patternColor = patternColor;
        return this;
    }

    public Item addEnchant(Map.Entry<Enchantment, Integer> enchant) {
        this.enchantments.put(enchant.getKey(), enchant.getValue());
        return this;
    }

    public Item addEnchants(Map<Enchantment, Integer> enchants) {
        for (Map.Entry<Enchantment, Integer> enchant : enchants.entrySet())
            addEnchant(enchant);
        return this;
    }

    public Item setIntNBTTag(String field, int value) {
        ItemUtils.setIntNBTTag(this.itemTag, field, value);
        return this;
    }

    public Item setStringNBTTag(String field, String value) {
        ItemUtils.setStringNBTTag(this.itemTag, field, value);
        return this;
    }

    public Item setBooleanNBTTag(String field, boolean value) {
        ItemUtils.setBooleanNBTTag(this.itemTag, field, value);
        return this;
    }

    public Object getNBTBase(String field) {
        return ItemUtils.getNBTBase(this.itemTag, field);
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

    public Item regen() {
        /*
         CHANGING NBT
         */
        Object NMSitemStack = ItemUtils.getNMSCopy(this.itemStack);
        ItemUtils.setNBTTagCompound(NMSitemStack, itemTag);
        this.itemStack = ItemUtils.fromNMS(NMSitemStack);

        /*
         CHANGING ITEM
         */
        if (type != null)
            this.itemStack.setType(this.type);
        if (amount != itemStack.getAmount())
            this.itemStack.setAmount(this.amount);

        /*
         CHANGING ITEM META
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
        itemMeta.addItemFlags(this.itemFlags.toArray(new ItemFlag[0]));

        if (this.enchantments.size() > 0)
            for (Map.Entry<Enchantment, Integer> enchant : this.enchantments.entrySet())
                itemMeta.addEnchant(enchant.getKey(), enchant.getValue(), true);

        itemMeta.setLore(this.lore);

        this.itemStack.setItemMeta(itemMeta);
        this.finalItemStack = this.itemStack;
        return this;
    }

    public ItemStack getItem() {
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

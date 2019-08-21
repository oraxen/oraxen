package io.th0rgal.oraxen.items;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Item {

    private ItemStack itemStack;
    private PackInfos packInfos;
    private Object itemTag;

    private Material type;
    private int amount;
    private int durability;
    private String displayName;
    private boolean unbreakable;
    private Set<ItemFlag> itemFlags;
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

        if (itemMeta.hasDisplayName())
            this.displayName = itemMeta.getDisplayName();

        this.unbreakable = itemMeta.isUnbreakable();

        if (itemMeta.getItemFlags().isEmpty())
            this.itemFlags = itemMeta.getItemFlags();

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

    public Item setDurability(int durability) {
        this.durability = durability;
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

    public Item addItemFlags(ItemFlag... itemFlags) {
        this.itemFlags.addAll(Arrays.asList(itemFlags));
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
            if (this.durability != damageable.getDamage())
                ((Damageable) itemMeta).setDamage(durability);
            this.itemStack.setItemMeta(itemMeta);
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

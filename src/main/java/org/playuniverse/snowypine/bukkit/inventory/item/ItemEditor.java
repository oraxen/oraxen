package org.playuniverse.snowypine.bukkit.inventory.item;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemEditor {

	public static ItemEditor of(Material material) {
		return new ItemEditor(material);
	}

	public static ItemEditor of(ItemStack itemStack) {
		return new ItemEditor(itemStack);
	}

	public static ItemEditor ofClone(ItemStack itemStack) {
		return new ItemEditor(itemStack.clone());
	}

	private final ItemStack itemStack;
	private ItemMeta itemMeta;

	private LoreEditor lore;
	private NameEditor name;

	public ItemEditor(Material material) {
		this(new ItemStack(material));
	}

	public ItemEditor(ItemStack stack) {
		this(stack, stack.getItemMeta());
	}

	public ItemEditor(Material material, ItemMeta meta) {
		this(new ItemStack(material), meta);
	}

	public ItemEditor(ItemStack stack, ItemMeta meta) {
		Objects.requireNonNull(stack);
		this.itemStack = stack;
		this.itemMeta = meta;
	}

	public boolean hasItemMeta() {
		return itemMeta != null;
	}

	public ItemMeta getItemMeta() {
		return itemMeta;
	}

	public LoreEditor lore() {
		return lore == null ? lore = new LoreEditor(this) : lore;
	}

	public NameEditor name() {
		return name == null ? name = new NameEditor(this) : name;
	}

	public boolean hasLore() {
		return hasItemMeta() ? itemMeta.hasLore() : false;
	}

	public Optional<List<String>> getLore() {
		return Optional.ofNullable(hasLore() ? itemMeta.getLore() : null);
	}

	public ItemEditor setLore(List<String> lore) {
		return lore().set(lore).apply();
	}

	public boolean hasName() {
		return hasItemMeta() ? itemMeta.hasDisplayName() : false;
	}

	public Optional<String> getName() {
		return Optional.ofNullable(hasName() ? itemMeta.getDisplayName() : null);
	}

	public ItemEditor setName(String name) {
		return name().set(name).apply();
	}

	public boolean hasEnchant(Enchantment enchantment) {
		return itemStack.containsEnchantment(enchantment);
	}

	public boolean hasEnchantConflict(Enchantment enchantment) {
		return hasItemMeta() ? itemMeta.hasConflictingEnchant(enchantment) : false;
	}

	public int getEnchant(Enchantment enchantment) {
		return itemStack.getEnchantmentLevel(enchantment);
	}

	public ItemEditor setEnchant(Enchantment enchantment, int level) {
		try {
			itemStack.addEnchantment(enchantment, level);
		} catch (IllegalArgumentException unsafe) {
			itemStack.addUnsafeEnchantment(enchantment, level);
		}
		return this;
	}

	public Material material() {
		return itemStack.getType();
	}

	public ItemEditor material(Material material) {
		itemStack.setType(material);
		return this;
	}

	public int amount() {
		return itemStack.getAmount();
	}

	public ItemEditor amount(int amount) {
		int max = material().getMaxStackSize();
		itemStack.setAmount(max < amount ? max : (amount < 0 ? 0 : amount));
		return this;
	}

	public boolean hasDurability() {
		return hasItemMeta() ? (itemMeta instanceof Damageable ? (itemMeta.isUnbreakable() ? false : true) : false) : false;
	}

	public int getDurability() {
		return hasDurability() ? material().getMaxDurability() - ((Damageable) itemMeta).getDamage() : -1;
	}

	public int getDamage() {
		return hasDurability() ? ((Damageable) itemMeta).getDamage() : 0;
	}

	public ItemEditor setDurability(int durability) {
		if (!hasDurability())
			return this;
		((Damageable) itemMeta).setDamage(material().getMaxDurability() - durability);
		return this;
	}

	public ItemEditor setDamage(int damage) {
		if (!hasDurability())
			return this;
		((Damageable) itemMeta).setDamage(damage);
		return this;
	}

	public boolean hasModel() {
		return hasItemMeta() ? itemMeta.hasCustomModelData() : false;
	}

	public int getModel() {
		return hasItemMeta() ? itemMeta.getCustomModelData() : 0;
	}

	public ItemEditor setModel(int model) {
		if (hasItemMeta())
			itemMeta.setCustomModelData(model);
		return this;
	}

	public int getMaxAmount() {
		return material().getMaxStackSize();
	}

	public int getMaxDurability() {
		return material().getMaxDurability();
	}

	public ItemStack asItemStack() {
		if (hasItemMeta())
			itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

}

package org.playuniverse.snowypine.bukkit.inventory;

import static java.lang.Math.*;
import static org.playuniverse.snowypine.bukkit.inventory.utils.TableMath.*;

import java.util.HashMap;

import static org.playuniverse.snowypine.bukkit.inventory.item.ItemEditor.*;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.playuniverse.snowypine.bukkit.inventory.item.ItemEditor;

public interface ItemStorage<E extends ItemStorage<E>> {

	int size();

	ItemStack get(int id);

	default ItemStack getStack(int id) {
		return get(max(id, 0));
	}

	default ItemStack getStack(int row, int column) {
		return get(getId(row, column));
	}

	default ItemStack getStack(int row, int column, int rowSize) {
		return get(getId(row, column, rowSize));
	}

	default ItemEditor getEditor(int id) {
		return of(getStack(id));
	}

	default ItemEditor getEditor(int row, int column) {
		return of(getStack(row, column));
	}

	default ItemEditor getEditor(int row, int column, int rowSize) {
		return of(getStack(row, column, rowSize));
	}

	E set(int id, ItemStack stack);

	E me();

	default E setStack(int id, ItemStack stack) {
		return set(max(id, 0), stack);
	}

	default E setStack(int row, int column, ItemStack stack) {
		return set(getId(row, column), stack);
	}

	default E setStack(int row, int column, int rowSize, ItemStack stack) {
		return set(getId(row, column, rowSize), stack);
	}

	default E setEditor(int id, ItemEditor editor) {
		if (editor == null) {
			return setStack(id, null);
		}
		return setStack(id, editor.asItemStack());
	}

	default E setEditor(int row, int column, ItemEditor editor) {
		if (editor == null) {
			return setStack(row, column, null);
		}
		return setStack(row, column, editor.asItemStack());
	}

	default E setEditor(int row, int column, int rowSize, ItemEditor editor) {
		if (editor == null) {
			return setStack(row, column, rowSize, null);
		}
		return setStack(row, column, rowSize, editor.asItemStack());
	}

	default HashMap<Integer, ItemStack> search(ItemStack stack) {
		HashMap<Integer, ItemStack> map = new HashMap<>();
		int size = size();
		for (int index = 0; index < size; index++) {
			ItemStack current = get(index);
			if (current == null || !current.isSimilar(stack)) {
				continue;
			}
			map.put(index, current);
		}
		return map;
	}

	default HashMap<Integer, ItemStack> possible(ItemStack stack) {
		HashMap<Integer, ItemStack> map = new HashMap<>();
		int size = size();
		for (int index = 0; index < size; index++) {
			ItemStack current = get(index);
			if (current != null && (!current.isSimilar(stack) && current.getType() != Material.AIR)) {
				continue;
			}
			map.put(index, current);
		}
		return map;
	}

	default int count(ItemStack stack) {
		int amount = 0;
		for (ItemStack current : search(stack).values()) {
			amount += current.getAmount();
		}
		return amount;
	}

}

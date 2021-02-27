package org.playuniverse.snowypine.bukkit.inventory;

import static java.lang.Math.*;

import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.inventory.ItemStack;

public class BasicItemStorage implements ItemStorage<BasicItemStorage> {

	private final ConcurrentHashMap<Integer, ItemStack> map = new ConcurrentHashMap<>();
	private int size;

	public BasicItemStorage(int size) {
		this.size = size;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public ItemStack get(int id) {
		return map.get(min(id, size));
	}

	@Override
	public BasicItemStorage set(int id, ItemStack stack) {
		map.put(min(id, size), stack);
		return this;
	}

	@Override
	public BasicItemStorage me() {
		return this;
	}

}

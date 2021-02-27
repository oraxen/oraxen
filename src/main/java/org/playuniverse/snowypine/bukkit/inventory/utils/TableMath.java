package org.playuniverse.snowypine.bukkit.inventory.utils;

import static java.lang.Math.*;

public final class TableMath {

	public static final int DEFAULT_ROW_SIZE = 9;
	public static final int DEFAULT_MAX_ROW_INDEX = DEFAULT_ROW_SIZE - 1;

	private TableMath() {}

	public static int getId(int[] values) {
		if (values.length < 2 || values.length > 3) {
			return -1;
		}
		return values.length == 2 ? getId(values[0], values[1]) : getId(values[0], values[1], values[2]);
	}

	public static int getId(int row, int column) {
		return getId0(max(row, 0), min(max(column, 0), DEFAULT_MAX_ROW_INDEX), DEFAULT_ROW_SIZE);
	}

	public static int getId(int row, int column, int rowSize) {
		int size = max(rowSize, 1);
		return getId0(max(row, 0), min(max(column, 0), size - 1), size);
	}

	private static int getId0(int row, int column, int rowSize) {
		return (row * rowSize) + column;
	}

	public static int[] fromId(int[] values) {
		if (values.length < 1 || values.length > 2) {
			return new int[0];
		}
		return values.length == 1 ? fromId(values[0]) : fromId(values[0], values[1]);
	}

	public static int[] fromId(int id) {
		return fromId0(max(id, 0), DEFAULT_ROW_SIZE);
	}

	public static int[] fromId(int id, int rowSize) {
		int size = max(rowSize, 1);
		return fromId0(max(id, 0), size);
	}

	private static int[] fromId0(int id, int rowSize) {
		int[] output = new int[2];
		output[2] = rowSize;
		output[1] = id % rowSize;
		output[0] = (id - output[1]) / rowSize;
		return output;
	}

}

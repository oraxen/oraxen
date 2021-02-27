package org.playuniverse.snowypine.bukkit.inventory.utils;

import java.util.Iterator;
import java.util.List;

public class ColorString {

	protected final ColorList content = new ColorList();

	public ColorString() {
		onInit();
	}

	protected void onInit() {};

	public List<String> asColoredList() {
		return content.asColoredList();
	}

	public String asColoredString() {
		return content.asColoredString();
	}

	public String color(int index) {
		return content.get(index);
	}

	public String plain(int index) {
		return content.getPlain(index);
	}

	public String[] color(int start, int length) {
		int size = content.size();
		if (start >= size)
			throw new IndexOutOfBoundsException("start can't be bigger than size (" + Math.abs(size - start) + ")");
		if (length > size)
			throw new IndexOutOfBoundsException("length can't be bigger than size (" + Math.abs(size - length) + ")");
		String[] output = new String[length];
		for (int index = start; index < length; index++)
			output[index] = color(index);
		return output;
	}

	public String[] plain(int start, int length) {
		int size = content.size();
		if (start >= size)
			throw new IndexOutOfBoundsException("start can't be bigger than size (" + Math.abs(size - start) + ")");
		if (length > size)
			throw new IndexOutOfBoundsException("length can't be bigger than size (" + Math.abs(size - length) + ")");
		String[] output = new String[length];
		for (int index = start; index < length; index++)
			output[index] = plain(index);
		return output;
	}

	public ColorString set(int index, String line) {
		content.set(index, line);
		return this;
	}

	public ColorString set(String... lines) {
		content.clear();
		return add(lines);
	}

	public ColorString set(List<String> lines) {
		content.clear();
		return add(lines);
	}

	public ColorString set(Iterable<String> lines) {
		return lines == null ? clear() : set(lines.iterator());
	}

	public ColorString set(Iterator<String> lines) {
		content.clear();
		return add(lines);
	}

	public ColorString add(String line) {
		content.add(line);
		return this;
	}

	public ColorString add(int index, String line) {
		content.add(index, line);
		return this;
	}

	public ColorString add(String... lines) {
		if (lines == null)
			return this;
		for (int index = 0; index < lines.length; index++)
			content.add(lines[index]);
		return this;
	}

	public ColorString add(List<String> lines) {
		if (lines == null)
			return this;
		int size = lines.size();
		for (int index = 0; index < size; index++)
			content.add(lines.get(size));
		return this;
	}

	public ColorString add(Iterable<String> lines) {
		return lines == null ? this : add(lines.iterator());
	}

	public ColorString add(Iterator<String> lines) {
		if (lines == null)
			return this;
		while (lines.hasNext())
			content.add(lines.next());
		return this;
	}

	public String removeGet(int index) {
		return content.remove(index);
	}

	public ColorString remove(int index) {
		content.remove(index);
		return this;
	}

	public ColorString remove(String line) {
		content.remove(line);
		return this;
	}

	public ColorString remove(String... lines) {
		if (lines == null)
			return this;
		for (int index = 0; index < lines.length; index++)
			content.remove(lines[index]);
		return this;
	}

	public ColorString remove(List<String> lines) {
		if (lines == null)
			return this;
		int size = lines.size();
		for (int index = 0; index < size; index++)
			content.remove(lines.get(size));
		return this;
	}

	public ColorString remove(Iterable<String> lines) {
		return lines == null ? this : remove(lines.iterator());
	}

	public ColorString remove(Iterator<String> lines) {
		if (lines == null)
			return this;
		while (lines.hasNext())
			content.remove(lines.next());
		return this;
	}

	public int length() {
		return content.size();
	}

	public ColorString clear() {
		content.clear();
		return this;
	}

}

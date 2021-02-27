package org.playuniverse.snowypine.bukkit.inventory.item;

import java.util.Iterator;
import java.util.List;

import org.playuniverse.snowypine.bukkit.inventory.utils.ColorList;

@SuppressWarnings("rawtypes")
abstract class ColorEditor<E extends ColorEditor> {

	final ItemEditor editor;
	ColorList content = new ColorList();

	public ColorEditor(ItemEditor editor) {
		this.editor = editor;
		onInit();
	}

	abstract void onInit();

	@SuppressWarnings("unchecked")
	private E me() {
		return (E) this;
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

	public E set(int index, String line) {
		content.set(index, line);
		return me();
	}

	public E set(String... lines) {
		content.clear();
		return add(lines);
	}

	public E set(List<String> lines) {
		content.clear();
		return add(lines);
	}

	public E set(Iterable<String> lines) {
		return lines == null ? clear() : set(lines.iterator());
	}

	public E set(Iterator<String> lines) {
		content.clear();
		return add(lines);
	}

	public E add(String line) {
		content.add(line);
		return me();
	}

	public E add(int index, String line) {
		content.add(index, line);
		return me();
	}

	public E add(String... lines) {
		if (lines == null)
			return me();
		for (int index = 0; index < lines.length; index++)
			content.add(lines[index]);
		return me();
	}

	public E add(List<String> lines) {
		if (lines == null)
			return me();
		int size = lines.size();
		for (int index = 0; index < size; index++)
			content.add(lines.get(size));
		return me();
	}

	public E add(Iterable<String> lines) {
		return lines == null ? me() : add(lines.iterator());
	}

	public E add(Iterator<String> lines) {
		if (lines == null)
			return me();
		while (lines.hasNext())
			content.add(lines.next());
		return me();
	}

	public String removeGet(int index) {
		return content.remove(index);
	}

	public E remove(int index) {
		content.remove(index);
		return me();
	}

	public E remove(String line) {
		content.remove(line);
		return me();
	}

	public E remove(String... lines) {
		if (lines == null)
			return me();
		for (int index = 0; index < lines.length; index++)
			content.remove(lines[index]);
		return me();
	}

	public E remove(List<String> lines) {
		if (lines == null)
			return me();
		int size = lines.size();
		for (int index = 0; index < size; index++)
			content.remove(lines.get(size));
		return me();
	}

	public E remove(Iterable<String> lines) {
		return lines == null ? me() : remove(lines.iterator());
	}

	public E remove(Iterator<String> lines) {
		if (lines == null)
			return me();
		while (lines.hasNext())
			content.remove(lines.next());
		return me();
	}

	public int length() {
		return content.size();
	}

	public E clear() {
		content.clear();
		return me();
	}

	public abstract ItemEditor apply();

	public ItemEditor getHandle() {
		return editor;
	}

}

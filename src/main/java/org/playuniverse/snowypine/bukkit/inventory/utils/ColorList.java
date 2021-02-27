package org.playuniverse.snowypine.bukkit.inventory.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ColorList extends ArrayList<String> {

	public static final char UNCOLOR_CHAR = '\u0026';
	public static final char COLOR_CHAR = '\u00A7';

	private static final long serialVersionUID = 1310623158005774539L;

	public static ColorList convert(List<String> input) {
		if (input instanceof ColorList)
			return (ColorList) input;
		ColorList list = new ColorList();
		int size = input.size();
		for (int index = 0; index < size; index++)
			list.add(input.get(index));
		return list;
	}

	public List<String> asColoredList() {
		ArrayList<String> list = new ArrayList<>();
		int size = size();
		for (int index = 0; index < size; index++)
			list.add(get(index));
		return list;
	}

	public String asColoredString() {
		StringBuilder builder = new StringBuilder();
		int size = size();
		for (int index = 0; index < size; index++)
			builder.append(get(index));
		return builder.toString();
	}

	@Override
	public String get(int index) {
		return color(super.get(index));
	}

	public String getPlain(int index) {
		return super.get(index);
	}

	@Override
	public boolean addAll(Collection<? extends String> collection) {
		if (collection == null)
			return false;
		return super.addAll(collection);
	}

	@Override
	public boolean addAll(int index, Collection<? extends String> collection) {
		if (collection == null)
			return false;
		return super.addAll(index, collection);
	}

	@Override
	public String set(int index, String element) {
		if (element == null)
			return element;
		return super.set(index, element);
	}

	@Override
	public boolean add(String element) {
		if (element == null)
			return false;
		return super.add(element);
	}

	@Override
	public void add(int index, String element) {
		if (element == null)
			return;
		super.add(index, element);
	}

	@Override
	public boolean remove(Object object) {
		if (!(object instanceof String))
			return false;
		return super.remove(object);
	}

	@Override
	public boolean contains(Object object) {
		if (!(object instanceof String))
			return false;
		return super.contains(object);
	}

	private String color(String msg) {
		return msg.replace(UNCOLOR_CHAR, COLOR_CHAR);
	}

}
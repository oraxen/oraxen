package org.playuniverse.snowypine.helper;

public final class ListHelper {
	
	private ListHelper() {}
	
	public static String[] toLowerCase(String... values) {
		for(int index = 0; index < values.length; index++) {
			values[index] = values[index].toLowerCase();
		}
		return values;
	}

}

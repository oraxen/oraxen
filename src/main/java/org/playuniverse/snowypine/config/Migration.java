package org.playuniverse.snowypine.config;

public abstract class Migration {

	protected static String ofEnum(String enumName) {
		return enumName.toLowerCase().replace('_', '.').replace("222", "-");
	}
	
	protected static String enumPath(String path, String enumName) {
		return path + ofEnum(enumName);
	}

}

package org.playuniverse.snowypine.utils.reflection;

public class Reflections {

	public static void setup(NmsReflectionProvider provider) {

		generalSetup(provider);

	}

	public static void generalSetup(NmsReflectionProvider provider) {

		provider.createReflect("java_arraylist", "java.util.ArrayList").searchField("elements", "elementData");
		provider.createReflect("java_classloader", "java.lang.ClassLoader").searchMethod("getPackages", "getPackages").searchField("classes", "classes");

	}

}

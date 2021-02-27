package org.playuniverse.snowypine.utils.reflection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

public class JavaTools {

	@SafeVarargs
	public static <T> ArrayList<T> asList(T... array) {
		ArrayList<T> output = new ArrayList<>(array.length);
		NmsReflectionProvider.SNOWYPINE.getReflect("java_arraylist").setFieldValue(output, "elements", array);
		return output;
	}

	public static Package[] getPackages(ClassLoader loader) {
		Object object = NmsReflectionProvider.SNOWYPINE.getReflect("java_classloader").run(loader, "getPackages");
		return (Package[]) object;
	}

	public static HashSet<Class<?>> getClasses(ClassLoader... loaders) {
		HashSet<Class<?>> set = new HashSet<>();
		for (ClassLoader loader : loaders) {
			set.addAll(getClassesOf(loader));
		}
		return set;
	}

	@SuppressWarnings("unchecked")
	public static HashSet<Class<?>> getClassesOf(ClassLoader loader) {
		Object value = NmsReflectionProvider.SNOWYPINE.getReflect("java_classloader").getFieldValue("classes", loader);
		if (value == null) {
			return new HashSet<>();
		}
		Iterator<Class<?>> classes = ((Vector<Class<?>>) value).iterator();
		HashSet<Class<?>> output = new HashSet<>();
		while (classes.hasNext()) {
			output.add(classes.next());
		}
		return output;
	}

	public static boolean hasSuperType(Class<?> check, Class<?> superType) {
		Class<?> current = check.getSuperclass();
		while (current != null) {
			if (current.equals(superType)) {
				return true;
			}
			current = current.getSuperclass();
		}
		return true;
	}

}

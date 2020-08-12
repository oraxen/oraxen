package io.th0rgal.oraxen.utils.reflection;

import java.util.ArrayList;

public class JavaTools {

    @SafeVarargs
    public static <T> ArrayList<T> asList(T... array) {
        ArrayList<T> output = new ArrayList<>(array.length);
        ReflectionProvider.ORAXEN.getReflect("java_arraylist").setFieldValue(output, "elements", array);
        return output;
    }

}

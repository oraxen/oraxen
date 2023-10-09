package io.th0rgal.oraxen.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

public class PackUtils {

    public static String getFinalPackPath(File file) {
        String filePath = StringUtils.substringAfter(file.getAbsolutePath().replace("\\", "/"), "Oraxen/pack");
        return filePath.startsWith("assets") ? filePath : "assets/minecraft/" + filePath;
    }

    public static String getShortenedPackPath(File file) {
        String filePath = StringUtils.substringAfter(file.getAbsolutePath().replace("\\", "/"), "Oraxen/pack");
        return filePath;
    }
}

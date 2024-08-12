package io.th0rgal.oraxen.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FileUtil {

    public static boolean setHidden(Path path) {
        try {
            Files.setAttribute(path, "dos:hidden", true);
            return Files.isHidden(path);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<File> listFiles(File directory) {
        if (!directory.exists() || !directory.isDirectory()) return new ArrayList<>();
        return Arrays.stream(Optional.ofNullable(directory.listFiles()).orElse(new File[]{})).toList();
    }
}

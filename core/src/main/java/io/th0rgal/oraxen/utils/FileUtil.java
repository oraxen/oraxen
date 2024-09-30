package io.th0rgal.oraxen.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

    public static List<File> listFiles(File directory, Predicate<File> fileFilter) {
        if (!directory.exists() || !directory.isDirectory()) return new ArrayList<>();
        return Arrays.stream(Optional.ofNullable(directory.listFiles()).orElse(new File[]{})).filter(fileFilter).toList();
    }

    public static Stream<File> fileStream(File directory) {
        if (!directory.exists() || !directory.isDirectory()) return Stream.empty();
        return Arrays.stream(Optional.ofNullable(directory.listFiles()).orElse(new File[]{}));
    }

    public static Stream<File> fileStream(File directory, Predicate<File> fileFilter) {
        if (!directory.exists() || !directory.isDirectory()) return Stream.empty();
        return Arrays.stream(Optional.ofNullable(directory.listFiles()).orElse(new File[]{})).filter(fileFilter);
    }
}

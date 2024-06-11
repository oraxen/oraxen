package io.th0rgal.oraxen.utils;

import java.nio.file.Files;
import java.nio.file.Path;

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
}

package io.th0rgal.oraxen.utils.customarmor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class LegacyArmorTextures {
    public static final String RESOURCE_DIRECTORY = "customarmor/legacy/";
    private static final List<String> FILE_NAMES = List.of(
            "chainmail_layer_1.png", "chainmail_layer_2.png",
            "diamond_layer_1.png", "diamond_layer_2.png",
            "gold_layer_1.png", "gold_layer_2.png",
            "iron_layer_1.png", "iron_layer_2.png",
            "leather_layer_1.png", "leather_layer_2.png",
            "leather_layer_1_overlay.png", "leather_layer_2_overlay.png",
            "netherite_layer_1.png", "netherite_layer_2.png",
            "transparent_layer_1.png", "transparent_layer_2.png");

    private LegacyArmorTextures() {
    }

    public static boolean isLegacyPackResource(String resourcePath) {
        String prefix = "pack/textures/models/armor/";
        return resourcePath.startsWith(prefix) && FILE_NAMES.contains(resourcePath.substring(prefix.length()));
    }

    /** Removes only known defaults whose bytes still match the bundled resources. */
    public static int cleanExtractedDefaults(Path packFolder) throws IOException {
        Path armorFolder = packFolder;
        for (String directory : List.of("textures", "models", "armor")) {
            armorFolder = armorFolder.resolve(directory);
            if (!Files.isDirectory(armorFolder, LinkOption.NOFOLLOW_LINKS)) return 0;
        }

        int removed = 0;
        for (String fileName : FILE_NAMES) {
            Path file = armorFolder.resolve(fileName);
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;

            try (InputStream resource = LegacyArmorTextures.class.getResourceAsStream("/" + RESOURCE_DIRECTORY + fileName)) {
                if (resource == null) continue;
                byte[] bundled = resource.readAllBytes();
                if (Files.size(file) == bundled.length && Arrays.equals(Files.readAllBytes(file), bundled)) {
                    Files.delete(file);
                    removed++;
                }
            }
        }

        try (var entries = Files.newDirectoryStream(armorFolder)) {
            if (entries.iterator().hasNext()) return removed;
        }
        Files.delete(armorFolder);
        return removed;
    }
}

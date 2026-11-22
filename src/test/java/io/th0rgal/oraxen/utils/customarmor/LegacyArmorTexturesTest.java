package io.th0rgal.oraxen.utils.customarmor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.abort;

class LegacyArmorTexturesTest {
    @TempDir
    Path packFolder;

    @Test
    void excludesOnlyKnownLegacyDefaultsFromExtraction() {
        assertTrue(LegacyArmorTextures.isLegacyPackResource("pack/textures/models/armor/leather_layer_1.png"));
        assertFalse(LegacyArmorTextures.isLegacyPackResource("pack/textures/models/armor/custom_armor_layer_1.png"));
        assertFalse(LegacyArmorTextures.isLegacyPackResource("pack/textures/models/armor/custom/leather_layer_1.png"));
    }

    @Test
    void removesDefaultsButPreservesEditedAndCustomTextures() throws IOException {
        Path armorFolder = Files.createDirectories(packFolder.resolve("textures/models/armor"));
        byte[] original = bundled("chainmail_layer_1.png");
        Files.write(armorFolder.resolve("chainmail_layer_1.png"), original);
        byte[] edited = bundled("chainmail_layer_2.png");
        edited[edited.length - 1] ^= 1;
        Path editedFile = Files.write(armorFolder.resolve("chainmail_layer_2.png"), edited);
        Path customFile = Files.write(armorFolder.resolve("custom_armor_layer_1.png"), original);
        Path unknownFile = Files.write(armorFolder.resolve("unknown.png"), original);

        assertEquals(1, LegacyArmorTextures.cleanExtractedDefaults(packFolder));
        assertFalse(Files.exists(armorFolder.resolve("chainmail_layer_1.png")));
        assertArrayEquals(edited, Files.readAllBytes(editedFile));
        assertArrayEquals(original, Files.readAllBytes(customFile));
        assertTrue(Files.exists(unknownFile));
        assertEquals(0, LegacyArmorTextures.cleanExtractedDefaults(packFolder));
    }

    @Test
    void removesEmptyArmorDirectoryAndAllowsRepeatedCleanup() throws IOException {
        Path armorFolder = Files.createDirectories(packFolder.resolve("textures/models/armor"));
        Files.write(armorFolder.resolve("leather_layer_1_overlay.png"), bundled("leather_layer_1_overlay.png"));

        assertEquals(1, LegacyArmorTextures.cleanExtractedDefaults(packFolder));
        assertFalse(Files.exists(armorFolder));
        assertEquals(0, LegacyArmorTextures.cleanExtractedDefaults(packFolder));
    }

    @Test
    void doesNotFollowSymbolicLinks() throws IOException {
        Path external = Files.createDirectories(packFolder.resolve("external"));
        Path original = Files.write(external.resolve("chainmail_layer_1.png"), bundled("chainmail_layer_1.png"));
        Path armorFolder = Files.createDirectories(packFolder.resolve("textures/models/armor"));
        try {
            Files.createSymbolicLink(armorFolder.resolve("chainmail_layer_1.png"), original);
        } catch (IOException | UnsupportedOperationException e) {
            abort("This environment cannot create symbolic links. " + e.getMessage());
        }
        assertEquals(0, LegacyArmorTextures.cleanExtractedDefaults(packFolder));
        assertTrue(Files.exists(original));

        Files.delete(armorFolder.resolve("chainmail_layer_1.png"));
        Files.delete(armorFolder);
        Files.createSymbolicLink(armorFolder, external);
        assertEquals(0, LegacyArmorTextures.cleanExtractedDefaults(packFolder));
        assertTrue(Files.exists(original));
    }

    private byte[] bundled(String fileName) throws IOException {
        try (var resource = getClass().getResourceAsStream("/" + LegacyArmorTextures.RESOURCE_DIRECTORY + fileName)) {
            assertNotNull(resource);
            return resource.readAllBytes();
        }
    }
}

package io.th0rgal.oraxen.pack.upload.hosts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LobfileTest {

    @Test
    void buildUploadFileName_appendsZipExtension() {
        assertEquals("Oraxen_1.21.4.zip", Lobfile.buildUploadFileName("Oraxen_1.21.4"));
    }

    @Test
    void buildUploadFileName_keepsExistingZipExtension() {
        assertEquals("Oraxen.zip", Lobfile.buildUploadFileName("Oraxen.zip"));
    }

    @Test
    void buildUploadFileName_sanitizesUnsafeCharacters() {
        assertEquals("Oraxen_Pack_1.21.4.zip", Lobfile.buildUploadFileName("Oraxen Pack/1.21.4"));
    }

    @Test
    void buildVersionedPackName_usesConfiguredBaseNameAndVersion() {
        Lobfile lobfile = new Lobfile("api-key", "Oraxen");

        assertEquals("Oraxen_1.21.4", lobfile.buildVersionedPackName("1.21.4"));
    }

    @Test
    void buildVersionedPackName_stripsZipExtensionFromConfiguredBaseName() {
        Lobfile lobfile = new Lobfile("api-key", "Oraxen.zip");

        assertEquals("Oraxen_1.21.4", lobfile.buildVersionedPackName("1.21.4"));
    }
}

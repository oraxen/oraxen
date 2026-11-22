package io.th0rgal.oraxen.pack.upload.hosts;

import java.io.File;
import java.util.UUID;
import java.util.function.Supplier;

public interface HostingProvider {

    boolean uploadPack(File resourcePack);

    default boolean uploadPack(File resourcePack, String packName) {
        return uploadPack(resourcePack);
    }

    default boolean requiresNewInstancePerUpload() {
        return false;
    }

    String getPackURL();

    /**
     * Returns the download URL of the latest upload, even after later uploads replace it.
     * Providers with expiring links return a fresh link on each call.
     */
    default Supplier<String> getPackURLSource() {
        String url = getPackURL();
        return () -> url;
    }

    byte[] getSHA1();

    String getOriginalSHA1();

    UUID getPackUUID();

}

package io.th0rgal.oraxen.pack.upload.hosts;

import java.io.File;
import java.util.UUID;

public interface HostingProvider {

    boolean uploadPack(File resourcePack);

    String getPackURL();

    byte[] getSHA1();

    String getOriginalSHA1();

    UUID getPackUUID();

}

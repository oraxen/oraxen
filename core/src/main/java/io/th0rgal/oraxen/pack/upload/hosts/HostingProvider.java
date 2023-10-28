package io.th0rgal.oraxen.pack.upload.hosts;

import java.io.File;

public interface HostingProvider {

    boolean uploadPack(File resourcePack);

    String getPackURL();

    String getMinecraftPackURL();

    byte[] getSHA1();

    String getOriginalSHA1();

}

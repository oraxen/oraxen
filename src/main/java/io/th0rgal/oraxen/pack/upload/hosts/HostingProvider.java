package io.th0rgal.oraxen.pack.upload.hosts;

import java.io.File;

public interface HostingProvider {

    boolean uploadPack(File resourcePack);

    String getPackURL();

    byte[] getSHA1();

}

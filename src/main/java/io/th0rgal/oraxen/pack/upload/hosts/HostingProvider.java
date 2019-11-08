package io.th0rgal.oraxen.pack.upload.hosts;

import java.io.File;

public interface HostingProvider {

    void uploadPack(File resourcePack);

    String getPackURL();

}

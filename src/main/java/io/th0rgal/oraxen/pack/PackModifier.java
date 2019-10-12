package io.th0rgal.oraxen.pack;

import java.io.File;

public abstract class PackModifier {

    protected File packDirectoy;

    public abstract void update(File packDirectoy);

}

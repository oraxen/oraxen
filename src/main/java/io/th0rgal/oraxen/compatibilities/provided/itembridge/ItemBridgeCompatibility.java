package io.th0rgal.oraxen.compatibilities.provided.itembridge;

import com.jojodmo.itembridge.*;
import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;

public class ItemBridgeCompatibility extends CompatibilityProvider<Main> {

    public ItemBridgeCompatibility() {
        OraxenItemBridge.setup(OraxenPlugin.get());
    }

}

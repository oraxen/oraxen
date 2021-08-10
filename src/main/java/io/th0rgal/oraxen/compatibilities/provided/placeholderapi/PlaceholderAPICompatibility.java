package io.th0rgal.oraxen.compatibilities.provided.placeholderapi;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import me.clip.placeholderapi.PlaceholderAPIPlugin;

public class PlaceholderAPICompatibility extends CompatibilityProvider<PlaceholderAPIPlugin> {

    public PlaceholderAPICompatibility() {
        new OraxenExpansion(OraxenPlugin.get()).register();
    }

}
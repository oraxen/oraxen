package io.th0rgal.oraxen.compatibilities.provided.placeholderapi;

import io.th0rgal.oraxen.OraxenPlugin;
import io.th0rgal.oraxen.compatibilities.CompatibilityProvider;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderAPICompatibility extends CompatibilityProvider<PlaceholderAPIPlugin> {
    public static PlaceholderExpansion expansion = null;
    public PlaceholderAPICompatibility() {
        expansion = new OraxenExpansion(OraxenPlugin.get());
        expansion.register();
    }

    @Override
    public void disable() {
        super.disable();
        if (expansion != null)
            expansion.unregister();
    }

}

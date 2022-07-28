package io.th0rgal.oraxen.utils.limitedplacing;

import org.bukkit.configuration.ConfigurationSection;

public class LimitedPlacing {
    private final AllowPlacingOn allowedPlacingOn;
    private final DenyPlacingOn denyPlacingOn;

    public LimitedPlacing(ConfigurationSection section) {
        ConfigurationSection allowSection = section.getConfigurationSection("allowed_on");
        ConfigurationSection denySection = section.getConfigurationSection("deny_on");
        if (allowSection != null) {
            allowedPlacingOn = new AllowPlacingOn(allowSection);
        } else allowedPlacingOn = null;

        if (denySection != null) {
            denyPlacingOn = new DenyPlacingOn(denySection);
        } else denyPlacingOn = null;

    }

    public AllowPlacingOn getAllowedPlacing() { return allowedPlacingOn; }
    public DenyPlacingOn getDeniedPlacing() { return denyPlacingOn; }
}

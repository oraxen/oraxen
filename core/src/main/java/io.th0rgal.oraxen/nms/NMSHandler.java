package io.th0rgal.oraxen.nms;

import org.bukkit.entity.Player;

public interface NMSHandler {

    void inject(Player player);
    void uninject(Player player);

    default boolean getSupported () {
        return false;
    }
}

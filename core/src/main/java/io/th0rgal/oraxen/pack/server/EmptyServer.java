package io.th0rgal.oraxen.pack.server;

import org.bukkit.entity.Player;

public class EmptyServer implements OraxenPackServer {
    @Override
    public void sendPack(Player player) {

    }

    @Override
    public String packUrl() {
        return "";
    }
}
